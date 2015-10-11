package com.github.marschall.jrtexplorer;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.TreeSelectionModel;

public class Explorer {
  private static final String NODE_NAME = "/com/github/marschall/jrtexplorer";

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

    final JFrame frame = new JFrame("JRT Explorer");
    frame.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        savePreferences(frame);
      }

    });
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(createMainPanel(root));
    applyPreferences(frame);

    frame.setVisible(true);
  }

  private static void applyPreferences(JFrame frame) {
    Preferences preferences = Preferences.userRoot().node(NODE_NAME);
    boolean present = preferences.getBoolean("present", false);
    if (present) {
      int x = preferences.getInt("x", 0);
      int y = preferences.getInt("y", 0);
      int width = preferences.getInt("width", 400);
      int height = preferences.getInt("height", 600);
      frame.setBounds(x, y, width, height);
    } else {
      frame.pack();
      Dimension screenSize = frame.getToolkit().getScreenSize();
      // center of default screen
      frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
    }
  }


  private static void savePreferences(JFrame frame) {
    Preferences preferences = Preferences.userRoot().node(NODE_NAME);
    Rectangle bounds = frame.getBounds();
    preferences.putInt("x", bounds.x);
    preferences.putInt("y", bounds.y);
    preferences.putInt("width", bounds.width);
    preferences.putInt("height", bounds.height);
    preferences.putBoolean("present", true);
  }

  private static JPanel createMainPanel(Path root) {
    JPanel panel = new JPanel(new GridLayout(1, 1));

    panel.setPreferredSize(new Dimension(400, 600));

    JTree tree = new PathTree(new PathModel(root));
//    tree.setRootVisible(false);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(tree);

    panel.add(scrollPane);

    return panel;
  }

}
