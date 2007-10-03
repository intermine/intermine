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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.model.fulldata.Attribute;
import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.xml.full.ItemHelper;

/**
 * Mimic behaviour of an ItemWriter but actually store Items in a Map rather than
 * an ObjectStoreWriter
 * @author MarkWoodbridge
 */
public class MockItemWriter implements ItemWriter
{
    Map<String, Item> storedItems;
    Map<Integer, Item> storedItemIds = new HashMap<Integer, Item>();
    private static int idCounter = 0;

    /**
     * Constructor
     * @param map Map in which to store Items
     */
    public MockItemWriter(Map<String, Item> map) {
        storedItems = map;
    }

    /**
     * {@inheritDoc}
     */
    public Integer store(Item item) {
        item.setId(idCounter++);
        storedItems.put(item.getIdentifier(), item);
        storedItemIds.put(item.getId(), item);
        return item.getId();
    }

    /**
     * {@inheritDoc}
     */
    public void store(ReferenceList refList, Integer itemId) {
        refList.setId(idCounter++);
        Item item = storedItemIds.get(itemId);
        refList.setItem(item);
        item.addCollections(refList);
    }

    /**
     * {@inheritDoc}
     */
    public void store(Reference ref, Integer itemId) {
        ref.setId(idCounter++);
        Item item = storedItemIds.get(itemId);
        ref.setItem(item);
        item.addReferences(ref);
    }

    /**
     * {@inheritDoc}
     */
    public void store(Attribute ref, Integer itemId) {
        ref.setId(idCounter++);
        Item item = storedItemIds.get(itemId);
        ref.setItem(item);
        item.addAttributes(ref);
    }

    /**
     * {@inheritDoc}
     */
    public void storeAll(Collection<Item> items) {
        for (Iterator<Item> i = items.iterator(); i.hasNext();) {
            store(i.next());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        // empty
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
    public String toString() {
        return "" + storedItems;
    }
}
