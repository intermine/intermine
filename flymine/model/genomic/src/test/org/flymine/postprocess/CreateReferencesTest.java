package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.xml.full.ItemFactory;

import org.flymine.model.genomic.*;
import org.apache.log4j.Logger;

/**
 * Tests for the CreateReferences class.
 */
public class CreateReferencesTest extends TestCase {

    private ObjectStoreWriter osw;
    private Model model;
    private Chromosome storedChromosome = null;
    private Protein storedProtein = null;
    private Protein storedProtein1 = null;
    private Protein storedProtein2 = null;
    private Protein storedProtein3 = null;
    private Gene storedGene = null;
    private Gene storedGene1 = null;
    private Gene storedGene2 = null;
    private Transcript storedTranscript = null;
    private Transcript storedTranscript1 = null;
    private Transcript storedTranscript2 = null;
    private Transcript storedTranscript3 = null;
    private Exon storedExon = null;
    private Location storedChromosomeRelation = null;
    private SimpleRelation storedTranscriptRelation = null;
    private RankedRelation storedExonRelation = null;
    private Orthologue storedOrthologue1 = null;
    private Orthologue storedOrthologue2 = null;
    private GOTerm storedGOTerm = null;
    private Phenotype storedPhenotype = null;
    private Annotation storedGOTermAnnotation = null;
    private Annotation storedPhenotypeAnnotation = null;
    private Evidence storedPhenotypeEvidence = null;
    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(CreateReferencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
        createData();
    }

    public void tearDown() throws Exception {
        LOG.info("in tear down");
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        LOG.info("created results");
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.info("deleting: " +o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }

    public void testGeneOrthologueCollection() throws Exception {
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences(Gene.class, Orthologue.class, "subjects", "orthologues");
        compareGeneOrthologuesToExpected();
    }

    public void testInsertGeneAnnotationReferences() throws Exception {
        CreateReferences cr = new CreateReferences(osw);
        cr.insertGeneAnnotationReferences();
        compareInsertGeneAnnotationReferencesToExpected();
    }

    public void testGeneGOTermsCollection() throws Exception {
        CreateReferences cr = new CreateReferences(osw);
        cr.insertGeneAnnotationReferences();
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
        cr.insertReferenceField(Chromosome.class, "subjects", Relation.class, "subject",
                                Exon.class, "chromosome");

        compareChromosomeExonResultsToExpected();
    }

    public void testInsertCollectionField1() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);

        cr.insertCollectionField(Gene.class, "transcripts", Transcript.class, "protein",
                                 Protein.class, "genes", false);

