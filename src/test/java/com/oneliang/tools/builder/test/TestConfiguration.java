package com.oneliang.tools.builder.test;

import com.oneliang.tools.builder.base.Configuration;

public class TestConfiguration extends Configuration {

	protected void initialize() {
		long begin=System.currentTimeMillis();
		System.out.println("Cost:"+(System.currentTimeMillis()-begin));
	}
}
