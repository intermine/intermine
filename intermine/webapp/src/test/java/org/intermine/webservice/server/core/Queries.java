package org.intermine.webservice.server.core;

import java.io.StringReader;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

public class Queries
{
    private static final String BUNDLE_NAME = "org.intermine.webservice.server.core.queries"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    private Queries() {
    }
    
    public static PathQuery getPathQuery(String key) {
    	return PathQueryBinding.unmarshalPathQuery(new StringReader(getXML(key)), PathQuery.USERPROFILE_VERSION);
    }

    public static String getXML(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            throw new RuntimeException(e);
        }
    }
}
