package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Date;

import org.flymine.util.TypeUtil;

/**
 * Represents a QueryClass field that is neither a collection or reference to
 * another business object.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryField implements QueryEvaluable
{
    private FromElement qc;
    private String fieldName;
    private String secondFieldName;
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
        if (Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        if (!(Number.class.isAssignableFrom(field.getType())
                || String.class.isAssignableFrom(field.getType())
                || Boolean.class.isAssignableFrom(field.getType())
                || Date.class.isAssignableFrom(field.getType())
                || field.getType().isPrimitive())) {
            throw new IllegalArgumentException("Field " + fieldName + " is an object reference");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        secondFieldName = null;
        Class fieldType = field.getType();
        type = fieldType.isPrimitive() ? TypeUtil.instantiate(fieldType.toString()) : fieldType;
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
        this.qc = q;
        this.fieldName = (String) q.getAliases().get(qc);
        secondFieldName = fieldName;
        Class fieldType = field.getType();
        type = fieldType.isPrimitive() ? TypeUtil.instantiate(fieldType.toString()) : fieldType;
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
        this.qc = q;
        this.fieldName = (String) q.getAliases().get(v);
        this.secondFieldName = null;
        this.type = v.getType();
    }

    /**
     * Constructs a QueryField object - intended for cloning operations.
     *
     * @param qc the FromElement
     * @param fieldName the first field name
     * @param secondFieldName the second field name
     * @param type the Class of the value
     */
    protected QueryField(FromElement qc, String fieldName, String secondFieldName, Class type) {
        this.qc = qc;
        this.fieldName = fieldName;
        this.secondFieldName = secondFieldName;
        this.type = type;
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
     * Returns the name of the second field.
     *
     * @return second field name
     */
    public String getSecondFieldName() {
        return secondFieldName;
    }

    /**
     * Produces a String, for debugging purposes.
     *
     * @return a String representation
     */
    public String toString() {
        return "QueryField(" + qc + ", " + fieldName + ")";
    }

    /**
     * @see QueryEvaluable#youAreType
     */
    public void youAreType(Class cls) {
        throw new ClassCastException("youAreType called on a QueryField");
    }

    /**
     * @see QueryEvaluable#getApproximateType
     */
    public int getApproximateType() {
        throw new ClassCastException("getApproximateType called on a QueryField");
    }
}
