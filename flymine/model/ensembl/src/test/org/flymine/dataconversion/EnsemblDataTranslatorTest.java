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

import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.ontology.OntologyUtil;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.FullParser;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.translating.ObjectStoreTranslatingImpl;
import org.intermine.model.InterMineObject;
import org.intermine.dataloader.IntegrationWriterSingleSourceImpl;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.dataloader.DataLoader;
import org.intermine.dataloader.ObjectStoreDataLoader;
import org.intermine.metadata.Model;

public class EnsemblDataTranslatorTest extends TestCase {
    private String srcNs = "http://www.flymine.org/model/ensembl#";
    private String tgtNs = "http://www.flymine.org/model/genomic#";
    protected Map itemMap;
    ObjectStoreWriter osw;

    public void setUp() throws Exception {
        itemMap = new LinkedHashMap();
        osw = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.fulldatatest");
    }

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
            System.out.println("Deleting " + obj);
            osw.delete(obj);
        }
        osw.commitTransaction();
        osw.close();
    }


    public void testEnsemblTranslator() throws Exception {
        LinkedHashMap itemMap = new LinkedHashMap();
        ItemWriter iw = new MockItemWriter(itemMap);
        Iterator i = getSrcItems().iterator();
        while (i.hasNext()) {
            iw.store(ItemHelper.convert((Item) i.next()));
        }
        OntModel model = getInterMineOwl();
        DataTranslator translator = new EnsemblDataTranslator(new MockItemReader(itemMap), getInterMineOwl(), tgtNs, "wildebeast", "W. beast", "1001");
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }

    public void testDataLoadEnsembl() throws Exception {
        ObjectStoreWriter osw = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.fulldatatest");
        ItemWriter iw = new ObjectStoreItemWriter(osw);

        // store items
        Iterator i = getExpectedItems().iterator();
        while (i.hasNext()) {
            Item item = (Item) i.next();
            iw.store(ItemHelper.convert(item));
        }
        iw.close();

        ItemToObjectTranslator t = new ItemToObjectTranslator(Model.getInstanceByName("genomic"), osw.getObjectStore());
        ObjectStore os = new ObjectStoreTranslatingImpl(Model.getInstanceByName("genomic"), osw.getObjectStore(), t);
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setDistinct(false);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        Iterator iter = res.iterator();
        while (iter.hasNext()) {
            //InterMineObject obj = t.translateFromDbObject((InterMineObject) iter.next());
            InterMineObject o = (InterMineObject) iter.next();
        }
    }


    private Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_tgt.xml"));
    }

    private Collection getSrcItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/EnsemblDataTranslatorFunctionalTest_src.xml"));
    }


    protected OntModel getInterMineOwl() {
        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("genomic.n3"));

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(reader, null, "N3");
        return ont;
    }

    private class MockIntegrationWriter {


    }
}
