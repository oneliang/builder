package com.oneliang.tools.builder.base;

public class ChangedFile{

	public final String directory;
	public final String fullFilename;

	public ChangedFile(String directory, String fullFilename) {
		this.directory = directory;
		this.fullFilename = fullFilename;
	}
}