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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.objectstore.translating.Translator;
import org.flymine.objectstore.translating.ObjectStoreTranslatingImpl;
import org.flymine.xml.full.Item;
import org.flymine.xml.full.FullRenderer;
import org.flymine.xml.full.FullParser;
import org.flymine.xml.full.ItemHelper;

import junit.framework.TestCase;

public class ItemToObjectTranslatorFunctionalTest extends TestCase
{
    List items;
    
    public ItemToObjectTranslatorFunctionalTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        items = FullParser.parse(is);
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ObjectStoreItemWriter itemWriter = new ObjectStoreItemWriter(osw);
        for (Iterator i = items.iterator(); i.hasNext();) {
            itemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        itemWriter.close();
    }

    public void tearDown() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
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
 
    public void testTranslation() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Translator translator = new ItemToObjectTranslator(model);
        ObjectStore os = new ObjectStoreTranslatingImpl(model, ObjectStoreFactory.getObjectStore("os.unittest"), translator);

        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        List objects = new SingletonResults(q, os, os.getSequence());

        assertEquals(items, FullRenderer.toItems(objects, model));
    }
}
