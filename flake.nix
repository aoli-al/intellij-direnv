{
  outputs = { self, nixpkgs }:
    let
      pkgs = nixpkgs.legacyPackages.x86_64-linux;
    in
    {
      devShell.x86_64-linux = pkgs.mkShell {
        buildInputs = [
          pkgs.openjdk21
          pkgs.fontconfig
          pkgs.freetype
          pkgs.jetbrains.jdk
        ];
        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
          pkgs.freetype
          pkgs.fontconfig
        ];
        FONTCONFIG_FILE = pkgs.makeFontsConf {
          fontDirectories = [ pkgs.dejavu_fonts ];
        };
        JETBRAINS_JDK_HOME="${pkgs.jetbrains.jdk.home}";
      };
    };
}
