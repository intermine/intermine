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
import java.util.Iterator;
import java.util.List;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
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

import junit.framework.TestCase;

public class ItemToObjectTranslatorFunctionalTest extends TestCase
{
    public void testTranslation() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("test/testmodel_data.xml");
        List items = FullParser.parse(is);
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ItemStore itemStore = new ItemStore(osw);
        for (Iterator i = items.iterator(); i.hasNext();) {
            itemStore.store((Item) i.next());
        }

        Model model = Model.getInstanceByName("testmodel");
        Translator translator = new ItemToObjectTranslator(model);
        ObjectStore os = new ObjectStoreTranslatingImpl(model, osw, translator);

        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        List objects = new SingletonResults(q, os, os.getSequence());

        assertEquals(items, FullRenderer.toItems(objects, model));
    }
}
