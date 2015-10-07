package com.github.marschall.jrtexplorer;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.TreeSelectionModel;

public class Explorer {

  public static void main(String[] args) {
    FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
    Path root = fileSystem.getPath("/");
    SwingUtilities.invokeLater(() -> createAndShowGUI(root));
  }

  private static void createAndShowGUI(Path root) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      throw new RuntimeException("could not switch to default look and feel", e);
    }

    JFrame frame = new JFrame("JRT Explorer");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.add(createMainPanel(root));

    frame.pack();
    frame.setVisible(true);
  }

  private static JPanel createMainPanel(Path root) {
    JPanel panel = new JPanel(new GridLayout(1, 1));

    panel.setPreferredSize(new Dimension(700, 500));

    JTree tree = new PathTree(new PathModel(root));
//    tree.setRootVisible(false);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(tree);

    panel.add(scrollPane);

    return panel;
  }

}
