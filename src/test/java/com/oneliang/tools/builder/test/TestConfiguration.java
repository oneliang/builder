package com.oneliang.tools.builder.test;

import com.oneliang.tools.builder.base.Configuration;

public class TestConfiguration extends Configuration {

	protected void initialize() {
		long begin=System.currentTimeMillis();
		System.out.println("TestConfiguration.initialize,cost:"+(System.currentTimeMillis()-begin));
	}
}
