package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.util.StringUtil;
import org.intermine.util.XmlUtil;

/**
 * Representation of an object
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class Item implements Comparable
{
    private String identifier = "";
    private String className = "";
    private String implementations = "";
    private Map attributes = new HashMap();
    private Map references = new HashMap();
    private Map collections = new HashMap();
    private Model model = null;
    private ClassDescriptor classDescriptor = null;
    private Set implementationClassDescriptors = null;

    /**
     * Construct an item.
     */
    public Item() { }

    /**
     * Construct an item.
     * @see ItemFactory
     * @param model the Model used to type-check set methods; if null no type checking is done
     * @param identifier item identifier
     * @param className name of described class
     * @param implementations names of implemented classes
     */
    public Item(Model model, String identifier, String className, String implementations) {
        this.identifier = identifier;
        this.className = className;
        this.implementations = implementations;
        setModel(model);
    }

    /**
     * Construct an item withno Model.  The calls to the set methods won't be type checked unless
     * setModel() is called.
     * @see ItemFactory
     * @param identifier item identifier
     * @param className name of described class
     * @param implementations names of implemented classes
     */
    public Item(String identifier, String className, String implementations) {
        this(null, identifier, className, implementations);
    }

    /**
     * Set the Model to use when checking calls to the other set methods
     * @param model the Model
     */
    public void setModel(Model model) {
        this.model = model;
        implementationClassDescriptors = null;
        setClassDescriptor(className);
    }

    /**
     * Return the model that was passed to the constructor or set with setModel().
     * @return the Model
     */
    public Model getModel() {
        return model;
    }


    /**
     * Set the identifier of this item
     * @param identifier the identifier
     */
    public void setIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier argument cannot be null");
        }
        this.identifier = identifier;
    }

    /**
     * Get the identifier of this item
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the class of this item
     * @param className the class
     */
    public void setClassName(String className) {
        if (className == null) {
            throw new IllegalArgumentException("className argument cannot be null");
        }
        classDescriptor = getClassDescriptorByName(className);
        this.className = className;
    }

    /**
     * Get the class name of this item
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the "implements" of this item
     * @param implementations the interfaces that this item implements
     */
    public void setImplementations(String implementations) {
        if (implementations == null) {
            throw new IllegalArgumentException("implementations argument cannot be null");
        }
        implementationClassDescriptors = null;
        checkImplementations(implementations);
        this.implementations = implementations;
    }

    /**
     * Get the interfaces implemented by this item
     * @return the implemented interfaces
     */
    public String getImplementations() {
        return implementations;
    }

    /**
     * Add an attribute
     * @param attribute the Attribute to add
     */
    public void addAttribute(Attribute attribute) {
        checkAttribute(attribute.getName());
        attributes.put(attribute.getName(), attribute);
    }

    /**
     * Remove a attribute of the specified name if it exists
     * @param attributeName name of the attribute to remove
     */
    public void removeAttribute(String attributeName) {
        checkAttribute(attributeName);
        attributes.remove(attributeName);
    }

    /**
     * Get all the attributes
     * @return all the attributes
     */
    public Collection getAttributes() {
        return attributes.values();
    }

    /**
     * Get a named attribute
     * @param attributeName the attribute name
     * @return the Attribute with the given name
     */
    public Attribute getAttribute(String attributeName) {
        checkAttribute(attributeName);
        return (Attribute) attributes.get(attributeName);
    }

    /**
     * Return true if named attribute exists
     * @param attributeName the attribute name
     * @return true if the attribute exists
     */
    public boolean hasAttribute(String attributeName) {
        //checkAttribute(attributeName);
        return attributes.containsKey(attributeName);
    }

    /**
     * Add a reference
     * @param reference the reference to add
     */
    public void addReference(Reference reference) {
        checkReference(reference.getName());
        references.put(reference.getName(), reference);
    }

    /**
     * Remove a reference of the specified name if it exists
     * @param referenceName name of the reference to remove
     */
    public void removeReference(String referenceName) {
        checkReference(referenceName);
        references.remove(referenceName);
    }

    /**
     * Get all the references
     * @return all the references
     */
    public Collection getReferences() {
        return references.values();
    }

    /**
     * Get a named reference
     * @param referenceName the reference name
     * @return the Reference with the given name
     */
    public Reference getReference(String referenceName) {
        checkReference(referenceName);
        return (Reference) references.get(referenceName);
    }

    /**
     * Return true if named reference exists
     * @param referenceName the reference name
     * @return true if the reference exists
     */
    public boolean hasReference(String referenceName) {
        checkReference(referenceName);
        return references.containsKey(referenceName);
    }

    /**
     * Add a collection
     * @param collection the collection to add
     */
    public void addCollection(ReferenceList collection) {
        checkCollection(collection.getName());
        collections.put(collection.getName(), collection);
    }

    /**
     * Remove a collection of the specified name if it exists
     * @param collectionName name of the collection to remove
     */
    public void removeCollection(String collectionName) {
        checkCollection(collectionName);
        collections.remove(collectionName);
    }

    /**
     * Get all the collections
     * @return all the collections
     */
    public Collection getCollections() {
        return collections.values();
    }

    /**
     * Return true if named collection exists
     * @param collectionName the collection name
     * @return true if the collection exists
     */
    public boolean hasCollection(String collectionName) {
        checkCollection(collectionName);
        return collections.containsKey(collectionName);
    }

    /**
     * Get a named collection
     * @param collectionName the collection name
     * @return the Collection with the given name
     */
    public ReferenceList getCollection(String collectionName) {
        checkCollection(collectionName);
        return (ReferenceList) collections.get(collectionName);
    }

    
    /**
     * Set a colletion.
     * @param collectionName collection name
     * @param refIds ids to reference
     */
    public void setCollection(String collectionName, List refIds) {
        addCollection(new ReferenceList(collectionName, refIds));
    }   
    
    /**
     * Add an attribute to this item
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void setAttribute(String name, String value) {
        addAttribute(new Attribute(name, value));
    }

    /**
     * Add a reference to this item
     * @param name the name of the attribute
     * @param refId the value of the attribute
     */
    public void setReference(String name, String refId) {
        if (refId.equals("")) {
            throw new RuntimeException("empty string used as ref_id for: " + name);
        }
        addReference(new Reference(name, refId));
    }
    
    /**
     * Add a reference that points to a particular item.
     * @param name the name of the attribute
     * @param item the item to refer to
     */
    public void setReference(String name, Item item) {
        addReference(new Reference(name, item.getIdentifier()));
    }

    /**
     * Add the identifier of the given Item to a collection
     * @param name the name of the collection
     * @param item the item whose identifier is to be added to the collection
     */
    public void addToCollection(String name, Item item) {
        ReferenceList list = getCollection(name);
        if (list == null) {
            list = new ReferenceList(name);
            addCollection(list);
        }
        list.addRefId(item.getIdentifier());
    }

    /**
     * Add a reference to a collection of this item
     * @param name the name of the collection
     * @param identifier the item to add to the collection
     */
    public void addToCollection(String name, String identifier) {
        if (identifier.equals("")) {
            throw new RuntimeException("empty string added to collection for: " + name);
        }
        ReferenceList list = getCollection(name);
        if (list == null) {
            list = new ReferenceList(name);
            addCollection(list);
        }
        list.addRefId(identifier);
    }

    /**
     * Throw a RuntimeException if the name parameter isn't an attribute of the class set by
     * setClassName() in the Model set by setModel().  Returns immediately if the Model or the
     * className of this Item haven't been set.
     * @param name the attribute name
     */
    protected void checkAttribute(String name) {
        if (model == null || classDescriptor == null) {
            return;
        }

        Iterator cdIter = getAllClassDescriptors().iterator();

        while (cdIter.hasNext()) {
            ClassDescriptor cd = (ClassDescriptor) cdIter.next();
            if (cd.getAttributeDescriptorByName(name, true) != null) {
                return;
            }
        }

        throw new RuntimeException("class \"" + classDescriptor.getName() + "\" has no \""
                                   + name + "\" attribute");
    }

    /**
     * Throw a RuntimeException if the name parameter isn't an reference in the class set by
     * setClassName() in the Model set by setModel().  Returns immediately if the Model or the
     * className of this Item haven't been set.
     * @param name the reference name
     *
     */
    protected void checkReference(String name) {
        if (model == null || classDescriptor == null) {
            return;
        }

        if (!canReference(name)) {
            throw new RuntimeException("class \"" + classDescriptor.getName() + "\" has no \""
                                       + name + "\" reference");
        }
    }

    /**
     * Return true if and only if the argument names a possible reference for this Item.  ie. the
     * ClassDescriptor for this Item contains a ReferenceDescriptor for this name.
     * @param name the field name
     * @return Return true if and only if this Item can have a reference of the given name
     */
    public boolean canReference(String name) {
        return classDescriptor.getReferenceDescriptorByName(name, true) != null;
    }

    /**
     * Throw a RuntimeException if the name parameter isn't an collection in the class set by
     * setClassName() in the Model set by setModel().  Returns immediately if the Model or the
     * className of this Item haven't been set.
     * @param name the collection name
     */
    protected void checkCollection(String name) {
        if (model == null || classDescriptor == null) {
            return;
        }

        if (classDescriptor.getCollectionDescriptorByName(name, true) == null) {
            throw new RuntimeException("class \"" + classDescriptor.getName() + "\" has no \""
                                       + name + "\" collection");
        }
    }

    /**
     * Throw RuntimeException if the given implementations don't match the model.
     * @param implementations the interfaces that this item implements
     */
    protected void checkImplementations(String implementations) {
        if (model == null) {
            return;
        }

        getImplementClassDescriptors(implementations);
    }

    /**
     * Throw a RuntimeException if any of the named class isn't in the Model set by setModel().
     * Returns null if the model isn't set or className is "".
     * @param className the class name
     * @return the ClassDescriptor for the given class
     */
    protected ClassDescriptor getClassDescriptorByName(String className) {
        if (model == null) {
            return null;
        }

        if (className.equals("")) {
            return null;
        }

        String classNameNS = XmlUtil.getNamespaceFromURI(className);

        if (!model.getNameSpace().toString().equals(classNameNS)) {
            throw new RuntimeException("class \"" + className + "\" is not in the Model "
                                       + "(namespace doesn't match \""
                                       + model.getNameSpace() + "\" != \"" + classNameNS + "\")");
        }

        String fullClassName = model.getPackageName() + "." + XmlUtil.getFragmentFromURI(className);

        ClassDescriptor cd = model.getClassDescriptorByName(fullClassName);

        if (cd == null) {
            throw new IllegalArgumentException("class \"" + fullClassName
                                               + "\" is not in the Model");
        }

        return cd;
    }

    /**
     * Set the classDescriptor attribute to be the ClassDescriptor for the given className in the
     * Model set by setModel().  Returns immediately if the Model hasn't been set or the className
     * parameter is "".
     * @param className the class name
     */
    protected void setClassDescriptor(String className) {
        if (model != null && !className.equals("")) {
            String fullClassName =
                model.getPackageName() + "." + XmlUtil.getFragmentFromURI(className);

            classDescriptor = getClassDescriptorByName(fullClassName);
        }
    }

    /**
     * Return the ClassDescriptors of the class of this Item (as given by className) and all the
     * implementations.  Call only if model, className and implementations are set.
     * @return all the ClassDescriptors for this Item
     */
    protected Set getAllClassDescriptors() {
        Set cds = new HashSet();
        Set implementationCds = getImplementClassDescriptors(implementations);
        if (implementationCds != null) {
            cds.addAll(implementationCds);
        }
        cds.add(classDescriptor);
        return cds;
    }

    /**
     * Returns the ClassDescriptors for the given implementations.  Returns null if the Model hasn't
     * been set.  Throw a RuntimeException if any of the classes named in the implementations
     * parameter aren't in the Model.
     * @param implementations the interfaces that this item implements
     * @return the ClassDescriptors for the given implementations.  Returns null if the Model hasn't
     * been set
     */
    protected Set getImplementClassDescriptors(String implementations) {
        if (implementations.length() == 0) {
            return null;
        }

        if (implementationClassDescriptors != null) {
            return implementationClassDescriptors;
        }

        Set cds = new HashSet();

        String [] bits = StringUtil.split(implementations, " ");

        for (int i = 0; i < bits.length; i++) {
            ClassDescriptor cd = getClassDescriptorByName(bits[i]);
            cds.add(cd);
        }

        implementationClassDescriptors = cds;

        return implementationClassDescriptors;
    }


    /**
     * @see Object#equals
     */
    public boolean equals(Object o) {
        if (o instanceof Item) {
            Item i = (Item) o;
            return identifier.equals(i.identifier)
            && className.equals(i.className)
            && implementations.equals(i.implementations)
            && attributes.equals(i.attributes)
            && references.equals(i.references)
            && collections.equals(i.collections);
        }
        return false;
    }

    /**
     * Compare items first by class, then by identifier, intended for creating
     * ordered output files.
     * @see Comparable#compareTo
     */ 
    public int compareTo(Object o) {
        if (!(o instanceof Item)) {
            throw new RuntimeException("Attempt to compare an item to a " + o.getClass() + " ("
                                       + o.toString() + ")");
        }
        
        Item i = (Item) o;
        int compValue = this.getClassName().compareTo(i.getClassName());
        if (compValue == 0) {
            compValue = this.getIdentifier().compareTo(i.getIdentifier());
        }
        return compValue;
    }
    
    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return identifier.hashCode()
        + 3 * className.hashCode()
        + 5 * implementations.hashCode()
        + 7 * attributes.hashCode()
        + 11 * references.hashCode()
        + 13 * collections.hashCode();
    }

    /**
    * @see Object#toString
    */
    public String toString() {
        return XmlUtil.indentXmlSimple(FullRenderer.render(this));
    }
}
