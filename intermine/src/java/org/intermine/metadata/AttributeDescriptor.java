package org.flymine.metadata;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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
            throw new IllegalArgumentException("name cannot be null or empty");
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
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof AttributeDescriptor) {
            AttributeDescriptor attr = (AttributeDescriptor) obj;
            return name.equals(attr.name) 
                && primaryKey == attr.primaryKey
                && type.equals(attr.type);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * name.hashCode()
            + 5 * (primaryKey ? 1 : 0)
            + 7 * type.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return "<attribute name=\"" + name + "\" type=\"" + type + "\" primary-key=\""
            + primaryKey + "\"/>";
    }
}
