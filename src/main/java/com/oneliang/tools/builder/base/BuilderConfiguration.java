package com.oneliang.tools.builder.base;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.Handler.Executor;
import com.oneliang.util.common.ClassUtil;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.ObjectUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

/**
 * only builder configuration
 */
public class BuilderConfiguration {

	private static final Logger logger=LoggerManager.getLogger(BuilderConfiguration.class);

	public static final String MAP_KEY_CONCURRENT_MAX_THREADS="concurrent.max.threads";
	public static final String MAP_KEY_TASK_NODE_TIME_FILE="task.node.time.file";
	public static final String MAP_KEY_TARGET_TASK="target.task";
	
	private int maxThreads=Runtime.getRuntime().availableProcessors();
	private String taskNodeTimeFile=null;
	private String targetTask=null;
	private Configuration configuration=null;
	private ConfigurationClassBean configurationClassBean=new ConfigurationClassBean();
	private List<TaskNodeInsertBean> taskNodeInsertBeanList=new ArrayList<TaskNodeInsertBean>();
	private Map<String,TaskNodeInsertBean> taskNodeInsertBeanMap=new HashMap<String,TaskNodeInsertBean>();
	private Map<String,List<TaskNodeInsertBean>> childTaskNodeInsertBeanMap=new HashMap<String,List<TaskNodeInsertBean>>();
	private List<HandlerBean> handlerBeanList=new ArrayList<HandlerBean>();
	private Map<String,HandlerBean> handlerBeanMap=new HashMap<String,HandlerBean>();
	private Map<String,ConfigurationBean> configurationBeanMap=new HashMap<String,ConfigurationBean>();
	private Map<String,String> configurationMap=new HashMap<String,String>();
	private Map<String,String> environmentMap=System.getenv();
	private String configurationFullFilename=null;
	private String[] overrideArgs=null;
	/**
	 * constructor
	 * @param configurationFullFilename
	 * @param overrideArgs
	 */
	public BuilderConfiguration(String configurationFullFilename,String[] overrideArgs){
		this.configurationFullFilename=configurationFullFilename;
		this.overrideArgs=overrideArgs;
	}

