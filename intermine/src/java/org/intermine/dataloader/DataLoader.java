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

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreException;

import org.apache.log4j.Logger;

/**
 * Loads information from a data source into the Flymine database.
 * This class defines the store method, which can be used by the process method of
 * subclasses.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class DataLoader
{
    protected static final Logger LOG = Logger.getLogger(DataLoader.class);
    protected IntegrationWriter iw;
    
    /**
     * No-arg constructor for testing purposes
     */
    protected DataLoader() {
    }

    /**
     * Construct a DataLoader
     * 
     * @param iw an IntegrationWriter to write to
     */
    public DataLoader(IntegrationWriter iw) {
        this.iw = iw;
    }

    /**
     * Stores an object, with all of the objects referenced by it as skeletons.
     *
     * @param obj an object to store
     * @throws ObjectStoreException if something goes wrong
     */
    public void store(FlyMineBusinessObject obj) throws ObjectStoreException {
//        store(obj, new ConsistentSet(), false);
    }

}
