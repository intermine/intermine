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
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.ItemHelper;

/**
 * Mimic behaviour ItemStore but actually store Items in a set rather than
 * an ObjectStoreWriter.
 */
public class MockItemStore extends ItemStore
{
    protected Map items;

    public MockItemStore(ObjectStoreWriter osw) {
        super(osw);
        throw new UnsupportedOperationException("cannot construct a MockItemStore with an ObjectStoreWriter");
    }

    public MockItemStore() {
        super(null);
        items = new HashMap();
    }

    public Iterator getItems() throws ObjectStoreException {
        return new ItemIterator(items.values());
    }

    public Set getItemSet() throws ObjectStoreException {
        Set ret = new HashSet();
        Iterator i = getItems();
        while (i.hasNext()) {
            ret.add((Item) i.next());
        }
        return ret;
    }

    public void store(Item item) {
        items.put(item.getIdentifier(), ItemHelper.convert(item));
    }

    public void store(org.flymine.model.fulldata.Item dbItem) {
        items.put(dbItem.getIdentifier(), dbItem);
    }

    public void delete(Item item) {
        items.remove(item.getIdentifier());
    }

    public Item getItemByIdentifier(String identifier) {
        return ItemHelper.convert((org.flymine.model.fulldata.Item) items.get(identifier));
    }

    class ItemIterator implements Iterator
    {
        private Iterator itemIter;

        public ItemIterator(Collection items) {
            itemIter = items.iterator();
        }

        public boolean hasNext() {
            return itemIter.hasNext();
        }

        public Object next() {
            return ItemHelper.convert((org.flymine.model.fulldata.Item) itemIter.next());
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
