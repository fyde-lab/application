{ pkgs ? import <nixpkgs> {
    # The Android SDK is unfree and gated behind Google's license.
    config = {
      allowUnfree = true;
      android_sdk.accept_license = true;
    };
    overlays = [
      (import (builtins.fetchTarball
        "https://github.com/oxalica/rust-overlay/archive/master.tar.gz"))
    ];
  }
}:

let
  androidTargets = [
    "aarch64-linux-android"
    "armv7-linux-androideabi"
    "i686-linux-android"
    "x86_64-linux-android"
  ];

  # sdkBridge cross-compiles ../sdk (fyde-sdk) for Android via the
  # ch.ubique.uniffi.plugin cargo integration; the JVM/Desktop/Wasm targets
  # only ever need the host target.
  # ../sdk's wreq/wreq-util dependency requires rustc >=1.98 (see ../sdk/shell.nix).
  rustToolchain = pkgs.rust-bin.stable."1.98.1".default.override {
    extensions = [ "rust-src" "clippy" "rustfmt" "rust-analyzer" ];
    targets = [ "x86_64-unknown-linux-gnu" ] ++ androidTargets;
  };
  # Must match gradle/libs.versions.toml (android-compileSdk, android-ndk) and
  # AGP's default build-tools (36.0.0 for AGP 9.1.1): the nix store SDK is
  # read-only, so AGP can't download missing components into it.
  buildToolsVersion = "36.0.0";
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "37.0" ];
    buildToolsVersions = [ buildToolsVersion ];
    includeNDK = true;
    ndkVersions = [ "27.2.12479018" ];
    includeEmulator = false;
    includeSystemImages = false;
  };
  androidSdk = "${androidComposition.androidsdk}/libexec/android-sdk";
in
pkgs.mkShell {
  packages = with pkgs; [
    rustToolchain
    sccache

    # required by ../sdk's build.rs, which fetches its .proto files from the
    # Buf Schema Registry (`buf export buf.build/fyde-lab/api`, needs network
    # access) and compiles them with protoc — see ../sdk/CLAUDE.md.
    buf
    protobuf

    # ../sdk's wreq dependency vendors a patched BoringSSL (btls-sys) that
    # needs these to build — see ../sdk/shell.nix.
    cmake
    perl
    go
    clang
    # Sets up LIBCLANG_PATH/BINDGEN_EXTRA_CLANG_ARGS correctly for bindgen.
    rustPlatform.bindgenHook

    # required (via pkg-config) to build ../sdk's wry/tao dependency, the real
    # embedded webview behind fyde.browser — see ../sdk/shell.nix. sdkBridge
    # compiles ../sdk from source for the uniffi bindings, so this shell needs
    # the same system libraries sdk's own shell.nix provides.
    pkg-config
    webkitgtk_4_1
    gtk3
    dbus

    # webkitgtk's HTTPS support goes through GIO's TLS backend, shipped by
    # nixpkgs as the separate glib-networking module — see GIO_EXTRA_MODULES
    # below and ../sdk/shell.nix's longer comment on this.
    glib-networking

    androidComposition.androidsdk

    jdk21
    libglvnd libx11 libxext libxrender libxtst libxi libxrandr
    libxcursor libxcomposite libxdamage libxfixes libxxf86vm
    fontconfig freetype
  ];

  shellHook = ''
    # direnv (`use nix`) caches the env from the nix-shell invocation that computed it,
    # including the ephemeral TMPDIR nix-shell creates and deletes on exit. Later shells
    # reusing that cached env point sccache at a TMPDIR that's already gone - pin it.
    export TMPDIR="/tmp"

    export RUSTC_WRAPPER=${pkgs.sccache}/bin/sccache
    export SCCACHE_DIR="$HOME/.cache/sccache"
    export CC="${pkgs.sccache}/bin/sccache ${pkgs.clang}/bin/clang"
    export CXX="${pkgs.sccache}/bin/sccache ${pkgs.clang}/bin/clang++"

    # sdkBridge builds ../sdk with cargo: keep its artifacts out of ../sdk/target,
    # which ../sdk's own shell.nix builds into with a different environment.
    # Sharing it made the two invalidate each other (and the plugin's Gradle
    # inputs pick up every generated *.rs under ../sdk/target), so each switch
    # between working on ../sdk and running the app rebuilt the whole sdk.
    export CARGO_TARGET_DIR="${toString ./.}/build/cargo"

    # bindgenHook's flags carry a -frandom-seed derived from this shell's own
    # store path, which changes whenever any input of this shell does (e.g. the
    # unpinned nixpkgs/rust-overlay above). bindgen's build script reruns when
    # BINDGEN_EXTRA_CLANG_ARGS changes, which recompiles btls-sys (wreq's
    # vendored BoringSSL) and everything above it - drop the seed, bindgen only
    # parses headers with these flags.
    export BINDGEN_EXTRA_CLANG_ARGS="$(echo "$BINDGEN_EXTRA_CLANG_ARGS" | sed -E 's/ ?-frandom-seed=[^ ]+//g')"

    # See the `glib-networking` comment above - GIO only looks for TLS/proxy
    # backend modules on this path, which nixpkgs never adds to a plain
    # shell's environment on its own.
    export GIO_EXTRA_MODULES="${pkgs.glib-networking}/lib/gio/modules''${GIO_EXTRA_MODULES:+:$GIO_EXTRA_MODULES}"

    export JAVA_HOME="${pkgs.jdk21}"
    export PATH="$JAVA_HOME/bin:$PATH"
    # Java AWT (used by Compose Desktop/Skiko) mishandles resize under
    # non-reparenting Wayland tiling WMs (sway, i3, dwm): the window
    # surface isn't reallocated correctly, leaving stale/garbled
    # framebuffer content on screen. This is the standard workaround.
    export _JAVA_AWT_WM_NONREPARENTING=1
    export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath [
      pkgs.libglvnd pkgs.libx11 pkgs.libxext pkgs.libxrender pkgs.libxtst
      pkgs.libxi pkgs.libxrandr pkgs.libxcursor pkgs.libxcomposite
      pkgs.libxdamage pkgs.libxfixes pkgs.libxxf86vm pkgs.fontconfig pkgs.freetype
    ]}:$LD_LIBRARY_PATH"

    export ANDROID_HOME="${androidSdk}"
    export ANDROID_SDK_ROOT="${androidSdk}"
    export ANDROID_NDK_HOME="${androidSdk}/ndk-bundle"
    # AGP otherwise runs the aapt2 it fetches from Maven, a dynamically linked
    # binary that can't run on NixOS - use the SDK's patched one instead.
    export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/build-tools/${buildToolsVersion}/aapt2''${GRADLE_OPTS:+ $GRADLE_OPTS}"
  '';
}
