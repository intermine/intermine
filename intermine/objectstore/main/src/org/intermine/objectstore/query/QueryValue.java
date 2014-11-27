package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
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

    /* Constant instances of Query Value */
    /** The true value **/
    public static final QueryValue TRUE = new QueryValue(Boolean.TRUE);
    /** The false value **/
    public static final QueryValue FALSE = new QueryValue(Boolean.FALSE);

    /**
     * Construct a QueryValue
     *
     * @param value the value of this QueryValue
     * @throws IllegalArgumentException if value is not a Number, String, Boolean or Date
     */
    public QueryValue(Object value) {
        if (value == null) {
            throw new NullPointerException("Cannot create a QueryValue with null");
        }
        if (!((value instanceof Number) || (value instanceof String) || (value instanceof Boolean)
                    || (value instanceof Date) || (value instanceof UnknownTypeValue)
                    || (value instanceof Class<?>))) {
            throw new IllegalArgumentException("value (" + value
                    + ") must be a Number, String, Boolean, Date, Class or unknown but was: "
                    + value.getClass());
        }
        this.value = value;
    }

    /**
       * {@inheritDoc}
       */
    public Class<?> getType() {
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
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QueryValue) {
            Object objValue = ((QueryValue) obj).getValue();
            if ((value instanceof Number) && (objValue instanceof Number)) {
                if ((value instanceof BigDecimal) || (objValue instanceof BigDecimal)) {
                    BigDecimal bd1 = null;
                    BigDecimal bd2 = null;
                    if (value instanceof BigDecimal) {
                        bd1 = (BigDecimal) value;
                    } else {
                        bd1 = new BigDecimal(value.toString());
                    }
                    if (objValue instanceof BigDecimal) {
                        bd2 = (BigDecimal) objValue;
                    } else {
                        bd2 = new BigDecimal(objValue.toString());
                    }
                    return bd1.compareTo(bd2) == 0;
                } else if ((value instanceof Float) || (value instanceof Double)
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
    @Override
    public int hashCode() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return value.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public void youAreType(Class<?> cls) {
        if (value.getClass().equals(UnknownTypeValue.class)) {
            value = ((UnknownTypeValue) value).getConvertedValue(cls);
        } else {
            throw new ClassCastException("youAreType called where type already known");
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getApproximateType() {
        if (value.getClass().equals(UnknownTypeValue.class)) {
            return ((UnknownTypeValue) value).getApproximateType();
        } else {
            throw new ClassCastException("getApproximateType called when type is known");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return value.getClass().getName() + ": \"" + value + "\"";
    }
}
