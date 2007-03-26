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

import java.util.Date;

/**
 * Represents a type cast in a Query.
 *
 * @author Matthew Wakeling
 */
public class QueryCast implements QueryEvaluable
{
    private QueryEvaluable value;
    private Class type;

    /**
     * Construct a QueryCast.
     *
     * @param value the value of this QueryCast
     * @param type the type of the value for this QueryCast to represent
     */
    public QueryCast(QueryEvaluable value, Class type) {
        if (value == null) {
            throw new NullPointerException("Cannot create a QueryCast with null");
        }
        if (!(Number.class.isAssignableFrom(type)
                    || String.class.isAssignableFrom(type)
                    || Boolean.class.isAssignableFrom(type)
                    || Date.class.isAssignableFrom(type))) {
            throw new IllegalArgumentException("type must be a Number, String, Boolean, or Date");
        }
        this.value = value;
        this.type = type;
    }

    /**
     * @see QueryEvaluable#getType
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns the QueryEvaluable that is the actual value.
     *
     * @return the value
     */
    public QueryEvaluable getValue() {
        return value;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof QueryCast) {
            QueryCast objCast = (QueryCast) obj;
            if (objCast.value.equals(value) && objCast.type.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * value.hashCode() + 5 * type.hashCode();
    }

    /**
     * @see QueryEvaluable#youAreType
     */
    public void youAreType(Class cls) {
        throw new ClassCastException("youAreType called on a QueryCast");
    }

    /**
     * @see QueryEvaluable#getApproximateType
     */
    public int getApproximateType() {
        throw new ClassCastException("getApproximateType called on QueryCast");
    }
}
