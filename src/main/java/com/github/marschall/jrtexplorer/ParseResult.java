package com.github.marschall.jrtexplorer;


class ParseResult {

    private final String className;
    private final boolean isPublic;
    private final boolean isAnnotation;

    ParseResult(String className, boolean isPublic, boolean isAnnotation) {
        this.className = className;
        this.isPublic = isPublic;
        this.isAnnotation = isAnnotation;
    }

    String getClassName() {
        return this.className;
    }

    boolean isPublic() {
        return this.isPublic;
    }

    boolean isAnnotation() {
        return this.isAnnotation;
    }
}
