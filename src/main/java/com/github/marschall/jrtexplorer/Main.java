package com.github.marschall.jrtexplorer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

  public static void main(String[] args) throws URISyntaxException, IOException {
    FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
    System.out.println("supported FileAttributeViews: " + fileSystem.supportedFileAttributeViews());
    System.out.println("file stores: " + fileSystem.getFileStores());
    for (Path root : fileSystem.getRootDirectories()) {
      iterate(root, 0);
    }
    Path packages = fileSystem.getPath("/packages");
    Path modules = fileSystem.getPath("/modules");
    Path object = fileSystem.getPath("/java.base/java/lang/Object.class");
  }

  private static void iterate(Path parent, int level) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
      for (Path path : stream) {
        for (int i = 0; i < level; ++i) {
          System.out.print("  ");
        }
        System.out.println(path);
        if (Files.isDirectory(path)) {
          iterate(path, level + 1);
        }
      }
    }
  }

}
