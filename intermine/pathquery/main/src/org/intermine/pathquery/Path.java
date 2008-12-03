package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * Object to represent a path through an InterMine model.  Construction from
 * a String validates against model.
 * @author Richard Smith
 */
public class Path
{
    private ClassDescriptor startCld;
    private List<String> elements;
    private FieldDescriptor endFld;
    private Model model;
    private String path;
    private boolean containsCollections = false;
    private boolean containsReferences = false;
    private Map<String, String> subClassConstraintPaths;
    private List<ClassDescriptor> elementClassDescriptors;
    private List<Boolean> outers;

    /**
     * Create a new Path object. The Path must start with a class name.
     * @param model the Model used to check ClassDescriptors and FieldDescriptors
     * @param path a String of the form "Department.manager.name" or
     * "Department.employees[Manager].seniority"
     * @throws PathError thrown if there is a problem resolving the path eg. a reference doesn't
     * exist in the model
     */
     public Path(Model model, String path) throws PathError {
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
        subClassConstraintPaths = new HashMap<String, String>();

        Pattern p = Pattern.compile("([^\\[\\]]+)\\[(.*)\\]");

        List<String> newPathBits = new ArrayList();
        List<Boolean> newPathOuters = new ArrayList();
        StringTokenizer bits = new StringTokenizer(". " + path, ".:", true);
        boolean notFirst = false;
        while (bits.hasMoreTokens()) {
            String separator = bits.nextToken();
            String thisBit = bits.nextToken();
            if (notFirst) {
                newPathOuters.add(Boolean.valueOf(":".equals(separator)));
            } else {
                thisBit = thisBit.substring(1);
                notFirst = true;
            }
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
        this.outers = newPathOuters;
        initialise();
    }

    /**
     * Create a Path object using class constraints from a Map.  Unlike the other constructor, the
     * stringPath cannot contain class constraint annotation.  Either call the other constructor
     * like this: new Path(model, "Department.employees[Manager].seniority") or call this
     * constructor like this: new Path(model, "Department.employees.seniority", map) where the
     * map contains: key "Department.employees" -&gt; value: "Manager"
     *
     * @param model the Model used to check ClassDescriptors and FieldDescriptors
     * @param stringPath a String of the form "Department.manager.name"
     * @param constraintMap a Map from paths as string to class names - use when parts of the path
     * are constrained to be sub-classes
     * @throws PathError thrown if there is a problem resolving the path eg. a reference doesn't
     * exist in the model
      */
    public Path(Model model, String stringPath, Map<String, String> constraintMap)
        throws PathError {
        this.model = model;
        this.path = stringPath;
        this.subClassConstraintPaths = new HashMap<String, String>(constraintMap);

        for (String constaintPath: subClassConstraintPaths.keySet()) {
            if (constaintPath.indexOf(':') != -1) {
                throw new IllegalArgumentException("illegal character (':') in constraint map");
            }
        }

        if (path.indexOf("[") != -1) {
            throw new IllegalArgumentException("path: " + stringPath
                                               + " contains illegal character '['");
        }

        List<String> newPathBits = new ArrayList();
        List<Boolean> newPathOuters = new ArrayList();
        StringTokenizer bits = new StringTokenizer(". " + path, ".:", true);
        boolean notFirst = false;
        while (bits.hasMoreTokens()) {
            String separator = bits.nextToken();
            String thisBit = bits.nextToken();
            if (notFirst) {
                newPathOuters.add(Boolean.valueOf(":".equals(separator)));
            } else {
                thisBit = thisBit.substring(1);
                notFirst = true;
            }
            newPathBits.add(thisBit);
        }
        this.path = StringUtil.join(newPathBits, ".");
        this.outers = newPathOuters;
        initialise();
    }

    private void initialise() throws PathError {
        elements = new ArrayList();
        elementClassDescriptors = new ArrayList<ClassDescriptor>();
        String parts[] = path.split("[.]");
        String clsName = parts[0];
        ClassDescriptor cld = null;
        if (!("".equals(clsName))) {
            cld = model.getClassDescriptorByName(model.getPackageName() + "." + clsName);
            if (cld == null) {
                throw new PathError("Unable to resolve path '" + path + "': class '" + clsName
                                    + "' not found in model '" + model.getName() + "'", path);
            }
            this.startCld = cld;
            elementClassDescriptors.add(cld);
        }

        StringBuffer currentPath = new StringBuffer(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            currentPath.append(".");
            String thisPart = parts[i];
            currentPath.append(thisPart);
            elements.add(thisPart);
            if (!("".equals(clsName))) {
                FieldDescriptor fld = cld.getFieldDescriptorByName(thisPart);
                if (fld == null) {
                    throw new PathError("Unable to resolve path '" + path + "': field '"
                                        + thisPart + "' of class '" + cld.getName()
                                        + "' not found in model '" + model.getName() + "'", path);
                }
                // if this is a collection then mark the whole path as containing collections
                if (fld.isCollection()) {
                    this.containsCollections = true;
                }
                if (fld.isReference()) {
                    this.containsReferences = true;
                }

                if (i < parts.length - 1) {

                    // check if attribute and not at end of path
                    if (fld.isAttribute()) {
                        throw new PathError("Unable to resolve path '" + path + "': field '"
                                            + thisPart + "' of class '"
                                            + cld.getName()
                                            + "' is not a reference/collection field in "
                                            + "the model '"
                                            + model.getName() + "'", path);
                    }
                } else {
                    this.endFld = fld;
                }
                if (!fld.isAttribute()) {
                    String constrainedClassName =
                        subClassConstraintPaths.get(currentPath.toString());
                    if (constrainedClassName == null) {
                        // the class of this reference/collection is not constrained so get the
                        // class name from the model
                        cld = ((ReferenceDescriptor) fld).getReferencedClassDescriptor();
                    } else {
                        String qualifiedClassName = model.getPackageName() + "."
                            + constrainedClassName;
                        cld = model.getClassDescriptorByName(qualifiedClassName);
                        if (cld == null) {
                            throw new PathError("Unable to resolve path '" + path + "': class '"
                                    + qualifiedClassName + "' not found in model '"
                                    + model.getName() + "'", path);
                        }
                    }
                    elementClassDescriptors.add(cld);
                }
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
     * Return true if and only if any part of the path is a reference.
     * @return the references flag
     */
    public boolean containsReferences() {
        return containsReferences;
    }

    /**
     * Return true if the Path does not contain references or collections
     * @return a boolean
     */
    public boolean isOnlyAttribute() {
        return (!containsReferences && !containsCollections);
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
     * Return a Path object that represents the prefix of this path, ie this Path without the
     * last element.  If the Path contains only one element, an exception is thrown.
     * @return the prefix Path
     */
    public Path getPrefix() {
        if (getElements().size() == 0) {
            throw new RuntimeException("path (" + this + ") has only one element");
        }
        String pathString = toString();
        int lastDotIndex = pathString.lastIndexOf('.');
        int lastIndex = pathString.lastIndexOf(':');
        if (lastDotIndex > lastIndex) {
            lastIndex = lastDotIndex;
        }
        return new Path(model, pathString.substring(0, lastIndex));
    }

    /**
     * Return new Path that has this Path as its prefix and has fieldName as the last element.
     * @param fieldName the field name
     * @return the new Path
     */
    public Path append(String fieldName) {
       return new Path(model, toString() + "." + fieldName);
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
     * Returns the last ClassDescriptor in the path. If the last element is an attribute, then the
     * class before it in the path is returned. Otherwise, class of the last element is returned.
     * The class of an element is the referenced type of the FieldDescriptor (modified by the class
     * constraint), or simply the class if it is the first element in the path. For example, if the
     * path is "Department.manager.name" then this method will return Manager. If the path is
     * "Department.manager[CEO].name" then this method will return CEO.
     *
     * @return the ClassDescriptor
     */
    public ClassDescriptor getLastClassDescriptor() {
        List<ClassDescriptor> l = getElementClassDescriptors();
        return l.get(l.size() - 1);
    }

    /**
     * Return the last string element of this path, throw an exception of there is only one element,
     * i.e. for 'Company.departments.manager' return 'manager'.
     * @return the last string element of the path 
     */
    public String getLastElement() {
        if (getElements().size() == 0) {
            throw new RuntimeException("path (" + this + ") has only one element");
        }
        
        return elements.get(elements.size() - 1);
    }


    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public String toString() {
        String cdUnqualifiedName = getStartClassDescriptor().getUnqualifiedName();
        StringBuffer returnStringBuffer = new StringBuffer(cdUnqualifiedName);
        // the path without class constraints
        StringBuffer simplePath = new StringBuffer(cdUnqualifiedName);
        for (int i = 0; i < elements.size(); i++) {
            returnStringBuffer.append(outers.get(i).booleanValue() ? ":" : ".");
            simplePath.append(".");
            String fieldName = elements.get(i);
            returnStringBuffer.append(fieldName);
            simplePath.append(fieldName);
            String constraintClassName =
                subClassConstraintPaths.get(simplePath.toString());
            if (constraintClassName != null) {
                if (startCld != null) {
                    FieldDescriptor fieldDescriptor = elementClassDescriptors.get(i)
                        .getFieldDescriptorByName(fieldName);
                    if (fieldDescriptor.isReference() || fieldDescriptor.isCollection()) {
                        String referencedClassName =
                            ((ReferenceDescriptor) fieldDescriptor).getReferencedClassName();
                        if (!TypeUtil.unqualifiedName(referencedClassName)
                                .equals(constraintClassName)) {
                            returnStringBuffer.append('[');
                            returnStringBuffer.append(constraintClassName);
                            returnStringBuffer.append(']');
                        }
                    }
                } else {
                    returnStringBuffer.append('[')
                        .append(constraintClassName)
                        .append(']');
                }
            }
        }
        return returnStringBuffer.toString();
    }


    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 0;
    }

    /**
     * Return a list of FieldDescriptors, one per path element (except for the first, which is a
     * class).
     * @return the FieldDescriptor
     */
    public List<String> getElements() {
        return elements;
    }

    /**
     * Return a List of the ClassDescriptor objects for each element of the path.
     * @return the ClassDescriptors
     */
    public List<ClassDescriptor> getElementClassDescriptors() {
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
            sb.append(outers.get(i).booleanValue() ? ":" : ".");
            sb.append(elements.get(i));
        }
        return getStartClassDescriptor().getUnqualifiedName() + sb.toString();
    }

    /**
     * Required for jsp
     * @return a String version of the Path
     */
    public String getNoConstraintsString() {
        return toStringNoConstraints();
    }

    /**
     * Returns a Map from simplified path string (with dots instead of colons, and no constraints)
     * to constraint class name.
     *
     * @return subClassConstraintPaths
     */
    public Map<String, String> getSubClassConstraintPaths() {
        return subClassConstraintPaths;
    }
    
    /**
     * Return the model that this path is created for.
     * @return the model
     */
    public Model getModel() {
        return model;
    }
}
