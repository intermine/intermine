package org.flymine.objectstore.query;

import java.lang.reflect.Field;

import org.flymine.util.TypeUtil;

/**
 * Represents a QueryClass field that is not a collection
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryField implements QueryEvaluable
{
    private FromElement qc;
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
            throw new NoSuchFieldException("Field " + fieldName + " not found in "
                                           + qc.getType());
        }
        if (java.util.Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        if (!(Number.class.isAssignableFrom(field.getType())
                || String.class.isAssignableFrom(field.getType())
                || Boolean.class.isAssignableFrom(field.getType())
                || java.util.Date.class.isAssignableFrom(field.getType())
                || field.getType().isPrimitive())) {
            throw new IllegalArgumentException("Field " + fieldName + " is an object reference");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        this.type = TypeUtil.toContainerType(field.getType());
    }
    
    /**
     * Constructs a QueryField representing the specified field of the specified QueryClass, as seen
     * outside the specified subquery.
     *
     * @param q the Query object that is the subquery
     * @param qc the QueryClass that the field is a member of
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws NoSuchFieldException if the field does not exist
     * @throws IllegalArgumentException if the field is a collection
     */
    public QueryField(Query q, QueryClass qc, String fieldName)
        throws NullPointerException, NoSuchFieldException, IllegalArgumentException {
        if (q == null) {
            throw new NullPointerException("Subquery parameter is null");
        }
        Field field = TypeUtil.getField(qc.getType(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field " + fieldName + " not found in "
                                           + qc.getType());
        }
        if (java.util.Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        if (!(Number.class.isAssignableFrom(field.getType())
                || String.class.isAssignableFrom(field.getType())
                || Boolean.class.isAssignableFrom(field.getType())
                || java.util.Date.class.isAssignableFrom(field.getType())
                || field.getType().isPrimitive())) {
            throw new IllegalArgumentException("Field " + fieldName + " is an object reference");
        }
        this.fieldName = ((String) q.getAliases().get(qc)) + fieldName;
        this.qc = q;
        this.type = TypeUtil.toContainerType(field.getType());
    }

    /**
     * Constructs a QueryField representing the specified entry from the SELECT list of the
     * specified subquery.
     *
     * @param q the Query object that is the subquery
     * @param v the entry of the SELECT list
     * @throws NullPointerException if the field name is null
     */
    public QueryField(Query q, QueryEvaluable v)
        throws NullPointerException {
        if (q == null) {
            throw new NullPointerException("Subquery parameter is null");
        }
        this.fieldName = (String) q.getAliases().get(v);
        this.qc = q;
        this.type = v.getType();
    }

    /**
     * Gets the QueryClass of which the field is a member
     *
     * @return the QueryClass
     */    
    public FromElement getFromElement() {
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

    /**
     * Produces a String, for debugging purposes.
     *
     * @return a String representation
     */
    public String toString() {
        return "QueryField(" + qc + ", " + fieldName + ")";
    }
}
