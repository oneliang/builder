package com.oneliang.tools.builder.base;

import java.io.Serializable;
import java.util.Map;

public class Cache implements Serializable {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 2434016621632971356L;

	public final Map<String, String> fileMd5Map;
	public volatile Map<String,String> incrementalFileMd5Map=null;
	public volatile Map<String,String> modifiedFileMd5Map=null;

	public Cache(Map<String, String> fileMd5Map) {
		this.fileMd5Map=fileMd5Map;
	}
}
