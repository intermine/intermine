package org.flymine.objectstore.query;

/**
 * Represents the database extent of a Java class
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryClass implements QueryNode 
{
    private Class type;
    
    /**
     * Constructs a QueryClass representing the specified Java class
     *
     * @param type the Java class
     */    
    public QueryClass(Class type) {
        this.type = type;
    }
    
    /**
     * Gets the Java class represented by this QueryClass
     *
     * @return the class name
     */    
    public Class getType() {
        return type;
    }
}
