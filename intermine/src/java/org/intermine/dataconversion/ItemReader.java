package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;

import org.flymine.model.fulldata.Item;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Provides an interface between a DataTranslator and the source Item ObjectStore which it wishes to
 * read.
 *
 * @author Matthew Wakeling
 */
public interface ItemReader
{
    /**
     * Returns an iterator through the entire set of Items present in the source Item ObjectStore.
     *
     * @return an Iterator
     * @throws ObjectStoreException if something goes wrong
     */
    public Iterator itemIterator() throws ObjectStoreException;

    /**
     * Returns the Item from the source Item ObjectStore indexed by objectId.
     *
     * @param objectId the objectId of the Item
     * @return an Item
     * @throws ObjectStoreException if something goes wrong
     */
    public Item getItemById(String objectId) throws ObjectStoreException;

    /**
     * Returns a set of items of given class with a field constrained to the given value.
     *
     * @param className the class name of items to query
     * @param fieldName the field to constrain
     * @param value a value for fieldName
     * @return a set of Items
     * @throws ObjectStoreException if something goes wrong
     */
    public Iterator getItemsByAttributeValue(String className, String fieldName, String value)
        throws ObjectStoreException;

}
