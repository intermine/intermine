package org.flymine.metadata;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.flymine.util.StringUtil;


/**
 * Describe a business model class.  Gives access to attribute, reference and collection
 * descriptors.  Includes primary key information.
 *
 * @author Richard Smith
 */


public class ClassDescriptor
{

    private String name;        // name of this class
    private String superclassName;
    private String interfaces;
    private ClassDescriptor superclassDescriptor;
    private List interfaceNames = new ArrayList();
    private List interfaceDescriptors = new ArrayList(); // names of interfaces

    private ClassDescriptor ultimateSuperclassDesc;
    private boolean ultimateSuperSet = false;

    private boolean isInterface;
    private List attDescriptors;
    private List refDescriptors;
    private List colDescriptors;
    private Map fieldDescriptors = new HashMap();

    private List pkFields = new ArrayList();
    private Model model;  // set when ClassDesriptor added to DescriptorRespository
    private boolean modelSet = false;
    private List subclassDescriptors;
    private boolean subSet = false;
    private List implementorDescriptors;
    private boolean implSet = false;

    /**
     * Construct a ClassDescriptor.
     * @param name the fully qualified name of the described class
     * @param superclassName the fully qualified super class name if one exists
     * @param interfaces a space string of fully qualified interface names
     * @param isInterface true if describing an interface
     * @param atts a list of AttributeDescriptors
     * @param refs a list of ReferenceDescriptors
     * @param cols a list of CollectionDescriptors
     * @throws IllegalArgumentException if fields are null
     */
    public ClassDescriptor(String name, String superclassName, String interfaces,
                              boolean isInterface, List atts, List refs, List cols)
        throws IllegalArgumentException {

        // must provide class name
        if (name == null || name == "") {
            throw new IllegalArgumentException("'name' parameter must be a valid String");
        }
        this.name = name;
        this.superclassName = superclassName;
        this.interfaces = interfaces;

        // split interface string into a Set
        if (interfaces != null && interfaces != "") {
            interfaceNames = StringUtil.tokenize(interfaces);
        }

        this.isInterface = isInterface;
        this.attDescriptors = atts;
        this.refDescriptors = refs;
        this.colDescriptors = cols;

        // build maps of names to FieldDescriptors and populate pkFields list

        Iterator attIter = attDescriptors.iterator();
        while (attIter.hasNext()) {
            AttributeDescriptor attDesc = (AttributeDescriptor) attIter.next();
            try {
                attDesc.setClassDescriptor(this);
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("AttributeDescriptor: " + attDesc.getName()
                                                   + "already has ClassDescriptor set.");
            }
            fieldDescriptors.put(attDesc.getName(), attDesc);
            if (attDesc.isPrimaryKey()) {
                this.pkFields.add(attDesc);
            }
        }

        Iterator refIter = refDescriptors.iterator();
        while (refIter.hasNext()) {
            ReferenceDescriptor refDesc = (ReferenceDescriptor) refIter.next();
            try {
                refDesc.setClassDescriptor(this);
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("ReferenceDescriptor: " + refDesc.getName()
                                                   + "already has ClassDescriptor set.");
            }
            fieldDescriptors.put(refDesc.getName(), refDesc);
            if (refDesc.isPrimaryKey()) {
                this.pkFields.add(refDesc);
            }
        }

        Iterator colIter = colDescriptors.iterator();
        while (colIter.hasNext()) {
            CollectionDescriptor colDesc = (CollectionDescriptor) colIter.next();
            try {
                colDesc.setClassDescriptor(this);
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("CollectionDescriptor: " + colDesc.getName()
                                                   + "already has ClassDescriptor set.");
            }
            fieldDescriptors.put(colDesc.getName(), colDesc);
            if (colDesc.isPrimaryKey()) {
                this.pkFields.add(colDesc);
            }
        }
    }


    /**
     * Returns the fully qualified class name described by this ClassDescriptor.
     * @return name of the described Class
     */
    public String getClassName() {
        return this.name;
    }


