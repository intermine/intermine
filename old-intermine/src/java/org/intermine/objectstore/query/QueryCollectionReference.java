package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;

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
            throw new IllegalArgumentException("Field " + fieldName + " not found in class "
                                           + qc.getType());
        }
        if (!java.util.Collection.class.isAssignableFrom(field.getReturnType())) {
            throw new IllegalArgumentException("Field " + fieldName + " is not a collection type");
        }
        this.qc = qc;
        this.fieldName = fieldName;
        this.type = field.getReturnType();
    }
}
