package org.flymine.objectstore.query;

/**
 * Represents the database extent of a Java class
 * NOTE - No equals() method is defined for this class and none should be.
 * org.flymine.objectstore.query.Query.equals relies on QueryClass using
 * Object.equals() to prevent ambiguity.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class QueryClass implements QueryNode, FromElement
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

    /**
     * Returns a String representation.
     *
     * @return a String representation
     */
    public String toString() {
        return type.getName();
    }
}
