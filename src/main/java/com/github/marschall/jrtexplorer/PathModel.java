package com.github.marschall.jrtexplorer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class PathModel implements TreeModel {

  private final List<TreeModelListener> listeners;

  private final Map<Path, List<Path>> cache;

  private final Map<Path, Boolean> leaves;

  private final Path root;

  public PathModel(Path root) {
    this.root = root;
    this.listeners = new CopyOnWriteArrayList<>();
    this.cache = new HashMap<>();
    this.leaves = new HashMap<>();
  }

  @Override
  public Object getRoot() {
    return this.root;
  }

  private void populateCache(Path parent) {
    if (this.cache.containsKey(parent)) {
      return;
    }

    List<Path> children = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream((Path) parent)) {
      for (Path each : stream) {
        children.add(each);
        this.leaves.put(each, !Files.isDirectory(each));
      }
    } catch (IOException e) {
      throw new RuntimeException("could not get children", e);
    }

    children.sort((a, b) -> {

      boolean aIsDirectory = !this.leaves.get(a);
      boolean bIsDirectory = !this.leaves.get(b);

      if (aIsDirectory && !bIsDirectory) {
        return -1;
      }
      if (bIsDirectory && !aIsDirectory) {

      }

      return a.compareTo(b);
    });

    this.cache.put(parent, children);
  }

  @Override
  public Object getChild(Object parent, int index) {
    this.populateCache((Path) parent);
    return this.cache.get(parent).get(index);
  }

  @Override
  public int getChildCount(Object parent) {
    this.populateCache((Path) parent);
    return this.cache.get(parent).size();
  }

  @Override
  public boolean isLeaf(Object node) {
    return this.leaves.computeIfAbsent((Path) node,
            path -> !Files.isDirectory(path));
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    // ignore
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    this.populateCache((Path) parent);
    return this.cache.get(parent).indexOf(child);
  }

  @Override
  public void addTreeModelListener(TreeModelListener l) {
    this.listeners.add(l);
  }

  @Override
  public void removeTreeModelListener(TreeModelListener l) {
    this.listeners.remove(l);
  }

}