	/**
	 * initialize
	 */
	protected void initialize(){
		this.parseFromXml(this.configurationFullFilename, this.overrideArgs);
		this.parseOverrideCommand(this.overrideArgs);
		this.autoSetNewConfiguration();
		String mapKeyConcurrentMaxThreads=this.configurationMap.get(MAP_KEY_CONCURRENT_MAX_THREADS);
		if(!StringUtil.isBlank(mapKeyConcurrentMaxThreads)){
			this.maxThreads=Integer.parseInt(String.valueOf(this.maxThreads).trim());
		}
		this.taskNodeTimeFile=this.configurationMap.get(MAP_KEY_TASK_NODE_TIME_FILE);
		this.targetTask=this.configurationMap.get(MAP_KEY_TARGET_TASK);
		logger.info("Version:"+Version.MAJOR+Constant.Symbol.DOT+Version.MINOR+Constant.Symbol.DOT+Version.PATCH+"\tbuildDate:"+Version.BUILD_DATE);
		logger.info("Default("+MAP_KEY_CONCURRENT_MAX_THREADS+Constant.Symbol.COLON+this.maxThreads+")");
		logger.info("Default("+MAP_KEY_TASK_NODE_TIME_FILE+Constant.Symbol.COLON+this.taskNodeTimeFile+")");
		Iterator<Entry<String,String>> iterator=this.configurationMap.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String,String> entry=iterator.next();
			String key=entry.getKey();
			String value=entry.getValue();
			boolean needToLog=true;
			if(this.configurationBeanMap.containsKey(key)){
				ConfigurationBean configurationBean=this.configurationBeanMap.get(key);
				if(!configurationBean.isLog()){
					needToLog=false;
				}
			}
			if(needToLog){
				logger.info("Configuration("+key+Constant.Symbol.COLON+value+")");
			}
		}
		this.configuration.initialize();
		this.configuration.initializeAllProject();
		List<HandlerBean> increaseHandlerBeanList=this.configuration.increaseHandlerBeanList();
		if(increaseHandlerBeanList!=null){
			for(HandlerBean handlerBean:increaseHandlerBeanList){
				this.addHandlerBean(handlerBean);
			}
		}
		this.initializeHandler();
		List<TaskNodeInsertBean> increaseTaskNodeInsertBeanList=this.configuration.increaseTaskNodeInsertBeanList();
		if(increaseTaskNodeInsertBeanList!=null){
			for(TaskNodeInsertBean taskNodeInsertBean:increaseTaskNodeInsertBeanList){
				this.addTaskNodeInsertBean(taskNodeInsertBean);
			}
		}
		this.initializeTaskNodeInsertBean();
	}

	public String getTargetTask() {
		return targetTask;
	}

	/**
	 * parse from xml
	 * @param configurationFullFilename
	 * @param overrideArgs
	 * @return Properties
	 */
	private void parseFromXml(String configurationFullFilename, String[] overrideArgs){
		Document document=JavaXmlUtil.parse(configurationFullFilename);
		Element root=document.getDocumentElement();
		//configuration-class
		NodeList configurationClassNodeList=root.getElementsByTagName(ConfigurationClassBean.XML_TAG_CONFIGURATION_CLASS);
		if(configurationClassNodeList!=null&&configurationClassNodeList.getLength()>0){
			Node configurationClassNode=configurationClassNodeList.item(0);
			NamedNodeMap configurationClassNamedNodeMap=configurationClassNode.getAttributes();
			JavaXmlUtil.initializeFromAttributeMap(this.configurationClassBean, configurationClassNamedNodeMap);
			try {
				Object configurationObject=Class.forName(this.configurationClassBean.getType()).newInstance();
				this.configuration=(Configuration)configurationObject;
				this.configuration.setBuilderConfiguration(this);
			} catch (Exception e) {
				logger.error(Constant.Base.EXCEPTION,e);
				throw new BuilderConfigurationException(e);
			}
		}
		
		NodeList configurationNodeList=root.getElementsByTagName(ConfigurationBean.XML_TAG_CONFIGURATION);
		if(configurationNodeList!=null){
			int configurationNodeListLength=configurationNodeList.getLength();
			for(int i=0;i<configurationNodeListLength;i++){
				Node configurationNode=configurationNodeList.item(i);
				NamedNodeMap configurationNamedNodeMap=configurationNode.getAttributes();
				ConfigurationBean configurationBean=new ConfigurationBean();
				JavaXmlUtil.initializeFromAttributeMap(configurationBean, configurationNamedNodeMap);
				this.configurationBeanMap.put(configurationBean.getName(), configurationBean);
				this.configurationMap.put(configurationBean.getName(), configurationBean.getValue());
			}
		}
		NodeList handlerNodeList=root.getElementsByTagName(HandlerBean.XML_TAG_HANDLER);
		if(handlerNodeList!=null){
			int handlerNodeListLength=handlerNodeList.getLength();
			for(int i=0;i<handlerNodeListLength;i++){
				Node handlerNode=handlerNodeList.item(i);
				NamedNodeMap handlerNamedNodeMap=handlerNode.getAttributes();
				HandlerBean handlerBean=new HandlerBean();
				JavaXmlUtil.initializeFromAttributeMap(handlerBean, handlerNamedNodeMap);

				NodeList handlerChildNodeList=handlerNode.getChildNodes();
				List<Executor> executeBeanList=new ArrayList<Executor>();
				for(int j=0;j<handlerChildNodeList.getLength();j++){
					Node childNode=handlerChildNodeList.item(j);
					if (childNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					String nodeName=childNode.getNodeName();
					NamedNodeMap childNodeNamedNodeMap=childNode.getAttributes();
					if(childNodeNamedNodeMap!=null){//not #text
						String value=childNodeNamedNodeMap.getNamedItem(HandlerBean.XML_ATTRIBUTE_HANDLER_CHILD_NODE_VALUE).getNodeValue();
						if(nodeName.equals(HandlerBean.XML_TAG_HANDLER_COMMAND)){
							Executor executor=new Executor(value, Executor.Type.COMMAND);
							executeBeanList.add(executor);
						}else if(nodeName.equals(HandlerBean.XML_TAG_HANDLER_REFERENCE_HANDLER_NAME)){
							Executor executor=new Executor(value, Executor.Type.HANDLER);
							executeBeanList.add(executor);
						}
					}
				}
				if(!executeBeanList.isEmpty()){
					handlerBean.setExecutors(executeBeanList.toArray(new Executor[]{}));
				}
				this.addHandlerBean(handlerBean);
			}
		}
		NodeList taskNodeInsertNodeList=root.getElementsByTagName(TaskNodeInsertBean.XML_TAG_TASK_NODE_INSERT);
		if(taskNodeInsertNodeList!=null){
			int taskNodeInsertNodeListLength=taskNodeInsertNodeList.getLength();
			for(int i=0;i<taskNodeInsertNodeListLength;i++){
				Node taskNodeInsertNode=taskNodeInsertNodeList.item(i);
				NamedNodeMap taskNodeInsertNamedNodeMap=taskNodeInsertNode.getAttributes();
				TaskNodeInsertBean taskNodeInsertBean=new TaskNodeInsertBean();
				JavaXmlUtil.initializeFromAttributeMap(taskNodeInsertBean, taskNodeInsertNamedNodeMap);
				NodeList taskNodeInsertNodeChildNodeList=taskNodeInsertNode.getChildNodes();
				List<String> parentNameList=new ArrayList<String>();
				for(int j=0;j<taskNodeInsertNodeChildNodeList.getLength();j++){
					Node childNode=taskNodeInsertNodeChildNodeList.item(j);
					if (childNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					String nodeName=childNode.getNodeName();
					NamedNodeMap childNodeNamedNodeMap=childNode.getAttributes();
					if(childNodeNamedNodeMap!=null){//not #text
						String value=childNodeNamedNodeMap.getNamedItem(TaskNodeInsertBean.XML_ATTRIBUTE_TASK_NODE_INSERT_CHILD_NODE_VALUE).getNodeValue();
						if(nodeName.equals(TaskNodeInsertBean.XML_TAG_TASK_NODE_INSERT_PARENT_NAME)){
							parentNameList.add(value);
						}else if(nodeName.equals(TaskNodeInsertBean.XML_TAG_TASK_NODE_INSERT_HANDLER_NAME)){
							taskNodeInsertBean.setHandlerName(value);
						}
					}
				}
				if(!parentNameList.isEmpty()){
					taskNodeInsertBean.setParentNames(parentNameList.toArray(new String[]{}));
				}
				this.addTaskNodeInsertBean(taskNodeInsertBean);
			}
		}
	}

	/**
	 * parse override command
	 * @param args
	 */
	protected void parseOverrideCommand(String[] overrideArgs){
		if(overrideArgs!=null){
			for(String arg:overrideArgs){
				String[] keyValue=arg.split(Constant.Symbol.EQUAL);
				if(keyValue!=null&&keyValue.length==2){
					String key=keyValue[0];
					String value=keyValue[1];
					this.configurationMap.put(key, value);
				}
			}
		}
	}

	/**
	 * auto set new configuration
	 */
	private void autoSetNewConfiguration(){
		Method[] methods=this.configuration.getClass().getMethods();
		for(Method method:methods){
            String methodName=method.getName();
            String fieldName=null;
            if(methodName.startsWith(Constant.Method.PREFIX_SET)){
                fieldName=ObjectUtil.methodNameToFieldName(Constant.Method.PREFIX_SET, methodName);
            }
            if(fieldName!=null){
            	String configurationValue=this.configurationMap.get(fieldName);
            	if(configurationValue!=null){
            		Class<?>[] classes=method.getParameterTypes();
            		if(classes.length==1){
                    	Class<?> objectClass=classes[0];
                    	Object value=ClassUtil.changeType(objectClass, new String[]{configurationValue});
                    	try {
							method.invoke(this.configuration, value);
                    	}catch(Exception e){
                    		logger.error(Constant.Base.EXCEPTION,e);
                    	}
            		}
            	}
            }
		}
	}

	/**
	 * initialize instance,must after configuration initialize
	 */
	private void initializeHandler(){
		//handler
		for(HandlerBean handlerBean:this.handlerBeanList){
			try{
				Handler instance=(Handler)Class.forName(handlerBean.getType()).newInstance();
				instance.setConfiguration(this.configuration);
				instance.setInputKeyValue(handlerBean.getInputKeyValue());
				instance.setOutputKey(handlerBean.getOutputKey());
				handlerBean.setInstance(instance);
				this.configuration.initializingHandlerBean(handlerBean);
			}catch (Exception e) {
				logger.error(Constant.Base.EXCEPTION,e);
				throw new BuilderConfigurationException(e);
			}
			Executor[] executors=handlerBean.getExecutors();
			if(executors!=null){
				for(Executor executor:executors){
					handlerBean.getInstance().addExecutor(executor);
				}
			}
		}
	}

	/**
	 * initialize task node insert bean
	 */
	private void initializeTaskNodeInsertBean(){
		//task node insert
		for(TaskNodeInsertBean taskNodeInsertBean:this.taskNodeInsertBeanList){
			Handler handler=taskNodeInsertBean.getHandlerInstance();
			if(handler==null){
				String handlerName=taskNodeInsertBean.getHandlerName();
				HandlerBean handlerBean=this.handlerBeanMap.get(handlerName);
				if(handlerBean!=null){
					String mode=handlerBean.getMode();
					if(mode.equals(HandlerBean.MODE_SINGLETON)){
						taskNodeInsertBean.setHandlerInstance(handlerBean.getInstance());
					}else if(mode.equals(HandlerBean.MODE_MORE)){
						taskNodeInsertBean.setHandlerInstance(handlerBean.getInstance().clone());
					}
					this.configuration.initializingTaskNodeInsertBean(taskNodeInsertBean);
				}
			}
		}
	}

	/**
	 * @return the maxThreads
	 */
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * @return the configurationMap
	 */
	public Map<String, String> getConfigurationMap() {
		return configurationMap;
	}

	/**
	 * @return the environmentMap
	 */
	public Map<String, String> getEnvironmentMap() {
		return environmentMap;
	}

	/**
	 * @return the taskNodeTimeFile
	 */
	public String getTaskNodeTimeFile() {
		return taskNodeTimeFile;
	}

	/**
	 * @return the taskNodeInsertBeanList
	 */
	public List<TaskNodeInsertBean> getTaskNodeInsertBeanList() {
		return taskNodeInsertBeanList;
	}

	/**
	 * @return the taskNodeInsertBeanMap
	 */
	public Map<String, TaskNodeInsertBean> getTaskNodeInsertBeanMap() {
		return taskNodeInsertBeanMap;
	}

	/**
	 * @return the childTaskNodeInsertBeanMap
	 */
	public Map<String, List<TaskNodeInsertBean>> getChildTaskNodeInsertBeanMap() {
		return childTaskNodeInsertBeanMap;
	}

	/**
	 * @return the handlerBeanMap
	 */
	public Map<String, HandlerBean> getHandlerBeanMap() {
		return handlerBeanMap;
	}

	/**
	 * add handler bean
	 * @param handlerBean
	 */
	public void addHandlerBean(HandlerBean handlerBean){
		if(!this.handlerBeanMap.containsKey(handlerBean.getName())){
			this.handlerBeanList.add(handlerBean);
			this.handlerBeanMap.put(handlerBean.getName(), handlerBean);
		}
	}

	/**
	 * add task node insert bean
	 * @param taskNodeInsertBean
	 */
	public void addTaskNodeInsertBean(TaskNodeInsertBean taskNodeInsertBean){
		if(!this.taskNodeInsertBeanMap.containsKey(taskNodeInsertBean.getName())){
			this.taskNodeInsertBeanList.add(taskNodeInsertBean);
			this.taskNodeInsertBeanMap.put(taskNodeInsertBean.getName(), taskNodeInsertBean);
			String[] parentNames=taskNodeInsertBean.getParentNames();
			if(parentNames!=null&&parentNames.length>0){
				for(String parentName:parentNames){
					List<TaskNodeInsertBean> taskNodeInsertBeanList=null;
					if(this.childTaskNodeInsertBeanMap.containsKey(parentName)){
						taskNodeInsertBeanList=this.childTaskNodeInsertBeanMap.get(parentName);
					}else{
						taskNodeInsertBeanList=new ArrayList<TaskNodeInsertBean>();
						this.childTaskNodeInsertBeanMap.put(parentName, taskNodeInsertBeanList);
					}
					taskNodeInsertBeanList.add(taskNodeInsertBean);
				}
			}
		}
	}

	public static final class ConfigurationClassBean{
		public static final String XML_TAG_CONFIGURATION_CLASS="configuration-class";
		private String type=null;
		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}
		
	}

	public static final class ConfigurationBean{
		public static final String XML_TAG_CONFIGURATION="configuration";
		public static final String XML_ATTRIBUTE_CONFIGURATION_NAME="name";
		public static final String XML_ATTRIBUTE_CONFIGURATION_VALUE="value";
		public static final String XML_ATTRIBUTE_CONFIGURATION_LOG="log";
		private String name=null;
		private String value=null;
		private boolean log=true;
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
			
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}
		/**
		 * @param value the value to set
		 */
		public void setValue(String value) {
			this.value = value;
		}
		/**
		 * @return the log
		 */
		public boolean isLog() {
			return log;
		}
		/**
		 * @param log the log to set
		 */
		public void setLog(boolean log) {
			this.log = log;
		}
	}

	public static final class HandlerBean{
		
		public static final String XML_TAG_HANDLER="handler";
		public static final String XML_TAG_HANDLER_REFERENCE_HANDLER_NAME="reference-handler-name";
		public static final String XML_TAG_HANDLER_COMMAND="command";
		public static final String XML_ATTRIBUTE_HANDLER_CHILD_NODE_VALUE="value";
		public static final String MODE_SINGLETON="singleton";
		public static final String MODE_MORE="more";
		private String name=null;
		private String type=null;
		private String inputKeyValue=null;
		private String outputKey=null;
		private Handler instance=null;
		private String mode=MODE_SINGLETON;
		private Executor[] executors=null;
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}
		/**
		 * @return the instance
		 */
		public Handler getInstance() {
			return instance;
		}
		/**
		 * @param instance the instance to set
		 */
		public void setInstance(Handler instance) {
			this.instance = instance;
		}
		/**
		 * @return the executors
		 */
		public Executor[] getExecutors() {
			return executors;
		}
		/**
		 * @param executors the executors to set
		 */
		public void setExecutors(Executor[] executors) {
			this.executors = executors;
		}
		/**
		 * @return the mode
		 */
		public String getMode() {
			return mode;
		}
		/**
		 * @param mode the mode to set
		 */
		public void setMode(String mode) {
			this.mode = mode;
		}
		/**
		 * @return the inputKeyValue
		 */
		public String getInputKeyValue() {
			return inputKeyValue;
		}
		/**
		 * @param inputKeyValue the inputKeyValue to set
		 */
		public void setInputKeyValue(String inputKeyValue) {
			this.inputKeyValue = inputKeyValue;
		}
		/**
		 * @return the outputKey
		 */
		public String getOutputKey() {
			return outputKey;
		}
		/**
		 * @param outputKey the outputKey to set
		 */
		public void setOutputKey(String outputKey) {
			this.outputKey = outputKey;
		}
	}

	public static final class TaskNodeInsertBean{
		public static final String XML_TAG_TASK_NODE_INSERT="task-node-insert";
		public static final String XML_TAG_TASK_NODE_INSERT_PARENT_NAME="parent-name";
		public static final String XML_TAG_TASK_NODE_INSERT_HANDLER_NAME="handler-name";
		public static final String XML_ATTRIBUTE_TASK_NODE_INSERT_CHILD_NODE_VALUE="value";
		private String name=null;
		private String[] parentNames=null;
		private String handlerName=null;
		private boolean skip=false;
		private Handler handlerInstance=null;
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the parentNames
		 */
		public String[] getParentNames() {
			return parentNames;
		}
		/**
		 * @param parentNames the parentNames to set
		 */
		public void setParentNames(String[] parentNames) {
			this.parentNames = parentNames;
		}
		/**
		 * @return the handlerName
		 */
		public String getHandlerName() {
			return handlerName;
		}
		/**
		 * @param handlerName the handlerName to set
		 */
		public void setHandlerName(String handlerName) {
			this.handlerName = handlerName;
		}
		/**
		 * @return the handlerInstance
		 */
		public Handler getHandlerInstance() {
			return handlerInstance;
		}
		/**
		 * @param handlerInstance the handlerInstance to set
		 */
		public void setHandlerInstance(Handler handlerInstance) {
			this.handlerInstance = handlerInstance;
		}
		/**
		 * @return the skip
		 */
		public boolean isSkip() {
			return skip;
		}
		/**
		 * @param skip the skip to set
		 */
		public void setSkip(boolean skip) {
			this.skip = skip;
		}
	}

	private static class BuilderConfigurationException extends RuntimeException {
		private static final long serialVersionUID = -7385014336061695378L;
		public BuilderConfigurationException(Throwable cause) {
			super(cause);
		}
	}
}
