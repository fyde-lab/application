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

  # sdkBridge cross-compiles its rust/ crate (fyde-sdk-bridge, and through it
  # ../sdk) for Android via the
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
  ndkVersion = "27.2.12479018";
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    platformVersions = [ "37.0" ];
    buildToolsVersions = [ buildToolsVersion ];
    includeNDK = true;
    ndkVersions = [ ndkVersion ];
    includeEmulator = false;
    includeSystemImages = false;
  };
  androidSdk = "${androidComposition.androidsdk}/libexec/android-sdk";
  ndkSysroot = "${androidSdk}/ndk/${ndkVersion}/toolchains/llvm/prebuilt/linux-x86_64/sysroot";
  # btls-sys's bindgen run breaks when cross-compiling for Android
  # ('gnu/stubs-32.h' file not found) because the host's glibc/gcc headers leak
  # in twice: through bindgenHook's BINDGEN_EXTRA_CLANG_ARGS, and through
  # bindgen's include-path detection, which runs the `clang` on PATH - the nix
  # wrapper, injecting host glibc whatever the target. bindgen prefers a
  # BINDGEN_EXTRA_CLANG_ARGS_<target> over the generic one, so give each
  # Android target the NDK sysroot instead (keeping nix clang's own builtin
  # headers, which the unwrapped libclang can't locate by itself); CLANG_PATH
  # (shellHook) points detection at the unwrapped clang, which only reports
  # what those args say.
  androidBindgenArgs = pkgs.lib.concatMapStrings (target: ''
    export BINDGEN_EXTRA_CLANG_ARGS_${builtins.replaceStrings [ "-" ] [ "_" ] target}="--sysroot=${ndkSysroot} -resource-dir=${pkgs.clang}/resource-root"
  '') androidTargets;
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
    # compiles ../sdk from source for its uniffi bindings, so this shell needs
    # the same system libraries sdk's own shell.nix provides.
    pkg-config
    webkitgtk_4_1
    gtk3
    dbus

    # webkitgtk's HTTPS support goes through GIO's TLS backend, shipped by
    # nixpkgs as the separate glib-networking module — see GIO_EXTRA_MODULES
    # below and ../sdk/shell.nix's longer comment on this.
    glib-networking

    # GSettings schemas GTK reads at runtime - see XDG_DATA_DIRS below.
    gsettings-desktop-schemas

    androidComposition.androidsdk

    jdk21
    # jpackage's deb bundler (`:desktopApp:packageDeb`) needs dpkg-deb and
    # fakeroot on PATH; without them it fails with "Invalid or unsupported
    # type: [deb]".
    dpkg
    fakeroot
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

    # See the `glib-networking` comment above - GIO only looks for TLS/proxy
    # backend modules on this path, which nixpkgs never adds to a plain
    # shell's environment on its own.
    export GIO_EXTRA_MODULES="${pkgs.glib-networking}/lib/gio/modules''${GIO_EXTRA_MODULES:+:$GIO_EXTRA_MODULES}"

    # On Linux, java.awt.FileDialog (the desktop app's save/open dialogs, e.g.
    # the recovery kit's "Save as PDF") is a native GtkFileChooserDialog, and
    # GLib aborts the whole JVM (SIGABRT, "No GSettings schemas are installed
    # on the system") when it can't find the org.gtk.Settings.FileChooser
    # schema. nixpkgs installs schemas under a per-package directory that GLib
    # only finds through XDG_DATA_DIRS, which a plain shell never extends.
    export XDG_DATA_DIRS="${pkgs.gtk3}/share/gsettings-schemas/${pkgs.gtk3.name}:${pkgs.gsettings-desktop-schemas}/share/gsettings-schemas/${pkgs.gsettings-desktop-schemas.name}''${XDG_DATA_DIRS:+:$XDG_DATA_DIRS}"

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
    ${androidBindgenArgs}
    export CLANG_PATH="${pkgs.clang.cc}/bin/clang"
    # AGP otherwise runs the aapt2 it fetches from Maven, a dynamically linked
    # binary that can't run on NixOS - use the SDK's patched one instead.
    export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/build-tools/${buildToolsVersion}/aapt2''${GRADLE_OPTS:+ $GRADLE_OPTS}"
  '';
}
