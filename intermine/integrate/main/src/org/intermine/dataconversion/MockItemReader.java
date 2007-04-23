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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.intermine.model.fulldata.Item;

/**
 * An ItemReader backed by a Map of Items identified by id
 * @author Mark Woodbridge
 */
public class MockItemReader extends AbstractItemReader
{
    Map storedItems;

    /**
     * Constructor
     * @param map Map from which items are read
     */
    public MockItemReader(Map map) {
        storedItems = map;
    }

    /**
     * {@inheritDoc}
     */
    public Item getItemById(String objectId) {
        return (Item) storedItems.get(objectId);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator itemIterator() {
        return storedItems.values().iterator();
    }

    /**
     * Read items of one particular class only, or exclude one particular class.
     * @param clsName fully qualified name of class to include/exclude
     * @param notEquals if true then exclude the given classname
     * @return an iterator over the selected items
     */
    public Iterator itemIterator(String clsName, boolean notEquals) {
        return storedItems.values().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public List getItemsByDescription(Set constraints) {
        List items = new ArrayList();

        Iterator i = itemIterator();
        while (i.hasNext()) {
            Item item = (Item) i.next();

            boolean matches = true;
            Iterator descIter = constraints.iterator();
            while (descIter.hasNext()) {
                FieldNameAndValue f = (FieldNameAndValue) descIter.next();
                if (!f.matches(item)) {
                    matches = false;
                }
            }
            if (matches) {
                items.add(item);
            }
        }
        return items;
    }
}
