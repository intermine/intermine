package org.intermine.api.query.codegen;

import java.util.ResourceBundle;

public class JSStrings {

    private static final String BUNDLE_NAME = "org.intermine.api.query.codegen.jsmessages";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    private JSStrings() {
    }

    public static String getString(String key) {
        return RESOURCE_BUNDLE.getString(key);
    }

    public static String getString(String key, Object... args) {
        return String.format(RESOURCE_BUNDLE.getString(key), args);
    }
}
