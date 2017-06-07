package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.model.StringConstructor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.translating.ObjectStoreTranslatingImpl;
import org.intermine.objectstore.translating.Translator;

/**
 * Class which holds a reference to an object in the database
 *
 * @author Matthew Wakeling
 */
public class ProxyReference implements InterMineObject, Lazy
{
    private ObjectStore os;
    private Integer id;
    private Class<? extends InterMineObject> clazz;

    /**
     * Construct a ProxyReference object.
     *
     * @param os the ObjectStore to retrieve the object from
     * @param id the internal id of the real object
     * @param clazz a hint of the class that this object is - use InterMineObject if unsure
     */
    public ProxyReference(ObjectStore os, Integer id, Class<? extends InterMineObject> clazz) {
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
            InterMineObject retval = os.getObjectById(id, clazz);
            if (retval == null) {
                if (os instanceof ObjectStoreTranslatingImpl) {
                    Translator trans = ((ObjectStoreTranslatingImpl) os).getTranslator();
                    Object identifier = trans.translateIdToIdentifier(id);
                    throw new NullPointerException("Error retrieving object from Items database"
                            + " with identifier " + identifier);
                }
                throw new NullPointerException("Error retrieving object from proxy with ID " + id
                        + " for class " + clazz.getName() + " from ObjectStore " + os);
            }
            return retval;
        } catch (ObjectStoreException e) {
            throw new RuntimeException("ObjectStoreException while materialising proxy with ID "
                    + id + " for class " + clazz.getName() + " from ObjectStore " + os + ": "
                    + e.getMessage(), e);
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
    public void setId(@SuppressWarnings("unused") Integer id) {
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
    @Override
    public String toString() {
        return "<ProxyReference os: " + os + ", id: " + id + ", proxied class: " + clazz + ">";
    }

    /**
     * {@inheritDoc}
     */
    public StringConstructor getoBJECT() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setoBJECT(@SuppressWarnings("unused") String notXml,
            @SuppressWarnings("unused") ObjectStore os) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setoBJECT(@SuppressWarnings("unused") String[] notXml,
            @SuppressWarnings("unused") ObjectStore os) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object getFieldValue(String fieldName) {
        if ("id".equals(fieldName)) {
            return id;
        }
        throw new UnsupportedOperationException("Tried to get field " + fieldName + " from proxy");
    }

    /**
     * {@inheritDoc}
     */
    public Object getFieldProxy(String fieldName) {
        if ("id".equals(fieldName)) {
            return id;
        }
        throw new UnsupportedOperationException("Tried to get field " + fieldName + " from proxy");
    }

    /**
     * {@inheritDoc}
     */
    public void setFieldValue(String fieldName, Object value) {
        if ("id".equals(fieldName)) {
            throw new IllegalArgumentException("Cannot change the id of a ProxyReference");
        }
        throw new UnsupportedOperationException("Tried to set field " + fieldName + " to value "
                + value);
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getFieldType(String fieldName) {
        if ("id".equals(fieldName)) {
            return Integer.class;
        }
        throw new UnsupportedOperationException("Tried to get field type for field " + fieldName
                + " from proxy");
    }

    /**
     * {@inheritDoc}
     */
    public Class<? extends FastPathObject> getElementType(String fieldName) {
        throw new UnsupportedOperationException("Tried to get element type for field " + fieldName
                + " from proxy");
    }


}