    /**
     * Get a list of primary key FieldDescriptors for this Class and all its superclasses.
     * Could be a combination of attributes and references (and collections).
     * @return list of primary key fields
     * @throws IllegalStateException if model has not been set
     */
    public List getPkFieldDescriptors() {
        if (!modelSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet been added "
                                            + "to a model.");
        }
        List allPkFields = new ArrayList(this.pkFields);
        List supers = getAllSuperclassDescriptors();
        Iterator superIter = supers.iterator();
        while (superIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) superIter.next();
            allPkFields.addAll(cld.pkFields);
        }
        return allPkFields;
    }

    /**
     * Retrieve a FieldDescriptor by name
     * @param name the name
     * @return the FieldDescriptor
     */
    public FieldDescriptor getFieldDescriptorByName(String name) {
        return (FieldDescriptor) fieldDescriptors.get(name);
    }

    /**
     * Gets all AttributeDescriptors for this class - i.e. fields that are not references or
     * collections.
     * @return list of attributes for this Class
     */
    public List getAttributeDescriptors() {
        return this.attDescriptors;
    }

    /**
     * Gets an AttributeDescriptor for a field of the given name.  Returns null if
     * not found.
     * @param name the name of an AttributeDescriptor to find
     * @return an AttributeDescriptor
     */
    public AttributeDescriptor getAttributeDescriptorByName(String name) {
        if (name == null) {
            return null;
        }
        if (fieldDescriptors.containsKey(name)
            && fieldDescriptors.get(name) instanceof AttributeDescriptor) {
            return (AttributeDescriptor) fieldDescriptors.get(name);
        } else {
            return null;
        }
    }

    /**
     * Gets the descriptors for the external object references in this class.
     * @return list ReferenceDescriptors for this Class
     */
    public List getReferenceDescriptors() {
        return this.refDescriptors;
    }

    /**
     * Gets a ReferenceDescriptor for a field of the given name.  Returns null if
     * not found.
     * @param name the name of a ReferenceDescriptor to find
     * @return a ReferenceDescriptor
     */
    public ReferenceDescriptor getReferenceDescriptorByName(String name) {
        if (name == null) {
            return null;
        }
        if (fieldDescriptors.containsKey(name)
            && fieldDescriptors.get(name) instanceof ReferenceDescriptor) {
            return (ReferenceDescriptor) fieldDescriptors.get(name);
        } else {
            return null;
        }
    }

    /**
     * Gets all CollectionDescriptors for this class.
     * @return list of CollectionDescriptors for this Class
     */
    public List getCollectionDescriptors() {
        return this.colDescriptors;
    }

    /**
     * Gets a CollectionDescriptor for a collection of the given name.  Returns null if
     * not found.
     * @param name the name of a CollectionDescriptor to find
     * @return a CollectionDescriptor
     */
    public CollectionDescriptor getCollectionDescriptorByName(String name) {
        if (name == null) {
            return null;
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
     */
    public ClassDescriptor getSuperclassDescriptor() {
        if (!modelSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet been added "
                                            + "to a model.");
        }
        return this.superclassDescriptor;
    }

    /**
     * Get a list of ClassDescriptors for each of the interfaces that this class implements.
     * @return a List of descriptors for the interfaces this class implements
     * @throws IllegalStateException if the model is not set
     */
    public List getInterfaceDescriptors() {
        if (!modelSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet been added "
                                            + "to a model.");
        }
        return this.interfaceDescriptors;
    }

    /**
     * Return the model this class is a part of
     * @return the parent Model
     */
    public Model getModel() {
        return this.model;
    }

    /**
     * True if this class is an interface.
     * @return true if an interface
     */
    public boolean isInterface() {
        return this.isInterface;
    }

    /**
     * Return a List of ClassDescriptors for all classes that extend this class
     * @return list of subclass ClassDescriptors
     * @throws IllegalStateException if the list of subclasses has not been set
     */
    public List getSubclassDescriptors() throws IllegalStateException {
        if (!subSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet had subclass"
                                            + "Descriptors set.");
        }
        return this.subclassDescriptors;
    }

   /**
     * Return a List of ClassDescriptors for all classes that implement this interface
     * @return list of class that implement this class
     * @throws IllegalStateException if implementor descriptors have not been set
     */
    public List getImplementorDescriptors() throws IllegalStateException {
        if (!implSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet had implementor "
                                            + "Descriptors set.");
        }
        return this.implementorDescriptors;
    }

    /**
     * Return the highest level business object that is a superclass of this class
     * @return ClassDescriptor of ultimate superclass
     * @throws IllegalStateException if model not set
     */
    public ClassDescriptor getUltimateSuperclassDescriptor() throws IllegalStateException {
        if (!modelSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet been added "
                                            + "to a model.");
        }
        if (!ultimateSuperSet) {
            ClassDescriptor cld = this;
            while (cld.getSuperclassDescriptor() != null) {
                cld = cld.getSuperclassDescriptor();
                this.ultimateSuperclassDesc = cld;
            }
            ultimateSuperSet = true;
        }
        return this.ultimateSuperclassDesc;

    }

    /**
     * Return a List of AttributeDescriptors for all attribtes of this class and
     * all super classes.
     * @return list of AttributeDescriptors
     * @throws IllegalStateException if model has not been set
     */
    public List getAllAttributeDescriptors() {
        if (!modelSet) {
            throw new IllegalStateException("This ClassDescriptor has not yet been added "
                                            + "to a model.");
        }
        List atts = new ArrayList(this.attDescriptors);
        List supers = getAllSuperclassDescriptors();
        Iterator superIter = supers.iterator();
        while (superIter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) superIter.next();
            atts.addAll(cld.getAttributeDescriptors());
        }
        return atts;
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
     * Set list of ClassDescriptors that are direct subclasses of this class.
     * Called once during Model creation.
     * @param sub list of direct subclass descriptors
     */
    protected void setSubclassDescriptors(List sub) {
        this.subclassDescriptors = sub;
        subSet = true;
    }


    /**
     * Set list of ClassDescriptors for classes that are direct implemetations
     * of this class, i.e. not subclasses of implentations.
     * @param impl list of direct implementations
     */
    protected void setImplementorDescriptors(List impl) {
        if (this.isInterface()) {
            this.implementorDescriptors = impl;
        }
        implSet = true;
    }


    private void findSuperclassDescriptor() throws MetaDataException {
        // descriptor for super class
        if (superclassName != null && superclassName != "") {
            this.superclassDescriptor = model.getClassDescriptorByName(superclassName);
            if (superclassDescriptor == null) {
                throw new MetaDataException("No ClassDescripor for super class: "
                                            + superclassName + " found in model.");
            }
            if (this.isInterface() != superclassDescriptor.isInterface()) {
                throw new MetaDataException("This class (" + this.getClassName()
                                            + (this.isInterface() ? ") is " : ") is not ")
                                            + "an interface but superclass ("
                                            + superclassDescriptor.getClassName()
                                            + (superclassDescriptor.isInterface() ? ") is."
                                               : ") is not."));
            }
        }
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

    // build a list of the superclass chain for this class
    private List getAllSuperclassDescriptors() {
        List supers = new ArrayList();
        ClassDescriptor cld = this;
        while (cld.getSuperclassDescriptor() != null) {
            cld = cld.getSuperclassDescriptor();
            supers.add(cld);
        }
        return supers;
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
        List l = new ArrayList();
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
