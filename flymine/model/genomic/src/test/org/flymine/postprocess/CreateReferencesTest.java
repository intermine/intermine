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
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.Item;

import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

/**
 * Tests for the CreateReferences class.
 */
public class CreateReferencesTest extends TestCase {

    private ObjectStoreWriter osw;
    private Model model;
    private Chromosome storedChromosome = null;
    private Gene storedGene = null;
    private Transcript storedTranscript = null;
    private Exon storedExon = null;
    private Location storedChromosomeRelation = null;
    private SimpleRelation storedTranscriptRelation = null;
    private RankedRelation storedExonRelation = null;
    private Orthologue storedOrthologue1 = null;
    private Orthologue storedOrthologue2 = null;
    private GOTerm storedGOTerm = null;
    private Annotation storedAnnotation = null;

    private static final Logger LOG = Logger.getLogger(CreateReferencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        osw.getObjectStore().flushObjectById();
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
        //        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.error("deleting: " +o.getId());
            osw.delete(o);
        }
        //        osw.commitTransaction();
        LOG.error("committed transaction");
        osw.close();
        LOG.error("closed objectstore");
    }

    public void testGeneOrthologueCollection() throws Exception {
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Gene.class, Orthologue.class, "subjects", "orthologues");
        compareGeneOrthologuesToExpected();
    }

    public void testGeneGOTermsCollection() throws Exception {
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Gene.class, GOTerm.class, "GOTerms");
        compareGeneGOTermsToExpected();
    }

    public void testInsertGeneTranscriptReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Gene.class, Transcript.class, SimpleRelation.class, "transcripts");

        compareGeneTranscriptResultsToExpected();
    }

    public void testInsertChromosomeExonReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Chromosome.class, "subjects", Relation.class, "subject",
                            Exon.class, "chromosome");

        compareChromosomeExonResultsToExpected();
    }

    public void testInsertReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences();

        compareResultsToExpected();
    }

    private void compareChromosomeExonResultsToExpected() throws Exception {
        osw.flushObjectById();
        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        expectedExon.setIdentifier("exon1");
        expectedExon.setId(storedExon.getId());

        Chromosome expectedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        expectedChromosome.setIdentifier("chr1");
        expectedChromosome.setId(storedChromosome.getId());

        Relation expectedChromosomeRelation =
            (Relation) DynamicUtil.createObject(Collections.singleton(Relation.class));
        expectedChromosomeRelation.setId(storedChromosomeRelation.getId());
        expectedChromosomeRelation.setObject(expectedChromosome);
        expectedChromosomeRelation.setSubject(expectedExon);
        expectedChromosome.setSubjects(Arrays.asList(new Object[] { expectedChromosomeRelation }));
        expectedChromosome.setExons(Arrays.asList(new Object[] { expectedExon }));
        expectedExon.setChromosome(expectedChromosome);

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setSubject(expectedExon);
        expectedExon.setObjects(Arrays.asList(new Object[] {
                                                  expectedChromosomeRelation,
                                                  expectedExonRelation
                                              }));

        Item expExonItem = toItem(expectedExon);
        Item expChromosomeItem = toItem(expectedChromosome);

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;


        q = new Query();
        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        q.addFrom(qcChromosome);
        q.addToSelect(qcChromosome);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Chromosome resChromosome = (Chromosome) row.get(0);
        Item resChromosomeItem = toItem(resChromosome);
        assertEquals(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        assertEquals(expExonItem, resExonItem);
    }

    private void compareGeneTranscriptResultsToExpected() throws Exception {
        osw.flushObjectById();

        Orthologue expectedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Annotation expectedAnnotation = (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedAnnotation.setId(storedAnnotation.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene1");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedAnnotation}));

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setIdentifier("trans1");
        expectedTranscript.setId(storedTranscript.getId());
        expectedTranscript.setGene(expectedGene);
        expectedGene.setTranscripts(Arrays.asList(new Object[] { expectedTranscript }));

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());
        expectedTranscriptRelation.setObject(expectedGene);
        expectedTranscriptRelation.setSubject(expectedTranscript);
        expectedTranscript.setObjects(Arrays.asList(new Object[] { expectedTranscriptRelation }));
        expectedGene.setSubjects(Arrays.asList(new Object[] { expectedOrthologue1, expectedTranscriptRelation }));

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setObject(expectedTranscript);
        expectedTranscript.setSubjects(Arrays.asList(new Object[] { expectedExonRelation }));

        Item expGeneItem = toItem(expectedGene);
        Item expTranscriptItem = toItem(expectedTranscript);

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;


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
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);


    }
    private void compareGeneOrthologuesToExpected() throws Exception {
        osw.flushObjectById();
        Orthologue expectedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());
        expectedOrthologue1.setObject(storedGene);

        // in gene1 objects collection
        Orthologue expectedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());
        expectedOrthologue2.setSubject(storedGene);

        Annotation expectedAnnotation = (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedAnnotation.setId(storedAnnotation.getId());

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setId(storedTranscript.getId());

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene1");
        expectedGene.setId(storedGene.getId());
        expectedGene.setOrthologues(Arrays.asList(new Object[] {expectedOrthologue1}));
        expectedGene.setSubjects(Arrays.asList(new Object[] {expectedOrthologue1, expectedTranscriptRelation}));
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setTranscripts(Arrays.asList(new Object[] {expectedTranscript}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedAnnotation}));

        Item expGeneItem = toItem(expectedGene);
        Item expOrthItem1 = toItem(expectedOrthologue1);
        Item expOrthItem2 = toItem(expectedOrthologue2);

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;

        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);
    }

    private void compareGeneGOTermsToExpected() throws Exception {
        osw.flushObjectById();
        GOTerm expectedGOTerm = (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        expectedGOTerm.setId(storedGOTerm.getId());

        Annotation expectedAnnotation = (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedAnnotation.setId(storedAnnotation.getId());

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setId(storedTranscript.getId());

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());

        Orthologue expectedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        // in gene1 objects collection
        Orthologue expectedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene1");
        expectedGene.setId(storedGene.getId());
        expectedGene.setSubjects(Arrays.asList(new Object[] {expectedOrthologue1, expectedTranscriptRelation}));
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setTranscripts(Arrays.asList(new Object[] {expectedTranscript}));
        expectedGene.setGOTerms(Arrays.asList(new Object[] {expectedGOTerm}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedAnnotation}));

        Item expGeneItem = toItem(expectedGene);

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;

        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);
    }

    private void compareResultsToExpected() throws Exception {
        osw.flushObjectById();

        Orthologue expectedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Annotation expectedAnnotation = (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedAnnotation.setId(storedAnnotation.getId());

        GOTerm expectedGOTerm = (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        expectedGOTerm.setId(storedGOTerm.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene1");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setOrthologues(Arrays.asList(new Object[] {expectedOrthologue1}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedAnnotation}));
        expectedGene.setGOTerms(Arrays.asList(new Object[] {expectedGOTerm}));

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setIdentifier("trans1");
        expectedTranscript.setId(storedTranscript.getId());
        expectedTranscript.setGene(expectedGene);
        expectedGene.setTranscripts(Arrays.asList(new Object[] { expectedTranscript }));

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());
        expectedTranscriptRelation.setObject(expectedGene);
        expectedTranscriptRelation.setSubject(expectedTranscript);
        expectedTranscript.setObjects(Arrays.asList(new Object[] { expectedTranscriptRelation }));
        expectedGene.setSubjects(Arrays.asList(new Object[] { expectedOrthologue1, expectedTranscriptRelation }));

        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        expectedExon.setIdentifier("exon1");
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

        Chromosome expectedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        expectedChromosome.setIdentifier("chr1");
        expectedChromosome.setId(storedChromosome.getId());

        Relation expectedChromosomeRelation =
            (Relation) DynamicUtil.createObject(Collections.singleton(Relation.class));
        expectedChromosomeRelation.setId(storedChromosomeRelation.getId());
        expectedChromosomeRelation.setObject(expectedChromosome);
        expectedChromosomeRelation.setSubject(expectedExon);
        expectedChromosome.setSubjects(Arrays.asList(new Object[] { expectedChromosomeRelation }));
        expectedChromosome.setExons(Arrays.asList(new Object[] { expectedExon }));
        expectedChromosome.setGenes(Arrays.asList(new Object[] { expectedGene }));
        expectedChromosome.setTranscripts(Arrays.asList(new Object[] { expectedTranscript }));
        expectedGene.setChromosome(expectedChromosome);
        expectedTranscript.setChromosome(expectedChromosome);
        expectedExon.setChromosome(expectedChromosome);
        expectedExon.setObjects(Arrays.asList(new Object[] {
                                                  expectedChromosomeRelation, expectedExonRelation
                                              }));

        Item expGeneItem = toItem(expectedGene);
        Item expTranscriptItem = toItem(expectedTranscript);
        Item expExonItem = toItem(expectedExon);
        Item expChromosomeItem = toItem(expectedChromosome);

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;


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
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);

        q = new Query();
        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        q.addFrom(qcChromosome);
        q.addToSelect(qcChromosome);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Chromosome resChromosome = (Chromosome) row.get(0);
        Item resChromosomeItem = toItem(resChromosome);
        assertEquals(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        assertEquals(expExonItem, resExonItem);

    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedChromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setIdentifier("chr1");

        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setIdentifier("gene1");

        storedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript.setIdentifier("trans1");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set
        storedTranscript.setGene(storedGene);

        storedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon.setIdentifier("exon1");

        storedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        storedTranscriptRelation.setObject(storedGene);
        storedTranscriptRelation.setSubject(storedTranscript);

        storedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        storedExonRelation.setObject(storedTranscript);
        storedExonRelation.setSubject(storedExon);
        storedExonRelation.setRank(new Integer(1));

        storedChromosomeRelation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedChromosomeRelation.setObject(storedChromosome);
        storedChromosomeRelation.setSubject(storedExon);

        storedAnnotation =  (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        storedAnnotation.setSubject(storedGene);

        storedGOTerm =  (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        storedGOTerm.setIdentifier("GOTerm1");
        storedAnnotation.setProperty(storedGOTerm);

        // in gene1 subject collection
        storedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        storedOrthologue1.setObject(storedGene);

        // in gene1 objects collection
        storedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        storedOrthologue2.setSubject(storedGene);


        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedGene, storedTranscriptRelation,
                                                    storedExonRelation, storedTranscript,
                                                    storedExon, storedChromosomeRelation,
                                                    storedChromosome, storedOrthologue1,
                                                    storedOrthologue2, storedAnnotation,
                                                    storedGOTerm
                                                }));
        Iterator i = toStore.iterator();
        osw.beginTransaction();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        osw.commitTransaction();
    }

    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = FullRenderer.toItem(o, model);
        return item;
    }

}
