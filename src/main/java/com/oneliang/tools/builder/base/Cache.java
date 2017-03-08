package com.oneliang.tools.builder.base;

import java.io.Serializable;
import java.util.Map;

public class Cache implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2434016621632971356L;

    public final Map<String, CacheFile> cacheFileMap;
    public transient Map<String, ChangedFile> changedFileMap = null;
    public transient Map<String, String> addedFileMd5Map = null;
    public transient Map<String, String> modifiedFileMd5Map = null;
    public transient Map<String, String> deletedFileMd5Map = null;

    public Cache(Map<String, CacheFile> cacheFileMap) {
        this.cacheFileMap = cacheFileMap;
    }

    public static class CacheFile implements Serializable {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = -6945886950555948362L;
        public final String md5;
        public final String directory;
        public final String fullFilename;

        public CacheFile(String md5, String directory, String fullFilename) {
            this.md5 = md5;
            this.directory = directory;
            this.fullFilename = fullFilename;
        }
    }
}
