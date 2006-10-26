package org.intermine.path;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;

import org.apache.commons.lang.StringUtils;


/**
 * Object to represent a path through an InterMine model.  Construction from
 * a String validates against model.
 * @author Richard Smith
 */
public class Path
{
    private ClassDescriptor startCld;
    private List elements;
    private FieldDescriptor endFld;
    private Model model;
    private String path;
    private boolean containsCollections = false;
    
    
    /**
     * Create a new Path object. The Path must start with a class name.
     * @param model the Model used to check ClassDescriptors and FieldDescriptors
     * @param path a String of the form "Department.manager.name"
     */
    public Path(Model model, String path) {
        if (model == null) {
            throw new IllegalArgumentException("model argument is null");
        }
        this.model = model;
        if (path == null) {
            throw new IllegalArgumentException("path argument is null");
        }
        this.path = path;
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("path argument is blank");
        }
        elements = new ArrayList();
        String[] parts = path.split("[.]");
        String clsName = parts[0];
        ClassDescriptor cld =
            model.getClassDescriptorByName(model.getPackageName() + "." + clsName);
        this.startCld = cld;
        if (cld == null) {
            throw new RuntimeException("Unable to resolve path '" + path + "': class '" + clsName
                                       + "' not found in model '" + model.getName() + "'");
        }
        for (int i = 1; i < parts.length; i++) {
            FieldDescriptor fld = cld.getFieldDescriptorByName(parts[i]);
            elements.add(fld);
            if (fld == null) {
                throw new RuntimeException("Unable to resolve path '" + path + "': field '"
                                           + parts[i] + "' of class '" + cld.getName()
                                           + "' not found in model '" + model.getName() + "'");
            }
            // if this is a collection then mark the whole path as containing collections
            if (fld.isCollection()) {
                this.containsCollections = true;
            }

            // check if attribute and not at end of path
            if (i < parts.length - 1) {
                if (fld.isAttribute()) {
                    throw new RuntimeException("Unable to resolve path '" + path + "': field '"
                                               + parts[i] + "' of class '"
                                               + cld.getName()
                                               + "' is not a reference/collection field in "
                                               + "the model '"
                                               + model.getName() + "'");
                }
                
                cld = ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
            } else {
                this.endFld = fld;
            }
        }
    }

    /**
     * Return true if and only if any part of the path is a collection.
     * @return the collections flag
     */
    public boolean containsCollections() {
        return containsCollections;
    }

    /**
     * Return true if and only if the end of the path is an attribute.
     * @return the end-is-attribute flag
     */
    public boolean endIsAttribute() {
        return endFld.isAttribute();
    }

    /**
     * Return true if and only if the end of the path is a collection.
     * @return the end-is-collection flag
     */
    public boolean endIsCollection() {
        return endFld.isCollection();
    }

    /**
     * Return true if and only if the end of the path is a reference .
     * @return the end-is-reference flag
     */
    public boolean endIsReference() {
        return endFld.isReference();
    }

    /**
     * Return the ClassDescriptor of the first element in the path.  eg. for Department.name, 
     * return the Department descriptor.
     * @return the starting ClassDescriptor
     */
    public ClassDescriptor getStartClassDescriptor() {
        return startCld;
    }

    /**
     * Return the FieldDescriptor of the last element in the path or null if the path has just one
     * element.  eg. for "Employee.department.name", return the Department.name descriptor but
     * for "Employee" return null.
     * @return the end FieldDescriptor
     */
    public FieldDescriptor getEndFieldDescriptor() {
        return endFld;
    }
    
    /**
     * If the last element in the path is a reference or collection return the ClassDescriptor that
     * the reference or collection references.  If the path has one element (eg. "Employee"),
     * return its ClassDescriptor.  If the last element in the path is an attribute, return null.
     * @return the ClassDescriptor
     */
    public ClassDescriptor getEndClassDescriptor() {
        if (getEndFieldDescriptor() == null) {
            return getStartClassDescriptor();
        }
        
        if (!getEndFieldDescriptor().isAttribute()) {
            if (getEndFieldDescriptor().isCollection()) {
                CollectionDescriptor collDesc = (CollectionDescriptor) getEndFieldDescriptor();
                return collDesc.getReferencedClassDescriptor();
            }
            if  (getEndFieldDescriptor().isReference()) {
                ReferenceDescriptor refDesc =  (ReferenceDescriptor) getEndFieldDescriptor();
                return refDesc.getReferencedClassDescriptor();
            }
        }
        
        return null;
    }

    /**
     * If the last element in the path is an attribute, return the Class of the attribute,
     * otherwise return null
     * @return the Class of the last element if an attribute, or null otherwise
     */
    public Class getEndType() {
        if (endFld == null) {
            return null;
        }
        if (endFld.isAttribute()) {
            return ((AttributeDescriptor) endFld).getType().getClass();
        }
        return null;
    }

    public Object resolve(InterMineObject o) {
        Set clds = model.getClassDescriptorsForClass(o.getClass());
        if (!clds.contains(getStartClassDescriptor())) {
            throw new RuntimeException("ClassDescriptor from the start of path: " + path
                                       + " is not a superclass of the class: " + o.getClass()
                                       + " while resolving object: " + o);
        }

        Iterator iter = elements.iterator();

        Object current = o;
        
        while (iter.hasNext()) {
            FieldDescriptor element = (FieldDescriptor) iter.next();
            String fieldName = element.getName();
            try {
                current = TypeUtil.getFieldValue(current, fieldName);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("IllegalAccessException while trying to get value of "
                                           + "field \"" + fieldName + "\" in object: " + o);
            }
        }
        
        return current;
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o instanceof Path) {
            Path p = (Path) o;
            return (p.startCld.equals(this.startCld)
                    && p.elements.equals(this.elements));
        }
        return false;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < elements.size(); i++) {
            sb.append(".");
            sb.append(((FieldDescriptor) elements.get(i)).getName());
        }
        return getStartClassDescriptor().getUnqualifiedName() + sb.toString();
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    /**
     * @return
     */
    public List getElements() {
        return elements;
    }
}
