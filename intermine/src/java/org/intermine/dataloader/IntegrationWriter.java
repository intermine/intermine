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
import org.flymine.model.datatracking.Source;
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
     * Stores the given object in the objectstore. To update an objectstore with data in the form
     * of an interconnected graph of objects, call this method on each of the objects in that
     * structure. This method will take care of merging objects, and resolving field priority
     * issues.
     *
     * @param o the object to store
     * @param source the data Source to which to attribute the data
     * @throws ObjectStoreException if an error occurs in the underlying objectstore
     */
    public void store(FlyMineBusinessObject o, Source source) throws ObjectStoreException;
}
