package org.flymine.metadata;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;


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
    private ClassDescriptor superclassDescriptor;
    private List interfaceNames = new ArrayList();
    private List interfaceDescriptors = new ArrayList(); // names of interfaces

    private boolean isInterface;
    private List attDescriptors;
    private List refDescriptors;
    private List colDescriptors;
    private Map attDescriptorsNameMap = new HashMap();
    private Map refDescriptorsNameMap = new HashMap();
    private Map colDescriptorsNameMap = new HashMap();

    private List pkFields = new ArrayList();
    private Model model;  // set when ClassDesriptor added to DescriptorRespository
    private boolean modelSet = false;


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
    protected ClassDescriptor(String name, String superclassName, String interfaces,
                              boolean isInterface, List atts, List refs, List cols)
        throws IllegalArgumentException {

        // must provide class name
        if (name == null || name == "") {
            throw new IllegalArgumentException("'name' parameter must be a valid String");
        }
        this.name = name;
        this.superclassName = superclassName;

        // split interface string into a Set
        if (interfaces != null && interfaces != "") {
            StringTokenizer st = new StringTokenizer(interfaces);
            while (st.hasMoreTokens()) {
                this.interfaceNames.add(st.nextToken());
            }
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
            attDescriptorsNameMap.put(attDesc.getName(), attDesc);
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
            refDescriptorsNameMap.put(refDesc.getName(), refDesc);
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
            colDescriptorsNameMap.put(colDesc.getName(), colDesc);
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
     * Get a list of primary key FieldDescriptors for this Class.  Could be a combination
     * of attributes and references.
     * @return list of primary key fields
     */
    public List getPkFieldDescriptors() {
        return this.pkFields;
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
        if (attDescriptorsNameMap.containsKey(name)) {
            return (AttributeDescriptor) attDescriptorsNameMap.get(name);
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
        if (colDescriptorsNameMap.containsKey(name)) {
            return (CollectionDescriptor) colDescriptorsNameMap.get(name);
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
        if (refDescriptorsNameMap.containsKey(name)) {
            return (ReferenceDescriptor) refDescriptorsNameMap.get(name);
        } else {
            return null;
        }
    }


    /**
     * Get the name of the super class of this class (may be null)
     * @return the super class name
     */
    public ClassDescriptor getSuperclassDescriptor() {
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
                                            + "to a model");
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
     * True if this class is an interface.
     * @return true if an interface
     */
    public boolean isInterface() {
        return this.isInterface;
    }

    /**
     * Return a List of ClassDescriptors for all classes that extend this class
     * @return list of subclass ClassDescriptors
     */
    public List getSubclassDescriptors() {
        return new ArrayList();
    }

   /**
     * Return a List of ClassDescriptors for all classes that implement this interface
     * @return list of class that implement this class
     */
    public List getImplementorDescriptors() {
        // check if this is an interface
        return new ArrayList();
    }


    /**
     * Return a List of AttributeDescriptors for all attribtes of this class and
     * all super classes.
     * @return list of AttributeDescriptors
     */
    public List getAllAttributeDescriptors() {
        return new ArrayList();
    }


    private void findSuperclassDescriptor() throws MetaDataException {
        // descriptor for super class
        if (superclassName != null && superclassName != "") {
            this.superclassDescriptor = model.getDescriptorFor(superclassName);
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
                if (!model.hasDescriptorFor(iName)) {
                    throw new MetaDataException("No ClassDescriptor for interface ( "
                                                + iName + ") found in model.");
                }
                ClassDescriptor iDescriptor = model.getDescriptorFor(iName);
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
            CollectionDescriptor cod = (CollectionDescriptor) refIter.next();
            cod.findReferencedDescriptor();
        }

    }



}
