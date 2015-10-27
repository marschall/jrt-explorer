package com.github.marschall.jrtexplorer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Main {

  public static void main(String[] args) throws URISyntaxException, IOException {
    FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
    System.out.println("supported FileAttributeViews: " + fileSystem.supportedFileAttributeViews());
    System.out.println("file stores: " + fileSystem.getFileStores());
//    for (Path root : fileSystem.getRootDirectories()) {
//      iterate(root, 0);
//    }
    System.out.println("ConcurrentHashMap.class.getClassLoader(): " + ConcurrentHashMap.class.getClassLoader());
    Path packages = fileSystem.getPath("/packages");
    Path modules = fileSystem.getPath("/modules");
//    printAttributes(fileSystem.getPath("/modules/java.base/java/lang/Object.class"));
//    printAttributes(fileSystem.getPath("/modules/java.base"));
//    Path moduleInfo = fileSystem.getPath("/modules/java.xml/module-info.class");

    // Path javaClass = fileSystem.getPath("/modules/java.base/java/lang/Object.class");
    Path javaClass = fileSystem.getPath("/modules/java.base/java/lang/AbstractMethodError.class");

    byte[] allBytes = Files.readAllBytes(javaClass);
    /*
    for (int i = 0; i < 11; ++i) {
      System.out.println(i + ": " + allBytes[i]);
    }
    for (int i = 220; i < 300; ++i) {
      System.out.println(i + ": " + allBytes[i]);
    }
    */

    try (InputStream stream = new BufferedInputStream(Files.newInputStream(javaClass))) {
      ClassParser parser = new ClassParser();
      ParseResult result = parser.parse(stream, javaClass);
      System.out.println("class name: " + result.getClassName() + " public " + result.isPublic());
    }

    /*
    Files.list(modules).forEach(path -> {
      if (Files.isDirectory(path) && path.getFileName().toString().startsWith("java.")) {
        checkModule(path);
      }
    });
    */
//    Files.copy(moduleInfo, Paths.get("module-info.class"));
  }

  private static Set<Path> getExportedPaths(Path modules, ModuleDescriptor descriptor) {
    Path moduleBase = modules.resolve(descriptor.name());
    Set<Exports> exports = descriptor.exports();
    Set<Path> exportedPaths = new HashSet<>();
    //System.out.println("exports: " + exports);
    for (Exports export : exports) {
      Optional<Set<String>> targets = export.targets();

      if (!targets.isPresent()) {
        String source = export.source();
        //String[] elements = source.split("\\.");
        Path exportPath = moduleBase.resolve(source.replace('.', '/'));
        while (!exportPath.equals(modules)) {
          exportedPaths.add(exportPath);
          exportPath = exportPath.getParent();
        }

      }
    }
    return exportedPaths;
  }

  private static void checkModule(Path module) {
    try {
      List<Path> nonApiChildren = getNonApiChildren(module);
      if (!nonApiChildren.isEmpty()) {
        System.out.println(module.getFileName() + ":");
        for (Path child : nonApiChildren) {
          List<Path> classes = findPackages(child);
          classes.sort((p1, p2) -> p1.compareTo(p2));
          for (Path clazz : classes) {
            printClassName(clazz);
//          System.out.println("    " + clazz);
          }

        }
      }
    } catch (IOException e) {
      throw new RuntimeException("could not read", e);
    }
  }

  private static void printClassName(Path clazz) {
    System.out.print("    ");
    int nameCount = clazz.getNameCount();
    for (int i = 2; i < nameCount; ++i) {
      if (i > 2) {
        System.out.print(".");
      }
      if (i == nameCount - 1) {
        String fileName = clazz.getName(i).toString();
        if (fileName.endsWith(".class")) {
          System.out.println(fileName.substring(0, fileName.length() - 6));
        } else {
          System.out.println(fileName);
        }
      } else {
        System.out.print(clazz.getName(i));
      }
    }
//    System.out.print(".");

  }

  private static List<Path> findPackages(Path module) throws IOException {
    List<Path> packages = new ArrayList<>();
    Files.walkFileTree(module, Collections.emptySet(), 3, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (dir.getNameCount() == 5) {
          packages.add(dir);
          return FileVisitResult.SKIP_SUBTREE;
        } else {
          return FileVisitResult.CONTINUE;
        }
      }

    });
    return packages;
  }

  private static List<Path> findClasses(Path module) throws IOException {
    List<Path> classes = new ArrayList<>();
    Files.walkFileTree(module, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".class") && !fileName.contains("$")) {
          classes.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return classes;
  }

  private static List<Path> getNonApiChildren(Path path) throws IOException {
    Set<Path> paths = new HashSet<>();
    paths.add(path.getFileSystem().getPath("java"));
    paths.add(path.getFileSystem().getPath("javax"));
    paths.add(path.getFileSystem().getPath("org"));
//    List<Path> result = new ArrayList<>();
    return Files.list(path)
            .filter(e ->  Files.isDirectory(e))
            .filter(e ->  !paths.contains(e.getFileName()))
            .collect(Collectors.toList());
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
