package com.oneliang.tools.builder.base;

import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuilderConfiguration.HandlerBean;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class BaseHandler extends AbstractHandler {

	private static final Logger logger=LoggerManager.getLogger(BaseHandler.class);

	public boolean handle() {
		if(this.executorList!=null){
			for(Executor executor:this.executorList){
				if(executor!=null){
					switch(executor.getType()){
					case HANDLER:
						String handlerName=executor.getExecute();
						HandlerBean handlerBean=configuration.getBuilderConfiguration().getHandlerBeanMap().get(handlerName);
						if(handlerBean!=null){
							String mode=handlerBean.getMode();
							Handler handler=null;
							if(mode.equals(HandlerBean.MODE_SINGLETON)){
								handler=handlerBean.getInstance();
							}else if(mode.equals(HandlerBean.MODE_MORE)){
								handler=handlerBean.getInstance().clone();
							}
							if(handler!=null){
								try{
									this.beforeInnerHandlerHandle(handler);
									boolean result=handler.handle();
									if(!result){
										logger.error("Handler handle failure:"+handler, null);
										return false;
									}
								}catch (Exception e) {
									logger.error(Constant.Base.EXCEPTION, e);
									logger.error("Handler handle failure:"+handler, null);
									return false;
								}
							}
						}else{
							logger.error("Handler["+handlerName+"] is not exist.", null);
							return false;
						}
						break;
					case COMMAND:
					default:
						String command=executor.getExecute();
						final String regex="\\{[\\w\\.]*\\}";
						final String firstRegex="\\{";
						List<String> outputKeyList=StringUtil.parseStringGroup(command, regex, firstRegex, StringUtil.BLANK, 1);
						if(outputKeyList!=null){
							for(String outputKey:outputKeyList){
								Object outputValue=this.configuration.getTemporaryData(outputKey);
								String value=outputValue==null?StringUtil.BLANK:outputValue.toString();
								command=command.replace(Constant.Symbol.BIG_BRACKET_LEFT+outputKey+Constant.Symbol.BIG_BRACKET_RIGHT, value);
							}
						}
						String[] commandArray=command.split(StringUtil.SPACE);
						try{
							int result=BuilderUtil.executeCommand(commandArray, true);
							if(result!=0){
								return false;
							}
						}catch (Exception e) {
							logger.error(Constant.Base.EXCEPTION, e);
							logger.error("Command execute failure:"+command, null);
							return false;
						}
						break;
					}
				}
				
			}
		}
		return true;
	}

	protected void beforeInnerHandlerHandle(Handler handler){}
}
