package org.intermine.webservice.server.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Queries
{
    private static final String BUNDLE_NAME = "org.intermine.webservice.server.core.queries"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    private Queries() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
