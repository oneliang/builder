package com.oneliang.tools.builder.base;

public class BuildException extends RuntimeException {
	private static final long serialVersionUID = 8033287512123868072L;

	public BuildException(String message) {
		super(message);
	}

	public BuildException(Throwable cause) {
		super(cause);
	}

	public BuildException(String message, Throwable cause) {
		super(message, cause);
	}
}
