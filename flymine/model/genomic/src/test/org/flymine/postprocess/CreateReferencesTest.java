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
    private Exon storedExon = null;
    private SimpleRelation storedTranscriptRelation = null;
    private RankedRelation storedExonRelation = null;

    private static final Logger LOG = Logger.getLogger(CreateReferencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        model = Model.getInstanceByName("genomic");
    }

    public void testInsertReferences() throws Exception {
        createData();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Gene.class, Transcript.class, SimpleRelation.class, "transcripts");
        cr.insertReferences(Transcript.class, Exon.class, RankedRelation.class, "exons");

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setName("gene1");
        expectedGene.setId(storedGene.getId());
  
        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setName("trans1");
        expectedTranscript.setId(storedTranscript.getId());
        expectedTranscript.setGene(expectedGene);
        expectedGene.setTranscripts(Arrays.asList(new Object[] { expectedTranscript }));
  
        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());
        expectedTranscriptRelation.setObject(expectedGene);
        expectedTranscriptRelation.setSubject(expectedTranscript);
        expectedTranscript.setObjects(Arrays.asList(new Object[] { expectedTranscriptRelation }));
        expectedGene.setSubjects(Arrays.asList(new Object[] { expectedTranscriptRelation }));
  
        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        expectedExon.setName("exon1");
        expectedExon.setId(storedExon.getId());
        expectedTranscript.setExons(Arrays.asList(new Object[] {expectedExon}));
        expectedExon.setTranscripts(Arrays.asList(new Object[] {expectedTranscript}));
  
        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setObject(expectedTranscript);
        expectedExonRelation.setSubject(expectedExon);
        expectedTranscript.setSubjects(Arrays.asList(new Object[] { expectedExonRelation }));
        expectedExon.setObjects(Arrays.asList(new Object[] { expectedExonRelation }));
  
        Item expGeneItem = toItem(expectedGene);
        Item expTranscriptItem = toItem(expectedTranscript);
        Item expExonItem = toItem(expectedExon);
  
        ObjectStore os = osw.getObjectStore();
  
        Query q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        Results res = new Results(q, os, os.getSequence());
        ResultsRow row = (ResultsRow) res.iterator().next();
  
        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
  
        assertEquals(expGeneItem, resGeneItem);
  
        q = new Query();
        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);
  
        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();
  
        Transcript resTranscript = (Transcript) row.get(0);
        Item resTranscriptItem = toItem(resTranscript);
        assertEquals(expTranscriptItem, resTranscriptItem);
  
        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);
  
        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();
  
        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        assertEquals(expExonItem, resExonItem);
        removeData();
    }

    public void testInsertGeneReferences() throws Exception {
        createData();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Gene.class, Transcript.class, SimpleRelation.class, "transcripts");
        cr.insertReferences(Transcript.class, Exon.class, RankedRelation.class, "exons");
        cr.insertGeneExonReferences("exons");

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setName("gene1");
        expectedGene.setId(storedGene.getId());

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setName("trans1");
        expectedTranscript.setId(storedTranscript.getId());
        expectedTranscript.setGene(expectedGene);
        expectedGene.setTranscripts(Arrays.asList(new Object[] { expectedTranscript }));

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());
        expectedTranscriptRelation.setObject(expectedGene);
        expectedTranscriptRelation.setSubject(expectedTranscript);
        expectedTranscript.setObjects(Arrays.asList(new Object[] { expectedTranscriptRelation }));
        expectedGene.setSubjects(Arrays.asList(new Object[] { expectedTranscriptRelation }));

        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        expectedExon.setName("exon1");
        expectedExon.setId(storedExon.getId());
        expectedTranscript.setExons(Arrays.asList(new Object[] {expectedExon}));
        expectedExon.setTranscripts(Arrays.asList(new Object[] {expectedTranscript}));
        expectedGene.setExons(Arrays.asList(new Object[] {expectedExon}));
        expectedExon.setGene(expectedGene);

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setObject(expectedTranscript);
        expectedExonRelation.setSubject(expectedExon);
        expectedTranscript.setSubjects(Arrays.asList(new Object[] { expectedExonRelation }));
        expectedExon.setObjects(Arrays.asList(new Object[] { expectedExonRelation }));

        Item expGeneItem = toItem(expectedGene);
        Item expTranscriptItem = toItem(expectedTranscript);
        Item expExonItem = toItem(expectedExon);

        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        Results res = new Results(q, os, os.getSequence());
        ResultsRow row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);

        assertEquals(expGeneItem, resGeneItem);

        q = new Query();
        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Transcript resTranscript = (Transcript) row.get(0);
        Item resTranscriptItem = toItem(resTranscript);
        assertEquals(expTranscriptItem, resTranscriptItem);

        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        assertEquals(expExonItem, resExonItem);
        removeData();
    }
    
    private void createData() throws Exception {
        osw.flushObjectById();

        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setName("gene1");
        
        storedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript.setName("trans1");
        
        storedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon.setName("exon1");
        
        storedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        storedTranscriptRelation.setObject(storedGene);
        storedTranscriptRelation.setSubject(storedTranscript);
        
        storedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        storedExonRelation.setObject(storedTranscript);
        storedExonRelation.setSubject(storedExon);
        storedExonRelation.setRank(new Integer(1));
        
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedGene, storedTranscriptRelation,
                                                    storedExonRelation, storedTranscript,
                                                    storedExon
                                                }));
        Iterator i = toStore.iterator();
        osw.beginTransaction();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        osw.commitTransaction();
    }
    
    private void removeData() throws Exception {
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
    }

    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = FullRenderer.toItem(o, model);
        return item;
    }
}
