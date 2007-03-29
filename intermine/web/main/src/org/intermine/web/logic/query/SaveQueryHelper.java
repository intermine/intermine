package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * Helper methods for SaveQueryAction.
 *
 * @author Kim Rutherford
 */
public class SaveQueryHelper
{
    private static final String QUERY_NAME_PREFIX = "query_";

    /**
     * Return a query name that isn't currently in use.
     *
     * @param savedQueries the Map of current saved queries
     * @return the new query name
     */
    public static String findNewQueryName(Map savedQueries) {
        return findNewQueryName(savedQueries, null);
    }
    
    /**
     * Return a query name that isn't currently in use, returning the given name
     * if it is available.
     *
     * @param savedQueries the Map of current saved queries
     * @param name name to return if it's available
     * @return the new query name
     */
    public static String findNewQueryName(Map savedQueries, String name) {
        if (name != null && !savedQueries.containsKey(name)) {
            return name;
        }
        for (int i = 1;; i++) {
            String testName = QUERY_NAME_PREFIX + i;
            if (savedQueries == null || savedQueries.get(testName) == null) {
                return testName;
            }
        }
    }
}
