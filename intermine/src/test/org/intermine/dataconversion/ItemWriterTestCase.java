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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.fulldata.Item;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.xml.full.FullParser;
import org.flymine.xml.full.ItemHelper;

public class ItemWriterTestCase extends TestCase {
    protected List items = new ArrayList();
    protected ItemWriter itemWriter;
    protected ObjectStoreWriter osw;
    
    public ItemWriterTestCase(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/FullParserTest.xml");
        List xmlItems = FullParser.parse(is);
        Iterator iter = xmlItems.iterator();
        while (iter.hasNext()) {
            items.add((Item) ItemHelper.convert((org.flymine.xml.full.Item) iter.next()));
        }
        iter = items.iterator();
        while (iter.hasNext()) {
            itemWriter.store((Item) iter.next());
        }
        itemWriter.close();
    }
    
    public void tearDown() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        Collection toDelete = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                .getSequence());
        Iterator iter = toDelete.iterator();
        osw.beginTransaction();
        while (iter.hasNext()) {
            FlyMineBusinessObject obj = (FlyMineBusinessObject) iter.next();
            System.out.println("Deleting " + obj);
            osw.delete(obj);
        }
        osw.commitTransaction();
        osw.close();
    }
    
    public void testGetItems() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Item.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        Collection results = new SingletonResults(q, osw, osw.getSequence());
        //assertEquals(items.size(), results.size());
        assertEquals(items, results);
    }
}

