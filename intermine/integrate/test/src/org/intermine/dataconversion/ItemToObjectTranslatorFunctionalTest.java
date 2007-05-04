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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.translating.Translator;
import org.intermine.objectstore.translating.ObjectStoreTranslatingImpl;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.ItemHelper;

import junit.framework.TestCase;

public class ItemToObjectTranslatorFunctionalTest extends TestCase
{
    List items;

    public ItemToObjectTranslatorFunctionalTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("testmodel_data.xml");
        items = FullParser.parse(is);
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ObjectStoreItemWriter itemWriter = new ObjectStoreItemWriter(osw);
        for (Iterator i = items.iterator(); i.hasNext();) {
            itemWriter.store(ItemHelper.convert((Item) i.next()));
        }
        itemWriter.close();
        osw.close();
    }

    public void tearDown() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        Collection toDelete = osw.getObjectStore().executeSingleton(q);
        Iterator iter = toDelete.iterator();
        osw.beginTransaction();
        while (iter.hasNext()) {
            InterMineObject obj = (InterMineObject) iter.next();
            System.out.println("Deleting " + obj);
            osw.delete(obj);
        }
        osw.commitTransaction();
        osw.close();
    }

    public void testTranslation() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        ObjectStore sub = ObjectStoreFactory.getObjectStore("os.unittest");
        Translator translator = new ItemToObjectTranslator(model, sub);
        ObjectStore os = new ObjectStoreTranslatingImpl(model, sub, translator);

        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        List objects = os.executeSingleton(q);

        assertEquals(items, FullRenderer.toItems(objects, model));
    }
}
