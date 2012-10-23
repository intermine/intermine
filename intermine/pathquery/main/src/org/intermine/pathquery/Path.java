package org.intermine.pathquery;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.CollectionDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.util.Util;

/**
 * Object to represent a path through an InterMine model.  Construction from
 * a String validates against model.
 * @author Richard Smith
 */
public class Path
{
    protected static final Logger LOG = Logger.getLogger(Path.class);
    private ClassDescriptor startCld;
    private List<String> elements;
    private FieldDescriptor endFld;
    private final Model model;
    private String path;
    private boolean containsCollections = false;
    private boolean containsReferences = false;
    private final Map<String, String> subClassConstraintPaths;
    private List<ClassDescriptor> elementClassDescriptors;
    private final List<Boolean> outers;

    /**
     * Create a new Path object. The Path must start with a class name.
     * @param model the Model used to check ClassDescriptors and FieldDescriptors
     * @param path a String of the form "Department.manager.name" or
     * "Department.employees[Manager].seniority"
     * @throws PathException thrown if there is a problem resolving the path eg. a reference doesn't
     * exist in the model
     */
    public Path(Model model, String path) throws PathException {
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

        List<String> newPathBits = new ArrayList<String>();
        List<Boolean> newPathOuters = new ArrayList<Boolean>();
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
     * @throws PathException thrown if there is a problem resolving the path eg. a reference doesn't
     * exist in the model
      */
    public Path(Model model, String stringPath, Map<String, String> constraintMap)
        throws PathException {
        this.model = model;
        if (stringPath == null) {
            throw new IllegalArgumentException("path argument is null");
        }

        if (StringUtils.isBlank(stringPath)) {
            throw new IllegalArgumentException("path argument is blank");
        }
        this.path = stringPath;
        this.subClassConstraintPaths = new HashMap<String, String>(constraintMap);
//        validatePath(constraintMap, stringPath);

        for (String constaintPath: subClassConstraintPaths.keySet()) {
            if (constaintPath.indexOf(':') != -1) {
                throw new IllegalArgumentException("illegal character (':') in constraint map");
            }
        }

        if (path.indexOf("[") != -1) {
            throw new IllegalArgumentException("path: " + stringPath
                                               + " contains illegal character '['");
        }

        List<String> newPathBits = new ArrayList<String>();
        List<Boolean> newPathOuters = new ArrayList<Boolean>();
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

//    /**
//     * Method checks if there isn't incompatible join, when for example department is once joined
//     * in constraint as inner join and in other constraint as outer join.
//     * @param constraintMap
//     * @param stringPath
//     */
//    private void validatePath(Map<String, String> constraintMap, String stringPath) {
//        Set<String> paths = new HashSet<String>(constraintMap.keySet());
//        paths.add(stringPath);
//        for (String constraint : paths) {
//            String[] parts = constraint.split(":");
//            String prefix = "";
//            for (int i = 0; i < parts.length - 1; i++) {
//                String part = parts[i];
//                prefix += part + ".";
//                for (String checkedString : paths) {
//                    if (checkedString.startsWith(prefix)) {
//                      throw new IllegalArgumentException("Incompatible paths, different joins: "
//                              + constraint + ", " + checkedString);
//                    }
//                }
//            }
//        }
//    }

    private void initialise() throws PathException {
        elements = new ArrayList<String>();
        elementClassDescriptors = new ArrayList<ClassDescriptor>();
        String[] parts = path.split("[.]");
        String clsName = parts[0];
        ClassDescriptor cld = null;
        if (!("".equals(clsName))) {
            cld = model.getClassDescriptorByName(model.getPackageName() + "." + clsName);
            if (cld == null) {
                throw new PathException("Unable to resolve path '" + path + "': class '" + clsName
                                    + "' not found in model '" + model.getName() + "'", path);
            }
            this.startCld = cld;
            elementClassDescriptors.add(cld);
        } else {
            LOG.error("First part is empty. Path is \"" + path + "\"");
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
                    throw new PathException("Unable to resolve path '" + path + "': field '"
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
                        throw new PathException("Unable to resolve path '" + path + "': field '"
                                + thisPart + "' of class '" + cld.getName()
                                + "' is not a reference/collection field in the model '"
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
                            throw new PathException("Unable to resolve path '" + path + "': class '"
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
     * Returns a list of paths, each corresponding to one step further along the path. Starts with
     * the root path, and ends in the current path, ie:
     *
     * Company.departments.manager.name
     *         decomposes to:
     * Company, Company.departments, Company.departments.manager, Company.departments.manager.name
     *
     *  @return the list of composing paths.
     */
    public List<Path> decomposePath() {
        List<Path> pathList = new ArrayList<Path>();
        pathList.add(this);
        Path currentPath = this;
        while (!currentPath.isRootPath()) {
            Path nextPath = currentPath.getPrefix();
            pathList.add(nextPath);
            currentPath = nextPath;
        }
        Collections.reverse(pathList);
        return pathList;
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
     * last element.  If the Path contains only the root class, an exception is thrown.
     *
     * @return the prefix Path
     */
    public Path getPrefix() {
        if (isRootPath()) {
            throw new RuntimeException("path (" + this + ") has only one element");
        }
        String pathString = toString();
        int lastDotIndex = pathString.lastIndexOf('.');
        int lastIndex = pathString.lastIndexOf(':');
        if (lastDotIndex > lastIndex) {
            lastIndex = lastDotIndex;
        }
        try {
            return new Path(model, pathString.substring(0, lastIndex));
        } catch (PathException e) {
            // Should not happen
            throw new Error("There must be a bug", e);
        }
    }

    /**
     * Return new Path that has this Path as its prefix and has fieldName as the last element.
     *
     * @param fieldName the field name
     * @return the new Path
     * @throws PathException if the resulting Path is not valid
     */
    public Path append(String fieldName) throws PathException {
        return new Path(model, toString() + "." + fieldName);
    }

    /**
     * Return the type of the last element in the path, regardless of whether it is an attribute
     * or a class.
     * @return the Class of the last element
     */
    public Class<?> getEndType() {
        Class<?> retval = null;
        if (endFld != null && endFld.isAttribute()) {
            retval = Util.getClassFromString(((AttributeDescriptor) endFld).getType());
        } else {
            retval = getLastClassDescriptor().getType();
        }
        return retval;
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
     * Returns the second to last ClassDescriptor in the path. That is, the one before the one
     * returned by getLastClassDescriptor.
     *
     * @return the ClassDescriptor
     */
    public ClassDescriptor getSecondLastClassDescriptor() {
        List<ClassDescriptor> l = getElementClassDescriptors();
        if (l.size() >1) {
            return l.get(l.size() - 2);
        }
        return null;
    }

    /**
     * Return the last string element of this path, throw an exception of there is only one element,
     * i.e. for 'Company.departments.manager' return 'manager'.
     * @return the last string element of the path
     */
    public String getLastElement() {
        if (isRootPath()) {
            throw new RuntimeException("path (" + this + ") has only one element");
        }
        return elements.get(elements.size() - 1);
    }

    /**
     * Return true if this path represents just the starting class, e.g. 'Department'.  Returns
     * false of there are further elements, e.g. 'Department.manager'
     * @return true if this is a root path
     */
    public boolean isRootPath() {
        if (getElements().size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
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
    @Override public String toString() {
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
    @Override public int hashCode() {
        return 0;
    }

    /**
     * Return a list of field names, one per path element (except for the first, which is a
     * class). To clarify, this does not include the root class of the path.
     *
     * @return a list of field names
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

    /**
     * Return true if the first element in the path has the class given in input as type
     * @param cls the class
     * @return true if the first element contains the class
     */
    public boolean startContainsClass(String cls) {
        String rootClass = startCld.getSimpleName();
        if (rootClass.equals(cls)) {
            return true;
        }
        return false;
    }

    /**
     * Return the indexes of elements containing the field given in input
     * @param cls the class containing the field
     * @param field the field
     * @return list of indexes
     */
    public List<Integer> getElementsContainingField(String cls, String field) {
        List<Integer> indexElementsContainingField = new ArrayList<Integer>();
        ClassDescriptor cd;
        for (int index = 0; index < elements.size(); index++) {
            if (elements.get(index).equals(field)) {
                cd = getElementClassDescriptors().get(index);
                if (cd.getSimpleName().equals(cls)) {
                    indexElementsContainingField.add(index);
                } else {
                    for (String superClass : cd.getAllSuperclassNames()) {
                        if (superClass.equals(cls)) {
                            indexElementsContainingField.add(index);
                            break;
                        }
                    }
                }
            }
        }
        return indexElementsContainingField;
    }
}
