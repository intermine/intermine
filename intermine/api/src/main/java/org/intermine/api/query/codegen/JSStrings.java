package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ResourceBundle;

/**
 *
 * @author Alex
 *
 */
public final class JSStrings
{

    private static final String BUNDLE_NAME = "org.intermine.api.query.codegen.jsmessages";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    private JSStrings() {
    }

    /**
     * @param key key
     * @return JS message
     */
    public static String getString(String key) {
        return RESOURCE_BUNDLE.getString(key);
    }

    /**
     * @param key key
     * @param args arguments
     * @return JS message
     */
    public static String getString(String key, Object... args) {
        return String.format(RESOURCE_BUNDLE.getString(key), args);
    }
}
