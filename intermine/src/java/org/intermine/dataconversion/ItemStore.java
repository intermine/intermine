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
import java.util.NoSuchElementException;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.ItemHelper;

/**
* Store, retrieve and delete Items, performing the necessary conversion to database format
* @author Mark Woodbridge
*/
public class ItemStore
{
    protected ObjectStoreWriter osw;
    
    /**
    * Constructor
    * @param osw the ObjectStoreWriter used to access the Items
    */
    public ItemStore(ObjectStoreWriter osw) {
        this.osw = osw;
    }
    
    /**
    * Retrieve all the items from the database
    * @return Iterator an Iterator over the items
    * @throws ObjectStoreException if an error occuring in accessing the Items
    */
    public Iterator getItems() throws ObjectStoreException {
        FqlQuery fqlQuery = new FqlQuery("select i from Item as i", "org.flymine.model.fulldata");
        return new ItemIterator(osw.execute(fqlQuery.toQuery()));
    }
    
    /**
     * Store an item
     * @param item the Item to store
     * @throws ObjectStoreException if an error occuring in storing the Item
     */
    public void store(Item item) throws ObjectStoreException {
        store(ItemHelper.convert(item));
    }
    
    /**
     * Store a fulldata Item
     * @param dbItem the Item to store
     * @throws ObjectStoreException if an error occuring in storing the Item
     */
    public void store(org.flymine.model.fulldata.Item dbItem) throws ObjectStoreException {
        for (Iterator i = dbItem.getAttributes().iterator(); i.hasNext();) {
            osw.store((FlyMineBusinessObject) i.next());
        }
        for (Iterator i = dbItem.getReferences().iterator(); i.hasNext();) {
            osw.store((FlyMineBusinessObject) i.next());
        }
        for (Iterator i = dbItem.getCollections().iterator(); i.hasNext();) {
            osw.store((FlyMineBusinessObject) i.next());
        }
        osw.store((FlyMineBusinessObject) dbItem);
    }
    
    /**
    * Delete an item
    * @param item the Item to delete
    * @throws ObjectStoreException if an error occuring in accessing the Items
    */
    public void delete(Item item) throws ObjectStoreException {
        org.flymine.model.fulldata.Item dbItem = getDbItemByIdentifier(item.getIdentifier());
        for (Iterator i = dbItem.getAttributes().iterator(); i.hasNext();) {
            osw.delete((FlyMineBusinessObject) i.next());
        }
        for (Iterator i = dbItem.getReferences().iterator(); i.hasNext();) {
            osw.delete((FlyMineBusinessObject) i.next());
        }
        for (Iterator i = dbItem.getCollections().iterator(); i.hasNext();) {
            osw.delete((FlyMineBusinessObject) i.next());
        }
        osw.delete((FlyMineBusinessObject) dbItem);
    }
    
    /**
    * Retrieve an Item by identifier
    * @param identifier the identifier for an Item
    * @return the relevant Item
    * @throws ObjectStoreException if an error occuring in accessing the Items
    */
    public Item getItemByIdentifier(String identifier) throws ObjectStoreException {
        return ItemHelper.convert(getDbItemByIdentifier(identifier));
    }
    
    /**
    * Retrieve a data model Item by identifier
    * @param identifier the identifier for an Item
    * @return the relevant data model Item
    * @throws ObjectStoreException if an error occuring in accessing the Items
    */
    protected org.flymine.model.fulldata.Item getDbItemByIdentifier(String identifier)
        throws ObjectStoreException {
        Query q = new FqlQuery("select item from Item as item where item.identifier='" + identifier
            + "'", "org.flymine.model.fulldata").toQuery();
        Results r = osw.execute(q);
        return (org.flymine.model.fulldata.Item) ((ResultsRow) r.get(0)).get(0);
    }
    
    /**
    * Class to iterate through Items, converting from database format
    */
    class ItemIterator implements Iterator
    {
        protected Iterator resIter;
      
        /**
        * Constructor
        * @param res the underlying results set
        */
        public ItemIterator(Results res) {
            resIter = res.iterator();
        }
        
        /**
        * @see Iterator#hasNext
        */
        public boolean hasNext() {
            return resIter.hasNext();
        }
        
        /**
        * @see Iterator#next
        */
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ResultsRow resultsRow = (ResultsRow) resIter.next();
            return ItemHelper.convert((org.flymine.model.fulldata.Item) resultsRow.get(0));
        }
        
        /**
        * @see Iterator#remove
        */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
