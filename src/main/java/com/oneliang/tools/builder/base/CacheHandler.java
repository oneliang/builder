package com.oneliang.tools.builder.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oneliang.util.common.Generator;
import com.oneliang.util.common.ObjectUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public abstract class CacheHandler extends BaseHandler {

	private static final Logger logger=LoggerManager.getLogger(CacheHandler.class);

	private static final int CACHE_TYPE_DEFAULT=0;
	private static final int CACHE_TYPE_NO_CACHE=1;
	private static final int CACHE_TYPE_INCREMENTAL=2;
	private static final int CACHE_TYPE_MODIFY=3;

	public static interface CacheKeyProcessor{
		public String process(String directory, String fullFilename);
	}

	public static class ChangedFile{
		public final String directory;
		public final String fullFilename;
		public ChangedFile(String directory, String fullFilename) {
			this.directory=directory;
			this.fullFilename=fullFilename;
		}
	}

	public static interface ChangedFileProcessor{
		public boolean process(Iterable<ChangedFile> changedFileIterable);
	}

	public static class CacheOption{
		public final String cacheFullFilename;
		public final List<String> directoryList;
		public String fileSuffix=null;
		public CacheKeyProcessor cacheKeyProcessor=null;
		public ChangedFileProcessor changedFileProcessor=null;
		public CacheOption(String cacheFullFilename, List<String> directoryList) {
			this.cacheFullFilename=cacheFullFilename;
			this.directoryList=directoryList;
		}
	}

	/**
	 * deal with cache
	 * @param cacheOption
	 * @return Cache
	 */
	protected Cache dealWithCache(final CacheOption cacheOption){
		if(cacheOption==null){
			throw new NullPointerException("Cache option can not be null.");
		}
		Cache existCache=null;
		if(FileUtil.isExist(cacheOption.cacheFullFilename)){
			try {
				existCache=(Cache)ObjectUtil.readObject(new FileInputStream(cacheOption.cacheFullFilename));
			} catch (FileNotFoundException e) {
				logger.warning("File not found:"+cacheOption.cacheFullFilename);
			}
		}
		
		final Cache oldCache=existCache;
		final Map<String,String> fileMd5Map=new HashMap<String,String>();
		final Map<String,String> incrementalFileMd5Map=new HashMap<String,String>();
		final Map<String,String> modifiedFileMd5Map=new HashMap<String,String>();
		final Map<String,ChangedFile> changedFileMap=new HashMap<String,ChangedFile>();
		if(cacheOption.directoryList!=null){
			for(final String directory:cacheOption.directoryList){
				FileUtil.MatchOption matchOption=new FileUtil.MatchOption(directory);
				matchOption.fileSuffix=cacheOption.fileSuffix;
				matchOption.processor=new FileUtil.MatchOption.Processor() {
					public String onMatch(File file) {
						String fullFilename=file.getAbsolutePath();
						int cacheType=CACHE_TYPE_DEFAULT;
						if(oldCache!=null){
							if(oldCache.fileMd5Map!=null&&oldCache.fileMd5Map.containsKey(fullFilename)){
								String oldFileMd5=oldCache.fileMd5Map.get(fullFilename);
								String newFileMd5=Generator.MD5File(file.getAbsolutePath());
								if(oldFileMd5.equals(newFileMd5)){
									logger.debug("Same file:"+fullFilename);
								}else{
									logger.debug("Modify file:"+fullFilename);
									cacheType=CACHE_TYPE_MODIFY;
								}
							}else{
								logger.debug("Incremental file:"+fullFilename);
								cacheType=CACHE_TYPE_INCREMENTAL;
							}
						}else{
							cacheType=CACHE_TYPE_NO_CACHE;
						}
						if(cacheType!=0){
							String key=null;
							if(cacheOption.cacheKeyProcessor!=null){
								key=cacheOption.cacheKeyProcessor.process(directory, fullFilename);
							}else{
								key=fullFilename;
							}
							String newFileMd5=Generator.MD5File(file.getAbsolutePath());
							fileMd5Map.put(key, newFileMd5);
							changedFileMap.put(key, new ChangedFile(directory, fullFilename));
							switch (cacheType) {
							case CACHE_TYPE_INCREMENTAL:
								incrementalFileMd5Map.put(key, newFileMd5);
								break;
							case CACHE_TYPE_MODIFY:
								modifiedFileMd5Map.put(key, newFileMd5);
								break;
							}
						}
						return fullFilename;
					}
				};
				FileUtil.findMatchFile(matchOption);
			}
		}
		Cache cache=null;
		if(oldCache!=null){
			cache=new Cache(oldCache.fileMd5Map);
			cache.fileMd5Map.putAll(fileMd5Map);
		}else{
			cache=new Cache(fileMd5Map);
		}
		cache.incrementalFileMd5Map=incrementalFileMd5Map;
		cache.modifiedFileMd5Map=modifiedFileMd5Map;
		logger.debug("Changed size:"+fileMd5Map.size());
		logger.debug("All size:"+cache.fileMd5Map.size());
		logger.debug("Incremental size:"+cache.incrementalFileMd5Map.size());
		logger.debug("Modified size:"+cache.modifiedFileMd5Map.size());
		boolean needToSaveCache=false;
		if(cacheOption.changedFileProcessor!=null){
			needToSaveCache=cacheOption.changedFileProcessor.process(changedFileMap.values());
		}
		if(needToSaveCache){
			try {
				FileUtil.createFile(cacheOption.cacheFullFilename);
				ObjectUtil.writeObject(cache, new FileOutputStream(cacheOption.cacheFullFilename));
			} catch (FileNotFoundException e) {
				logger.warning("File not found:"+cacheOption.cacheFullFilename);
			}
		}
		return cache;
	}
}
