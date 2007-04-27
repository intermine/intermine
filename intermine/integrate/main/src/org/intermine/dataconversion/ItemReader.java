package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
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
    
    /**
     * Returns a single item found at the end of the path. If a null reference is found
     * while traversing the path, then the method will return "null". If a collection of
     * objects is found while traversing the path, an ObjectStoreException will be thrown.
     * 
     * @param path the path expression
     * @param startingPoint the item to start at
     * @param variables values of variables in the path
     * @return Item found at the end of the path
     * @throws ObjectStoreException if something goes wrong or a collection
     */
    public Item getItemByPath(ItemPath path, Item startingPoint, Object variables[])
        throws ObjectStoreException;
    
    /**
     * Returns a single item found at the end of a path with no variables.
     * 
     * @param path the path expression
     * @param startingPoint the item to start at
     * @return Item found at the end of the path
     * @throws ObjectStoreException if something goes wrong or a collection
     * @see #getItemsByPath(ItemPath, Item, Object[])
     */
    public Item getItemByPath(ItemPath path, Item startingPoint) throws ObjectStoreException;
    
    /**
     * Returns a set of items found at the end of a path. Throws an exception if a collection
     * is found mid-way through the path. Returns null if null occurs at some point while
     * following the path.
     * 
     * @param path the path expression
     * @param startingPoint the item to start at
     * @param variables values of variables in the path
     * @return a List of Items found at the end of the path
     * @throws ObjectStoreException if something goes wrong
     */
    public List getItemsByPath(ItemPath path, Item startingPoint, Object variables[])
        throws ObjectStoreException;
    
    /**
     * Returns a set of items found at the end of a path with no variables.
     * 
     * @param path the path expression
     * @param startingPoint the item to start at
     * @return Item found at the end of the path
     * @throws ObjectStoreException if something goes wrong or a collection
     * @see #getItemsByPath(ItemPath, Item, Object[])
     */
    public List getItemsByPath(ItemPath path, Item startingPoint) throws ObjectStoreException;
}
