package org.intermine.metadata;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.util.StringUtil;
import org.intermine.util.TextTable;
import org.intermine.util.TypeUtil;

/**
 * Describe a business model class.  Gives access to attribute, reference and collection
 * descriptors.  Includes primary key information.
 *
 * @author Richard Smith
 */
public class ClassDescriptor implements Comparable<ClassDescriptor>
{
    private static final String INTERMINEOBJECT_NAME = "org.intermine.model.InterMineObject";

    private final String className;        // name of this class

    // the supers string passed to the constructor
    private String origSuperNames;

    // supers set after redundant super classes have been removed
    private final Set<String> superNames = new LinkedHashSet<String>();
    private final Set<ClassDescriptor> superDescriptors = new LinkedHashSet<ClassDescriptor>();
    private ClassDescriptor superclassDescriptor;

    private final boolean isInterface;
    private final Set<AttributeDescriptor> attDescriptors;
    private final Set<ReferenceDescriptor> refDescriptors;
    private final Set<CollectionDescriptor> colDescriptors;
    private final Map<String, FieldDescriptor> fieldDescriptors
        = new LinkedHashMap<String, FieldDescriptor>();
    private Map<String, FieldDescriptor> allFieldDescriptors
        = new LinkedHashMap<String, FieldDescriptor>();

    private Model model;  // set when ClassDescriptor added to DescriptorRespository
    private boolean modelSet = false;

