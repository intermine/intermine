package org.intermine.webservice.client.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.client.core.ServiceFactory;
import org.intermine.webservice.client.util.PQUtil;

/**
 * A representation of an individual result from the web-services. These results are
 * backed by maps returned by the QueryService for each row, and used to represent
 * members of lists.
 *
 * Unlike the backing maps, these objects contain more information and assumptions
 * about the represented objects, in particular the fact that every object has an id
 * and a type.
 *
 * @author Alex Kalderimis
 *
 */
public class Item
{

    private final String type;
    private final Model model;

    /**
     * Get the type of this object. This is the class requested in the query
     * from which the rows originated, and the object may also belong to a more
     * specific type.
     * @return The class in the data model this object belongs to.
     */
    public String getType() {
        return type;
    }

    private final Map<String, Object> properties;
    private ServiceFactory factory;

    /**
     * Return a view of the properties in this row. Changes to this object will
     * not be reflected in the Item.
     * @return A new map containing the same information as the backing map.
     */
    public Map<String, Object> getProperties() {
        return new HashMap<String, Object>(properties);
    }

    /**
     * Construct an Item.
     *
     * @param factory The service this item comes from.
     * @param type The type of this class.
     * @param properties The backing properties.
     */
    public Item(ServiceFactory factory, String type, Map<String, Object> properties) {
        assert (factory != null);
        assert (type != null);
        assert (properties != null);
        this.factory = factory;
        this.model = factory.getModel();
        this.type = type;
        this.properties = Collections.unmodifiableMap(properties);
    }

    /**
     * Get a property known to be a string.
     * @param key The name of the property (eg: "symbol")
     * @return The property's value.
     */
    public String getString(String key) {
        return (String) properties.get(key);
    }

    /**
     * Get a property known to be an integer.
     * @param key The name of the property (eg: "length")
     * @return The property's value.
     */
    public Integer getInt(String key) {
        return (Integer) properties.get(key);
    }

    /**
     * Get a property known to be a double.
     * @param key The name of the property (eg: "score")
     * @return the propery's value
     */
    public Double getDouble(String key) {
        return (Double) properties.get(key);
    }

    /**
     * Get a property known to be an float.
     * @param key The name of the property (eg: "score")
     * @return The property's value.
     */
    public Float getFloat(String key) {
        return (Float) properties.get(key);
    }

    /**
     * Get a property known to be an boolean.
     * @param key The name of the property (eg: "active")
     * @return The property's value.
     */
    public Boolean getBoolean(String key) {
        return (Boolean) properties.get(key);
    }

    /**
     * Get the internal database id of the object. This value guarantees object identity.
     * @return The id of the object.
     */
    public int getId() {
        return getInt("id");
    }

    /**
     * Determine whether this object is equal to the other.
     * @param other The object to compare to.
     * @return Whether they are the same object in the database on the server.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Item) {
            return getId() == ((Item) other).getId();
        }
        return false;
    }

    /**
     * Get in Item representing the record referenced by a reference field.
     *
     * For example, for <code>gene.getItem("organism")</code> the return value will
     * be an Item representing the organism, or null, if the reference is unset.
     *
     * @param key The name of the reference.
     * @return The Item representing the referenced object.
     */
    public Item getReference(String key) {
        ReferenceDescriptor rd = getReferenceDescriptor(key);
        List<Map<String, Object>> results = getReferenceData(rd);
        if (results.isEmpty()) {
            return null;
        }
        String referencedType = rd.getReferencedClassDescriptor().getUnqualifiedName();
        Map<String, Object> result = results.get(0);
        return new Item(factory, referencedType, transformForReference(result));
    }

    /**
     * Get a set of items representing the elements in a collection field on an item.
     *
     * For example, a call to <code>gene.getCollection("proteins")</code> will return a
     * set of Items that each represent a protein in the genes "proteins" collection.
     * @param key The name of the collection.
     * @return A Set of Item objects.
     */
    public Set<Item> getCollection(String key) {
        ReferenceDescriptor rd = getReferenceDescriptor(key);
        List<Map<String, Object>> results = getReferenceData(rd);
        String referencedType = rd.getReferencedClassDescriptor().getUnqualifiedName();
        Set<Item> ret = new HashSet<Item>();
        for (Map<String, Object> result: results) {
            ret.add(new Item(factory, referencedType, transformForReference(result)));
        }
        return ret;
    }

    private ReferenceDescriptor getReferenceDescriptor(String key) {
        ClassDescriptor thisCd = model.getClassDescriptorByName(this.type);
        FieldDescriptor fd = thisCd.getFieldDescriptorByName(key);
        if (fd == null) {
            throw new NoSuchElementException(key + " is not a field");
        }
        try {
            return (ReferenceDescriptor) fd;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(key + " does not represent a reference");
        }
    }

    private Map<String, Object> transformForReference(Map<String, Object> orig) {
        Map<String, Object> transformed = new HashMap<String, Object>();
        for (String k: orig.keySet()) {
            String newKey = k.substring(k.lastIndexOf('.') + 1);
            transformed.put(newKey, orig.get(k));
        }
        return transformed;
    }

    private List<Map<String, Object>> getReferenceData(FieldDescriptor fd) {
        PathQuery query = new PathQuery(model);
        query.addViews(PQUtil.getStar(model, this.type + "." + fd.getName()));
        query.addConstraint(Constraints.eq(this.type + ".id", Integer.toString(getId())));
        return factory.getQueryService().getRowsAsMaps(query);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + getId();
        return hash;
    }

    /**
     * Determine whether this item belongs to the class of objects of the named type.
     * @param type The name of the class this object might belong to.
     * @return Whether or not this object is an object of the given type.
     */
    public boolean isa(String type) {
        if (this.type.equals(type)) {
            return true;
        } else {
            ClassDescriptor thisCd = model.getClassDescriptorByName(this.type);
            ClassDescriptor thatCd = model.getClassDescriptorByName(type);
            return thisCd.getAllSuperDescriptors().contains(thatCd);
        }
    }

    @Override
    public String toString() {
        return this.type + this.properties;
    }
}
