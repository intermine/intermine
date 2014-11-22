package org.intermine.web.logic;

import java.io.InputStream;

public interface ResourceOpener {

	/**
     * Open a resource by name, and return an InputStream to it.
     * @param resourceName The name of the resource.
     * @return The open input stream.
     */
	public InputStream openResource(String resourceName);
}
