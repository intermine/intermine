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
}
