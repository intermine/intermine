package org.flymine.objectstore.query;

import java.lang.reflect.Field;

import org.flymine.util.TypeUtil;

/**
 * Represents a QueryClass field that is not a collection
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryField implements QueryNode, QueryEvaluable
{
    private QueryClass qc;
    private String fieldName;
    private Class type;
    
    /**
     * Constructs a QueryField representing the specified field of a QueryClass
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws NoSuchFieldException if the field does not exist
     * @throws IllegalArgumentException if the field is a collection
     */    
    public QueryField(QueryClass qc, String fieldName)
        throws NullPointerException, NoSuchFieldException, IllegalArgumentException {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        Field field = TypeUtil.getField(qc.getType(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field " + fieldName + " not found in class "
                                           + qc.getType());
        }
        if (java.util.Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        this.type = TypeUtil.toContainerType(field.getType());
    }
    
    /**
     * Gets the QueryClass of which the field is a member
     *
     * @return the QueryClass
     */    
    public QueryClass getQueryClass() {
        return qc;
    }

    /**
       * @see QueryEvaluable
       */
    public Class getType() {
        return type;
    }

    /**
     * Returns the name of the field.
     *
     * @return field name
     */
    public String getFieldName() {
        return fieldName;
    }
}
