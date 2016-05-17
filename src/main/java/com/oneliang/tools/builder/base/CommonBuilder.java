package com.oneliang.tools.builder.base;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuilderConfiguration.TaskNodeInsertBean;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;
import com.oneliang.util.task.TaskEngine;
import com.oneliang.util.task.TaskNode;

public class CommonBuilder implements Builder {

	protected static final Logger logger=LoggerManager.getLogger(CommonBuilder.class);

	protected final List<TaskNode> rootTaskNodeList=new CopyOnWriteArrayList<TaskNode>();
	protected final Map<String,TaskNode> taskNodeMap=new ConcurrentHashMap<String,TaskNode>();
	protected final List<String> excludeTaskNodeNameList=new ArrayList<String>();
	protected TaskEngine taskEngine=null;
	protected BuilderConfiguration builderConfiguration=null;
	protected long begin=System.currentTimeMillis();

	public CommonBuilder(BuilderConfiguration builderConfiguration) {
		this.builderConfiguration=builderConfiguration;
		this.taskEngine=new TaskEngine(1,builderConfiguration.getMaxThreads());
		this.taskEngine.setTaskNodeTimeFile(this.builderConfiguration.getTaskNodeTimeFile());
	}

	public CommonBuilder(BuilderConfiguration builderConfiguration,TaskEngine taskEngine) {
		this.builderConfiguration=builderConfiguration;
		this.taskEngine=taskEngine;
	}

	public Map<String, TaskNode> prepare() {
		this.insertTaskNode(this.builderConfiguration.getTaskNodeInsertBeanList());
		this.taskEngine.prepare(this.rootTaskNodeList,this.excludeTaskNodeNameList);
		return this.taskNodeMap;
	}

	public void build() {
		this.taskEngine.commit();
		this.taskEngine.execute();
		this.taskEngine.waiting();
		this.taskEngine.clean();
		long cost=System.currentTimeMillis()-this.begin;
		Calendar costCalendar=Calendar.getInstance();
		costCalendar.setTimeInMillis(cost);
		int minute=costCalendar.get(Calendar.MINUTE);
		int second=costCalendar.get(Calendar.SECOND);
		int millisecond=costCalendar.get(Calendar.MILLISECOND);
		if(this.taskEngine.isSuccessful()){
			logger.info("BUILD SUCCESSFUL,cost:"+minute+"m\t"+second+"s\t"+millisecond+"ms");
		}else{
			logger.info("BUILD FAILURE,cost:"+minute+"m\t"+second+"s\t"+millisecond+"ms");
		}
	}

	/**
	 * insert task node
	 * @param taskNodeInsertBeanList
	 */
	protected void insertTaskNode(List<TaskNodeInsertBean> taskNodeInsertBeanList){
		if(taskNodeInsertBeanList!=null&&!taskNodeInsertBeanList.isEmpty()){
			for(TaskNodeInsertBean taskNodeInsertBean:taskNodeInsertBeanList){
				TaskNode taskNode=new TaskNode();
				taskNode.setName(taskNodeInsertBean.getName());
				this.taskNodeMap.put(taskNode.getName(), taskNode);
			}
			for(final TaskNodeInsertBean taskNodeInsertBean:taskNodeInsertBeanList){
				final TaskNode taskNode=this.taskNodeMap.get(taskNodeInsertBean.getName());
				taskNode.setRunnable(new Runnable(){
					public void run() {
						Handler handler=taskNodeInsertBean.getHandlerInstance();
						if(handler!=null){
							try{
								boolean result=handler.handle();
								if(!result){
									exit("Handler handle failure,taskNode:"+taskNode.getName()+",handler:"+handler);
								}
							}catch (Exception e) {
								logger.error(Constant.Base.EXCEPTION, e);
								exit("Handler handle failure,taskNode:"+taskNode.getName()+",handler:"+handler);
							}
						}
					}
				});
				String[] parentNames=taskNodeInsertBean.getParentNames();
				String[] childNames=taskNodeInsertBean.getChildNames();
				if(parentNames!=null){
					for(String parentName:parentNames){
						if(this.taskNodeMap.containsKey(parentName)){
							TaskNode parentTaskNode=this.taskNodeMap.get(parentName);
							parentTaskNode.addChildTaskNode(taskNode);
						}else{
							logger.warning("[WARNING]Insert node("+taskNodeInsertBean.getName()+") set parent task node name("+parentName+") is not exist");
						}
					}
				}
				if(childNames!=null){
					for(String childName:childNames){
						if(this.taskNodeMap.containsKey(childName)){
							TaskNode childTaskNode=this.taskNodeMap.get(childName);
							taskNode.addChildTaskNode(childTaskNode);
						}else{
							logger.warning("[WARNING]Insert node("+taskNodeInsertBean.getName()+") set child task node name("+childName+") is not exist");
						}
					}
				}
				if((parentNames==null||parentNames.length==0)&&(childNames==null||childNames.length==0)){
					this.rootTaskNodeList.add(taskNode);
				}
				if(taskNodeInsertBean.isSkip()){
					this.excludeTaskNodeNameList.add(taskNodeInsertBean.getName());
				}
			}
		}
	}

	/**
	 * exit
	 */
	protected final void exit(String message){
		logger.error("----------COMPILE FAILURE----------:"+message, null);
		System.exit(1);
	}
}
