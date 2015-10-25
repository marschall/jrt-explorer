package com.github.marschall.jrtexplorer;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;

class PathTree extends JTree {

  PathTree(TreeModel newModel) {
    super(newModel);
  }

  @Override
  public String convertValueToText(Object value, boolean selected,
          boolean expanded, boolean leaf, int row, boolean hasFocus) {
    TreeEntry entry = (TreeEntry) value;
    return entry.getClassName();
  }

}
