package org.intermine.xml.full;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of an object
 *
 * @author Andrew Varley
 */
public class Item
{
    protected String identifier = "";
    protected String className = "";
    protected String implementations = "";
    protected Map attributes = new HashMap();
    protected Map references = new HashMap();
    protected Map collections = new HashMap();


    /**
     * Construct an item.
     */
    public Item() { }

    /**
     * Construct an item.
     * @param identifier item identifier
     * @param className name of described class
     * @param implementations names of implemented classes
     */
    public Item(String identifier, String className, String implementations) {
        this.identifier = identifier;
        this.className = className;
        this.implementations = implementations;
    }

    /**
     * Set the identifier of this item
     *
     * @param identifier the identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Get the identifier of this item
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the class of this item
     *
     * @param className the class
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get the class name of this item
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the "implements" of this item
     *
     * @param implementations the interfaces that this item implements
     */
    public void setImplementations(String implementations) {
        this.implementations = implementations;
    }

    /**
     * Get the interfaces implemented by this item
     *
     * @return the implemented interfaces
     */
    public String getImplementations() {
        return implementations;
    }

    /**
     * Add an attribute
     *
     * @param attribute the Attribute to add
     */
    public void addAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    /**
     * Remove a attribute of the specified name if it exists
     *
     * @param attName name of the attribute to remove
     */
    public void removeAttribute(String attName) {
        attributes.remove(attName);
    }

    /**
     * Get all the attributes
     *
     * @return all the attributes
     */
    public Collection getAttributes() {
        return attributes.values();
    }

    /**
     * Get a named attribute
     *
     * @param attributeName the attribute name
     * @return the Attribute with the given name
     */
    public Attribute getAttribute(String attributeName) {
        return (Attribute) attributes.get(attributeName);
    }

    /**
     * Return true if named attribute exists
     *
     * @param attributeName the attribute name
     * @return true if the attribute exists
     */
    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    /**
     * Add a reference
     *
     * @param reference the reference to add
     */
    public void addReference(Reference reference) {
        references.put(reference.getName(), reference);
    }

    /**
     * Remove a reference of the specified name if it exists
     *
     * @param refName name of the reference to remove
     */
    public void removeReference(String refName) {
        references.remove(refName);
    }

    /**
     * Get all the references
     *
     * @return all the references
     */
    public Collection getReferences() {
        return references.values();
    }

    /**
     * Get a named reference
     *
     * @param referenceName the reference name
     * @return the Reference with the given name
     */
    public Reference getReference(String referenceName) {
        return (Reference) references.get(referenceName);
    }

    /**
     * Return true if named reference exists
     *
     * @param referenceName the reference name
     * @return true if the reference exists
     */
    public boolean hasReference(String referenceName) {
        return references.containsKey(referenceName);
    }

    /**
     * Add a collection
     *
     * @param collection the collection to add
     */
    public void addCollection(ReferenceList collection) {
        collections.put(collection.getName(), collection);
    }

    /**
     * Remove a collection of the specified name if it exists
     *
     * @param colName name of the collection to remove
     */
    public void removeCollection(String colName) {
        collections.remove(colName);
    }

    /**
     * Get all the collections
     *
     * @return all the collections
     */
    public Collection getCollections() {
        return collections.values();
    }

    /**
     * Return true if named collection exists
     *
     * @param collectionName the collection name
     * @return true if the collection exists
     */
    public boolean hasCollection(String collectionName) {
        return collections.containsKey(collectionName);
    }

    /**
     * Get a named collection
     *
     * @param collectionName the collection name
     * @return the Collection with the given name
     */
    public ReferenceList getCollection(String collectionName) {
        return (ReferenceList) collections.get(collectionName);
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
        return FullRenderer.render(this);
    }
}
