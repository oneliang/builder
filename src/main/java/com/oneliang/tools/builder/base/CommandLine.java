package com.oneliang.tools.builder.base;

import java.io.File;

import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;
import com.oneliang.util.task.TaskEngine;

public final class CommandLine {

	private static final Logger logger=LoggerManager.getLogger(CommandLine.class);

	/**
	 * initialize builder
	 * @param args
	 * @param configuration
	 * @param taskEngine
	 * @return Builder
	 * @throws Exception
	 */
	public static Builder initializeBuilder(String[] args,BuilderConfiguration builderConfiguration, TaskEngine taskEngine) throws Exception{
		String builderConfigurationFullFilename=new File(args[0]).getAbsolutePath();
		final int overrideArgsIndex=1;
		String[] overrideArgs=null;
		if(args.length>overrideArgsIndex){
			overrideArgs=new String[args.length-overrideArgsIndex];
			System.arraycopy(args, overrideArgsIndex, overrideArgs, 0, overrideArgs.length);
		}
		logger.info("Read builder configuration:"+builderConfigurationFullFilename);
		Builder builder=null;
		if(builderConfiguration==null){
			builderConfiguration=new BuilderConfiguration(builderConfigurationFullFilename, overrideArgs);
		}
		if(taskEngine==null){
			builder=new CommonBuilder(builderConfiguration);
		}else{
			builder=new CommonBuilder(builderConfiguration,taskEngine);
		}
		return builder;
	}
}
