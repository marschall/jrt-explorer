package com.github.marschall.jrtexplorer;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.TextEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeSelectionModel;

public class Explorer {

  private static final String NODE_NAME = "/com/github/marschall/jrtexplorer";

  public static void main(String[] args) {
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JRTExplorer");
    ModulePathData pathData = ModulePathData.create();
    SwingUtilities.invokeLater(() -> createAndShowGUI(pathData));
  }

  private static void createAndShowGUI(ModulePathData pathData) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      throw new RuntimeException("could not switch to default look and feel", e);
    }

    final JFrame frame = new JFrame("JRT Explorer");
    PathModel model = new PathModel(pathData);
    model.startPreloader();
    frame.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        savePreferences(frame);
        model.stopPreloader();
      }

    });
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(createMainPanel(model));
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

  private static JPanel createMainPanel(PathModel model) {
    JPanel panel = new JPanel(new GridLayout(1, 1));

    panel.setPreferredSize(new Dimension(400, 600));

    JTree tree = new PathTree(model);
    tree.addTreeExpansionListener(new TreeExpansionListener() {

      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        Object lastPathComponent = event.getPath().getLastPathComponent();
        if (lastPathComponent instanceof TreeEntry) {
          model.populateCache((TreeEntry) lastPathComponent);
        }
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        // ignore
      }
    });

    tree.addTreeWillExpandListener(new TreeWillExpandListener() {


      @Override
      public void treeWillExpand(TreeExpansionEvent event) {
        Object lastPathComponent = event.getPath().getLastPathComponent();
        if (lastPathComponent instanceof TreeEntry) {
          model.populateCache((TreeEntry) lastPathComponent);
        }
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event) {
        // ignore
      }
    });
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    JScrollPane scrollPane = new JScrollPane(tree);

    panel.add(scrollPane);

    return panel;
  }

}
