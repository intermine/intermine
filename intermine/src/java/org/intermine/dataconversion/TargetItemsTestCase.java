package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;

import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemHelper;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.translating.ObjectStoreTranslatingImpl;
import org.intermine.model.InterMineObject;
import org.intermine.metadata.Model;

/**
 * TestCase that specific data sources can use to test that translated/converted
 * items materialise correctly in given model.
 *
 * @author Richard Smith
 */
public abstract class TargetItemsTestCase extends TestCase
{
    ObjectStoreWriter osw;

    /**
     * Set up.
     * @throws Exception if anyhting goes wrong
     */
    public void setUp() throws Exception {
        osw = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.fulldatatest");
    }

    /**
     * Delete items put in temporary objectstore.
     * @throws Exception if anything goes wrong
     */
    public void tearDown() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        Collection toDelete = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                .getSequence());
        Iterator iter = toDelete.iterator();
        osw.beginTransaction();
        while (iter.hasNext()) {
            InterMineObject obj = (InterMineObject) iter.next();
            osw.delete(obj);
        }
        osw.commitTransaction();
        osw.close();
    }


    /**
     * Test that converted/translated items can be made into business objects.  Will highlight
     * problems with model incompatibility.
     * @throws Exception if anything goes wrong
     */
    public void testItemToObject() throws Exception {
        ItemWriter iw = new ObjectStoreItemWriter(osw);

        // store items
        Iterator i = getExpectedItems().iterator();
        while (i.hasNext()) {
            Item item = (Item) i.next();
            iw.store(ItemHelper.convert(item));
        }
        iw.close();

        ItemToObjectTranslator t
            = new ItemToObjectTranslator(Model.getInstanceByName(getModelName()),
                                         osw.getObjectStore());
        ObjectStore os = new ObjectStoreTranslatingImpl(Model.getInstanceByName(getModelName()),
                                                        osw.getObjectStore(), t);
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
        }
    }

    /**
     * Store a collection of items in a Map that can be used with a MockItemReader.
     * @param items the collection of items
     * @return the item map
     * @throws Exception if anything goes wrong
     */
    protected Map writeItems(Collection items) throws Exception {
        LinkedHashMap itemMap = new LinkedHashMap();
        ItemWriter iw = new MockItemWriter(itemMap);
        Iterator i = items.iterator();
        while (i.hasNext()) {
            iw.store(ItemHelper.convert((Item) i.next()));
        }
        return itemMap;
    }

    /**
     * Get the Collection of test expected Items
     * @return the Collection of Items
     * @throws Exception if an error occurs
     */
    protected abstract Collection getExpectedItems() throws Exception;

    /**
     * Get the name of the Model that the target Items should conform to
     * @return the Model name
     */
    protected abstract String getModelName();
}