    /**
     * Construct a ClassDescriptor.
     * @param name the fully qualified name of the described class
     * @param supers a space string of fully qualified interface and superclass names
     * @param isInterface true if describing an interface
     * @param atts a Collection of AttributeDescriptors
     * @param refs a Collection of ReferenceDescriptors
     * @param cols a Collection of CollectionDescriptors
     * @throws IllegalArgumentException if fields are null
     */
    public ClassDescriptor(String name, String supers,
            boolean isInterface, Set<AttributeDescriptor> atts, Set<ReferenceDescriptor> refs,
            Set<CollectionDescriptor> cols) throws IllegalArgumentException {

        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("'name' parameter must be a valid String");
        }
        // Java only accepts names that start with a character, $ or _, some characters
        // not allowed anywhere in name.
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            throw new IllegalArgumentException("Java field names must start with a character, "
                                               + "'$' or '_' but class name was: " + name);
        }
        String unqualified = name.substring(name.lastIndexOf('.') + 1);
        for (int i = 0; i < unqualified.length(); i++) {
            if (!Character.isJavaIdentifierPart(unqualified.charAt(i))) {
                throw new IllegalArgumentException("Java field names may not contain character: "
                                                   + unqualified.charAt(i)
                                                   + " but class name was: " + unqualified);
            }
        }
        this.className = name;

        if (supers != null && supers.equals("")) {
            throw new IllegalArgumentException("'supers' parameter must be null or a valid"
                    + " list of interface or superclass names");
        }

        if (supers == null) {
            this.origSuperNames = "";
        } else {
            this.origSuperNames = supers;
        }

        if (supers != null) {
            superNames.addAll(StringUtil.tokenize(supers));
        } else if (!INTERMINEOBJECT_NAME.equals(name)) {
            superNames.add(INTERMINEOBJECT_NAME);
        }

        this.isInterface = isInterface;

        // build maps of names to FieldDescriptors

        attDescriptors = new LinkedHashSet<AttributeDescriptor>(atts);
        refDescriptors = new LinkedHashSet<ReferenceDescriptor>(refs);
        colDescriptors = new LinkedHashSet<CollectionDescriptor>(cols);

        List<FieldDescriptor> fieldDescriptorList = new ArrayList<FieldDescriptor>();
        fieldDescriptorList.addAll(atts);
        fieldDescriptorList.addAll(refs);
        fieldDescriptorList.addAll(cols);

        for (FieldDescriptor fieldDescriptor : fieldDescriptorList) {
            try {
                fieldDescriptor.setClassDescriptor(this);
                fieldDescriptors.put(fieldDescriptor.getName(), fieldDescriptor);
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("FieldDescriptor '" + fieldDescriptor.getName()
                                                   + "' has already had ClassDescriptor set");
            }
        }
    }

    /**
     * Returns the fully qualified class name described by this ClassDescriptor.
     * @return qualified name of the described Class
     */
    public String getName() {
        return className;
    }

    /**
     * Returns the Class described by this ClassDescriptor.
     * @return a Class
     */
    public Class getType() {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find class for class descriptor", e);
        }
    }

    /**
     * Return set of superclass class names. The set will never contain
     * "org.intermine.model.InterMineObject".
     * @return set of superclass class names
     */
    public Set<String> getSuperclassNames() {
        Set<String> copy = new LinkedHashSet<String>(superNames);
        copy.remove(INTERMINEOBJECT_NAME);
        return copy;
    }

    /**
     * Returns unqualified name of class described by this ClassDescriptor.
     * @return unqualified name of the described Class
     */
    public String getUnqualifiedName() {
        return TypeUtil.unqualifiedName(className);
    }

    /**
     * Gets the FieldDescriptors for this class (but not superclasses)
     * @return set of FieldDescriptors
     */
    public Set<FieldDescriptor> getFieldDescriptors() {
        return new LinkedHashSet<FieldDescriptor>(fieldDescriptors.values());
    }

    /**
     * Gets the FieldDescriptors for this class and all superclasses and interfaces.
     * @return set of FieldDescriptors
     */
    public Set<FieldDescriptor> getAllFieldDescriptors() {
        return new LinkedHashSet<FieldDescriptor>(allFieldDescriptors.values());
    }

    /**
     * Sets up the object a little.
     *
     * @throws MetaDataException if something goes wrong
     */
    protected void setAllFieldDescriptors() throws MetaDataException {
        allFieldDescriptors = findAllFieldDescriptors();
    }

    private LinkedHashMap<String, FieldDescriptor> findAllFieldDescriptors()
    throws MetaDataException {
        LinkedHashMap<String, FieldDescriptor> map
            = new LinkedHashMap<String, FieldDescriptor>(fieldDescriptors);
        for (ClassDescriptor superDesc : superDescriptors) {
            Map<String, FieldDescriptor> toAdd = superDesc.findAllFieldDescriptors();
            for (FieldDescriptor fd : toAdd.values()) {
                FieldDescriptor fdAlready = map.get(fd.getName());
                if ((fdAlready != null) && (fd != fdAlready)) {
                    if (!((fd instanceof AttributeDescriptor)
                                && (fdAlready instanceof AttributeDescriptor)
                                && (((AttributeDescriptor) fd).getType()
                                    .equals(((AttributeDescriptor) fdAlready).getType())))) {
                        throw new MetaDataException("Incompatible similarly named fields ("
                                                    + fd.getName() + ") inherited"
                                + " from multiple superclasses and interfaces in " + getName());
                    }
                } else {
                    map.put(fd.getName(), fd);
                }
            }
        }
        return map;
    }

    /**
     * Retrieve a FieldDescriptor by name. The class and all superclasses and interfaces are
     * searched.
     *
     * @param name the name
     * @return the FieldDescriptor
     */
    public FieldDescriptor getFieldDescriptorByName(String name) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        return allFieldDescriptors.get(name);
    }

    /**
     * Gets AttributeDescriptors for this class - i.e. fields that are not references or
     * collections.
     * @return set of attributes for this Class
     */
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return attDescriptors;
    }

    /**
     * Gets all AttributeDescriptors for this class and its super classes - i.e. fields that are
     * not references or collections.
     * @return set of attributes for this Class
     */
    public Set<AttributeDescriptor> getAllAttributeDescriptors() {
        Set<AttributeDescriptor> set = new LinkedHashSet<AttributeDescriptor>();
        for (FieldDescriptor fd : getAllFieldDescriptors()) {
            if (fd instanceof AttributeDescriptor) {
                set.add((AttributeDescriptor) fd);
            }
        }
        return set;
    }

    /**
     * Gets the descriptors for the external object references in this class.
     * @return a Set of ReferenceDescriptors for this Class
     */
    public Set<ReferenceDescriptor> getReferenceDescriptors() {
        return refDescriptors;
    }

    /**
     * Gets all ReferenceDescriptors for this class - i.e. including those from superclass
     * @return a Set of references (but not CollectionDescriptors) for this Class
     */
    public Set<ReferenceDescriptor> getAllReferenceDescriptors() {
        Set<ReferenceDescriptor> set = new LinkedHashSet<ReferenceDescriptor>();
        for (FieldDescriptor fd : getAllFieldDescriptors()) {
            if (fd.isReference()) {
                set.add((ReferenceDescriptor) fd);
            }
        }
        return set;
    }

    /**
     * Gets a ReferenceDescriptor for a field of the given name.  Returns null if
     * not found. Does NOT look in any superclasses or interfaces.
     * @param name the name of a ReferenceDescriptor to find
     * @return a ReferenceDescriptor
     */
    public ReferenceDescriptor getReferenceDescriptorByName(String name) {
        return getReferenceDescriptorByName(name, false);
    }

    /**
     * Gets a ReferenceDescriptor for a field of the given name.  Returns null if
     * not found.  If ascend flag is true will also look in superclasses.
     * @param name the name of a ReferenceDescriptor to find
     * @param ascend if true search in super class hierarchy
     * @return a ReferenceDescriptor
     */
    public ReferenceDescriptor getReferenceDescriptorByName(String name, boolean ascend) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        Map<String, FieldDescriptor> map = null;
        if (ascend) {
            map = allFieldDescriptors;
        } else {
            map = fieldDescriptors;
        }
        FieldDescriptor fd = map.get(name);
        if ((fd != null) && (fd instanceof ReferenceDescriptor)
                && (!(fd instanceof CollectionDescriptor))) {
            return (ReferenceDescriptor) fd;
        }
        return null;
    }

    /**
     * Gets an AttributeDescriptor for a field of the given name.  Returns null if
     * not found. Does NOT look in any superclasses or interfaces.
     * @param name the name of an AttributeDescriptor to find
     * @return an AttributeDescriptor
     */
    public AttributeDescriptor getAttributeDescriptorByName(String name) {
        return getAttributeDescriptorByName(name, false);
    }

    /**
     * Gets an AttributeDescriptor for a field of the given name.  Returns null if
     * not found.  If ascend flag is true will also look in superclasses.
     * @param name the name of an AttributeDescriptor to find
     * @param ascend if true search in super class hierarchy
     * @return an AttributeDescriptor
     */
    public AttributeDescriptor getAttributeDescriptorByName(String name, boolean ascend) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        Map<String, FieldDescriptor> map = null;
        if (ascend) {
            map = allFieldDescriptors;
        } else {
            map = fieldDescriptors;
        }
        FieldDescriptor fd = map.get(name);
        if ((fd != null) && (fd instanceof AttributeDescriptor)) {
            return (AttributeDescriptor) fd;
        }
        return null;
    }

    private void configureReferenceDescriptors() throws MetaDataException {
        // ReferenceDescriptors need to find a ClassDescriptor for their referenced class
        for (ReferenceDescriptor rfd : refDescriptors) {
            rfd.findReferencedDescriptor();
        }

        // CollectionDescriptors need to find a ClassDescriptor for their referenced class
        for (CollectionDescriptor cod : colDescriptors) {
            cod.findReferencedDescriptor();
        }
    }

    /**
     * Gets all CollectionDescriptors for this class - i.e. including those from superclass
     * @return set of collections for this Class
     */
    public Set<CollectionDescriptor> getAllCollectionDescriptors() {
        Set<CollectionDescriptor> set = new LinkedHashSet<CollectionDescriptor>();
        for (FieldDescriptor fd : getAllFieldDescriptors()) {
            if (fd instanceof CollectionDescriptor) {
                set.add((CollectionDescriptor) fd);
            }
        }
        return set;
    }

    /**
     * Gets CollectionDescriptors for this class.
     * @return set of CollectionDescriptors for this Class
     */
    public Set<CollectionDescriptor> getCollectionDescriptors() {
        return colDescriptors;
    }

    /**
     * Gets a CollectionDescriptor for a collection of the given name.  Returns null if
     * not found. Does NOT search in any superclasses or interfaces.
     * @param name the name of a CollectionDescriptor to find
     * @return a CollectionDescriptor
     */
    public CollectionDescriptor getCollectionDescriptorByName(String name) {
        return getCollectionDescriptorByName(name, false);
    }

    /**
     * Gets a CollectionDescriptor for a field of the given name.  Returns null if
     * not found.  If ascend flag is true will also look in superclasses.
     * @param name the name of an CollectionDescriptor to find
     * @param ascend if true search in super class hierarchy
     * @return an CollectionDescriptor
     */
    public CollectionDescriptor getCollectionDescriptorByName(String name, boolean ascend) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        Map<String, FieldDescriptor> map = null;
        if (ascend) {
            map = allFieldDescriptors;
        } else {
            map = fieldDescriptors;
        }
        FieldDescriptor fd = map.get(name);
        if (fd instanceof CollectionDescriptor) {
            return (CollectionDescriptor) fd;
        }
        return null;
    }



    /**
     * Get the name of the super class of this class (may be null)
     * @return the super class name
     * @throws IllegalStateException if model not set
     */
    public ClassDescriptor getSuperclassDescriptor() {
        checkModel();
        return superclassDescriptor;
    }

    private void findSuperclassDescriptor() throws MetaDataException {
        // descriptor for super class
        for (ClassDescriptor cld : superDescriptors) {
            if (!cld.isInterface()) {
                if (this.isInterface()) {
                    throw new MetaDataException("An interface (" + this
                            + " may not have a superclass (" + cld + ")");
                }
                if (superclassDescriptor != null) {
                    throw new MetaDataException("Cannot have multiple superclasses for: " + this);
                }
                superclassDescriptor = cld;
            }
        }
    }

    /**
     * Get a set of ClassDescriptors for the interfaces superclasses that this class implements.
     * The set contains all direct superclasses and interfaces, and may contain some indirect
     * superclasses or interfaces.
     *
     * @return a Set of ClassDescriptors
     * @throws IllegalStateException if the model is not set
     */
    public Set<ClassDescriptor> getSuperDescriptors() {
        checkModel();
        return superDescriptors;
    }

    /**
     * True if this class is an interface.
     * @return true if an interface
     */
    public boolean isInterface() {
        return isInterface;
    }

    private void findSuperDescriptors() throws MetaDataException {
        // descriptors for superclasses and interfaces
        if (superNames.size() > 0) {
            for (String superName : superNames) {
                if (!model.hasClassDescriptor(superName)) {
                    throw new MetaDataException("No ClassDescriptor for superclass or interface ( "
                            + superName + ") found in model.");
                }
                ClassDescriptor superDescriptor = model.getClassDescriptorByName(superName);
                superDescriptors.add(superDescriptor);
            }
        }
    }

    /**
     * Return a Set of ClassDescriptors for all classes that directly extend or implement this class
     * or interface.
     * @return set of subclass ClassDescriptors
     * @throws IllegalStateException if the set of subclasses has not been set
     */
    public Set<ClassDescriptor> getSubDescriptors() throws IllegalStateException {
        checkModel();
        return model.getDirectSubs(this);
    }

    /**
     * Set the model for this ClassDescriptor, this is only be called once and will
     * throw an Exception if called again.  Is called by Model when the ClassDescriptor
     * is added to it during metadata creation.
     * @param model the parent model for this ClassDescriptor
     * @throws IllegalStateException if the model is already set
     * @throws MetaDataException if references not found
     */
    protected void setModel(Model model) throws IllegalStateException, MetaDataException  {
        if (modelSet) {
            throw new IllegalStateException("Model has already been set and may not be changed.");
        }
        this.model = model;

        Set<String> allSupers = new LinkedHashSet<String>();
        findSuperClassNames(model, className, allSupers);
        if (allSupers.contains(className)) {
            throw new MetaDataException("circular dependency: " + className
                                        + " is a super class of itself");
        }

        List<String> redundantSupers = new ArrayList<String>();

        if (superNames.size() > 0) {
            for (String superName : superNames) {
                for (String otherSuperName : superNames) {
                    if (superName.equals(otherSuperName)) {
                        continue;
                    }
                    int testResult = classInheritanceCompare(model, superName, otherSuperName);

                    if (testResult == 0) {
                        // incomparable neither super is a sub class of the other
                        continue;
                    } else {
                        if (testResult == -1) {
                            // superName is a super class of otherSuperName
                            redundantSupers.add(superName);
                        } else {
                            // otherSuperName is a super class of superName
                            redundantSupers.add(otherSuperName);
                        }
                    }
                }
            }
        }

        superNames.removeAll(redundantSupers);

        findSuperDescriptors();
        findSuperclassDescriptor();
        configureReferenceDescriptors();

        modelSet = true;
    }

    /**
     * Return -1 if superName names a class that is a super class of otherSuperName, 1 if
     * otherSuperName names a class that is a super class of superName, 0 if they neither is a super
     * class of the other.
     * @param model the Model to use to find super classes
     * @param className1 a super class name
     * @param className2 a super class name
     * @throws MetaDataException of superName names a class that is a super class of otherSuperName
     * and otherSuperName names a class that is a super class of superName - ie. a circular
     * dependency
     * @return -1, 1, or 0
     */
    static int classInheritanceCompare(Model model, String className1, String className2)
        throws MetaDataException {
        Set<String> class1Supers = new LinkedHashSet<String>();
        findSuperClassNames(model, className1, class1Supers);
        Set<String> class2Supers = new LinkedHashSet<String>();
        findSuperClassNames(model, className2, class2Supers);
        boolean class1InClass2Supers = class2Supers.contains(className1);
        boolean class2InClass1Supers = class1Supers.contains(className2);

        if (class1InClass2Supers) {
            if (class2InClass1Supers) {
                throw new MetaDataException("circular dependency: " + className1
                                            + " is a super class of " + className2
                                            + " and vice versa");
            } else {
                return -1;
            }
        } else {
            if (class2InClass1Supers) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Return a list of the super class names for the given class name.  The search is performed
     * breadth-first and the returned Set is a LinkedHashSet so the direct super class names will
     * be first in the list.
     * @param model the Model
     * @param className the className
     * @return set of super class names
     * @throws MetaDataException if className isn't in the model
     */
    static public Set<String> findSuperClassNames(Model model, String className)
        throws MetaDataException {
        Set<String> superClassNames = new LinkedHashSet<String>();
        findSuperClassNames(model, className, superClassNames);
        return superClassNames;
    }
    /**
     * Return a list of the super class names for the given class name
     * @param model the Model
     * @param className the className
     * @param superClassNames return set of super class names
     * @throws MetaDataException if className isn't in the model
     */
    static void findSuperClassNames(Model model, String className,
                                    Set<String> superClassNames) throws MetaDataException {
        ClassDescriptor cd = model.getClassDescriptorByName(className);
        if (cd == null) {
            throw new MetaDataException("Model construction failed - class: " + className
                                        + " is not in the model but is used as a super class");
        }
        for (String superName: cd.getSuperclassNames()) {
            if (superClassNames.contains(superName)) {
                continue;
            } else {
                superClassNames.add(superName);
                findSuperClassNames(model, superName, superClassNames);
            }
        }
    }

    /**
     * Return the model this class is a part of
     * @return the parent Model
     */
    public Model getModel() {
        return model;
    }

    private void checkModel() {
        if (!modelSet) {
            throw new IllegalArgumentException("ClassDescriptor '" + getName()
                                               + "' has not been added to a Model");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof ClassDescriptor) {
            ClassDescriptor cld = (ClassDescriptor) obj;
            return className.equals(cld.className)
                && superNames.equals(cld.superNames)
                && isInterface == cld.isInterface
                && fieldDescriptors.equals(cld.fieldDescriptors);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return 3 * className.hashCode()
            + 7 * origSuperNames.hashCode()
            + 11 * (isInterface ? 1 : 0)
            + 13 * fieldDescriptors.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(ClassDescriptor cld) {
        int retval = className.compareTo(cld.className);
        if (retval == 0) {
            retval = superNames.toString().compareTo(cld.superNames.toString());
        }
        if (retval == 0) {
            retval = (isInterface ? 1 : 0) - (cld.isInterface ? 1 : 0);
        }
        if (retval == 0) {
            retval = fieldDescriptors.hashCode() - cld.fieldDescriptors.hashCode();
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        Set<String> superClassNames = getSuperclassNames();
        sb.append("<class name=\"" + className + "\"")
            .append(superClassNames.size() > 0
                    ? " extends=\"" + StringUtil.join(superClassNames, " ") + "\""
                    : "")
            .append(" is-interface=\"" + isInterface + "\">");
        Set<FieldDescriptor> l = new LinkedHashSet<FieldDescriptor>();
        l.addAll(getAttributeDescriptors());
        l.addAll(getReferenceDescriptors());
        l.addAll(getCollectionDescriptors());
        for (FieldDescriptor fd : l) {
            sb.append(fd.toString());
        }
        sb.append("</class>");
        return sb.toString();
    }

    /**
     * Returns a String that contains a multi-line human-readable description of the
     * ClassDescriptor.
     *
     * @return a String
     */
    public String getHumanReadableText() {
        StringBuffer retval = new StringBuffer(isInterface ? "Interface " : "Class ")
            .append(terseClass(className));
        if (superNames != null) {
            retval.append(" extends ").append(StringUtil.join(terseClasses(superNames), ", "));
        }
        retval.append("\n");
        TextTable table = new TextTable(true, true, true);
        table.addRow(TextTable.ROW_SEPARATOR);
        for (AttributeDescriptor desc : getAllAttributeDescriptors()) {
            ClassDescriptor cld = desc.getClassDescriptor();
            table.addRow(new String[] {desc.getName(), terseClass(desc.getType()),
                (cld == this ? "" : "from " + terseClass(cld.getName()))});
        }
        table.addRow(TextTable.ROW_SEPARATOR);
        for (ReferenceDescriptor desc : getAllReferenceDescriptors()) {
            ClassDescriptor cld = desc.getClassDescriptor();
            table.addRow(new String[] {desc.getName(), terseClass(desc.getReferencedClassName()),
                (cld == this ? "" : "from " + terseClass(cld.getName()))});
        }
        table.addRow(TextTable.ROW_SEPARATOR);
        for (CollectionDescriptor desc : getAllCollectionDescriptors()) {
            ClassDescriptor cld = desc.getClassDescriptor();
            table.addRow(new String[] {desc.getName(), "collection of "
                + terseClass(desc.getReferencedClassName()),
                (cld == this ? "" : "from " + terseClass(cld.getName()))});
        }
        table.addRow(TextTable.ROW_SEPARATOR);
        retval.append(table.toString());
        return retval.toString();
    }

    /**
     * Strips everything before the last dot out of a String.
     *
     * @param c a String
     * @return a String
     */
    public static String terseClass(String c) {
        int p = c.lastIndexOf('.');
        if (p != -1) {
            return c.substring(p + 1);
        }
        return c;
    }

    /**
     * Return a new List that contains everything in the argument but strips everything before the
     * last dot out.
     * @param list the List
     * @return the compact List
     */
    private static Set<String> terseClasses(Set<String> list) {
        Set<String> retList = new LinkedHashSet<String>();
        for (String name: list) {
            retList.add(terseClass(name));
        }
        return retList;
    }
}
