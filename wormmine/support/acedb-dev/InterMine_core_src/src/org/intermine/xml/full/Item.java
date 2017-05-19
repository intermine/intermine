package org.intermine.xml.full;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.util.StringUtil;
import org.intermine.util.XmlUtil;

/**
 * Representation of an object
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class Item implements Comparable<Item>
{
    private String identifier = "";
    private String className = "";
    private String implementations = "";
    private Map<String, Attribute> attributes = new HashMap<String, Attribute>();
    private Map<String, Reference> references = new HashMap<String, Reference>();
    private Map<String, ReferenceList> collections = new HashMap<String, ReferenceList>();
    private Model model = null;
    private ClassDescriptor classDescriptor = null;
    private List<ClassDescriptor> implementationClassDescriptors = null;

    /**
     * Construct an item.
     */
    protected Item() {
        // nothing to do
    }

    /**
     * Construct an item.
     *
     * @see ItemFactory
     * @param model the Model used to type-check set methods; if null no type checking is done
     * @param identifier item identifier
     * @param className name of described class
     * @param implementations names of implemented classes
     */
    protected Item(Model model, String identifier, String className, String implementations) {
        this.identifier = identifier;
        this.className = className;
        this.implementations = implementations;
        setModel(model);
    }

    /**
     * Construct an item with no Model.  The calls to the set methods won't be type checked unless
     * setModel() is called.
     *
     * @see ItemFactory
     * @param identifier item identifier
     * @param className name of described class
     * @param implementations names of implemented classes
     */
    protected Item(String identifier, String className, String implementations) {
        this(null, identifier, className, implementations);
    }

    /**
     * Set the Model to use when checking calls to the other set methods
     *
     * @param model the Model
     */
    public void setModel(Model model) {
        this.model = model;
        implementationClassDescriptors = null;
        setClassDescriptor(className);
    }

    /**
     * Return the model that was passed to the constructor or set with setModel().
     *
     * @return the Model
     */
    public Model getModel() {
        return model;
    }


    /**
     * Set the identifier of this item.
     *
     * @param identifier the identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Get the identifier of this item.
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the class of this item.
     *
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
     * Get the class name of this item.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the "implements" of this item.
     *
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
     * Get the interfaces implemented by this item.
     *
     * @return the implemented interfaces
     */
    public String getImplementations() {
        return implementations;
    }

    /**
     * Add an attribute.
     *
     * @param attribute the Attribute to add
     */
    public void addAttribute(Attribute attribute) {
        String name = attribute.getName();
        if (!checkAttribute(name)) {
            throw new RuntimeException("class \"" + className + "\" has no \""
                                       + name + "\" attribute");
        }
        if (attribute.getValue() == null) {
            throw new RuntimeException("value cannot be null for attribute "
                                       + className + "."  + name);
        }
        if ("".equals(attribute.getValue())) {
            throw new RuntimeException("value cannot be an empty string for attribute "
                                       + className + "."  + name);
        }

        attributes.put(name, attribute);
    }

    /**
     * Remove a attribute of the specified name if it exists.
     *
     * @param attributeName name of the attribute to remove
     */
    public void removeAttribute(String attributeName) {
        if (!checkAttribute(attributeName)) {
            throw new RuntimeException("class \"" + className + "\" has no \""
                                       + attributeName + "\" attribute");
        }
        attributes.remove(attributeName);
    }

    /**
     * Get all the attributes.
     *
     * @return all the attributes
     */
    public Collection<Attribute> getAttributes() {
        return attributes.values();
    }

    /**
     * Get a named attribute.
     *
     * @param attributeName the attribute name
     * @return the Attribute with the given name
     */
    public Attribute getAttribute(String attributeName) {
        if (!checkAttribute(attributeName)) {
            throw new RuntimeException("class \"" + classDescriptor.getName() + "\" has no \""
                                       + attributeName + "\" attribute");
        }
        return attributes.get(attributeName);
    }

    /**
     * Return true if named attribute exists.
     *
     * @param attributeName the attribute name
     * @return true if the attribute exists
     */
    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    /**
     * Add a reference.
     *
     * @param reference the reference to add
     */
    public void addReference(Reference reference) {
        checkReference(reference.getName());
        references.put(reference.getName(), reference);
    }

    /**
     * Remove a reference of the specified name if it exists.
     *
     * @param referenceName name of the reference to remove
     */
    public void removeReference(String referenceName) {
        checkReference(referenceName);
        references.remove(referenceName);
    }

    /**
     * Get all the references.
     *
     * @return all the references
     */
    public Collection<Reference> getReferences() {
        return references.values();
    }

    /**
     * Get a named reference.
     *
     * @param referenceName the reference name
     * @return the Reference with the given name
     */
    public Reference getReference(String referenceName) {
        checkReference(referenceName);
        return references.get(referenceName);
    }

    /**
     * Return true if named reference exists.
     *
     * @param referenceName the reference name
     * @return true if the reference exists
     */
    public boolean hasReference(String referenceName) {
        checkReference(referenceName);
        return references.containsKey(referenceName);
    }

    /**
     * Add a collection.
     *
     * @param collection the collection to add
     */
    public void addCollection(ReferenceList collection) {
        checkCollection(collection.getName());
        collections.put(collection.getName(), collection);
    }

    /**
     * Remove a collection of the specified name if it exists.
     *
     * @param collectionName name of the collection to remove
     */
    public void removeCollection(String collectionName) {
        checkCollection(collectionName);
        collections.remove(collectionName);
    }

    /**
     * Get all the collections.
     *
     * @return all the collections
     */
    public Collection<ReferenceList> getCollections() {
        return collections.values();
    }

    /**
     * Return true if named collection exists.
     *
     * @param collectionName the collection name
     * @return true if the collection exists
     */
    public boolean hasCollection(String collectionName) {
        checkCollection(collectionName);
        return collections.containsKey(collectionName);
    }

    /**
     * Get a named collection.
     *
     * @param collectionName the collection name
     * @return the Collection with the given name
     */
    public ReferenceList getCollection(String collectionName) {
        checkCollection(collectionName);
        return collections.get(collectionName);
    }

    /**
     * Set a collection.
     *
     * @param collectionName collection name
     * @param refIds ids to reference
     */
    public void setCollection(String collectionName, List<String> refIds) {
        addCollection(new ReferenceList(collectionName, refIds));
    }

    /**
     * Add an attribute to this item.
     *
     * @param name the name of the attribute
     * @param value the value of the attribute - cannot be null or empty
     */
    public void setAttribute(String name, String value) {
        if (value == null) {
            throw new RuntimeException("value cannot be null for attribute "
                                       + className + "."  + name);
        }
        if ("".equals(value)) {
            throw new RuntimeException("value cannot be an empty string for attribute "
                                       + className + "."  + name);
        }
        addAttribute(new Attribute(name, value));
    }

    /**
     * Add an attribute to this item and set it to the empty string.
     *
     * @param name the name of the attribute
     */
    public void setAttributeToEmptyString(String name) {
        attributes.put(name, new Attribute(name, ""));
    }

    /**
     * Add a reference to this item.
     *
     * @param name the name of the attribute
     * @param refId the value of the attribute
     */
    public void setReference(String name, String refId) {
        if ("".equals(refId)) {
            throw new RuntimeException("empty string used as ref_id for: " + name);
        }
        addReference(new Reference(name, refId));
    }

    /**
     * Add a reference that points to a particular item.
     *
     * @param name the name of the attribute
     * @param item the item to refer to
     */
    public void setReference(String name, Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Attempt to set reference '" + name + "' to null"
                    + " in '" + className + "' item with identifier: " + identifier);
        }
        addReference(new Reference(name, item.getIdentifier()));
    }

    /**
     * Add the identifier of the given Item to a collection.
     *
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
     * Add a reference to a collection of this item.
     *
     * @param name the name of the collection
     * @param refId the item to add to the collection
     */
    public void addToCollection(String name, String refId) {
        if (StringUtils.isEmpty(refId)) {
            throw new RuntimeException("empty string added to collection for: " + name);
        }
        ReferenceList list = getCollection(name);
        if (list == null) {
            list = new ReferenceList(name);
            addCollection(list);
        }
        list.addRefId(refId);
    }

    /**
     * Return true if the name parameter is an attribute of the class for this Item or if
     * the Model or the className of this Item haven't been set.
     *
     * @param name the attribute name
     * @return true if the name is a valid attribute name
     */
    public boolean checkAttribute(String name) {
        if (model == null || classDescriptor == null) {
            return true;
        }

        for (ClassDescriptor cld : getAllClassDescriptors()) {
            if (cld.getAttributeDescriptorByName(name, true) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Throw a RuntimeException if the name parameter isn't an reference in the class set by
     * setClassName() in the Model set by setModel().  Returns immediately if the Model or the
     * className of this Item haven't been set.
     *
     * @param name the reference name
     *
     */
    protected void checkReference(String name) {
        if (model == null || classDescriptor == null) {
            return;
        }

        if (!canHaveReference(name)) {
            throw new RuntimeException("class \"" + classDescriptor.getName() + "\" has no \""
                                       + name + "\" reference");
        }
    }

    /**
     * Return true if and only if the argument names a possible reference for this Item.  ie. the
     * ClassDescriptor for this Item contains a ReferenceDescriptor for this name.
     *
     * @param name the field name
     * @return Return true if and only if this Item has a reference of the given name in the model
     */
    public boolean canHaveReference(String name) {
        return classDescriptor.getReferenceDescriptorByName(name, true) != null;
    }

    /**
     * Return true if and only if the argument names a possible collection for this Item.  ie. the
     * ClassDescriptor for this Item contains a CollectionDescriptor for this name.
     *
     * @param name the field name
     * @return Return true if and only if this Item has a collection of the given name in the model
     */
    public boolean canHaveCollection(String name) {
        return classDescriptor.getCollectionDescriptorByName(name, true) != null;
    }

    /**
     * Throw a RuntimeException if the name parameter isn't an collection in the class set by
     * setClassName() in the Model set by setModel().  Returns immediately if the Model or the
     * className of this Item haven't been set.
     *
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
     *
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
     *
     * @param className the class name
     * @return the ClassDescriptor for the given class
     */
    protected ClassDescriptor getClassDescriptorByName(String className) {
        if (model == null) {
            return null;
        }

        if ("".equals(className)) {
            return null;
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
     *
     * @param className the class name
     */
    protected void setClassDescriptor(String className) {
        if (model != null && !"".equals(className)) {
            String fullClassName =
                model.getPackageName() + "." + XmlUtil.getFragmentFromURI(className);
            classDescriptor = getClassDescriptorByName(fullClassName);
        }
    }

    private static ThreadLocal<Map<String, List<ClassDescriptor>>> getAllClassDescriptorsCache
        = new ThreadLocal<Map<String, List<ClassDescriptor>>>() {
            @Override protected Map<String, List<ClassDescriptor>> initialValue() {
                return new HashMap<String, List<ClassDescriptor>>();
            }
        };

    /**
     * Return the ClassDescriptors of the class of this Item (as given by className) and all the
     * implementations.  Call only if model, className and implementations are set.
     *
     * @return all the ClassDescriptors for this Item
     */
    protected List<ClassDescriptor> getAllClassDescriptors() {
        Map<String, List<ClassDescriptor>> cache = getAllClassDescriptorsCache.get();
        String key = implementations + "___" + classDescriptor.getName();
        List<ClassDescriptor> retval = cache.get(key);
        if (retval == null) {
            Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>();
            clds.addAll(getImplementClassDescriptors(implementations));
            clds.add(classDescriptor);
            retval = new ArrayList<ClassDescriptor>(clds);
            cache.put(key, retval);
        }
        return retval;
    }

    private static ThreadLocal<Map<String, List<ClassDescriptor>>> getImplementClassDescriptorsCache
        = new ThreadLocal<Map<String, List<ClassDescriptor>>>() {
            @Override protected Map<String, List<ClassDescriptor>> initialValue() {
                return new HashMap<String, List<ClassDescriptor>>();
            }
        };

    /**
     * Returns the ClassDescriptors for the given implementations.  Returns null if the Model hasn't
     * been set.  Throw a RuntimeException if any of the classes named in the implementations
     * parameter aren't in the Model.
     *
     * @param implementations the interfaces that this item implements
     * @return the ClassDescriptors for the given implementations.  Returns null if the Model hasn't
     * been set
     */
    protected List<ClassDescriptor> getImplementClassDescriptors(String implementations) {
        if (implementationClassDescriptors == null) {
            Map<String, List<ClassDescriptor>> cache = getImplementClassDescriptorsCache.get();
            implementationClassDescriptors = cache.get(implementations);
            if (implementationClassDescriptors == null) {
                implementationClassDescriptors = new ArrayList<ClassDescriptor>();
                String [] bits = StringUtil.split(implementations, " ");

                for (String clsName : bits) {
                    if (!StringUtils.isEmpty(clsName)) {
                        implementationClassDescriptors.add(getClassDescriptorByName(clsName));
                    }
                }
                cache.put(implementations, implementationClassDescriptors);
            }
        }
        return implementationClassDescriptors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Item) {
            Item i = (Item) o;
            return identifier.equals(i.identifier) && className.equals(i.className)
                && implementations.equals(i.implementations) && attributes.equals(i.attributes)
                && references.equals(i.references) && collections.equals(i.collections);
        }
        return false;
    }

    /**
     * Compare items first by class, then by identifier, intended for creating
     * ordered output files.
     *
     * {@inheritDoc}
     */
    public int compareTo(Item i) {
        int compValue = this.getClassName().compareTo(i.getClassName());
        if (compValue == 0) {
            compValue = this.getIdentifier().compareTo(i.getIdentifier());
        }
        return compValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return identifier.hashCode() + 3 * className.hashCode() + 5 * implementations.hashCode()
            + 7 * attributes.hashCode() + 11 * references.hashCode() + 13 * collections.hashCode();
    }

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString() {
        return XmlUtil.indentXmlSimple(FullRenderer.render(this));
    }
}
