package org.flymine.objectstore.query;

/**
 * Represents a field of a QueryClass that is a non-primitive type
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public abstract class QueryReference
{
    protected QueryClass qc;
    protected String fieldName;
    protected Class type;
    
    /**
     * Gets the QueryClass of which this reference is an member
     *
     * @return the QueryClass
     */    
    public QueryClass getQueryClass() {
        return qc;
    }

    /**
     * Gets the Java class of this QueryReference
     *
     * @return the class name
     */    
    public Class getType() {
        return type;
    }

    /**
     * Gets the fieldname of this QueryReference
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }
}
