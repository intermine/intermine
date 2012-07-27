package org.intermine.webservice.server.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Strings {

	private static final String BUNDLE_NAME = "org.intermine.webservice.server.core.strings";
	
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Strings() {
		// Don't
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			throw new RuntimeException(e);
		}
	}
}
