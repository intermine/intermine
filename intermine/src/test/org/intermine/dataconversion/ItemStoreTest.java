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

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.xml.full.FullParser;
import org.flymine.xml.full.Item;

public class ItemStoreTest extends TestCase {
    protected List items;
    protected ItemStore itemStore;
    
    public void setUp() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/FullParserTest.xml");
        items = FullParser.parse(is);
        itemStore = new ItemStore(ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest"));
        for (Iterator i = items.iterator(); i.hasNext();) {
            itemStore.store((Item) i.next());
        }
    }
    
    public void tearDown() throws Exception {
        for (Iterator i = items.iterator(); i.hasNext();) {
            itemStore.delete((Item) i.next());
        }
    }
    
    public void testGetItems() throws Exception {
        List newItems = new ArrayList();
        for (Iterator i = itemStore.getItems(); i.hasNext();) {
            newItems.add(i.next());
        }
        assertEquals(items, newItems);
    }
    
    public void testGetItemByIdentifier() throws Exception {
        Item expected = (Item) items.get(0);
        assertEquals(expected, itemStore.getItemByIdentifier(expected.getIdentifier()));
    }
}
