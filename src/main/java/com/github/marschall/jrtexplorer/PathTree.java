package com.github.marschall.jrtexplorer;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;

public class PathTree extends JTree {

  public PathTree(TreeModel newModel) {
    super(newModel);
  }

  @Override
  public String convertValueToText(Object value, boolean selected,
          boolean expanded, boolean leaf, int row, boolean hasFocus) {
    Path path = (Path) value;
    Path fileName = path.getFileName();
    if (fileName == null) {
      return "/";
    }
    if (Files.isDirectory(path)) {
      return '/' + fileName.toString();
    } else {
      return fileName.toString();
    }
  }

}
