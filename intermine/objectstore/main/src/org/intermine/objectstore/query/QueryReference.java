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

/**
 * Represents a field of a QueryClass that is a non-primitive type.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public abstract class QueryReference
{
    protected QueryClass qc = null;
    protected String fieldName;
    protected Class type;
    
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
        return type;
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
     * Returns the type of the queryclass of this reference
     *
     * @return a Class
     */
    public Class getQcType() {
        return qc.getType();
    }
}
