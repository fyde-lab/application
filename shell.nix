{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  packages = with pkgs; [
  claude-code

    jdk21
    libglvnd libx11 libxext libxrender libxtst libxi libxrandr
    libxcursor libxcomposite libxdamage libxfixes libxxf86vm
    fontconfig freetype
  ];

  shellHook = ''
    export JAVA_HOME="${pkgs.jdk21}"
    export PATH="$JAVA_HOME/bin:$PATH"
    export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath [
      pkgs.libglvnd pkgs.libx11 pkgs.libxext pkgs.libxrender pkgs.libxtst
      pkgs.libxi pkgs.libxrandr pkgs.libxcursor pkgs.libxcomposite
      pkgs.libxdamage pkgs.libxfixes pkgs.libxxf86vm pkgs.fontconfig pkgs.freetype
    ]}:$LD_LIBRARY_PATH"
  '';
}
