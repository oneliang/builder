package com.oneliang.tools.builder.base;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.oneliang.tools.builder.base.BuilderConfiguration.HandlerBean;
import com.oneliang.tools.builder.base.BuilderConfiguration.TaskNodeInsertBean;

/**
 * project environment configuration
 */
public class Configuration {

	protected BuilderConfiguration builderConfiguration=null;
	protected Map<String,Object> temporaryDataMap=new ConcurrentHashMap<String, Object>();
	protected IDEInitializer ideInitializer=null;
	protected ProjectInitializer projectInitializer=null;

	protected final List<Project> projectList=new CopyOnWriteArrayList<Project>();
	protected final Map<String, Project> projectMap=new ConcurrentHashMap<String, Project>();

	/**
	 * initialize
	 */
	protected void initialize(){}

	/**
	 * initialize all project
	 */
	protected void initializeAllProject(){}

	/**
	 * add android project
	 * @param project
	 */
	public void addProject(Project project){
		if(project!=null){
			if(!this.projectMap.containsKey(project.getName())){
				this.projectList.add(project);
				this.projectMap.put(project.getName(), project);
			}
		}
	}

	/**
	 * @return the projectList
	 */
	public List<Project> getProjectList() {
		return this.projectList;
	}
	/**
	 * @return the projectMap
	 */
	public Map<String, Project> getProjectMap() {
		return this.projectMap;
	}

	/**
	 * increase handler bean list
	 * @return List<HandlerBean>
	 */
	protected List<HandlerBean> increaseHandlerBeanList(){
		return null;
	}

	/**
	 * increase task node insert bean list
	 * @return List<TaskNodeInsertBean>
	 */
	protected List<TaskNodeInsertBean> increaseTaskNodeInsertBeanList(){
		return null;
	}

	/**
	 * initializing handler bean
	 * @param handlerBean
	 */
	protected void initializingHandlerBean(HandlerBean handlerBean){}

	/**
	 * initializing task node insert bean
	 * @param taskNodeInsertBean
	 */
	protected void initializingTaskNodeInsertBean(TaskNodeInsertBean taskNodeInsertBean){}

	/**
	 * @param builderConfiguration the builderConfiguration to set
	 */
	void setBuilderConfiguration(BuilderConfiguration builderConfiguration) {
		this.builderConfiguration = builderConfiguration;
	}

	/**
	 * @return the builderConfiguration
	 */
	protected BuilderConfiguration getBuilderConfiguration() {
		return builderConfiguration;
	}

	/**
	 * put temporary data
	 * @param key
	 * @param temporaryData
	 */
	public void putTemporaryData(String key, Object temporaryData){
		this.temporaryDataMap.put(key, temporaryData);
	}

	/**
	 * get temporary data
	 * @param key
	 * @return Object
	 */
	public Object getTemporaryData(String key){
		return this.temporaryDataMap.get(key);
	}

	public static interface IDEInitializer{

		/**
		 * initialize all project from ide
		 */
		public void initializeAllProjectFromIDE();
	
		/**
		 * parsing project.properties
		 * @param Project
		 * @param key
		 * @param value
		 */
		public void parsingProjectProperties(Project Project,String key,String value);

		/**
		 * is source directory when parsing classpath
		 * @param Project
		 * @param sourceDirectory
		 * @return boolean
		 */
		public boolean isSourceDirectory(Project Project, String sourceDirectory);
	}

	public static interface ProjectInitializer{
		/**
		 * read project other properties,just read what you want
		 * @param project
		 */
		public void readProjectOtherProperties(Project project);
	}

	protected static class ConfigurationException extends RuntimeException {
		private static final long serialVersionUID = -6301530391868574533L;
		public ConfigurationException(String message) {
			super(message);
		}
		public ConfigurationException(Throwable cause) {
			super(cause);
		}
		public ConfigurationException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
