package org.flymine.metadata;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import java.util.Map;
import java.util.Set;

import org.flymine.util.StringUtil;
import org.flymine.util.Util;

import org.apache.log4j.Logger;

/**
 * Describe a business model class.  Gives access to attribute, reference and collection
 * descriptors.  Includes primary key information.
 *
 * @author Richard Smith
 */
public class ClassDescriptor
{
    protected static final Logger LOG = Logger.getLogger(ClassDescriptor.class);

    private final String name;        // name of this class

    private final String superclassName;
    private ClassDescriptor superclassDescriptor;

    private final String interfaces;
    private final Set interfaceNames = new LinkedHashSet();
    private final Set interfaceDescriptors = new LinkedHashSet();

    private ClassDescriptor ultimateSuperclassDesc;
    private boolean ultimateSuperSet = false;

    private final boolean isInterface;
    private final Set attDescriptors;
    private final Set refDescriptors;
    private final Set colDescriptors;
    private final Map fieldDescriptors = new HashMap();
    private final Set pkFields = new LinkedHashSet();

    private Model model;  // set when ClassDescriptor added to DescriptorRespository
    private boolean modelSet = false;

    private Set subclassDescriptors;
    private boolean subSet = false;

    private Set implementorDescriptors;
    private boolean implSet = false;

    /**
     * Construct a ClassDescriptor.
     * @param name the fully qualified name of the described class
     * @param superclassName the fully qualified super class name if one exists
     * @param interfaces a space string of fully qualified interface names
     * @param isInterface true if describing an interface
     * @param atts a Collection of AttributeDescriptors
     * @param refs a Collection of ReferenceDescriptors
     * @param cols a Collection of CollectionDescriptors
     * @throws IllegalArgumentException if fields are null
     */
    public ClassDescriptor(String name, String superclassName, String interfaces,
            boolean isInterface, Set atts, Set refs, Set cols)
        throws IllegalArgumentException {

        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("'name' parameter must be a valid String");
        }

        this.name = name;

        if (superclassName != null && superclassName.equals("")) {
            throw new IllegalArgumentException("'superclassName' parameter must be null"
                                               + " or a valid class name");
        }

        this.superclassName = superclassName;

        if (interfaces != null && interfaces.equals("")) {
            throw new IllegalArgumentException("'interfaces' parameter must be null"
                                               + " or a valid list of interface names");
        }

        this.interfaces = interfaces;

        if (interfaces != null) {
            interfaceNames.addAll(StringUtil.tokenize(interfaces));
        }

        this.isInterface = isInterface;

        attDescriptors = new LinkedHashSet(atts);
        refDescriptors = new LinkedHashSet(refs);
        colDescriptors = new LinkedHashSet(cols);

        // build maps of names to FieldDescriptors and populate pkFields set

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
            if (fieldDescriptor.isPrimaryKey()) {
                pkFields.add(fieldDescriptor);
            }
        }
    }

    /**
     * Returns the fully qualified class name described by this ClassDescriptor.
     * @return name of the described Class
     */
    public String getClassName() {
        return name;
    }

    /**
     * Get a set of primary key FieldDescriptors for this Class and all its superclasses.
     * Could be a combination of attributes and references (and collections).
     * @return set of primary key fields
     * @throws IllegalStateException if model has not been set
     */
    public Set getPkFieldDescriptors() {
        checkModel();
        Set allPkFields = new LinkedHashSet(pkFields);
        if (superclassDescriptor != null) {
            allPkFields.addAll(superclassDescriptor.getPkFieldDescriptors());
        }
        return allPkFields;
    }
    
    /**
     * Gets the FieldDescriptors for this class (but not subclasses)
     * @return set of FieldDescriptors
     */
    public Set getFieldDescriptors() {
        return new LinkedHashSet(fieldDescriptors.values());
    }

