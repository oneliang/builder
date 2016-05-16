package com.oneliang.tools.builder.base;

import java.io.File;

import com.oneliang.frame.task.TaskEngine;
import com.oneliang.util.log.Logger;

public final class CommandLine {

	private static final Logger logger=Logger.getLogger(CommandLine.class);

	/**
	 * initialize builder
	 * @param args
	 * @param configuration
	 * @param taskEngine
	 * @return Builder
	 * @throws Exception
	 */
	public static Builder initializeBuilder(String[] args,BuilderConfiguration builderConfiguration, TaskEngine taskEngine) throws Exception{
		String builderConfigFullFilename=new File(args[0]).getAbsolutePath();
		final int overrideArgsIndex=1;
		String[] overrideArgs=null;
		if(args.length>overrideArgsIndex){
			overrideArgs=new String[args.length-overrideArgsIndex];
			System.arraycopy(args, overrideArgsIndex, overrideArgs, 0, overrideArgs.length);
		}
		logger.log("Read builder configuration:"+builderConfigFullFilename);
		Builder builder=null;
		if(builderConfiguration==null){
			builderConfiguration=new BuilderConfiguration(builderConfigFullFilename, overrideArgs);
		}
		builderConfiguration.initialize();
		if(taskEngine==null){
			builder=new CommonBuilder(builderConfiguration);
		}else{
			builder=new CommonBuilder(builderConfiguration,taskEngine);
		}
		return builder;
	}
}
