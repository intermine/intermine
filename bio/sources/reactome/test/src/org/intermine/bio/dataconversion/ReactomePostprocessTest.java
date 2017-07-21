package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.custommonkey.xmlunit.XMLTestCase;
import org.intermine.bio.postprocess.ReactomePostProcess;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the BioPAXPostprocess class.
 */
public class ReactomePostprocessTest extends XMLTestCase {

    private ObjectStoreWriter osw;

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        setUpData();
    }

    @SuppressWarnings("rawtypes")
    public void tearDown() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = os.executeSingleton(q);
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();
    }

    public void testPostProcess() throws Exception {
        ReactomePostProcess bp = new ReactomePostProcess(osw);
        bp.postProcess();

        Gene resGene = (Gene) getFromDb(Gene.class).iterator().next();

        // Gene should come back with a collection of pathways
        assertEquals(2, resGene.getPathways().size());
    }


    // Store a gene with two proteins, each protein has a pathway
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void setUpData() throws Exception {
        DataSet dataset = (DataSet) DynamicUtil.createObject(DataSet.class);
        dataset.setName("Reactome pathways data set");
        Gene gene = (Gene) DynamicUtil.createObject(Gene.class);
        Protein protein1 = (Protein) DynamicUtil.createObject(Protein.class);
        protein1.addGenes(gene);
        Protein protein2 = (Protein) DynamicUtil.createObject(Protein.class);
        protein2.addGenes(gene);

        Pathway pathway1 = (Pathway) DynamicUtil.createObject(Pathway.class);
        protein1.addPathways(pathway1);
        pathway1.addDataSets(dataset);
        Pathway pathway2 = (Pathway) DynamicUtil.createObject(Pathway.class);
        protein2.addPathways(pathway2);
        pathway2.addDataSets(dataset);

        List toStore = new ArrayList(Arrays.asList(new Object[] {dataset, gene, protein1, protein2, pathway1, pathway2}));

        osw.beginTransaction();
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        osw.commitTransaction();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Set<InterMineObject> getFromDb(Class relClass) throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(relClass);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Set<InterMineObject> results = new HashSet<InterMineObject>();
        Iterator resIter = res.iterator();
        while(resIter.hasNext()) {
            results.add((InterMineObject) resIter.next());
        }
        ObjectStore os = osw.getObjectStore();
        os.flushObjectById();
        return results;
    }
}
