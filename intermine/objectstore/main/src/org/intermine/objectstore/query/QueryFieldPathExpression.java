package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.util.Collection;

import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;

/**
 * An element that can appear in the SELECT clause of a query, representing extra data to be
 * collected for the Results - namely a field referenced in an object referenced by some other
 * object in the results.
 *
 * @author Matthew Wakeling
 */
public class QueryFieldPathExpression implements QueryPathExpression
{
    private QueryClass qc;
    private QueryObjectPathExpression qope;
    private String referenceName;
    private String fieldName;
    private Object defaultValue;
    private Class type;

    /**
     * Constructs a QueryFieldPathExpression representing a field reference from the given
     * QueryClass through the given reference name to the given fieldname.
     *
     * @param qc the QueryClass
     * @param referenceName the name of the relevant reference
     * @param fieldName the name of the relevant field
     * @param defaultValue the default value to return if the reference is null
     * @throws IllegalArgumentException if the reference is not an object reference or the field is
     * not a normal field
     */
    public QueryFieldPathExpression(QueryClass qc, String referenceName, String fieldName,
            Object defaultValue) {
        if (qc == null) {
            throw new NullPointerException("QueryClass parameter is null");
        }
        Class qcType = qc.getType();
        this.qc = qc;
        this.qope = null;
        this.referenceName = referenceName;
        this.fieldName = fieldName;
        check(qcType, referenceName, fieldName, defaultValue);
    }

    /**
     * Constructs a QueryFieldPathExpression representing a field reference from the given
     * Object reference through the given reference name to the given fieldname.
     *
     * @param qope the QueryObjectPathExpression
     * @param referenceName the name of the reference to follow
     * @param fieldName the name of the field
     * @param defaultValue the default value to return if the reference is null
     * @throws IllegalArgumentException if the reference is not an object reference or the field is
     * not a normal field
     */
    public QueryFieldPathExpression(QueryObjectPathExpression qope, String referenceName,
            String fieldName, Object defaultValue) {
        if (qope == null) {
            throw new NullPointerException("QueryObjectPathExpression parameter is null");
        }
        this.qc = null;
        this.qope = qope;
        this.referenceName = referenceName;
        this.fieldName = fieldName;
        check(qope.getType(), referenceName, fieldName, defaultValue);
    }

    private void check(Class qcType, String referenceName, String fieldName, Object defaultValue) {
        if (referenceName == null) {
            throw new NullPointerException("Reference name parameter is null");
        }
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        Method reference = TypeUtil.getGetter(qcType, referenceName);
        if (reference == null) {
            throw new IllegalArgumentException("Field " + referenceName + " not found in "
                    + qcType);
        }
        Class referenceType = reference.getReturnType();
        if (Collection.class.isAssignableFrom(referenceType)) {
            throw new IllegalArgumentException("Field " + referenceName + " is a collection type");
        }
        if (!InterMineObject.class.isAssignableFrom(referenceType)) {
            throw new IllegalArgumentException("Field " + referenceName + " is not an object"
                    + " reference type - was " + referenceType + " instead");
        }
        Method field = TypeUtil.getGetter(referenceType, fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + referenceType);
        }
        type = field.getReturnType();
        type = type.isPrimitive() ? TypeUtil.instantiate(type.toString()) : type;
        if (Collection.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Field " + referenceType.getName() + "." + fieldName
                    + " is a collection type");
        }
        if (InterMineObject.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Field " + referenceType.getName() + "." + fieldName
                    + " is an object reference");
        }
        if (defaultValue instanceof UnknownTypeValue) {
            try {
                defaultValue = ((UnknownTypeValue) defaultValue).getConvertedValue(type);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Default value " + defaultValue + " is not"
                        + " correctly typed for field " + referenceType.getName() + "."
                        + fieldName);
            }
        }
        if ((defaultValue != null) && (!type.isInstance(defaultValue))) {
            throw new IllegalArgumentException("Default value " + defaultValue + " is not correctly"
                    + " typed for field " + referenceType.getName() + "." + fieldName);
        }
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the QueryClass of which the field is a member.
     *
     * @return the QueryClass
     */
    public QueryClass getQueryClass() {
        return qc;
    }

    /**
     * Returns the QueryObjectPathExpression of which the field is a member.
     *
     * @return the QueryObjectPathExpression
     */
    public QueryObjectPathExpression getQope() {
        return qope;
    }

    /**
     * Returns the name of the object reference.
     *
     * @return the reference name
     */
    public String getReferenceName() {
        return referenceName;
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
     * Returns the default value for fields that do not exist because the reference is null.
     *
     * @return the default value object
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public Class getType() {
        return type;
    }

    /**
     * Returns a QueryObjectPathExpression that refers to the object reference part of this object.
     *
     * @return a QueryObjectPathExpression
     */
    public QueryObjectPathExpression getParent() {
        if (qc != null) {
            return new QueryObjectPathExpression(qc, referenceName);
        } else {
            return new QueryObjectPathExpression(qope, referenceName);
        }
    }
}
