package org.intermine.xml.lite;

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
import java.util.Map;

/**
 * Representation of an object
 *
 * @author Andrew Varley
 */
public class Item
{
    private String id;
    private String className;
    private String implementations;
    private Map fields = new HashMap();
    private Map references = new HashMap();

    /**
     * Set the id of this item
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the id of this item
     *
     * @return the id
     */
    public String getId() {
        return this.id;
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
        return this.className;
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
        return this.implementations;
    }

    /**
     * Add a field
     *
     * @param field the Field to add
     */
    public void addField(Field field) {
        fields.put(field.getName(), field);
    }

    /**
     * Get a named field
     *
     * @param fieldName the field name
     * @return the Field with the given name
     */
    public Field getField(String fieldName) {
        return (Field) fields.get(fieldName);
    }

    /**
     * Get all the fields
     *
     * @return all the fields
     */
    public Collection getFields() {
        return fields.values();
    }

    /**
     * Add a reference
     *
     * @param reference the reference to add
     */
    public void addReference(Field reference) {
        references.put(reference.getName(), reference);
    }

    /**
     * Get a named reference
     *
     * @param referenceName the reference name
     * @return the Reference with the given name
     */
    public Field getReference(String referenceName) {
        return (Field) references.get(referenceName);
    }

    /**
     * Get all the references
     *
     * @return all the references
     */
    public Collection getReferences() {
        return references.values();
    }

}
