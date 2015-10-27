package com.github.marschall.jrtexplorer;

import java.nio.file.Path;

class TreeEntry {

    private final TreeEntry parent;
    private final Path path;
    private final boolean leaf;
    private String className;

    TreeEntry(TreeEntry parent, Path path, boolean leaf) {
        this.parent = parent;
        this.path = path;
        this.leaf = leaf;
    }

    TreeEntry(TreeEntry parent, Path path, boolean leaf, String className) {
        this.parent = parent;
        this.path = path;
        this.leaf = leaf;
        this.className = className;
    }

    TreeEntry getParent() {
        return this.parent;
    }

    Path getPath() {
        return this.path;
    }

    boolean isLeaf() {
        return this.leaf;
    }

    String getClassName() {
        if (this.className == null) {
            this.className = this.computeClassName();
        }
        return this.className;
    }

    private String computeClassName() {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "/";
        }
        if (leaf) {
            // file
            String fullFileName = fileName.toString();
            if (fullFileName.endsWith(".class")) {
                // .replace('/', '.')
                return fullFileName.substring(0, fullFileName.length() - 6);
            } else {
                return fullFileName;
            }
        } else {
            // directory
            return fileName.toString() + '/';
        }
    }
}
