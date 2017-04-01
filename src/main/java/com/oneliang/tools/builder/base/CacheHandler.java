package com.oneliang.tools.builder.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.tools.builder.base.Cache.CacheFile;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.ObjectUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public abstract class CacheHandler extends BaseHandler {

    private static final Logger logger = LoggerManager.getLogger(CacheHandler.class);

    private static final int CACHE_TYPE_DEFAULT = 0;
    private static final int CACHE_TYPE_NO_CACHE = 1;
    private static final int CACHE_TYPE_ADDED = 2;
    private static final int CACHE_TYPE_MODIFIED = 3;
    private static final int CACHE_TYPE_EQUAL = 4;

    public static class CacheOption {
        public final String cacheFullFilename;
        public final List<String> directoryList;
        public String fileSuffix = null;
        public CacheKeyProcessor cacheKeyProcessor = null;
        public ChangedFileProcessor changedFileProcessor = null;

        public CacheOption(String cacheFullFilename, List<String> directoryList) {
            this.cacheFullFilename = cacheFullFilename;
            this.directoryList = directoryList;
        }

        public static interface CacheKeyProcessor {
            /**
             * process
             * 
             * @param directory
             * @param fullFilename
             * @return String, ignore when return null
             */
            public String process(String directory, String fullFilename);
        }

        public static interface ChangedFileProcessor {
            public boolean process(Iterable<ChangedFile> changedFileIterable);
        }
    }

    private Class<?> getCacheHandlerClass() {
        return getClass();
    }

    /**
     * deal with cache
     * 
     * @param cacheOption
     * @return Cache
     */
    protected Cache dealWithCache(final CacheOption cacheOption) {
        if (cacheOption == null) {
            throw new NullPointerException("Cache option can not be null.");
        }
        Cache existCache = null;
        if (FileUtil.isExist(cacheOption.cacheFullFilename)) {
            try {
                existCache = (Cache) ObjectUtil.readObject(new FileInputStream(cacheOption.cacheFullFilename));
            } catch (Exception e) {
                logger.warning("Read cache error:" + cacheOption.cacheFullFilename);
            }
        }
        final Cache oldCache = existCache;
        final Map<String, CacheFile> cacheFileMap = new HashMap<String, CacheFile>();
        final Map<String, String> addedFileMd5Map = new HashMap<String, String>();
        final Map<String, String> modifiedFileMd5Map = new HashMap<String, String>();
        final Map<String, String> deletedFileMd5Map = new HashMap<String, String>();
        final Map<String, ChangedFile> changedFileMap = new HashMap<String, ChangedFile>();
        if (cacheOption.directoryList != null) {
            for (String inputDirectory : cacheOption.directoryList) {
                final String directory = new File(inputDirectory).getAbsolutePath();
                FileUtil.MatchOption matchOption = new FileUtil.MatchOption(directory);
                matchOption.fileSuffix = cacheOption.fileSuffix;
                matchOption.processor = new FileUtil.MatchOption.Processor() {
                    public String onMatch(File file) {
                        String fullFilename = file.getAbsolutePath();
                        int cacheType = CACHE_TYPE_DEFAULT;
                        String key = null;
                        if (cacheOption.cacheKeyProcessor != null) {
                            key = cacheOption.cacheKeyProcessor.process(directory, fullFilename);
                        } else {
                            key = fullFilename;
                        }
                        if (key == null) {
                            return null;
                        }
                        String newFileMd5 = null;
                        if (oldCache != null) {
                            if (oldCache.cacheFileMap != null && oldCache.cacheFileMap.containsKey(key)) {
                                CacheFile oldCacheFile = oldCache.cacheFileMap.get(key);
                                newFileMd5 = Generator.MD5File(file.getAbsolutePath());
                                if (oldCacheFile.md5.equals(newFileMd5)) {
                                    logger.verbose("[Handler:" + getCacheHandlerClass() + "]Same file:" + key);
                                    cacheType = CACHE_TYPE_EQUAL;
                                } else {
                                    logger.debug("[Handler:" + getCacheHandlerClass() + "]Modified file:" + key);
                                    cacheType = CACHE_TYPE_MODIFIED;
                                }
                            } else {
                                logger.debug("[Handler:" + getCacheHandlerClass() + "]Added file:" + key);
                                cacheType = CACHE_TYPE_ADDED;
                            }
                        } else {
                            cacheType = CACHE_TYPE_NO_CACHE;
                        }
                        if (cacheType != CACHE_TYPE_DEFAULT) {
                            newFileMd5 = (newFileMd5 == null ? (newFileMd5 = Generator.MD5File(file.getAbsolutePath())) : newFileMd5);
                            cacheFileMap.put(key, new CacheFile(newFileMd5, directory, fullFilename));
                            switch (cacheType) {
                            case CACHE_TYPE_ADDED:
                            case CACHE_TYPE_NO_CACHE:
                                addedFileMd5Map.put(key, newFileMd5);
                                changedFileMap.put(key, new ChangedFile(ChangedFile.Status.ADDED, key, directory, fullFilename));
                                break;
                            case CACHE_TYPE_MODIFIED:
                                modifiedFileMd5Map.put(key, newFileMd5);
                                changedFileMap.put(key, new ChangedFile(ChangedFile.Status.MODIFIED, key, directory, fullFilename));
                                break;
                            }
                        }
                        return fullFilename;
                    }
                };
                FileUtil.findMatchFile(matchOption);
            }
        }
        Cache cache = null;
        if (oldCache != null) {
            Set<String> keySet = oldCache.cacheFileMap.keySet();
            for (String key : keySet) {
                if (cacheFileMap.containsKey(key)) {
                    continue;
                }
                logger.debug("[Handler:" + getCacheHandlerClass() + "]Deleted file:" + key);
                CacheFile oldCacheFile = oldCache.cacheFileMap.get(key);
                deletedFileMd5Map.put(key, oldCacheFile.md5);
                changedFileMap.put(key, new ChangedFile(ChangedFile.Status.DELETED, key, oldCacheFile.directory, oldCacheFile.fullFilename));
            }
            cache = new Cache(cacheFileMap);
        } else {
            cache = new Cache(cacheFileMap);
        }
        cache.addedFileMd5Map = addedFileMd5Map;
        cache.modifiedFileMd5Map = modifiedFileMd5Map;
        cache.deletedFileMd5Map = deletedFileMd5Map;
        cache.changedFileMap = changedFileMap;
        logger.debug("[Handler:" + getCacheHandlerClass() + "]Changed size:" + changedFileMap.size());
        logger.debug("[Handler:" + getCacheHandlerClass() + "]All size:" + cache.cacheFileMap.size());
        logger.debug("[Handler:" + getCacheHandlerClass() + "]Added size:" + cache.addedFileMd5Map.size());
        logger.debug("[Handler:" + getCacheHandlerClass() + "]Modified size:" + cache.modifiedFileMd5Map.size());
        logger.debug("[Handler:" + getCacheHandlerClass() + "]Deleted size:" + cache.deletedFileMd5Map.size());
        boolean needToSaveCache = false;
        if (cacheOption.changedFileProcessor != null) {
            needToSaveCache = cacheOption.changedFileProcessor.process(changedFileMap.values());
        }
        if (needToSaveCache) {
            try {
                FileUtil.createFile(cacheOption.cacheFullFilename);
                ObjectUtil.writeObject(cache, new FileOutputStream(cacheOption.cacheFullFilename));
            } catch (FileNotFoundException e) {
                logger.warning("File not found:" + cacheOption.cacheFullFilename);
            }
        }
        return cache;
    }
}
