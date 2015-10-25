package com.github.marschall.jrtexplorer;


class ParseResult {

    private final String className;
    private final boolean isPublic;

    ParseResult(String className, boolean isPublic) {
        this.className = className;
        this.isPublic = isPublic;
    }

    String getClassName() {
        return this.className;
    }

    boolean isPublic() {
        return this.isPublic;
    }
}
