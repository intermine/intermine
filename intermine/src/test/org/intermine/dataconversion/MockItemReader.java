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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.model.fulldata.Item;

public class MockItemReader implements ItemReader
{
    Map storedItems;

    public MockItemReader(Map map) {
        storedItems = map;
    }

    public Iterator itemIterator() throws ObjectStoreException {
        return storedItems.values().iterator();
    }

    public Item getItemById(String objectId) throws ObjectStoreException {
        return (Item) storedItems.get(objectId);
    }
}

