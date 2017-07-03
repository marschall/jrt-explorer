package com.github.marschall.jrtexplorer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

class PathModel implements TreeModel {

  private final List<TreeModelListener> listeners;
  private final ConcurrentMap<TreeEntry, List<TreeEntry>> cache;
  private final TreeEntry root;
  private final ModulePathData pathData;
  private final ClassParser classParser;
  private final BlockingQueue<TreeEntry> toLoad;
  private Thread preloader;

  PathModel(ModulePathData pathData) {
    this.pathData = pathData;
    this.listeners = new CopyOnWriteArrayList<>();
    this.cache = new ConcurrentHashMap<>();
    this.root = new TreeEntry(null, pathData.root, false);
    this.classParser = new ClassParser();
    this.toLoad = new LinkedBlockingDeque<>();
  }

  @Override
  public Object getRoot() {
    return this.root;
  }

  void startPreloader() {
    this.preloader = new Thread(this::preloadLoop, "node-preloader");
    this.preloader.setDaemon(true);
    this.preloader.start();
  }

  void stopPreloader() {
    this.preloader.interrupt();
    this.preloader = null;
  }

  void populateCache(TreeEntry parent) {
    Path parentPath = parent.getPath();
    if (this.cache.containsKey(parentPath)) {
      return;
    }
    this.toLoad.offer(parent);
  }

  private void preloadLoop() {
    while (true) {
      TreeEntry entry;
      try {
        entry = this.toLoad.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      this.preload(entry);
    }
  }

  private void preload(TreeEntry parent) {
    if (this.cache.containsKey(parent)) {
      return;
    }
    Path parentPath = parent.getPath();
    boolean isModule = parentPath.startsWith(this.pathData.modules);

    List<TreeEntry> children = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parentPath)) {
      for (Path each : directoryStream) {
        String fileName = each.getFileName().toString();
        if (fileName.indexOf('$') != -1) {
          continue;
        }
        boolean isDirectory = Files.isDirectory(each);
        if (isDirectory) {
          if (isModule && !this.pathData.exportedPaths.contains(each)) {
            continue;
          }
        } else {
          if (isModule && !this.pathData.exportedPaths.contains(each.getParent())) {
            continue;
          }
          if (fileName.endsWith(".class") && !fileName.equals("module-info.class")) {
            ParseResult result = this.parse(each);
            if (result.isPublic()) {
              children.add(new TreeEntry(parent, each, true, result.getClassName()));
            }
            continue;
          }
        }
        children.add(new TreeEntry(parent, each, !isDirectory));
      }
    } catch (IOException e) {
      throw new RuntimeException("could not get children", e);
    }

    children.sort((a, b) -> {

      boolean aIsDirectory = !a.isLeaf();
      boolean bIsDirectory = !b.isLeaf();

      if (aIsDirectory && !bIsDirectory) {
        return -1;
      }
      if (bIsDirectory && !aIsDirectory) {
        return 1;
      }

      return a.getPath().compareTo(b.getPath());
    });

    List<TreeEntry> previous = this.cache.putIfAbsent(parent, children);
    if (previous == null) {
      SwingUtilities.invokeLater(() -> insertChildren(parent, children));
    }
  }

  private ParseResult parse(Path path) {
    try {
      return this.classParser.parse(() -> {
        try {
          return Files.newInputStream(path);
        } catch (IOException e) {
          throw new RuntimeException("could not parse: " + path, e);
        }
      }, path);
    } catch (IOException e) {
      throw new RuntimeException("could not parse: " + path, e);
    }
  }

  private void insertChildren(TreeEntry parent, List<TreeEntry> childrenList) {
    Object source = parent;
    TreePath path = getPath(parent);
    int[] childIndices = getIndices(childrenList);
    Object[] children = childrenList.toArray(new Object[childrenList.size()]);
    TreeModelEvent event = new TreeModelEvent(source, path, childIndices, children);
    this.treeNodesInserted(event);
  }

  private static TreePath getPath(TreeEntry entry) {
    int count = countParents(entry);
    Object[] path = new Object[count];
    int i = count;
    // FIXME
    // TreeEntry current = entry.getParent();
    TreeEntry current = entry;
    while (current != null) {
      path[--i] = current;
      current = current.getParent();
    }
    return new TreePath(path);
  }

  private void treeNodesInserted(TreeModelEvent event) {
    for (TreeModelListener listener : this.listeners) {
      listener.treeNodesInserted(event);
    }
  }

  private static int countParents(TreeEntry node) {
    int count = 0;
    // FIXME
    // TreeEntry current = node.getParent();
    TreeEntry current = node;
    while (current != null) {
      current = current.getParent();
      count += 1;
    }
    return count;
  }

  private static int[] getIndices(List<?> list) {
    int size = list.size();
    int[] indices = new int[size];
    for (int i = 0; i < size; i++) {
      indices[i] = i;
    }
    return indices;
  }

  @Override
  public Object getChild(Object parent, int index) {
    this.populateCache((TreeEntry) parent);
    List<TreeEntry> children = this.cache.get(parent);
    return children.get(index);
  }

  @Override
  public int getChildCount(Object parent) {
    this.populateCache((TreeEntry) parent);
    List<TreeEntry> children = this.cache.get(parent);
    if (children != null) {
      return children.size();
    } else {
      return 0;
    }
  }

  @Override
  public boolean isLeaf(Object node) {
    return ((TreeEntry) node).isLeaf();
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    // ignore
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    // TODO reverse index? seems rarely used
    System.out.println("#getIndexOfChild");
    this.populateCache((TreeEntry) parent);
    return this.cache.get(parent).indexOf(child);
  }

  @Override
  public void addTreeModelListener(TreeModelListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removeTreeModelListener(TreeModelListener listener) {
    this.listeners.remove(listener);
  }

}
