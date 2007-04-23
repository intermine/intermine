package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Class which holds a reference to an object in the database
 *
 * @author Matthew Wakeling
 */
public class ProxyReference implements InterMineObject, Lazy
{
    private ObjectStore os;
    private Integer id;
    private Class clazz;

    /**
     * Construct a ProxyReference object.
     *
     * @param os the ObjectStore to retrieve the object from
     * @param id the internal id of the real object
     * @param clazz a hint of the class that this object is - use InterMineObject if unsure
     */
    public ProxyReference(ObjectStore os, Integer id, Class clazz) {
        this.os = os;
        this.id = id;
        this.clazz = clazz;
    }

    /**
     * Gets the real object from the database.
     *
     * @return a InterMineObject
     */
    public InterMineObject getObject() {
        try {
            return os.getObjectById(id, clazz);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("ObjectStoreException while materialising proxy: "
                                       + e.getMessage());
        }
    }

    /**
     * Gets the ID value
     *
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the ID value
     *
     * @param id the id
     */
    public void setId(Integer id) {
        throw new IllegalArgumentException("Cannot change the id of a ProxyReference");
    }

    /**
     * Returns the ObjectStore that this proxy will use
     *
     * @return an ObjectStore
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "<ProxyReference os: " + os + ", id: " + id + ", proxied class: " + clazz + ">";
    }
}
