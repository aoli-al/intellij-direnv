{
  outputs = { self, nixpkgs }:
    let
      pkgs = nixpkgs.legacyPackages.x86_64-linux;
    in
    {
      devShell.x86_64-linux = pkgs.mkShell {
        buildInputs = [
          pkgs.openjdk17
          pkgs.freetype
          pkgs.jetbrains.jdk
        ];
        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
          pkgs.freetype
        ];
        JETBRAINS_JDK_HOME="${pkgs.jetbrains.jdk.home}";
      };
    };
}
