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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.ItemHelper;

/**
 * Mimic behaviour of an ItemWriter but actually store Items in a Map rather than
 * an ObjectStoreWriter
 * @author MarkWoodbridge
 */
public class MockItemWriter implements ItemWriter
{
    Map storedItems;

    /**
     * Constructor
     * @param map Map in which to store Items
     */
    public MockItemWriter(Map map) {
        storedItems = map;
    }

    /**
     * {@inheritDoc}
     */
    public void store(Item item) {
        storedItems.put(item.getIdentifier(), item);
    }

    /**
     * {@inheritDoc}
     */
    public void storeAll(Collection<Item> items) {
        for (Iterator i = items.iterator(); i.hasNext();) {
            store((Item) i.next());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void close() {
    }
    
    /**
     * Get the items that have been stored
     * This method is not part of ItemWriter, and for testing convenience returns XML Items
     * @return the Items stored so far, in "XML" format
     */
    public Set getItems() {
        Set result = new HashSet();
        for (Iterator i = storedItems.values().iterator(); i.hasNext();) {
            result.add(ItemHelper.convert((Item) i.next()));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void store(ReferenceList refList) throws ObjectStoreException {
        throw new UnsupportedOperationException("store(ReferenceList) not implemented");
    }
}
