/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.query;

import java.util.Date;

/**
 * Represents a constant in a Query
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryValue implements QueryEvaluable
{
    private Object value;

    /**
     * Construct a QueryValue
     *
     * @param value the value of this QueryValue
     * @throws IllegalArgumentException if value is not a Number, String, Boolean or Date
     */
    public QueryValue(Object value) {
        if (!((value instanceof Number)
              || (value instanceof String)
              || (value instanceof Boolean)
              || (value instanceof Date))) {
            throw new IllegalArgumentException("value must be a Number, String, Boolean or Date");
        }
        this.value = value;
    }

    /**
       * @see QueryEvaluable
       */
    public Class getType() {
        return value.getClass();
    }

    /**
     * Returns the object that is the actual value.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Overrides Object.
     *
     * @param obj object to compare to
     * @return true if obj is a QueryValue with the same encapsulated object
     */
    public boolean equals(Object obj) {
        if (obj instanceof QueryValue) {
            Object objValue = ((QueryValue) obj).getValue();
            if ((value instanceof Number) && (objValue instanceof Number)) {
                if ((value instanceof Float) || (value instanceof Double)
                        || (objValue instanceof Float) || (objValue instanceof Double)) {
                    return ((Number) value).doubleValue() == ((Number) objValue).doubleValue();
                } else {
                    return ((Number) value).longValue() == ((Number) objValue).longValue();
                }
            } else {
                return value.equals(objValue);
            }
        }
        return false;
    }

    /**
     * Overrides Object.
     *
     * @return an integer based on the contents of this object
     */
    public int hashCode() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return value.hashCode();
    }
}
