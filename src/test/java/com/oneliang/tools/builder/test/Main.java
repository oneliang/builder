package com.oneliang.tools.builder.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.AbstractLogger;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.ComplexLogger;
import com.oneliang.util.logging.FileLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class Main {

	public static void main(String[] args) throws Exception{
	    try {
            Class.forName(com.oneliang.tools.builder.Main.class.getName(), true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
        }
		args=new String[]{"bin/config/builder.xml","target.task=test"};
		String projectRealPath = new File(StringUtil.BLANK).getAbsolutePath();
        List<AbstractLogger> loggerList = new ArrayList<AbstractLogger>();
        loggerList.add(new BaseLogger(Logger.Level.VERBOSE));
        loggerList.add(new FileLogger(Logger.Level.VERBOSE, new File(projectRealPath + "/log/default.log")));
        Logger logger = new ComplexLogger(Logger.Level.DEBUG, loggerList);
        LoggerManager.registerLogger("*", logger);
		com.oneliang.tools.builder.Main.main(args);
	}
}
