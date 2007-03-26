package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Set;

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
    private Class type;

    /**
     * Constructs a QueryClass representing the specified Java class
     *
     * @param type the Java class
     */
    public QueryClass(Class type) {
        this.type = type;
    }

    /**
     * Constructs a QueryClass representing the specified set of classes
     *
     * @param types the Set of classes
     */
    public QueryClass(Set types) {
        if (types.size() == 1) {
            this.type = (Class) types.iterator().next();
        } else {
            this.type = DynamicUtil.composeClass(types);
        }
    }

    /**
     * Gets the Java class represented by this QueryClass
     *
     * @return the Class
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns a String representation.
     *
     * @return a String representation
     */
    public String toString() {
        Set classes = DynamicUtil.decomposeClass(type);
        if (classes.size() == 1) {
            return type.getName();
        } else {
            boolean needComma = false;
            StringBuffer retval = new StringBuffer();
            Iterator classIter = classes.iterator();
            while (classIter.hasNext()) {
                retval.append(needComma ? ", " : "(");
                needComma = true;
                Class cls = (Class) classIter.next();
                retval.append(cls.getName());
            }
            return retval.toString() + ")";
        }
    }
}
