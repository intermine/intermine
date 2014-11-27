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

import java.lang.reflect.Method;
import java.util.Collection;

import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;

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
    private Class<?> type;

    /**
     * Constructs a QueryField representing the specified field of a QueryClass
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws IllegalArgumentException if the field is a reference, a collection or does not exist
     */
    public QueryField(QueryClass qc, String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        if ("class".equals(fieldName)) {
            this.qc = qc;
            this.fieldName = "class";
            secondFieldName = null;
            type = Class.class;
        } else {
            Method field = TypeUtil.getGetter(qc.getType(), fieldName);
            if (field == null) {
                throw new IllegalArgumentException("Field '" + fieldName + "' not found in "
                        + qc.getType());
            }
            if (Collection.class.isAssignableFrom(field.getReturnType())) {
                throw new IllegalArgumentException("Field '" + fieldName
                        + "' is a collection type");
            }
            if (InterMineObject.class.isAssignableFrom(field.getReturnType())) {
                throw new IllegalArgumentException("Field '" + fieldName
                        + "' is an object reference");
            }
            this.qc = qc;
            this.fieldName = fieldName;
            secondFieldName = null;
            Class<?> fieldType = field.getReturnType();
            type = fieldType.isPrimitive() ? TypeUtil.instantiate(fieldType.toString()) : fieldType;
        }
    }

    /**
     * Constructs a QueryField representing the specified field of the specified QueryClass, as seen
     * outside the specified subquery.
     *
     * @param q the Query object that is the subquery
     * @param qc the QueryClass that the field is a member of
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws IllegalArgumentException if the field is a collection or does not exist
     */
    public QueryField(Query q, QueryClass qc, String fieldName) {
        if (q == null) {
            throw new NullPointerException("Subquery parameter is null");
        }
        Method field = TypeUtil.getGetter(qc.getType(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in "
                                           + qc.getType());
        }
        if (java.util.Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is a collection type");
        }
        if (!(Number.class.isAssignableFrom(field.getReturnType())
                || String.class.isAssignableFrom(field.getReturnType())
                || Boolean.class.isAssignableFrom(field.getReturnType())
                || java.util.Date.class.isAssignableFrom(field.getReturnType())
                || field.getReturnType().isPrimitive())) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is an object reference");
        }
        this.qc = q;
        this.fieldName = q.getAliases().get(qc);
        secondFieldName = fieldName;
        Class<?> fieldType = field.getReturnType();
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
    public QueryField(Query q, QueryEvaluable v) {
        if (q == null) {
            throw new NullPointerException("Subquery parameter is null");
        }
        this.qc = q;
        this.fieldName = q.getAliases().get(v);
        if (this.fieldName == null) {
            throw new NullPointerException("Field not found in subquery");
        }
        this.secondFieldName = null;
        this.type = v.getType();
    }

    /**
     * Constructs the id QueryField for the given QueryClassBag.
     *
     * @param qcb the QueryClassBag
     */
    public QueryField(QueryClassBag qcb) {
        if (qcb == null) {
            throw new NullPointerException("QueryClassBag parameter is null");
        }
        this.qc = qcb;
        this.fieldName = "id";
        this.secondFieldName = null;
        this.type = Integer.class;
    }

    /**
     * Constructs a QueryField object - intended for cloning operations.
     *
     * @param qc the FromElement
     * @param fieldName the first field name
     * @param secondFieldName the second field name
     * @param type the Class of the value
     */
    protected QueryField(FromElement qc, String fieldName, String secondFieldName, Class<?> type) {
        this.qc = qc;
        this.fieldName = fieldName;
        this.secondFieldName = secondFieldName;
        this.type = type;
    }

    /**
     * Gets the FromElement of which the field is a member
     *
     * @return the FromElement
     */
    public FromElement getFromElement() {
        return qc;
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getType() {
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
    @Override
    public String toString() {
        return "QueryField(" + qc + ", " + fieldName + (secondFieldName == null ? "" : ", "
                    + secondFieldName) + ")";
    }

    /**
     * {@inheritDoc}
     */
    public void youAreType(@SuppressWarnings("unused") Class<?> cls) {
        throw new ClassCastException("youAreType called on a QueryField");
    }

    /**
     * {@inheritDoc}
     */
    public int getApproximateType() {
        throw new ClassCastException("getApproximateType called on a QueryField");
    }

    /**
     * Returns true if this is equal to the given object.
     *
     * @param o the object
     * @return a boolean
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof QueryField) {
            QueryField qf = (QueryField) o;
            return qc.equals(qf.qc) && fieldName.equals(qf.fieldName)
                && Util.equals(secondFieldName, qf.secondFieldName);
        }
        return false;
    }

    /**
     * Returns a hash code for this object.
     *
     * @return an int
     */
    @Override
    public int hashCode() {
        return 3 * qc.hashCode() + 5 * fieldName.hashCode() + (secondFieldName == null ? 0
                : 7 * secondFieldName.hashCode());
    }
}
