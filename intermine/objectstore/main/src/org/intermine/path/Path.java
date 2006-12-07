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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.util.StringUtil;
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
    private Map subClassConstraintPaths;
    private List elementClassDescriptors;
    
    /**
     * Create a new Path object. The Path must start with a class name.
     * @param model the Model used to check ClassDescriptors and FieldDescriptors
     * @param path a String of the form "Department.manager.name" or
     * "Department.employees[Manager].seniority"
     */
     public Path(Model model, String path) {
        if (model == null) {
            throw new IllegalArgumentException("model argument is null");
        }
        this.model = model;
        if (path == null) {
            throw new IllegalArgumentException("path argument is null");
        }

        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("path argument is blank");
        }
        subClassConstraintPaths = new HashMap();
        
        Pattern p = Pattern.compile("([^\\[\\]]+)\\[(.*)\\]");
        
        List newPathBits = new ArrayList();
        String[] bits = StringUtil.split(path, ".");
        for (int i = 0; i < bits.length; i++) {
            String thisBit = bits[i];
            Matcher m = p.matcher(thisBit);
            if (m.matches()) {
                String pathBit = m.group(1);
                String className = m.group(2);
                newPathBits.add(pathBit);
                subClassConstraintPaths.put(StringUtil.join(newPathBits, "."), className);
            } else {
                newPathBits.add(thisBit);
            }
        }
        this.path = StringUtil.join(newPathBits, ".");
        initialise();
    }

    /**
     * Create a Path object using class constraints from a Map.  Unlike the other constructor, the
     * stringPath cannot contain class constraint annotation.  Either call the other constructor
     * like this: new Path(model, "Department.employees[Manager].seniority") or call this
     * constructor like this: new Path(model, "Department.employees.seniority", map) where the
     * map contains: key "Department.employees" -> value: "Manager"
     * @param model the Model used to check ClassDescriptors and FieldDescriptors
     * @param stringPath a String of the form "Department.manager.name"
     * @param constraintMap a Map from paths as string to class names - use when parts of the path
     * are constrained to be sub-classes
     */
    public Path(Model model, String stringPath, Map constraintMap) {
        this.model = model;
        this.path = stringPath;
        this.subClassConstraintPaths = constraintMap;
        if (path.indexOf("[") != -1) {
            throw new IllegalArgumentException("path: " + stringPath 
                                               + " contains illegal character '['");
        }
        initialise();
    }

    private void initialise() {
        elements = new ArrayList();
        elementClassDescriptors = new ArrayList();
        String[] parts = path.split("[.]");
        String clsName = parts[0];
        ClassDescriptor cld =
            model.getClassDescriptorByName(model.getPackageName() + "." + clsName);
        this.startCld = cld;
        if (cld == null) {
            throw new RuntimeException("Unable to resolve path '" + path + "': class '" + clsName
                                       + "' not found in model '" + model.getName() + "'");
        }
        
        StringBuffer currentPath = new StringBuffer(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            currentPath.append(".");
            String thisPart = parts[i];
            currentPath.append(thisPart);
            FieldDescriptor fld = cld.getFieldDescriptorByName(thisPart);
            if (fld instanceof ReferenceDescriptor) {
                ClassDescriptor classDescriptor = 
                    ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
                elementClassDescriptors.add(classDescriptor);
            }
            elements.add(fld);
            if (fld == null) {
                throw new RuntimeException("Unable to resolve path '" + path + "': field '"
                                           + thisPart + "' of class '" + cld.getName()
                                           + "' not found in model '" + model.getName() + "'");
            }
            // if this is a collection then mark the whole path as containing collections
            if (fld.isCollection()) {
                this.containsCollections = true;
            }

            if (i < parts.length - 1) {

                // check if attribute and not at end of path
                if (fld.isAttribute()) {
                    throw new RuntimeException("Unable to resolve path '" + path + "': field '"
                                               + thisPart + "' of class '"
                                               + cld.getName()
                                               + "' is not a reference/collection field in "
                                               + "the model '"
                                               + model.getName() + "'");
                }
                
                String constrainedClassName =
                    (String) subClassConstraintPaths.get(currentPath.toString());
                if (constrainedClassName == null) {
                    // the class of this reference/collection is not constrained so get the
                    // class name from the model
                    cld = ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
                } else {
                    String qualifiedClassName = model.getPackageName() + "." + constrainedClassName;
                    cld = model.getClassDescriptorByName(qualifiedClassName);
                }
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
        if (endFld == null) {
            return false;
        }
        return endFld.isAttribute();
    }

    /**
     * Return true if and only if the end of the path is a collection.
     * @return the end-is-collection flag
     */
    public boolean endIsCollection() {
        if (endFld == null) {
            return false;
        }
        return endFld.isCollection();
    }

    /**
     * Return true if and only if the end of the path is a reference .
     * @return the end-is-reference flag
     */
    public boolean endIsReference() {
        if (endFld == null) {
            return false;
        }
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

    /**
     * If the path ends with an attribute, return the ClassDescriptor that contains that field,
     * otherwise return the ClassDescriptor referred to by the reference or collection at the end
     * of the path.
     * @return the ClassDescriptor
     */
    public ClassDescriptor getLastClassDescriptor() {
        FieldDescriptor endFD = getEndFieldDescriptor();
        if (endFD.isAttribute()) {
            return endFD.getClassDescriptor();
        } else {
            return ((ReferenceDescriptor) endFD).getReferencedClassDescriptor();
        }
    }
    
    /**
     * Return the object at the end of the pat, starting from the given object. 
     * @param o the start object
     * @return the attribute, object or collection at the end of the path
     */
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
                if (current == null) {
                    return null;
                }
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
     * Returns a representation of the Path as a String, with class constraint markers.  eg.
     * "Department.employees[Manager].seniority"
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String cdUnqualifiedName = getStartClassDescriptor().getUnqualifiedName();
        StringBuffer returnStringBuffer = new StringBuffer(cdUnqualifiedName);
        // the path without class constraints
        StringBuffer simplePath = new StringBuffer(cdUnqualifiedName);
        for (int i = 0; i < elements.size(); i++) {
            returnStringBuffer.append(".");
            simplePath.append(".");
            FieldDescriptor fieldDescriptor = (FieldDescriptor) elements.get(i);
            returnStringBuffer.append(fieldDescriptor.getName());
            simplePath.append(fieldDescriptor.getName());
            String constraintClassName = 
                (String) subClassConstraintPaths.get(simplePath.toString());
            if (constraintClassName != null
                && (fieldDescriptor.isReference() || fieldDescriptor.isCollection())) {
                String referencedClassName = 
                    ((ReferenceDescriptor) fieldDescriptor).getReferencedClassName();
                if (!TypeUtil.unqualifiedName(referencedClassName).equals(constraintClassName)) {
                    returnStringBuffer.append('[');
                    returnStringBuffer.append(constraintClassName);
                    returnStringBuffer.append(']');
                }
            }
        }
        return returnStringBuffer.toString();
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    /**
     * Return a list of FieldDescriptors, one per path element (except for the first, which is a
     * class).
     * @return the FieldDescriptor
     */
    public List getElements() {
        return elements;
    }

    public List getElementClassDescriptors() {
        return elementClassDescriptors;
    }
    
    /**
     * Returns a representation of the Path as a String, with no class constraint markers.  eg.
     * "Department.employees.seniority"
     * @return a String version of the Path
     */
    public String toStringNoConstraints() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < elements.size(); i++) {
            sb.append(".");
            sb.append(((FieldDescriptor) elements.get(i)).getName());
        }
        return getStartClassDescriptor().getUnqualifiedName() + sb.toString();
    }
}
