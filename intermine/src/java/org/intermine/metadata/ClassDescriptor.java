package org.intermine.metadata;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;

/**
 * Describe a business model class.  Gives access to attribute, reference and collection
 * descriptors.  Includes primary key information.
 *
 * @author Richard Smith
 */
public class ClassDescriptor
{
    private final String name;        // name of this class

    private final String supers;
    private final Set superNames = new LinkedHashSet();
    private final Set superDescriptors = new LinkedHashSet();
    private ClassDescriptor superclassDescriptor;

    private final boolean isInterface;
    private final Set attDescriptors;
    private final Set refDescriptors;
    private final Set colDescriptors;
    private final Map fieldDescriptors = new HashMap();
    private Map allFieldDescriptors = new LinkedHashMap();

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
            boolean isInterface, Set atts, Set refs, Set cols)
        throws IllegalArgumentException {

        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("'name' parameter must be a valid String");
        }

        this.name = name;

        if (supers != null && supers.equals("")) {
            throw new IllegalArgumentException("'supers' parameter must be null or a valid"
                    + " list of interface or superclass names");
        }

        this.supers = supers;

        if (supers != null) {
            superNames.addAll(StringUtil.tokenize(supers));
        } else if (!"org.intermine.model.InterMineObject".equals(name)) {
            superNames.add("org.intermine.model.InterMineObject");
        }

        this.isInterface = isInterface;

        attDescriptors = new LinkedHashSet(atts);
        refDescriptors = new LinkedHashSet(refs);
        colDescriptors = new LinkedHashSet(cols);

        // build maps of names to FieldDescriptors

        Set fieldDescriptorSet = new LinkedHashSet();
        fieldDescriptorSet.addAll(atts);
        fieldDescriptorSet.addAll(refs);
        fieldDescriptorSet.addAll(cols);

        Iterator fieldDescriptorIter = fieldDescriptorSet.iterator();
        while (fieldDescriptorIter.hasNext()) {
            FieldDescriptor fieldDescriptor = (FieldDescriptor) fieldDescriptorIter.next();
            try {
                fieldDescriptor.setClassDescriptor(this);
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("FieldDescriptor '" + fieldDescriptor.getName()
                                                   + "' has already had ClassDescriptor set");
            }
            fieldDescriptors.put(fieldDescriptor.getName(), fieldDescriptor);
        }
    }

    /**
     * Returns the fully qualified class name described by this ClassDescriptor.
     * @return qualified name of the described Class
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Class described by this ClassDescriptor.
     * @return a Class
     */
    public Class getType() {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Can't find class for class descriptor", e);
        }
    }

    /**
     * Returns unqualified name of class described by this ClassDescriptor.
     * @return unqualified name of the described Class
     */
    public String getUnqualifiedName() {
        return TypeUtil.unqualifiedName(name);
    }

    /**
     * Gets the FieldDescriptors for this class (but not superclasses)
     * @return set of FieldDescriptors
     */
    public Set getFieldDescriptors() {
        return new LinkedHashSet(fieldDescriptors.values());
    }

    /**
     * Gets the FieldDescriptors for this class and all superclasses and interfaces.
     * @return set of FieldDescriptors
     */
    public Set getAllFieldDescriptors() {
        return new LinkedHashSet(allFieldDescriptors.values());
    }

    /**
     * Sets up the object a little.
     *
     * @throws MetaDataException if something goes wrong
     */
    protected void setAllFieldDescriptors() throws MetaDataException {
        allFieldDescriptors = findAllFieldDescriptors();
    }

    private LinkedHashMap findAllFieldDescriptors() throws MetaDataException {
        LinkedHashMap map = new LinkedHashMap(fieldDescriptors);
        Iterator superIter = superDescriptors.iterator();
        while (superIter.hasNext()) {
            Map toAdd = ((ClassDescriptor) superIter.next()).findAllFieldDescriptors();
            Iterator addIter = toAdd.values().iterator();
            while (addIter.hasNext()) {
                FieldDescriptor fd = (FieldDescriptor) addIter.next();
                FieldDescriptor fdAlready = (FieldDescriptor) map.get(fd.getName());
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
        return (FieldDescriptor) allFieldDescriptors.get(name);
    }

    /**
     * Gets AttributeDescriptors for this class - i.e. fields that are not references or
     * collections.
     * @return set of attributes for this Class
     */
    public Set getAttributeDescriptors() {
        return attDescriptors;
    }

    /**
     * Gets all AttributeDescriptors for this class - i.e. fields that are not references or
     * collections.
     * @return set of attributes for this Class
     */
    public Set getAllAttributeDescriptors() {
        Set set = new LinkedHashSet(getAllFieldDescriptors());
        Iterator fieldIter = set.iterator();
        while (fieldIter.hasNext()) {
            if (!(fieldIter.next() instanceof AttributeDescriptor)) {
                fieldIter.remove();
            }
        }
        return set;
    }

    /**
     * Gets the descriptors for the external object references in this class.
     * @return set ReferenceDescriptors for this Class
     */
    public Set getReferenceDescriptors() {
        return refDescriptors;
    }

    /**
     * Gets all ReferenceDescriptors for this class - i.e. including those from superclass
     * @return set of references for this Class
     */
    public Set getAllReferenceDescriptors() {
        Set set = new LinkedHashSet(getAllFieldDescriptors());
        Iterator fieldIter = set.iterator();
        while (fieldIter.hasNext()) {
            if (!(((FieldDescriptor) fieldIter.next()).isReference())) {
                fieldIter.remove();
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
        Map map = null;
        if (ascend) {
            map = allFieldDescriptors;
        } else {
            map = fieldDescriptors;
        }
        if (map.containsKey(name)
            && map.get(name) instanceof ReferenceDescriptor
            && !(map.get(name) instanceof CollectionDescriptor)) {
            return (ReferenceDescriptor) map.get(name);
        } else {
            return null;
        }
    }

    /**
     * Gets an AttributeDescriptor for a field of the given name.  Returns null if
     * not found. Does NOT look in any superclasses or interfaces.
     * @param name the name of an AttributeDescriptor to find
     * @return an AttributeDescriptor
     */
    public AttributeDescriptor getAttributeDescriptorByName(String name) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        if (fieldDescriptors.containsKey(name)
            && fieldDescriptors.get(name) instanceof AttributeDescriptor) {
            return (AttributeDescriptor) fieldDescriptors.get(name);
        } else {
            return null;
        }
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
        Map map = null;
        if (ascend) {
            map = allFieldDescriptors;
        } else {
            map = fieldDescriptors;
        }
        if (map.containsKey(name)
            && map.get(name) instanceof AttributeDescriptor) {
            return (AttributeDescriptor) map.get(name);
        } else {
            return null;
        }
    }

    private void configureReferenceDescriptors() throws MetaDataException {
        // ReferenceDescriptors need to find a ClassDescriptor for their referenced class
        Iterator refIter = refDescriptors.iterator();
        while (refIter.hasNext()) {
            ReferenceDescriptor rfd = (ReferenceDescriptor) refIter.next();
            rfd.findReferencedDescriptor();
        }

        // CollectionDescriptors need to find a ClassDescriptor for their referenced class
        Iterator colIter = colDescriptors.iterator();
        while (colIter.hasNext()) {
            CollectionDescriptor cod = (CollectionDescriptor) colIter.next();
            cod.findReferencedDescriptor();
        }
    }

    /**
     * Gets all CollectionDescriptors for this class - i.e. including those from superclass
     * @return set of collections for this Class
     */
    public Set getAllCollectionDescriptors() {
        Set set = new LinkedHashSet(getAllFieldDescriptors());
        Iterator fieldIter = set.iterator();
        while (fieldIter.hasNext()) {
            if (!(fieldIter.next() instanceof CollectionDescriptor)) {
                fieldIter.remove();
            }
        }
        return set;
    }

    /**
     * Gets CollectionDescriptors for this class.
     * @return set of CollectionDescriptors for this Class
     */
    public Set getCollectionDescriptors() {
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
        Map map = null;
        if (ascend) {
            map = allFieldDescriptors;
        } else {
            map = fieldDescriptors;
        }
        if (map.containsKey(name)
            && map.get(name) instanceof CollectionDescriptor) {
            return (CollectionDescriptor) map.get(name);
        } else {
            return null;
        }
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
        Iterator superIter = superDescriptors.iterator();
        while (superIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) superIter.next();
            if (!cld.isInterface()) {
                if (this.isInterface()) {
                    throw new MetaDataException("Cannot have a superclass of an interface");
                }
                if (superclassDescriptor != null) {
                    throw new MetaDataException("Cannot have multiple superclasses");
                }
                superclassDescriptor = cld;
            }
        }
    }

    /**
     * Get a set of ClassDescriptors for the interfaces superclasses that this class implements.
     * @return a Set of ClassDescriptors
     * @throws IllegalStateException if the model is not set
     */
    public Set getSuperDescriptors() {
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
            Iterator iter = superNames.iterator();
            while (iter.hasNext()) {
                String superName = (String) iter.next();
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
    public Set getSubDescriptors() throws IllegalStateException {
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
            throw new IllegalStateException("Model has already been set and "
                                            + "may not be changed.");
        }
        this.model = model;
        findSuperDescriptors();
        findSuperclassDescriptor();
        configureReferenceDescriptors();

        modelSet = true;
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
     * @see Object#equals
     */
    public boolean equals(Object obj) {
        if (obj instanceof ClassDescriptor) {
            ClassDescriptor cld = (ClassDescriptor) obj;
            return name.equals(cld.name)
                && superNames.equals(cld.superNames)
                && isInterface == cld.isInterface
                && fieldDescriptors.equals(cld.fieldDescriptors);
        }
        return false;
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return 3 * name.hashCode()
            + 7 * superNames.hashCode()
            + 11 * (isInterface ? 1 : 0)
            + 13 * fieldDescriptors.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<class name=\"" + name + "\"")
            .append(supers != null ? " extends=\"" + supers + "\"" : "")
            .append(" is-interface=\"" + isInterface + "\">");
        Set l = new LinkedHashSet();
        l.addAll(getAttributeDescriptors());
        l.addAll(getReferenceDescriptors());
        l.addAll(getCollectionDescriptors());
        for (Iterator iter = l.iterator(); iter.hasNext();) {
            sb.append(iter.next().toString());
        }
        sb.append("</class>");
        return sb.toString();
    }
}
