package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Define methods needed to deal with integration when writing to an ObjectStore.  To retain
 * O/R mapping independence implementations of this interface should delegate writing to
 * a mapping tool specific implementation of ObjectStoreWriter.
 *
 * @author Richard Smith
 */

public interface IntegrationWriter extends ObjectStoreWriter
{

    /**
     * Given a new object from a data source find whether corresponding object exists in ObjectStore
     * and if so which fields the current data source has permission to write to.
     *
     * @param obj the example object
     * @return details of database object and which fields can be overwrittern
     * @throws ObjectStoreException if anything goes wrong
     */
    public IntegrationDescriptor getByExample(Object obj) throws ObjectStoreException;

    /**
     * Store an object in this ObjectStore.
     *
     * @param o the object to store
     * @param skeleton is this a skeleton object?
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(Object o, boolean skeleton) throws ObjectStoreException;
}
