package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.model.fulldata.Item;
import org.intermine.xml.full.ItemHelper;

/**
 * Mimic behaviour of an ItemWriter but actually store Items in a set rather than
 * an ObjectStoreWriter
 */
public class MockItemWriter implements ItemWriter {
    Map storedItems;

    public MockItemWriter(Map map) {
        storedItems = map;
    }

    public void store(Item item) throws ObjectStoreException {
        storedItems.put(item.getIdentifier(), item);
    }

    public void storeAll(Collection items) throws ObjectStoreException {
        for (Iterator i = items.iterator(); i.hasNext();) {
            store((Item) i.next());
        }
    }

    public void close() throws ObjectStoreException {}
    
    // this method is not part of ItemWriter, and for testing convenience returns xml items
    public Set getItems() {
        Set result = new HashSet();
        for (Iterator i = storedItems.values().iterator(); i.hasNext();) {
            result.add(ItemHelper.convert((Item) i.next()));
        }
        return result;
    }
}

