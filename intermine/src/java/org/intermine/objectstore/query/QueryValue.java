package org.flymine.objectstore.query;

import java.util.Date;

/**
 *
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryValue implements QueryEvaluable
{
    private Object value;

    /**
     * @param value the initial numeric value of this QueryValue
     */    
    public QueryValue(Number value) {
        this.value = value;
    }
    
    /**
     * @param value this initial string value of this QueryValue
     */    
    public QueryValue(String value) {
        this.value = value;
    }
    
    /**
     * @param value the initial boolean value of this QueryValue
     */    
    public QueryValue(Boolean value) {
        this.value = value;
    }
    
    /**
     * @param value the initial date value of this QueryValue
     */    
    public QueryValue(Date value) {
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
