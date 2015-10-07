package com.github.marschall.jrtexplorer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

  public static void main(String[] args) throws URISyntaxException, IOException {
    FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
    System.out.println("supported FileAttributeViews: " + fileSystem.supportedFileAttributeViews());
    System.out.println("file stores: " + fileSystem.getFileStores());
//    Explorer.main(args);
//    for (Path root : fileSystem.getRootDirectories()) {
//      iterate(root, 0);
//    }
    System.out.println("ConcurrentHashMap.class.getClassLoader(): " + ConcurrentHashMap.class.getClassLoader());
    Path packages = fileSystem.getPath("/packages");
    Path modules = fileSystem.getPath("/modules");
    printAttributes(fileSystem.getPath("/modules/java.base/java/lang/Object.class"));
    printAttributes(fileSystem.getPath("/modules/java.base"));
  }

  private static void printAttributes(Path path) throws IOException {
    System.out.println(path);
    Map<String, Object> attributes = Files.readAttributes(path, "*");
    for (Entry<String, Object> attribute : attributes.entrySet()) {
      System.out.println("  " + attribute.getKey() + ": " + attribute.getValue());
    }
  }

  private static void iterate(Path parent, int level) throws IOException {
    if (level > 2) {
      return;
    }
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
