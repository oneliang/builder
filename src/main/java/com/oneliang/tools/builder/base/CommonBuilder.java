package com.oneliang.tools.builder.base;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuilderConfiguration.TaskNodeInsertBean;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;
import com.oneliang.util.task.TaskEngine;
import com.oneliang.util.task.TaskNode;

public class CommonBuilder implements Builder {

    protected static final Logger logger = LoggerManager.getLogger(CommonBuilder.class);

    protected final List<TaskNode> rootTaskNodeList = new CopyOnWriteArrayList<TaskNode>();
    protected final Map<String, TaskNode> taskNodeMap = new ConcurrentHashMap<String, TaskNode>();
    protected final List<String> excludeTaskNodeNameList = new ArrayList<String>();
    protected TaskEngine taskEngine = null;
    protected BuilderConfiguration builderConfiguration = null;
    protected long begin = System.currentTimeMillis();

    public CommonBuilder(BuilderConfiguration builderConfiguration) {
        this.builderConfiguration = builderConfiguration;
        this.taskEngine = new TaskEngine(1, builderConfiguration.getMaxThreads());
        this.taskEngine.setTaskNodeTimeFile(this.builderConfiguration.getTaskNodeTimeFile());
    }

    public CommonBuilder(BuilderConfiguration builderConfiguration, TaskEngine taskEngine) {
        this.builderConfiguration = builderConfiguration;
        this.taskEngine = taskEngine;
    }

    public Map<String, TaskNode> prepare() {
        this.insertTaskNode(this.builderConfiguration.getTaskNodeInsertBeanList(), this.builderConfiguration.getTargetTask());
        this.taskEngine.prepare(this.rootTaskNodeList, this.excludeTaskNodeNameList);
        return this.taskNodeMap;
    }

    public void build() {
        this.taskEngine.commit();
        this.taskEngine.execute();
        this.taskEngine.waiting();
        this.taskEngine.clean();
        long cost = System.currentTimeMillis() - this.begin;
        Calendar costCalendar = Calendar.getInstance();
        costCalendar.setTimeInMillis(cost);
        int minute = costCalendar.get(Calendar.MINUTE);
        int second = costCalendar.get(Calendar.SECOND);
        int millisecond = costCalendar.get(Calendar.MILLISECOND);
        if (this.taskEngine.isSuccessful()) {
            logger.info("BUILD SUCCESSFUL,cost:" + minute + "m\t" + second + "s\t" + millisecond + "ms");
        } else {
            logger.info("BUILD FAILURE,cost:" + minute + "m\t" + second + "s\t" + millisecond + "ms");
        }
    }

    /**
     * filter task by target task node name
     * 
     * @param taskNodeInsertBeanList
     * @param targetTaskNodeName
     * @return List<TaskNodeInsertBean>
     */
    private List<TaskNodeInsertBean> filterTaskByTargetTaskNodeName(List<TaskNodeInsertBean> taskNodeInsertBeanList, String targetTaskNodeName) {
        List<TaskNodeInsertBean> list = new ArrayList<TaskNodeInsertBean>();
        if (taskNodeInsertBeanList != null) {
            Map<String, TaskNodeInsertBean> map = new HashMap<String, TaskNodeInsertBean>();
            for (TaskNodeInsertBean taskNodeInsertBean : taskNodeInsertBeanList) {
                String taskNodeName = taskNodeInsertBean.getName();
                if (taskNodeName.endsWith(targetTaskNodeName)) {
                    targetTaskNodeName = taskNodeName;
                }
                map.put(taskNodeName, taskNodeInsertBean);
            }
            if (map.containsKey(targetTaskNodeName)) {
                Queue<String> taskNodeNameQueue = new ConcurrentLinkedQueue<String>();
                taskNodeNameQueue.add(targetTaskNodeName);
                Set<String> taskNodeNameSet = new HashSet<String>();
                while (!taskNodeNameQueue.isEmpty()) {
                    String taskNodeName = taskNodeNameQueue.poll();
                    if (!taskNodeNameSet.contains(taskNodeName)) {
                        taskNodeNameSet.add(taskNodeName);
                    }
                    TaskNodeInsertBean taskNodeInsertBean = map.get(taskNodeName);
                    if (taskNodeInsertBean != null) {
                        list.add(taskNodeInsertBean);
                        String[] parentNames = taskNodeInsertBean.getParentNames();
                        if (parentNames != null && parentNames.length != 0) {
                            for (String parentName : parentNames) {
                                if (!taskNodeNameSet.contains(parentName)) {
                                    taskNodeNameSet.add(parentName);
                                    taskNodeNameQueue.add(parentName);
                                }
                            }
                        }
                    }
                }
            } else {
                logger.warning("Can not find the " + BuilderConfiguration.MAP_KEY_TARGET_TASK + ":" + targetTaskNodeName);
            }
        }
        return list;
    }

    /**
     * insert task node
     * 
     * @param taskNodeInsertBeanList
     * @param targetTaskNodeName
     */
    protected void insertTaskNode(List<TaskNodeInsertBean> taskNodeInsertBeanList, String targetTaskNodeName) {
        if (StringUtil.isNotBlank(targetTaskNodeName)) {
            taskNodeInsertBeanList = this.filterTaskByTargetTaskNodeName(taskNodeInsertBeanList, targetTaskNodeName);
        }
        if (taskNodeInsertBeanList != null && !taskNodeInsertBeanList.isEmpty()) {
            for (TaskNodeInsertBean taskNodeInsertBean : taskNodeInsertBeanList) {
                TaskNode taskNode = new TaskNode();
                taskNode.setName(taskNodeInsertBean.getName());
                this.taskNodeMap.put(taskNode.getName(), taskNode);
            }
            for (final TaskNodeInsertBean taskNodeInsertBean : taskNodeInsertBeanList) {
                final TaskNode taskNode = this.taskNodeMap.get(taskNodeInsertBean.getName());
                taskNode.setRunnable(new Runnable() {
                    public void run() {
                        Handler handler = taskNodeInsertBean.getHandlerInstance();
                        if (handler != null) {
                            try {
                                boolean result = handler.handle();
                                if (!result) {
                                    exit("Handler handle failure,taskNode:" + taskNode.getName() + ",handler:" + handler);
                                }
                            } catch (Exception e) {
                                logger.error(Constant.Base.EXCEPTION, e);
                                exit("Handler handle failure,taskNode:" + taskNode.getName() + ",handler:" + handler);
                            }
                        }
                    }
                });
                String[] parentNames = taskNodeInsertBean.getParentNames();
                if (parentNames != null && parentNames.length != 0) {
                    for (String parentName : parentNames) {
                        if (this.taskNodeMap.containsKey(parentName)) {
                            TaskNode parentTaskNode = this.taskNodeMap.get(parentName);
                            parentTaskNode.addChildTaskNode(taskNode);
                        } else {
                            logger.warning("[WARNING]Insert node(" + taskNodeInsertBean.getName() + ") set parent task node name(" + parentName + ") is not exist");
                        }
                    }
                }
                if (parentNames == null || parentNames.length == 0) {
                    this.rootTaskNodeList.add(taskNode);
                }
                if (taskNodeInsertBean.isSkip()) {
                    this.excludeTaskNodeNameList.add(taskNodeInsertBean.getName());
                }
            }
        }
    }

    /**
     * exit
     */
    protected final void exit(String message) {
        logger.error("----------BUILD FAILURE----------:" + message, null);
        System.exit(1);
    }
}
