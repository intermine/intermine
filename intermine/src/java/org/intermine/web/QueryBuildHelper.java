package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.flymine.util.TypeUtil;

/**
 * Helper methods for Query building.
 *
 * @author Kim Rutherford
 */

public class QueryBuildHelper
{
    /**
     * Return the field name (in an object) for a given field name in the form.
     *
     * @param fieldName the field name from a form.
     * @return the field name as it appears in the class.
     */
    public static String getFieldName(String fieldName) {
        return fieldName.substring(0, fieldName.lastIndexOf("_"));
    }

    /**
     * Give a class name a unique alias given the existing aliases (eg Company -> Company_3)
     * @param existingAliases the Collection of existing aliases
     * @param type the class name
     * @return a new alias
     */
    protected static String aliasClass(Collection existingAliases, String type) {
        String prefix = toAlias(type);
        int max = 0;
        for (Iterator i = existingAliases.iterator(); i.hasNext();) {
            String alias = (String) i.next();
            if (alias.substring(0, alias.lastIndexOf("_")).equals(prefix)) {
                int suffix = Integer.valueOf(alias.substring(alias.lastIndexOf("_") + 1))
                    .intValue();
                if (suffix >= max) {
                    max = suffix + 1;
                }
            }
        }
        return prefix + "_" + max;
    }

    /**
     * Convert a class name to a alias prefix
     * @param type the class name
     * @return a suitable prefix for an alias
     */
    protected static String toAlias(String type) {
        return TypeUtil.unqualifiedName(type);
    }

    /**
     * Add a new DisplayQueryClass of type className to the current query
     * @param queryClasses the exiting queryClasses
     * @param className the class name
     */
    protected static void addClass(Map queryClasses, String className) {
        DisplayQueryClass d = new DisplayQueryClass();
        d.setType(className);

        String alias = aliasClass(queryClasses.keySet(), className);
        queryClasses.put(alias, d);
    }
}
