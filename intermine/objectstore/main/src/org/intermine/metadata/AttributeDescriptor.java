package org.intermine.metadata;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
     * This is a list of the valid type strings.
     */
    public static final Set<String> VALID_TYPES = new LinkedHashSet<String>(Arrays.asList("short",
            "int", "long", "float", "double", "boolean", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Boolean",
            "java.lang.String", "java.util.Date", "java.math.BigDecimal",
            "org.intermine.objectstore.query.ClobAccess"));

    /**
     * Construct, name and type cannot be null.
     * @param name name of field in the class
     * @param type name of primitive or a fully qualified class name
     * @throws IllegalArgumentException if arguments are null
     */
    public AttributeDescriptor(String name, String type) {
        super(name);
        if (type == null || "".equals(type)) {
            throw new IllegalArgumentException("Type cannot be null or empty");
        }
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Type \"" + type + "\" is not valid - must be one"
                    + " of " + VALID_TYPES);
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
     * {@inheritDoc}
     */
    @Override
    public int relationType() {
        return NOT_RELATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AttributeDescriptor) {
            AttributeDescriptor attr = (AttributeDescriptor) obj;
            return name.equals(attr.name)
                && type.equals(attr.type);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 3 * name.hashCode()
            + 7 * type.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "<attribute name=\"" + name + "\" type=\"" + type + "\"/>";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toJSONString() {
        return "{\"name\":\"" + name + "\",\"type\":\"" + type + "\"}";
    }

    /**
     * Returns true if the type of the attribute is a primitive type (rather than object).
     *
     * @return true or false
     */
    public boolean isPrimitive() {
        return "short".equals(type) || "int".equals(type) || "long".equals(type)
            || "float".equals(type) || "double".equals(type) || "boolean".equals(type);
    }
}
