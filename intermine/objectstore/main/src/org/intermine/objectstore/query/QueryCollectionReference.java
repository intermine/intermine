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

import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;

/**
 * Represents a field of a QueryClass that is a collection
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryCollectionReference extends QueryReference
{
    protected InterMineObject qcObj = null;
    protected QueryClassBag qcb = null;

    /**
     * Constructs a QueryCollectionReference representing the specified field of a QueryClass
     *
     * @param qc the QueryClass
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws IllegalArgumentException if the field is not a collection or does not exist
     */
    public QueryCollectionReference(QueryClass qc, String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        Method field = TypeUtil.getGetter(qc.getType(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qc.getType());
        }
        if (!java.util.Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " in " + qc.getType()
                    + " is not a collection type");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        this.type = field.getReturnType();
    }

    /**
     * Constructs a QueryCollectionReference representing the specified field of a QueryClassBag
     *
     * @param qcb the QueryClassBag
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws IllegalArgumentException if the field is not a collection or does not exist
     */
    public QueryCollectionReference(QueryClassBag qcb, String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        Method field = TypeUtil.getGetter(qcb.getType(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qcb.getType());
        }
        if (!java.util.Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " in " + qcb.getType()
                    + " is not a collection type");
        }
        this.qcb = qcb;
        this.fieldName = fieldName;
        this.type = field.getReturnType();
    }

    /**
     * Constructs a QueryCollectionReference representing the specified field of an object
     *
     * @param qcObj the InterMineObject
     * @param fieldName the name of the relevant field
     * @throws NullPointerException if the field name is null
     * @throws IllegalArgumentException if the field is not a collection or does not exist
     */    
    public QueryCollectionReference(InterMineObject qcObj, String fieldName) {
        if (fieldName == null) {
            throw new NullPointerException("Field name parameter is null");
        }
        Method field = TypeUtil.getGetter(qcObj.getClass(), fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field " + fieldName + " not found in "
                    + qcObj.getClass());
        }
        if (!java.util.Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " in " + qcObj.getClass()
                    + " is not a collection type");
        }
        this.qcObj = qcObj;
        this.fieldName = fieldName;
        this.type = field.getReturnType();
    }

    /**
     * Gets the InterMineObject of this QueryReference.
     *
     * @return an InterMineObject
     */
    public InterMineObject getQcObject() {
        return qcObj;
    }

    /**
     * Gets the QueryClassBag of this QueryReference.
     *
     * @return a QueryClassBag
     */
    public QueryClassBag getQcb() {
        return qcb;
    }

    /**
     * {@inheritDoc}
     */
    public Class getQcType() {
        if (qc != null) {
            return qc.getType();
        } else if (qcObj != null) {
            return qcObj.getClass();
        } else {
            return qcb.getType();
        }
    }
}
