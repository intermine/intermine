package org.intermine.web.struts;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MockServletContext
    extends org.apache.struts.mock.MockServletContext {

    private Map<String, InputStream> inputStreams = new HashMap<String, InputStream>();

    public MockServletContext() {
        // Auto-generated constructor stub
    }

	public void addInputStream(String resourceName, InputStream is) {
		inputStreams.put(resourceName, is);
	}

	public InputStream getResourceAsStream(String name) {
		return inputStreams.get(name);
	}



}
