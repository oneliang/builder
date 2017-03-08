package com.oneliang.tools.builder.test;

import java.util.Arrays;
import java.util.List;

import com.oneliang.tools.builder.base.CacheHandler;
import com.oneliang.tools.builder.base.CacheHandler.CacheOption.ChangedFileProcessor;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class TestCacheHandler extends CacheHandler {

    private static final Logger logger = LoggerManager.getLogger(TestCacheHandler.class);

    public boolean handle() {
        String cacheFullFilename = "/D:/cache.test";
        List<String> directoryList = Arrays.asList("/D:/testCache");
        CacheOption cacheOption = new CacheOption(cacheFullFilename, directoryList);
        cacheOption.fileSuffix = ".txt";
        cacheOption.changedFileProcessor = new ChangedFileProcessor() {
            public boolean process(Iterable<ChangedFile> changedFileIterable) {
                for (ChangedFile changedFile : changedFileIterable) {
                    logger.debug("status:" + changedFile.status + ",key:" + changedFile.key + ",path:" + changedFile.fullFilename);
                }
                return false;
            }
        };
        this.dealWithCache(cacheOption);
        return true;
    }
}
