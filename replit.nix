{ pkgs }: {
  deps = [
    pkgs.jdk17
    pkgs.gradle
    pkgs.unzip
    pkgs.wget
    pkgs.git
    pkgs.which
  ];
}
