package org.intermine.web.logic;

import java.io.InputStream;

public class ClassResourceOpener implements ResourceOpener {

	private final Class<?> clazz;
	public ClassResourceOpener(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public InputStream openResource(String resourceName) {
		return clazz.getResourceAsStream(resourceName);
	}


}
