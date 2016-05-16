package com.oneliang.tools.builder.base;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractHandler implements Handler {

	protected Configuration configuration=null;
	protected List<Executor> executorList=new CopyOnWriteArrayList<Executor>();
	protected String inputKeyValue=null;
	protected String outputKey=null;

	/**
	 * @param configuration the configuration to set
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * add executor
	 * @param executor
	 */
	public void addExecutor(Executor executor){
		this.executorList.add(executor);
	}

	public Handler clone() {
		AbstractHandler handler=null;
		try {
			handler=(AbstractHandler)this.getClass().newInstance();
			handler.setConfiguration(this.configuration);
			handler.inputKeyValue=this.inputKeyValue;
			handler.outputKey=this.outputKey;
			for(Executor executor:this.executorList){
				handler.addExecutor(executor);
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return handler; 
	}

	/**
	 * @param inputKeyValue the inputKeyValue to set
	 */
	public void setInputKeyValue(String inputKeyValue) {
		this.inputKeyValue = inputKeyValue;
	}

	/**
	 * @param outputKey the outputKey to set
	 */
	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}
}
