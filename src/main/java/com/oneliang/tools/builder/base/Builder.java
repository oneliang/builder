package com.oneliang.tools.builder.base;

import java.util.Map;

import com.oneliang.util.task.TaskNode;

public abstract interface Builder {

	/**
	 * prepare,for task engine prepare,return the outer join task node,may be return null
	 * @return Map<String,TaskNode>
	 */
	public abstract Map<String,TaskNode> prepare();

	/**
	 * build,for task engine execute
	 */
	public abstract void build();
}
