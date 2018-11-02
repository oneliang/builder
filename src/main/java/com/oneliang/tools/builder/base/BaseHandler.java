package com.oneliang.tools.builder.base;

import java.util.List;

import com.oneliang.Constants;
import com.oneliang.tools.builder.base.BuilderConfiguration.HandlerBean;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class BaseHandler extends AbstractHandler {

	private static final Logger logger = LoggerManager.getLogger(BaseHandler.class);

	public boolean handle() {
		if (this.executorList == null) {
			return true;
		}
		for (Executor executor : this.executorList) {
			if (executor == null) {
				continue;
			}
			switch (executor.getType()) {
			case HANDLER:
				String handlerName = executor.getExecute();
				HandlerBean handlerBean = configuration.getBuilderConfiguration().getHandlerBeanMap().get(handlerName);
				if (handlerBean != null) {
					String mode = handlerBean.getMode();
					Handler innerHandler = null;
					if (mode.equals(HandlerBean.MODE_SINGLETON)) {
						innerHandler = handlerBean.getInstance();
					} else if (mode.equals(HandlerBean.MODE_MORE)) {
						innerHandler = handlerBean.getInstance().clone();
					}
					if (innerHandler != null) {
						try {
							this.beforeInnerHandlerHandle(innerHandler);
							boolean result = innerHandler.handle();
							if (!result) {
								logger.error("Handler handle failure:" + innerHandler, null);
								return false;
							}
						} catch (Exception e) {
							logger.error(Constants.Base.EXCEPTION, e);
							logger.error("Handler handle failure:" + innerHandler, null);
							return false;
						}
					}
				} else {
					logger.error("Handler[" + handlerName + "] is not exist.", null);
					return false;
				}
				break;
			case COMMAND:
			default:
				String command = executor.getExecute();
				final String regex = "\\{[\\w\\.]*\\}";
				final String firstRegex = "\\{";
				List<String> outputKeyList = StringUtil.parseStringGroup(command, regex, firstRegex, StringUtil.BLANK, 1);
				if (outputKeyList != null) {
					for (String outputKey : outputKeyList) {
						Object outputValue = this.configuration.getTemporaryData(outputKey);
						String value = outputValue == null ? StringUtil.BLANK : outputValue.toString();
						command = command.replace(Constants.Symbol.BIG_BRACKET_LEFT + outputKey + Constants.Symbol.BIG_BRACKET_RIGHT, value);
					}
				}
				String[] commandArray = command.split(StringUtil.SPACE);
				try {
					int result = BuilderUtil.executeCommand(commandArray, true);
					if (result != 0) {
						return false;
					}
				} catch (Exception e) {
					logger.error(Constants.Base.EXCEPTION, e);
					logger.error("Command execute failure:" + command, null);
					return false;
				}
				break;
			}
		}
		return true;
	}

	/**
	 * before inner handler handle
	 * 
	 * @param innerHandler
	 */
	protected void beforeInnerHandlerHandle(Handler innerHandler) {
	}
}
