package com.oneliang.tools.builder.base;

public abstract interface Handler {

	/**
	 * handle
	 * @return boolean
	 */
	public abstract boolean handle();

	/**
	 * set configuration
	 * @param configuration
	 */
	public abstract void setConfiguration(Configuration configuration);

	/**
	 * @param inputKeyValue the inputKeyValue to set
	 */
	public abstract void setInputKeyValue(String inputKeyValue);

	/**
	 * @param outputKey the outputKey to set
	 */
	public abstract void setOutputKey(String outputKey);

	/**
	 * copy
	 * @return Handler
	 */
	public abstract Handler clone();

	/**
	 * add executor
	 * @param executor
	 */
	public abstract void addExecutor(Executor executor);

	public static final class Executor{
		public enum Type{
			COMMAND,HANDLER
		}
		private String execute=null;
		private Type type=null;
		public Executor(String execute,Type type) {
			this.execute=execute;
			this.type=type;
		}
		/**
		 * @return the execute
		 */
		public String getExecute() {
			return execute;
		}
		/**
		 * @param execute the execute to set
		 */
		public void setExecute(String execute) {
			this.execute = execute;
		}
		/**
		 * @return the type
		 */
		public Type getType() {
			return type;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(Type type) {
			this.type = type;
		}
		
	}
}
