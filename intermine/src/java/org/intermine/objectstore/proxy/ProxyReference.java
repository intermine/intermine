package org.intermine.objectstore.proxy;

/*
 * Copyright (C) 2002-2004 FlyMine
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
public class ProxyReference implements InterMineObject
{
    private ObjectStore os;
    private Integer id;

    /**
     * Construct a ProxyReference object.
     *
     * @param os the ObjectStore to retrieve the object from
     * @param id the internal id of the real object
     */
    public ProxyReference(ObjectStore os, Integer id) {
        this.os = os;
        this.id = id;
    }

    /**
     * Gets the real object from the database.
     *
     * @return a InterMineObject
     */
    public InterMineObject getObject() {
        try {
            return os.getObjectById(id);
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
        this.id = id;
    }
}
