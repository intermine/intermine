package org.flymine.metadata;

/**
 * Describes an attribute of a class - i.e. a field that is neither an object
 * reference or a collection.
 *
 * @author Richard Smith
 */

public class AttributeDescriptor extends FieldDescriptor
{

    protected final String type;

    /**
     * Construct, name and type cannot be null.
     * @param name name of field in the class
     * @param primaryKey true if part of the primary key
     * @param type name of primitive or a fully qualified class name
     * @throws IllegalArgumentException if arguments are null
     */
    public AttributeDescriptor(String name, boolean primaryKey, String type)
        throws IllegalArgumentException {

        super(name, primaryKey);
        if (type == null || type == "") {
            throw new IllegalArgumentException("type (" + name + ") parameter cannot be null");
        }
        this.type = type;
    }

    /**
     * Get the type of the attribute - either name of primitive or fully qualified
     * class name.
     * @return type of attribute
     */
    public String getType() {
        return this.type;
    }
    
    /**
     * @see FieldDescriptor#relationType
     */
    public int relationType() {
        return NOT_RELATION;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "<attribute name=\"" + name + "\" type=\"" + type + "\" primary-key=\""
            + primaryKey + "\"/>";
    }
}
