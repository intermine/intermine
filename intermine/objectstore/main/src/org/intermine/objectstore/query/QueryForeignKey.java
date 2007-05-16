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

import java.lang.reflect.Method;
import java.util.Collection;

import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;

/**
 * Represents the ID of a field of a QueryClass that is a business object
 *
 * @author Matthew Wakeling
 */
public class QueryForeignKey implements QueryEvaluable
{
    protected QueryClass qc = null;
    protected String fieldName;

    /**
     * Constructs a QueryForeignKey representing the specified field of a QueryClass
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws IllegalArgumentException if the field is not a reference, or does not exist
     */    
    public QueryForeignKey(QueryClass qc, String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        Method field = TypeUtil.getGetter(qc.getType(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                                           + qc.getType());
        }
        if (Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is a collection type");
        }
        if (!InterMineObject.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is not a separate database "
                    + "object");
        }
        this.qc = qc;
        this.fieldName = fieldName;
    }
    
    /**
     * Gets the QueryClass of which this reference is an member.
     *
     * @return the QueryClass
     */    
    public QueryClass getQueryClass() {
        return qc;
    }

    /**
     * Gets the Java class of this QueryReference.
     *
     * @return the class name
     */    
    public Class getType() {
        return Integer.class;
    }

    /**
     * Gets the fieldname of this QueryReference.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * {@inheritDoc}
     */
    public void youAreType(Class cls) {
        throw new ClassCastException("youAreType called on Foreign Key");
    }

    /**
     * {@inheritDoc}
     */
    public int getApproximateType() {
        throw new ClassCastException("getApproximateType called on a Foreign Key");
    }
}
