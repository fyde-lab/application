{ pkgs ? import <nixpkgs> {
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
in
pkgs.mkShell {
  packages = with pkgs; [
    rustToolchain
    sccache

    # required by ../sdk's build.rs to compile ../api-protos/*.proto
    protobuf

    # ../sdk's wreq dependency vendors a patched BoringSSL (btls-sys) that
    # needs these to build — see ../sdk/shell.nix.
    cmake
    perl
    go
    clang
    # Sets up LIBCLANG_PATH/BINDGEN_EXTRA_CLANG_ARGS correctly for bindgen.
    rustPlatform.bindgenHook

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
    export CC="${pkgs.sccache}/bin/sccache ${pkgs.clang}/bin/clang"
    export CXX="${pkgs.sccache}/bin/sccache ${pkgs.clang}/bin/clang++"

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

    export ANDROID_HOME="$HOME/Android/Sdk"
    export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
    export PATH="$HOME/Android/Sdk/platform-tools:$HOME/Android/Sdk/cmdline-tools/latest/bin:$HOME/Android/Sdk/emulator:$PATH"
  '';
}
