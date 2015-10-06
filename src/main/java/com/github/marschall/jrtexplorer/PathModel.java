package com.github.marschall.jrtexplorer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class PathModel implements TreeModel {

  private final List<TreeModelListener> listeners;

  private final Path root;

  public PathModel(Path root) {
    this.root = root;
    this.listeners = new CopyOnWriteArrayList<>();
  }

  @Override
  public Object getRoot() {
    return this.root;
  }

  @Override
  public Object getChild(Object parent, int index) {
    // TODO sort
    // TODO cache?
    int i = 0;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream((Path) parent)) {
      for (Path each : stream) {
        if (i == index) {
          return each;
        }
        i += 1;
      }
    } catch (IOException e) {
      throw new RuntimeException("could not get children", e);
    }
    throw new IllegalArgumentException("invalid index: " + index + " for: " + parent);
  }

  @Override
  public int getChildCount(Object parent) {
    int count = 0;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream((Path) parent)) {
      for (Path each : stream) {
        count += 1;
      }
    } catch (IOException e) {
      throw new RuntimeException("could not get children", e);
    }
    return count;
  }

  @Override
  public boolean isLeaf(Object node) {
    return !Files.isDirectory((Path) node);
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    // ignore
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    // TODO sort
    // TODO cache?
    int i = 0;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream((Path) parent)) {
      for (Path each : stream) {
        if (each.equals(child)) {
          return i;
        }
        i += 1;
      }
    } catch (IOException e) {
      throw new RuntimeException("could not get children", e);
    }
    throw new IllegalArgumentException("not a child: " + child + " of: " + parent);
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
