package com.github.marschall.jrtexplorer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

  PathModel(ModulePathData pathData) {
    this.pathData = pathData;
    this.listeners = new CopyOnWriteArrayList<>();
    this.cache = new ConcurrentHashMap<>();
    this.root = new TreeEntry(null, pathData.root, false);
    this.classParser = new ClassParser();
  }

  @Override
  public Object getRoot() {
    return this.root;
  }

  private void populateCache(TreeEntry parent) {
    Path parentPath = parent.getPath();
    if (this.cache.containsKey(parentPath)) {
      return;
    }
    boolean isModule = parentPath.startsWith(this.pathData.modules);

    List<TreeEntry> children = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parentPath)) {
      for (Path each : directoryStream) {
        String fileName = each.getFileName().toString();
        if (fileName.indexOf('$') != -1) {
          continue;
        }
        boolean isDirectory = Files.isDirectory(each);
        if (isModule) {
          if (isDirectory) {
            if (!this.pathData.exportedPaths.contains(each)) {
              continue;
            }
          } else {
            if (!this.pathData.exportedPaths.contains(each.getParent())) {
              continue;
            }
            /*
            */
            if (fileName.endsWith(".class")) {
              try (InputStream stream = new BufferedInputStream(Files.newInputStream(each))) {
                ParseResult result = this.classParser.parse(stream, each);
                if (result.isPublic()) {
                  children.add(new TreeEntry(parent, each, true, result.getClassName()));
                }
                continue;
              }
            }
          }
        }
        children.add(new TreeEntry(parent, each, !isDirectory));
      }
    } catch (IOException e) {
      throw new RuntimeException("could not get children", e);
    }

    children.sort((a, b) -> {

      boolean aIsDirectory = !a.isLeaf();
      boolean bIsDirectory = !a.isLeaf();

      if (aIsDirectory && !bIsDirectory) {
        return -1;
      }
      if (bIsDirectory && !aIsDirectory) {

      }

      return a.getPath().compareTo(b.getPath());
    });

    List<TreeEntry> previous = this.cache.putIfAbsent(parent, children);
    if (previous == null) {

    }
  }

  private void insertChildren(TreeEntry parent, List<TreeEntry> childrenList) {
    Object source = parent;
    Object[] path = getPath(parent);
    int[] childIndices = getIndices(childrenList);
    Object[] children = childrenList.toArray(new Object[childrenList.size()]);
    TreeModelEvent event = new TreeModelEvent(source, path, childIndices, children);
    this.treeNodesInserted(event);
  }

  private static Object[] getPath(TreeEntry entry) {
    int count = countParents(entry);
    Object[] path = new Object[count];
    int i = count;
    TreeEntry current = entry.getParent();
    while (current != null) {
      path[--i] = current;
      current = current.getParent();
    }
    return  path;
  }

  private void treeNodesInserted(TreeModelEvent event) {
    for (TreeModelListener listener : this.listeners) {
      listener.treeNodesInserted(event);
    }
  }

  private static int countParents(TreeEntry node) {
    int count = 0;
    TreeEntry current = node.getParent();
    while (current != null) {
      current = current.getParent();
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
    return this.cache.get(parent).get(index);
  }

  @Override
  public int getChildCount(Object parent) {
    this.populateCache((TreeEntry) parent);
    return this.cache.get(parent).size();
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
    this.populateCache((TreeEntry) parent);
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