//     public Set getAllFieldDescriptors() {
//         if (superclassDescriptor == null) {
//             return getFieldDescriptors();
//         } else {
//             Set set = new LinkedHashSet(getFieldDescriptors());
//             set.addAll(superclassDescriptor.getAllFieldDescriptors());
//             return set;
//         }
//     }

    /**
     * Retrieve a FieldDescriptor by name
     * @param name the name
     * @return the FieldDescriptor
     */
    public FieldDescriptor getFieldDescriptorByName(String name) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        FieldDescriptor fd = (FieldDescriptor) fieldDescriptors.get(name);
        if (fd == null) {
            if (superclassDescriptor != null) {
                return superclassDescriptor.getFieldDescriptorByName(name);
            } else {
                throw new NullPointerException("ClassDescriptor '" + getClassName() 
                                               + "' has no field named '" + name + "'");
            }
        }
        return fd;
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
        if (superclassDescriptor == null) {
            return getAttributeDescriptors();
        } else {
            Set set = new LinkedHashSet(getAttributeDescriptors());
            set.addAll(superclassDescriptor.getAllAttributeDescriptors());
            return set;
        }
    }

    /**
     * Gets an AttributeDescriptor for a field of the given name.  Returns null if
     * not found.
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
        }
        return null;
    }

    /**
     * Gets the descriptors for the external object references in this class.
     * @return set ReferenceDescriptors for this Class
     */
    public Set getReferenceDescriptors() {
        return refDescriptors;
    }

    /**
     * Gets a ReferenceDescriptor for a field of the given name.  Returns null if
     * not found.
     * @param name the name of a ReferenceDescriptor to find
     * @return a ReferenceDescriptor
     */
    public ReferenceDescriptor getReferenceDescriptorByName(String name) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        if (fieldDescriptors.containsKey(name)
            && fieldDescriptors.get(name) instanceof ReferenceDescriptor) {
            return (ReferenceDescriptor) fieldDescriptors.get(name);
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

        // ReferenceDescriptors need to find a ClassDescriptor for their referenced class
        Iterator colIter = colDescriptors.iterator();
        while (colIter.hasNext()) {
            CollectionDescriptor cod = (CollectionDescriptor) colIter.next();
            cod.findReferencedDescriptor();
        }
    }

    /**
     * Gets all CollectionDescriptors for this class.
     * @return set of CollectionDescriptors for this Class
     */
    public Set getCollectionDescriptors() {
        return colDescriptors;
    }

    /**
     * Gets a CollectionDescriptor for a collection of the given name.  Returns null if
     * not found.
     * @param name the name of a CollectionDescriptor to find
     * @return a CollectionDescriptor
     */
    public CollectionDescriptor getCollectionDescriptorByName(String name) {
        if (name == null) {
            throw new NullPointerException("Argument 'name' cannot be null");
        }
        if (fieldDescriptors.containsKey(name)
            && fieldDescriptors.get(name) instanceof CollectionDescriptor) {
            return (CollectionDescriptor) fieldDescriptors.get(name);
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
        if (superclassName != null) {
            superclassDescriptor = model.getClassDescriptorByName(superclassName);
            if (superclassDescriptor == null) {
                throw new MetaDataException("No ClassDescriptor for super class: "
                                            + superclassName + " found in model.");
            }
            if (isInterface() != superclassDescriptor.isInterface()) {
                throw new MetaDataException("This class (" + getClassName()
                                            + (isInterface() ? ") is " : ") is not ")
                                            + "an interface but superclass ("
                                            + superclassDescriptor.getClassName()
                                            + (superclassDescriptor.isInterface() ? ") is."
                                               : ") is not."));
            }
        }
    }

    /**
     * Get a set of ClassDescriptors for each of the interfaces that this class implements.
     * @return a Set of descriptors for the interfaces this class implements
     * @throws IllegalStateException if the model is not set
     */
    public Set getInterfaceDescriptors() {
        checkModel();
        return interfaceDescriptors;
    }

    /**
     * True if this class is an interface.
     * @return true if an interface
     */
    public boolean isInterface() {
        return isInterface;
    }

    private void findInterfaceDescriptors() throws MetaDataException {
        // descriptors for interfaces
        if (interfaceNames.size() > 0) {
            Iterator iter = interfaceNames.iterator();
            while (iter.hasNext()) {
                String iName = (String) iter.next();
                if (!model.hasClassDescriptor(iName)) {
                    throw new MetaDataException("No ClassDescriptor for interface ( "
                                                + iName + ") found in model.");
                }
                ClassDescriptor iDescriptor = model.getClassDescriptorByName(iName);
                if (!iDescriptor.isInterface()) {
                    throw new MetaDataException("ClassDescriptor for ( " + iName
                                                + ") does not describe and interface.");
                }
                interfaceDescriptors.add(iDescriptor);
            }
        }
    }

    /**
     * Set set of ClassDescriptors that are direct subclasses of this class.
     * Called once during Model creation.
     * @param sub set of direct subclass descriptors
     * @throws IllegalStateException if subclasses already set
     */
    protected void setSubclassDescriptors(Set sub) {
        if (subSet) {
            throw new IllegalStateException("subclasses have already been set for this "
                                            + "ClassDescriptor (" + name + ").");
        }
        subclassDescriptors = new LinkedHashSet(sub);
        subSet = true;
    }

    /**
     * Return a Set of ClassDescriptors for all classes that extend this class
     * @return set of subclass ClassDescriptors
     * @throws IllegalStateException if the set of subclasses has not been set
     */
    public Set getSubclassDescriptors() throws IllegalStateException {
        if (!subSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet had subclass"
                                            + "Descriptors set.");
        }
        return subclassDescriptors;
    }

    /**
     * Set set of ClassDescriptors for classes that are direct implemetations
     * of this class, i.e. not subclasses of implentations.
     * @param impl set of direct implementations
     * @throws IllegalStateException if implementors already set
     */
    protected void setImplementorDescriptors(Set impl) {
        if (implSet) {
            throw new IllegalStateException("implementors have already been set for this "
                                            + "ClassDescriptor (" + name + ").");
        }

        if (isInterface()) {
            implementorDescriptors = impl;
        }
        implSet = true;
    }

   /**
     * Return a Set of ClassDescriptors for all classes that implement this interface
     * @return set of class that implement this class
     * @throws IllegalStateException if implementor descriptors have not been set
     */
    public Set getImplementorDescriptors() throws IllegalStateException {
        if (!implSet) {
            throw new IllegalStateException("This ClassDescriptor (" + getClassName() 
                                            + ") has not yet had implementor Descriptors set.");
        }
        return implementorDescriptors;
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
        findSuperclassDescriptor();
        findInterfaceDescriptors();
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
            throw new IllegalArgumentException("ClassDescriptor '" + getClassName()
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
                && Util.equals(superclassName, cld.superclassName)
                && interfaceNames.equals(cld.interfaceNames)
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
            + 5 * Util.hashCode(superclassName)
            + 7 * interfaceNames.hashCode()
            + 11 * (isInterface ? 1 : 0)
            + 13 * fieldDescriptors.hashCode();
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("<class name=\"" + name + "\"")
            .append(superclassName != null ?  " extends=\"" + superclassName + "\"" : "")
            .append(interfaces != null ? " implements=\"" + interfaces + "\"" : "")
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
