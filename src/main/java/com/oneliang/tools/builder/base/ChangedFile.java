package com.oneliang.tools.builder.base;

public class ChangedFile {

    public enum Status {
        ADDED, MODIFIED, DELETED
    }

    public final Status status;
    public final String key;
    public final String directory;
    public final String fullFilename;

    public ChangedFile(Status status, String key, String directory, String fullFilename) {
        this.status = status;
        this.key = key;
        this.directory = directory;
        this.fullFilename = fullFilename;
    }
}