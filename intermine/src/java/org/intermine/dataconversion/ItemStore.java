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
import org.flymine.util.StringUtil;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.Attribute;
import org.flymine.xml.full.Reference;
import org.flymine.xml.full.ReferenceList;

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
    * @throws ObjectStoreException if an error occuring in accessing the Items
    */
    public void store(Item item) throws ObjectStoreException {
        org.flymine.model.fulldata.Item dbItem = convert(item);
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
        return convert(getDbItemByIdentifier(identifier));
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
    * Convert an XML item to a data model one
    * @param item the XML item
    * @return an equivalent data model item
    */
    protected static org.flymine.model.fulldata.Item convert(Item item) {
        org.flymine.model.fulldata.Item newItem = new org.flymine.model.fulldata.Item();
        
        newItem.setIdentifier(item.getIdentifier());
        newItem.setClassName(item.getClassName());
        newItem.setImplementations(item.getImplementations());
        
        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            Attribute attr = (Attribute) i.next();
            org.flymine.model.fulldata.Attribute newAttr 
            = new org.flymine.model.fulldata.Attribute();
            newAttr.setName(attr.getName());
            newAttr.setValue(attr.getValue());
            newItem.getAttributes().add(newAttr);
        }
        
        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            Reference ref = (Reference) i.next();
            org.flymine.model.fulldata.Reference newRef
            = new org.flymine.model.fulldata.Reference();
            newRef.setName(ref.getName());
            newRef.setRefId(ref.getRefId());
            newItem.getReferences().add(newRef);
        }

        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            ReferenceList refs = (ReferenceList) i.next();
            org.flymine.model.fulldata.ReferenceList newRefs
                = new org.flymine.model.fulldata.ReferenceList();
            newRefs.setName(refs.getName());
            newRefs.setRefIds(StringUtil.join(refs.getRefIds(), " "));
            newItem.getCollections().add(newRefs);
        }

        return newItem;
    }
    
    /**
    * Convert a data model item to an XML one
    * @param item the data model Item
    * @return an equivalent XML Item
    */
    public static Item convert(org.flymine.model.fulldata.Item item) {
        Item newItem = new Item();
        newItem.setIdentifier(item.getIdentifier());
        newItem.setClassName(item.getClassName());
        newItem.setImplementations(item.getImplementations());
        
        for (Iterator i = item.getAttributes().iterator(); i.hasNext();) {
            org.flymine.model.fulldata.Attribute attr =
                (org.flymine.model.fulldata.Attribute) i.next();
            Attribute newAttr = new Attribute();
            newAttr.setName(attr.getName());
            newAttr.setValue(attr.getValue());
            newItem.addAttribute(newAttr);
        }

        for (Iterator i = item.getReferences().iterator(); i.hasNext();) {
            org.flymine.model.fulldata.Reference ref =
                (org.flymine.model.fulldata.Reference) i.next();
            Reference newRef = new Reference();
            newRef.setName(ref.getName());
            newRef.setRefId(ref.getRefId());
            newItem.addReference(newRef);
        }

        for (Iterator i = item.getCollections().iterator(); i.hasNext();) {
            org.flymine.model.fulldata.ReferenceList refs
                = (org.flymine.model.fulldata.ReferenceList) i.next();
            ReferenceList newRefs = new ReferenceList();
            newRefs.setName(refs.getName());
            newRefs.setRefIds(StringUtil.tokenize(refs.getRefIds()));
            newItem.addCollection(newRefs);
        }

        return newItem;
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
            return convert((org.flymine.model.fulldata.Item) resultsRow.get(0));
        }
        
        /**
        * @see Iterator#remove
        */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
