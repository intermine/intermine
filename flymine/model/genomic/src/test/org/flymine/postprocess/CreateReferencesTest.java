package org.flymine.postprocess;

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
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.io.InputStream;

import org.intermine.objectstore.*;
import org.intermine.objectstore.query.*;
import org.intermine.dataloader.IntegrationWriterFactory;
import org.intermine.dataloader.XmlDataLoader;
import org.intermine.dataloader.IntegrationWriter;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.datatracking.Source;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.FullRenderer;
import org.intermine.xml.full.Item;

import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

/**
 * Tests for the CreateReferences class.
 */
public class CreateReferencesTest extends TestCase {

    private ObjectStoreWriter osw;
    private Model model;
    private Gene storedGene = null;
    private Transcript storedTranscript = null;
    private SimpleRelation storedRelation = null;

    private static final Logger LOG = Logger.getLogger(CreateReferencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        model = Model.getInstanceByName("genomic");
        createData();
    }

    public void tearDown() throws Exception {
        LOG.error("in tear down");
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        LOG.error("created results");
        Iterator resIter = res.iterator();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.error("deleting: " +o.getId());
            osw.delete(o);
        }
        osw.close();
        LOG.error("closed objectstore");
    }


    public void testCreatesReferencesToGene() throws Exception {
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences();
        
        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setName("adh");
        expectedGene.setId(storedGene.getId());

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setName("trans1");
        expectedTranscript.setId(storedTranscript.getId());
        
        SimpleRelation expectedRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedRelation.setId(storedRelation.getId());

        List expectedTranscriptsList = Arrays.asList(new Object[] { expectedTranscript });
        expectedGene.setTranscripts(expectedTranscriptsList);

        List expectedRelationsList = Arrays.asList(new Object[] { expectedRelation });
        expectedGene.setSubjects(expectedRelationsList);

        Item expGeneItem = toItem(expectedGene);

        Query q = new Query();
        QueryClass qcObj = new QueryClass(Gene.class);
        q.addFrom(qcObj);
        q.addToSelect(qcObj);
        ObjectStore os = osw.getObjectStore();
        Results res = new Results(q, os, os.getSequence());
        ResultsRow row = (ResultsRow) res.iterator().next();
        Gene resGene = (Gene) row.get(0);

        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);
    }

    private void createData() throws Exception {
        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setName("adh");

        storedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript.setName("tran1");

        storedRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        storedRelation.setObject(storedGene);
        storedRelation.setSubject(storedTranscript);

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedGene, storedRelation, storedTranscript
                                                }));
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
    }

    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = FullRenderer.toItem(o, model);
        return item;
    }
}