        compareCollectionField1ResultsToExpected();

    }

    public void testInsertReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences();
        cr.populateOrthologuesCollection();

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

    private void compareCollectionField1ResultsToExpected() throws Exception {
        osw.flushObjectById();

        Gene expectedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene1.setIdentifier("gene1");
        expectedGene1.setId(storedGene1.getId());

        Gene expectedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene2.setIdentifier("gene2");
        expectedGene2.setId(storedGene2.getId());

        Transcript expectedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript1.setIdentifier("trans1");
        expectedTranscript1.setId(storedTranscript1.getId());

        Transcript expectedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript2.setIdentifier("trans2");
        expectedTranscript2.setId(storedTranscript2.getId());

        Transcript expectedTranscript3 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript3.setIdentifier("trans3");
        expectedTranscript3.setId(storedTranscript3.getId());

        Protein expectedProtein1 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein1.setIdentifier("protein1");
        expectedProtein1.setId(storedProtein1.getId());

        Protein expectedProtein2 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein2.setIdentifier("protein2");
        expectedProtein2.setId(storedProtein2.getId());

        Protein expectedProtein3 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein3.setIdentifier("protein3");
        expectedProtein3.setId(storedProtein3.getId());

        expectedGene1.setTranscripts(Arrays.asList(new Object[] {
                                                       expectedTranscript2, expectedTranscript1
                                                   }));

        expectedGene2.setTranscripts(Arrays.asList(new Object[] {
                                                       expectedTranscript3
                                                   }));

        expectedGene1.setProteins(Arrays.asList(new Object[] {expectedProtein2, expectedProtein1}));
        expectedGene2.setProteins(Arrays.asList(new Object[] {expectedProtein3}));

        Item expGene1Item = toItem(expectedGene1);
        Item expGene2Item = toItem(expectedGene2);

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;


        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf1 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("gene1"));
        q.setConstraint(sc1);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene1 = (Gene) row.get(0);
        Item resGene1Item = toItem(resGene1);

        assertEquals(2, resGene1.getProteins().size());

        assertEquals(expGene1Item, resGene1Item);


        q = new Query();
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc2 =
            new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("gene2"));
        q.setConstraint(sc2);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene2 = (Gene) row.get(0);
        Item resGene2Item = toItem(resGene2);

        assertEquals(1, resGene2.getProteins().size());

        assertEquals(expGene2Item, resGene2Item);
    }

    private void compareGeneTranscriptResultsToExpected() throws Exception {
        osw.flushObjectById();

        Orthologue expectedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Annotation expectedPhenotypeAnnotation = (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedPhenotypeAnnotation.setId(storedPhenotypeAnnotation.getId());

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedPhenotypeAnnotation}));
        expectedGene.setProteins(Arrays.asList(new Object[] {expectedProtein}));

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setIdentifier("trans0");
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

        QueryField qf1 = new QueryField(qcTranscript, "identifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("trans0"));
        q.setConstraint(sc1);

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

        // in gene0 objects collection
        Orthologue expectedOrthologue2 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());
        expectedOrthologue2.setSubject(storedGene);

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setId(storedTranscript.getId());

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());

        Annotation expectedPhenotypeAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedPhenotypeAnnotation.setId(storedPhenotypeAnnotation.getId());

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedPhenotypeAnnotation}));
        expectedGene.setOrthologues(Arrays.asList(new Object[] {expectedOrthologue1}));
        expectedGene.setSubjects(Arrays.asList(new Object[] {expectedOrthologue1,
                                                       expectedTranscriptRelation}));
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setTranscripts(Arrays.asList(new Object[] {expectedTranscript}));
        expectedGene.setProteins(Arrays.asList(new Object[] {expectedProtein}));

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

        QueryField qf1 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("gene0"));
        q.setConstraint(sc1);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);
    }

    private void compareGeneGOTermsToExpected() throws Exception {
        osw.flushObjectById();
        ObjectStore os = osw.getObjectStore();

        Query q;
        Results res;
        ResultsRow row;

        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf1 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("gene0"));
        q.setConstraint(sc1);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);

        List resGOTerms = resGene.getGOTerms();

        assertEquals(1, resGOTerms.size());
        assertEquals(storedGOTerm, resGOTerms.get(0));
    }

    private void compareResultsToExpected() throws Exception {
        osw.flushObjectById();

        Orthologue expectedOrthologue1 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Annotation expectedGOTermAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedGOTermAnnotation.setId(storedGOTermAnnotation.getId());

        Annotation expectedPhenotypeAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedPhenotypeAnnotation.setId(storedPhenotypeAnnotation.getId());

        Phenotype expectedPhenotype =
            (Phenotype) DynamicUtil.createObject(Collections.singleton(Phenotype.class));
        expectedPhenotype.setId(storedPhenotype.getId());

        GOTerm expectedGOTerm =
            (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        expectedGOTerm.setId(storedGOTerm.getId());

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setOrthologues(Arrays.asList(new Object[] {expectedOrthologue1}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedPhenotypeAnnotation,
                                                                expectedGOTermAnnotation}));
        expectedGene.setPhenotypes(Arrays.asList(new Object[] {expectedPhenotype}));
        expectedGene.setGOTerms(Arrays.asList(new Object[] {expectedGOTerm}));
        expectedGene.setProteins(Arrays.asList(new Object[] {expectedProtein}));

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setIdentifier("trans0");
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

        ObjectStore os = osw.getObjectStore();

        os.flushObjectById();

        Query q;
        Results res;
        ResultsRow row;


        q = new Query();
        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);

        QueryField qf1 = new QueryField(qcTranscript, "identifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("trans0"));
        q.setConstraint(sc1);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Item expTranscriptItem = toItem(expectedTranscript);

        Transcript resTranscript = (Transcript) row.get(0);
        Item resTranscriptItem = toItem(resTranscript);
        assertEquals(expTranscriptItem, resTranscriptItem);



        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS,
                                                    new QueryValue("gene0"));
        q.setConstraint(sc2);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);

        // fix the ID of the GOTerm - insertGeneAnnotationReferences() will have created a new
        // object
        List resGeneAnnotations = resGene.getAnnotations();

        Annotation newGOTermAnnotation;
        if (resGeneAnnotations.get(0).equals(expectedPhenotypeAnnotation)) {
            newGOTermAnnotation = (Annotation) resGeneAnnotations.get(1);
        } else {
            newGOTermAnnotation = (Annotation) resGeneAnnotations.get(0);
        }
        expectedGOTermAnnotation.setId(newGOTermAnnotation.getId());


        Item expGeneItem = toItem(expectedGene);
        Item resGeneItem = toItem(resGene);
        assertEquals(expGeneItem, resGeneItem);

        q = new Query();
        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        q.addFrom(qcChromosome);
        q.addToSelect(qcChromosome);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Item expChromosomeItem = toItem(expectedChromosome);

        Chromosome resChromosome = (Chromosome) row.get(0);
        Item resChromosomeItem = toItem(resChromosome);
        assertEquals(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Item expExonItem = toItem(expectedExon);

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        assertEquals(expExonItem, resExonItem);

    }

    private void compareInsertGeneAnnotationReferencesToExpected() throws Exception {
        osw.flushObjectById();

        Orthologue expectedOrthologue1 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Annotation expectedGOTermAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedGOTermAnnotation.setId(storedGOTermAnnotation.getId());

        Annotation expectedPhenotypeAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        expectedPhenotypeAnnotation.setId(storedPhenotypeAnnotation.getId());

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setId(storedTranscript.getId());

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(Arrays.asList(new Object[] {expectedOrthologue2}));
        expectedGene.setSubjects(Arrays.asList(new Object[] {expectedOrthologue1,
                                                             expectedTranscriptRelation}));
        expectedGene.setTranscripts(Arrays.asList(new Object[] {expectedTranscript}));
        expectedGene.setAnnotations(Arrays.asList(new Object[] {expectedPhenotypeAnnotation,
                                                                expectedGOTermAnnotation}));
        expectedGene.setProteins(Arrays.asList(new Object[] {expectedProtein}));

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

        QueryField qf1 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS,
                                                    new QueryValue("gene0"));
        q.setConstraint(sc1);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);

        // the difference should be that expectedGOTermAnnotation has a different ID
        assertFalse(expGeneItem.equals(resGeneItem));
        List resGeneAnnotations = resGene.getAnnotations();
        assertEquals(2, resGeneAnnotations.size());
        assertTrue(resGeneAnnotations.contains(expectedPhenotypeAnnotation));
        assertFalse(resGeneAnnotations.contains(expectedGOTermAnnotation));

        Annotation newGOTermAnnotation;

        if (resGeneAnnotations.get(0).equals(expectedPhenotypeAnnotation)) {
            newGOTermAnnotation = (Annotation) resGeneAnnotations.get(1);
        } else {
            newGOTermAnnotation = (Annotation) resGeneAnnotations.get(0);
        }

        assertEquals(storedGOTermAnnotation.getProperty(), newGOTermAnnotation.getProperty());
        assertEquals(storedGOTermAnnotation.getEvidence(), newGOTermAnnotation.getEvidence());
        assertEquals(storedGOTermAnnotation.getQualifier(), newGOTermAnnotation.getQualifier());
        assertEquals(expectedGene, newGOTermAnnotation.getSubject());
    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedChromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setIdentifier("chr1");

        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setIdentifier("gene0");

        // used by testInsertCollectionField1
        storedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene1.setIdentifier("gene1");

        storedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene2.setIdentifier("gene2");

        storedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript.setIdentifier("trans0");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set
        storedTranscript.setGene(storedGene);

        storedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript1.setIdentifier("trans1");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set

        storedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript2.setIdentifier("trans2");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set

        storedTranscript3 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript3.setIdentifier("trans3");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set

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

        storedProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein.setIdentifier("Protein0");
        storedProtein.setGenes(Arrays.asList(new Object[] {storedGene}));

        storedProtein1 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein1.setIdentifier("Protein1");

        storedProtein2 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein2.setIdentifier("Protein1");

        storedProtein3 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein3.setIdentifier("Protein3");

        storedPhenotypeEvidence =
            (Evidence) DynamicUtil.createObject(Collections.singleton(Evidence.class));

        storedPhenotypeAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        storedPhenotypeAnnotation.setEvidence(Arrays.asList(new Object[]{storedPhenotypeEvidence}));

        storedPhenotype =
            (Phenotype) DynamicUtil.createObject(Collections.singleton(Phenotype.class));
        storedPhenotype.setIdentifier("Phenotype1");
        storedPhenotypeAnnotation.setProperty(storedPhenotype);
        storedPhenotypeAnnotation.setSubject(storedGene);

        storedGOTermAnnotation =
            (Annotation) DynamicUtil.createObject(Collections.singleton(Annotation.class));
        storedGOTermAnnotation.setSubject(storedProtein);

        storedGOTerm = (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        storedGOTerm.setIdentifier("GOTerm1");
        storedGOTermAnnotation.setProperty(storedGOTerm);
        storedGOTermAnnotation.setSubject(storedProtein);

        // in gene1 subject collection
        storedOrthologue1 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        storedOrthologue1.setObject(storedGene);

        // in gene1 objects collection
        storedOrthologue2 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        storedOrthologue2.setSubject(storedGene);


        // used by testInsertCollectionField1()
        storedTranscript1.setGene(storedGene1);
        storedTranscript2.setGene(storedGene1);
        storedTranscript3.setGene(storedGene2);

        storedTranscript1.setProtein(storedProtein1);
        storedTranscript2.setProtein(storedProtein2);
        storedTranscript3.setProtein(storedProtein3);

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedGene, storedGene1, storedGene2,
                                                    storedTranscriptRelation,
                                                    storedExonRelation, storedTranscript,
                                                    storedTranscript1,
                                                    storedTranscript2, storedTranscript3,
                                                    storedExon, storedChromosomeRelation,
                                                    storedChromosome, storedOrthologue1,
                                                    storedOrthologue2, storedGOTermAnnotation,
                                                    storedGOTerm, storedPhenotype,
                                                    storedPhenotypeAnnotation, storedProtein,
                                                    storedProtein1, storedProtein2, storedProtein3,
                                                    storedPhenotypeEvidence
                                                }));
        Iterator i = toStore.iterator();
        osw.beginTransaction();
        LOG.info("begun transaction in createData()");
        while (i.hasNext()) {
            InterMineObject object = (InterMineObject) i.next();
            osw.store(object);
        }

        osw.commitTransaction();
        LOG.info("committed transaction in createData()");
    }

    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = itemFactory.makeItem(o);
        return item;
    }

}
