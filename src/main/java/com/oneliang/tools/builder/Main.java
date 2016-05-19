package com.oneliang.tools.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.tools.builder.base.Builder;
import com.oneliang.tools.builder.base.CommandLine;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.AbstractLogger;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.ComplexLogger;
import com.oneliang.util.logging.FileLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class Main {
	static{
		String projectRealPath=new File(StringUtil.BLANK).getAbsolutePath();
		List<AbstractLogger> loggerList=new ArrayList<AbstractLogger>();
		loggerList.add(new BaseLogger(Logger.Level.VERBOSE));
		loggerList.add(new FileLogger(Logger.Level.VERBOSE,new File(projectRealPath+"/log/default.log")));
		Logger logger=new ComplexLogger(Logger.Level.INFO, loggerList);
		LoggerManager.registerLogger("*", logger);
	}
	

	public static void main(String[] args){
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		final int overrideArgsIndex=1;
		if(args==null||args.length<overrideArgsIndex){
			System.exit(1);
		}else{
			try{
				Builder builder=CommandLine.initializeBuilder(args,null,null);
				if(builder!=null){
					builder.prepare();
					builder.build();
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
