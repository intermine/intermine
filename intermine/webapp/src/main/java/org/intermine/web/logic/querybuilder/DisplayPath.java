package org.intermine.web.logic.querybuilder;

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
import org.intermine.metadata.FieldDescriptor;
import org.intermine.pathquery.Path;

/**
 * A small wrapper around Path with more jsp-accessible data.
 *
 * @author Matthew Wakeling
 */
public class DisplayPath
{
    Path path;

    /**
     * Constructs a new DisplayPath object from a Path.
     *
     * @param path a Path to wrap
     */
    public DisplayPath(Path path) {
        this.path = path;
    }

    /**
     * Return the underlying path object.
     * @return path the path object wrapped by this DisplayPath
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the number of elements in the path, where a simple class is zero.
     *
     * @return number of elements in path
     */
    public int getIndentation() {
        return path.getElements().size();
    }

    /**
     * Returns the last element of the path.
     *
     * @return a String
     */
    public String getFriendlyName() {
        if (path.isRootPath()) {
            return path.getNoConstraintsString();
        } else {
            return path.getLastElement();
        }
    }

    /**
     * Return the last field descriptor (the last element) of the path. If the
     * path represents the root class, return null.
     * @return A field descriptor.
     */
    public FieldDescriptor getFieldDescriptor() {
        return path.getEndFieldDescriptor();
    }

    /**
     * Returns true if the path represents an attribute (rather than a class, reference, or
     * collection).
     *
     * @return a boolean
     */
    public boolean isAttribute() {
        return path.endIsAttribute();
    }

    /**
     * Returns true if the path represents a reference.
     *
     * @return a boolean
     */
    public boolean isReference() {
        return path.endIsReference();
    }

    /**
     * Returns true if the path represents a collection.
     *
     * @return a boolean
     */
    public boolean isCollection() {
        return path.endIsCollection();
    }

    /**
     * Return true if the end element of the path is an attribute and the attribute is a primitive
     * type.
     * @return true if path represents a primitive attribute
     */
    public boolean isPrimitive() {
        if (path.endIsAttribute()) {
            AttributeDescriptor attDescriptor = (AttributeDescriptor) path.getEndFieldDescriptor();
            return attDescriptor.isPrimitive();
        }
        return false;
    }

    /**
     * Returns the full path string for this path.
     *
     * @return a String
     */
    public String getPathString() {
        return path.getNoConstraintsString();
    }

    /**
     * Returns the fieldname for this path.
     *
     * @return a String
     */
    public String getFieldName() {
        if (path.isRootPath()) {
            return null;
        } else {
            return path.getLastElement();
        }
    }

    /**
     * Returns the type of the value represented by this path, as a string.
     *
     * @return a String unqualified class name
     */
    public String getType() {
        return path.getEndType().getSimpleName();
    }

    /**
     * Returns a new DisplayPath that represents the parent path, or null if this path is just a
     * root class.
     *
     * @return a new DisplayPath
     */
    public DisplayPath getParent() {
        if (path.isRootPath()) {
            return null;
        } else {
            return new DisplayPath(path.getPrefix());
        }
    }

    /**
     * Fetch the unqualified class name of the final class on this path, if the path represents an
     * attribute this will be the parent class of that attribute.  For example Company.name will
     * return Company.
     * @return an unqualified class name
     */
    public String getLastClassName() {
        return path.getLastClassDescriptor().getUnqualifiedName();
    }

    /**
     * Returns a representation of the Path as a String, with class constraint markers.  eg.
     * "Department.employees[Manager].seniority"
     * {@inheritDoc}
     */
    @Override public String toString() {
        return path.toString();
    }
}
