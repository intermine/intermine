package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.intermine.model.fulldata.Item;
import org.intermine.objectstore.ObjectStoreException;

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
     * Returns a set of items with the fields constrained to certain values. This method takes a Set
     * of FieldNameAndValue objects, each describing a constraint on a field of the items to be
     * returned. The field names "identifier" and "classname" are mapped onto the Item fields of
     * those names, and the remaining FieldNameAndValue objects are searched for with the Attribute
     * objects attached to the Item.
     *
     * @param constraints a Set of FieldNameAndValue objects
     * @return a List of Items
     * @throws ObjectStoreException if something goes wrong
     */
    public List getItemsByDescription(Set constraints) throws ObjectStoreException;
}
