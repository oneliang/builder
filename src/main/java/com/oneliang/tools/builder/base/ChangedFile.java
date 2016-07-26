package com.oneliang.tools.builder.base;

public class ChangedFile{

	public final String key;
	public final String directory;
	public final String fullFilename;

	public ChangedFile(String key, String directory, String fullFilename) {
		this.key=key;
		this.directory = directory;
		this.fullFilename = fullFilename;
	}
}