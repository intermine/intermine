package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.TypeUtil;

/**
 * Superclass of left and right nodes
 * @author Mark Woodbridge
 */
public class Node
{
    String fieldName, path, prefix, type, parentType;
    boolean attribute = false, reference = false, collection = false;
    int indentation;

    /**
     * Constructor for a root node
     * @param type the root type of this tree
     */
    public Node(String type) {
        this.type = type;
        path = type;
        prefix = "";
        indentation = 0;
    }

    /**
     * Constructor for a non-root node
     * @param parent the parent node of this node
     * @param fieldName the name of the field that this node represents
     * @param model the model used to resolve paths
     */
    public Node(Node parent, String fieldName, Model model) {
        this.fieldName = fieldName;
        prefix = parent.getPath();
        path = prefix + "." + fieldName;
        parentType = parent.getType();
        ClassDescriptor cld = MainHelper.getClassDescriptor(parent.getType(), model);
        FieldDescriptor fd = cld.getFieldDescriptorByName(fieldName);
        type = TypeUtil.unqualifiedName(fd.isAttribute()
                                        ? ((AttributeDescriptor) fd).getType()
                                        : ((ReferenceDescriptor) fd)
                                        .getReferencedClassDescriptor().getType().getName());
        attribute = fd.isAttribute();
        reference = fd.isReference();
        collection = fd.isCollection();
        indentation = path.split("[.]").length - 1;
    }

    /**
     * Type of parent node. Required for MainController to find field value
     * enumerations with fieldName and parentType.
     *
     * @return  type of parent node
     */
    public String getParentType() {
        return parentType;
    }
    
    /**
     * Gets the value of path
     *
     * @return the value of path
     */
    public String getPath()  {
        return path;
    }

    /**
     * Gets the value of type
     *
     * @return the value of type
     */
    public String getType()  {
        return type;
    }

    /**
     * Set the value of type
     * 
     * @param type the value of type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the value of prefix
     *
     * @return the value of prefix
     */
    public String getPrefix()  {
        return prefix;
    }

    /**
     * Gets the value of fieldName
     *
     * @return the value of fieldName
     */
    public String getFieldName()  {
        return fieldName;
    }

    /**
     * Gets the value of attribute
     *
     * @return the value of attribute
     */
    public boolean isAttribute()  {
        return attribute;
    }

    /**
     * Gets the value of reference
     *
     * @return the value of reference
     */
    public boolean isReference()  {
        return reference;
    }

    /**
     * Gets the value of collection
     *
     * @return the value of collection
     */
    public boolean isCollection()  {
        return collection;
    }

    /**
     * Gets the value of indentation
     *
     * @return the value of indentation
     */
    public int getIndentation()  {
        return indentation;
    }
    
    /**
     * @see Object#toString
     */
    public String toString() {
        return path + ":" + type;
    }
    
    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        return (o instanceof Node)
            && path.equals(((Node) o).path)
            && type.equals(((Node) o).type);
    }
    
    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 2 * path.hashCode()
            + 3 * type.hashCode();
    }
}