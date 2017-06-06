package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;

/**
 * Superclass of left and right nodes
 * @author Mark Woodbridge
 */
public class Node
{
    private Node parent;
    private String fieldName, type;
    private boolean attribute = false, reference = false, collection = false, outer = false;
    private Path minimalPath = null;
    private FieldDescriptor fd = null;
    private Model model = null;

    /**
     * Constructor for a root node
     * @param type the root type of this tree
     */
    public Node(String type) {
        this.type = type;
        parent = null;
        fieldName = null;
    }

    /**
     * Constructor for a non-root node.
     *
     * @param parent the parent node of this node
     * @param fieldName the name of the field that this node represents
     * @param outer true if this node should be an outer join
     */
    public Node(Node parent, String fieldName, boolean outer) {
        this.fieldName = fieldName;
        this.parent = parent;
        this.outer = outer;
    }

    /**
     * Attach the model. Throws IllegalArgumentExceptions if node doesn't map onto the model.
     *
     * @param model model to attach
     * @throws IllegalArgumentException if class or field are not found in the model
     */
    public void setModel(Model model) {
        ClassDescriptor cld = model.getClassDescriptorByName(getParentType());
        if (cld == null) {
            throw new IllegalArgumentException("No class '" + getParentType()
                + "' found in model '" + model.getName() + "'.");
        }
        fd = cld.getFieldDescriptorByName(fieldName);
        if (fd == null) {
            throw new IllegalArgumentException("Class '" + cld.getName()
                + "' does not have field '" + fieldName + "'.");
        }
        type = TypeUtil.unqualifiedName(fd.isAttribute()
                                        ? ((AttributeDescriptor) fd).getType()
                                        : ((ReferenceDescriptor) fd)
                                        .getReferencedClassDescriptor().getName());
        attribute = fd.isAttribute();
        reference = fd.isReference();
        collection = fd.isCollection();
        this.model = model;
    }

    /**
     * Type of parent node. Required for MainController to find field value
     * enumerations with fieldName and parentType.
     *
     * @return  type of parent node
     */
    public String getParentType() {
        if (parent == null) {
            return null;
        } else {
            return parent.getType();
        }
    }

    /**
     * @return the minimal path that describes this node (just the parent type and its field)
     **/
    public Path getMinimalPath() {
        if (minimalPath == null) {
            String minimalPathString = getParentType() + "." + fd.getName();
            try {
                minimalPath = new Path(model, minimalPathString);
            } catch (PathException e) {
                throw new IllegalStateException(minimalPathString + " is not a valid path", e);
            }
        }
        return minimalPath;
    }

    /**
     * @return The field descriptor of this node.
     */
    public FieldDescriptor getFieldDescriptor() {
        return fd;
    }

    /**
     * Get the parent node.
     * @return the parent node
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Gets the String value of pathString of this node (eg. "Department.manager.name")
     *
     * @return the String value of pathString
     */
    public String getPathString()  {
        return parent == null ? type : getPrefix() + (outer ? ":" : ".") + fieldName;
    }

    /**
     * Returns a String describing the outer join group that this Node is in. Nodes in the same
     * group can be constrained to one another, as they are fetched in the same query.
     *
     * @return a String
     */
    public String getOuterJoinGroup() {
        return getOuterJoinGroup(getPathString());
    }

    /**
     * Returns a String describing the outer join group that the given path string is in.
     *
     * @param path the path string
     * @return a String
     */
    public static String getOuterJoinGroup(String path) {
        int lastIndexOf = path.lastIndexOf(":");
        if (lastIndexOf == -1) {
            int nextIndex = path.indexOf(".");
            if (nextIndex == -1) {
                return path;
            } else {
                return path.substring(0, nextIndex);
            }
        } else {
            int nextDot = path.indexOf(".", lastIndexOf + 1);
            if (nextDot == -1) {
                return path;
            } else {
                return path.substring(0, nextDot);
            }
        }
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
        return parent == null ? "" : parent.getPathString();
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
     * Gets a friendly name for display on the web site.
     *
     * @return a String
     */
    public String getFriendlyName() {
        if (fieldName == null) {
            return type;
        }
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
        return parent == null ? 0 : parent.getIndentation() + 1;
    }

    /**
     * Returns true if this should be an outer join.
     *
     * @return a boolean
     */
    public boolean isOuterJoin() {
        return outer;
    }

    /**
     * Sets whether this should be an outer join.
     *
     * @param outer a boolean
     */
    public void setOuterJoin(boolean outer) {
        this.outer = outer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getPathString() + ":" + type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof Node)
            && Util.equals(type, ((Node) o).type)
            && Util.equals(parent, ((Node) o).parent)
            && Util.equals(fieldName, ((Node) o).fieldName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (type == null ? 0 : 3 * type.hashCode())
            + (parent == null ? 0 : 5 * parent.hashCode())
            + (fieldName == null ? 0 : 7 * fieldName.hashCode());
    }
}
