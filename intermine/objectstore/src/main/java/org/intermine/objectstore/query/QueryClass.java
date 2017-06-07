package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.util.DynamicUtil;

/**
 * Represents the database extent of a Java class
 * NOTE - No equals() method is defined for this class and none should be.
 * org.intermine.objectstore.query.Query.equals relies on QueryClass using
 * Object.equals() to prevent ambiguity.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryClass implements QueryNode, FromElement
{
    private Class<? extends FastPathObject> type;

    /**
     * Constructs a QueryClass representing the specified Java class
     *
     * @param type the Java class
     */
    public QueryClass(Class<? extends FastPathObject> type) {
        this.type = type;
    }

    /**
     * Constructs a QueryClass representing the specified set of classes
     *
     * @param types the Set of classes
     */
    public QueryClass(Set<Class<?>> types) {
        this(types.toArray(new Class[0]));
    }

    /**
     * Constructs a QueryClass representing the specified array of classes
     *
     * @param types the array of classes
     */
    public QueryClass(Class<?>... types) {
        this.type = DynamicUtil.composeDescriptiveClass(types);
    }

    /**
     * Gets the Java class represented by this QueryClass
     *
     * @return the Class
     */
    public Class<? extends FastPathObject> getType() {
        return type;
    }

    /**
     * Returns a String representation.
     *
     * @return a String representation
     */
    @Override
    public String toString() {
        Set<Class<?>> classes = Util.decomposeClass(type);
        if (classes.size() == 1) {
            return type.getName();
        } else {
            boolean needComma = false;
            StringBuffer retval = new StringBuffer();
            for (Class<?> clazz : classes) {
                retval.append(needComma ? ", " : "(");
                needComma = true;
                retval.append(clazz.getName());
            }
            return retval.toString() + ")";
        }
    }
}
