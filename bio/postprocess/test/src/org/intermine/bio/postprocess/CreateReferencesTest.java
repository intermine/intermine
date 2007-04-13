package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.FivePrimeUTR;
import org.flymine.model.genomic.GOTerm;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.MRNA;
import org.flymine.model.genomic.Orthologue;
import org.flymine.model.genomic.OverlapRelation;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.RankedRelation;
import org.flymine.model.genomic.Relation;
import org.flymine.model.genomic.SimpleRelation;
import org.flymine.model.genomic.ThreePrimeUTR;
import org.flymine.model.genomic.Transcript;
import org.flymine.model.genomic.UTR;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

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
    private Location storedExonLocation = null;
    private Location storedGeneLocation = null;
    private Location storedGeneLocation1 = null;
    private Location storedGeneLocation2 = null;
    private Location storedTranscriptLocation = null;
    private Location storedTranscriptLocation1 = null;
    private Location storedTranscriptLocation2 = null;
    private Location storedTranscriptLocation3 = null;
    private SimpleRelation storedTranscriptRelation = null;
    private RankedRelation storedExonRankedRelation = null;
    private Orthologue storedOrthologue1 = null;
    private Orthologue storedOrthologue2 = null;
    private GOTerm storedGOTerm = null;
    private OverlapRelation storedOverlapRelation = null;
    private MRNA storedMRNA1 = null;
    private MRNA storedMRNA2 = null;
    private UTR storedUTR1 = null;
    private UTR storedUTR2 = null;
    private UTR storedUTR3 = null;
    private UTR storedUTR4 = null;

    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(CreateReferencesTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
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
        //ObjectStore os = osw.getObjectStore();
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

    public void testInsertGeneTranscriptReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertCollectionField(Transcript.class, "objects", SimpleRelation.class, "object",
                                 Gene.class, "transcripts", false);

        compareGeneTranscriptResultsToExpected();
    }

    public void testInsertChromosomeLSFReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferenceField(Chromosome.class, "subjects", Relation.class, "subject",
                                LocatedSequenceFeature.class, "chromosome");

        compareChromosomeLSFResultsToExpected();
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

    /*public void testInsertSymmetricalRelationReferences() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();
        cl.createLocations();
        CreateReferences cr = new CreateReferences(osw);
        cr.insertReferences();
        cr.insertSymmetricalRelationReferences();

        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc2 =
            new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("gene1"));
        q.setConstraint(sc2);

        Results res = new Results(q, os, os.getSequence());
        ResultsRow row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);

        Set overlappingFeatures = resGene.getOverlappingFeatures();

        Assert.assertEquals(1, overlappingFeatures.size());

        Set expectedIDs = new HashSet();
        expectedIDs.add(storedGene2.getId());

        Set actualIDs = new HashSet();
        Iterator ofIter = overlappingFeatures.iterator();
        actualIDs.add(((Gene) ofIter.next()).getId());

        Assert.assertEquals(expectedIDs, actualIDs);
    }*/

    public void testCreateUtrRefs() throws Exception {
        storedMRNA1 = (MRNA) DynamicUtil.createObject(Collections.singleton(MRNA.class));
        storedMRNA1.setIdentifier("mrna1");
        storedMRNA1.setId(new Integer(1000));
        storedMRNA2 = (MRNA) DynamicUtil.createObject(Collections.singleton(MRNA.class));
        storedMRNA2.setIdentifier("mrna2");
        storedMRNA2.setId(new Integer(1001));

        storedUTR1 = (UTR) DynamicUtil.createObject(Collections.singleton(ThreePrimeUTR.class));
        storedUTR1.setIdentifier("utr1-threePrimeUTR");
        storedUTR2 = (UTR) DynamicUtil.createObject(Collections.singleton(FivePrimeUTR.class));
        storedUTR2.setIdentifier("utr2-fivePrimeUTR");
        storedUTR3 = (UTR) DynamicUtil.createObject(Collections.singleton(ThreePrimeUTR.class));
        storedUTR3.setIdentifier("utr3-threePrimeUTR");
        storedUTR4 = (UTR) DynamicUtil.createObject(Collections.singleton(FivePrimeUTR.class));
        storedUTR4.setIdentifier("utr4-fivePrimeUTR");

        storedMRNA1.setuTRs(new HashSet(Arrays.asList(new Object[] {
                                                          storedUTR1, storedUTR2
                                                      })));
        storedMRNA2.setuTRs(new HashSet(Arrays.asList(new Object[] {
                                                          storedUTR3, storedUTR4
                                                      })));

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedMRNA1,
                                                    storedMRNA2,
                                                    storedUTR1,
                                                    storedUTR2,
                                                    storedUTR3,
                                                    storedUTR4,
                                                }));

        Iterator i = toStore.iterator();
        osw.beginTransaction();
        while (i.hasNext()) {
            InterMineObject object = (InterMineObject) i.next();
            osw.store(object);
        }
        osw.commitTransaction();

        CreateReferences cr = new CreateReferences(osw);
        cr.createUtrRefs();

        MRNA dbMRNA1 = (MRNA) osw.getObjectStore().getObjectById(new Integer(1000));
        MRNA dbMRNA2 = (MRNA) osw.getObjectStore().getObjectById(new Integer(1001));

        Assert.assertEquals(storedUTR1.getIdentifier(), dbMRNA1.getThreePrimeUTR().getIdentifier());
        Assert.assertEquals(storedUTR2.getIdentifier(), dbMRNA1.getFivePrimeUTR().getIdentifier());
        Assert.assertEquals(storedUTR3.getIdentifier(), dbMRNA2.getThreePrimeUTR().getIdentifier());
        Assert.assertEquals(storedUTR4.getIdentifier(), dbMRNA2.getFivePrimeUTR().getIdentifier());
    }

    private void compareChromosomeLSFResultsToExpected() throws Exception {
        osw.flushObjectById();
        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        expectedExon.setIdentifier("exon1");
        expectedExon.setId(storedExon.getId());

        Chromosome expectedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        expectedChromosome.setIdentifier("chr1");
        expectedChromosome.setId(storedChromosome.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setId(storedGene.getId());
        Gene expectedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene1.setId(storedGene1.getId());
        Gene expectedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene2.setId(storedGene2.getId());
        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setId(storedTranscript.getId());
        Transcript expectedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript1.setId(storedTranscript1.getId());
        Transcript expectedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript2.setId(storedTranscript2.getId());
        Transcript expectedTranscript3 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript3.setId(storedTranscript3.getId());

        Relation expectedChromosomeExonLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeExonLocation.setId(storedExonLocation.getId());
        expectedChromosomeExonLocation.setObject(expectedChromosome);
        expectedChromosomeExonLocation.setSubject(expectedExon);

        Location expectedChromosomeGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeGeneLocation.setId(storedGeneLocation.getId());
        expectedChromosomeGeneLocation.setObject(expectedChromosome);
        expectedChromosomeGeneLocation.setSubject(expectedGene);

        Location expectedChromosomeGeneLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeGeneLocation1.setId(storedGeneLocation1.getId());
        expectedChromosomeGeneLocation1.setObject(expectedChromosome);

        Location expectedChromosomeGeneLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeGeneLocation2.setId(storedGeneLocation2.getId());
        expectedChromosomeGeneLocation2.setObject(expectedChromosome);

        Location expectedChromosomeTranscriptLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation.setId(storedTranscriptLocation.getId());
        expectedChromosomeTranscriptLocation.setObject(expectedChromosome);
        expectedChromosomeTranscriptLocation.setSubject(expectedTranscript);

        Location expectedChromosomeTranscriptLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation1.setId(storedTranscriptLocation1.getId());
        expectedChromosomeTranscriptLocation1.setObject(expectedChromosome);

        Location expectedChromosomeTranscriptLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation2.setId(storedTranscriptLocation2.getId());
        expectedChromosomeTranscriptLocation2.setObject(expectedChromosome);

        Location expectedChromosomeTranscriptLocation3 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation3.setId(storedTranscriptLocation3.getId());
        expectedChromosomeTranscriptLocation3.setObject(expectedChromosome);

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRankedRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setSubject(expectedExon);
        expectedExon.setObjects(new HashSet(Arrays.asList(new Object[] {
            expectedChromosomeExonLocation, expectedExonRelation})));
        expectedExon.setChromosome(expectedChromosome);

        expectedChromosome.setSubjects(Collections.singleton(expectedExonRelation));

        OverlapRelation expectedOverlapRelation =
            (OverlapRelation) DynamicUtil.createObject(Collections.singleton(OverlapRelation.class));
        expectedOverlapRelation.addBioEntities(storedGene1);
        expectedOverlapRelation.addBioEntities(storedGene2);


        expectedChromosome.setSubjects(new HashSet(Arrays.asList(new Object[] {
                expectedChromosomeTranscriptLocation3,
                expectedChromosomeTranscriptLocation2,
                expectedChromosomeTranscriptLocation1,
                expectedChromosomeTranscriptLocation,
                expectedChromosomeGeneLocation2,
                expectedChromosomeGeneLocation1,
                expectedChromosomeGeneLocation,
                expectedChromosomeExonLocation,
            })));

        expectedChromosome.addFeatures(expectedTranscript3);
        expectedChromosome.addFeatures(expectedTranscript2);
        expectedChromosome.addFeatures(expectedTranscript1);
        expectedChromosome.addFeatures(expectedTranscript);
        expectedChromosome.addFeatures(expectedGene2);
        expectedChromosome.addFeatures(expectedGene1);
        expectedChromosome.addFeatures(expectedGene);
        expectedChromosome.addFeatures(expectedExon);

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
        compareItemsCollectionOrderInsensitive(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        compareItemsCollectionOrderInsensitive(expExonItem, resExonItem);
    }

    private void compareCollectionField1ResultsToExpected() throws Exception {
        osw.flushObjectById();

        Location expectedGeneLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedGeneLocation1.setId(storedGeneLocation1.getId());

        Gene expectedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene1.setIdentifier("gene1");
        expectedGene1.setId(storedGene1.getId());
        expectedGene1.setObjects(Collections.singleton(expectedGeneLocation1));

        Location expectedGeneLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedGeneLocation2.setId(storedGeneLocation2.getId());

        Gene expectedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene2.setIdentifier("gene2");
        expectedGene2.setId(storedGene2.getId());
        expectedGene2.setObjects(Collections.singleton(expectedGeneLocation2));

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

        expectedGene1.setTranscripts(new HashSet(Arrays.asList(new Object[] {expectedTranscript2,
            expectedTranscript1})));

        expectedGene2.setTranscripts(Collections.singleton(expectedTranscript3));

        expectedGene1.setProteins(new HashSet(Arrays.asList(new Object[] {expectedProtein2, expectedProtein1})));
        expectedGene2.setProteins(Collections.singleton(expectedProtein3));

        OverlapRelation expectedOverlapRelation =
            (OverlapRelation) DynamicUtil.createObject(Collections.singleton(OverlapRelation.class));
        expectedOverlapRelation.setId(storedOverlapRelation.getId());
        expectedOverlapRelation.addBioEntities(expectedGene1);
        expectedOverlapRelation.addBioEntities(expectedGene2);

        expectedGene1.addRelations(expectedOverlapRelation);
        expectedGene2.addRelations(expectedOverlapRelation);

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

        Assert.assertEquals(2, resGene1.getProteins().size());

        compareItemsCollectionOrderInsensitive(expGene1Item, resGene1Item);


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

        Assert.assertEquals(1, resGene2.getProteins().size());

        compareItemsCollectionOrderInsensitive(expGene2Item, resGene2Item);
    }

    private void compareGeneTranscriptResultsToExpected() throws Exception {
        osw.flushObjectById();

        Orthologue expectedOrthologue1 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 = (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Location expectedGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedGeneLocation.setId(storedGeneLocation.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(new HashSet(Arrays.asList(new Object[] {expectedGeneLocation, expectedOrthologue2})));
        expectedGene.setProteins(Collections.singleton(expectedProtein));

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setIdentifier("trans0");
        expectedTranscript.setId(storedTranscript.getId());
        expectedTranscript.setGene(expectedGene);
        expectedGene.setTranscripts(Collections.singleton(expectedTranscript));

        Location expectedTranscriptLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedTranscriptLocation.setId(storedTranscriptLocation.getId());

        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());
        expectedTranscriptRelation.setObject(expectedGene);
        expectedTranscriptRelation.setSubject(expectedTranscript);
        expectedTranscript.setObjects(new HashSet(Arrays.asList(new Object[] { expectedTranscriptLocation, expectedTranscriptRelation })));
        expectedGene.setSubjects(new HashSet(Arrays.asList(new Object[] { expectedOrthologue1, expectedTranscriptRelation })));

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRankedRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setObject(expectedTranscript);
        expectedTranscript.setSubjects(Collections.singleton(expectedExonRelation));

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
        compareItemsCollectionOrderInsensitive(expTranscriptItem, resTranscriptItem);



        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "identifier");
        SimpleConstraint sc2 =
            new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("gene0"));
        q.setConstraint(sc2);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        compareItemsCollectionOrderInsensitive(expGeneItem, resGeneItem);
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

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Location expectedGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedGeneLocation.setId(storedGeneLocation.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setOrthologues(Collections.singleton(expectedOrthologue1));
        expectedGene.setSubjects(new HashSet(Arrays.asList(new Object[] {expectedOrthologue1,
            expectedTranscriptRelation})));
        expectedGene.setObjects(new HashSet(Arrays.asList(new Object[] {expectedGeneLocation,
            expectedOrthologue2})));
        expectedGene.setTranscripts(Collections.singleton(expectedTranscript));
        expectedGene.setProteins(Collections.singleton(expectedProtein));

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
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("gene0"));
        q.setConstraint(sc1);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);
        Item resGeneItem = toItem(resGene);
        compareItemsCollectionOrderInsensitive(expGeneItem, resGeneItem);
    }

    private void compareResultsToExpected() throws Exception {
        osw.flushObjectById();

        Chromosome expectedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        Gene expectedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        Gene expectedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        Transcript expectedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        Transcript expectedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        Transcript expectedTranscript3 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));

        Orthologue expectedOrthologue1 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue1.setId(storedOrthologue1.getId());

        Orthologue expectedOrthologue2 =
            (Orthologue) DynamicUtil.createObject(Collections.singleton(Orthologue.class));
        expectedOrthologue2.setId(storedOrthologue2.getId());

        GOTerm expectedGOTerm =
            (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        expectedGOTerm.setId(storedGOTerm.getId());

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Relation expectedChromosomeExonLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeExonLocation.setId(storedExonLocation.getId());
        expectedChromosomeExonLocation.setObject(expectedChromosome);
        expectedChromosomeExonLocation.setSubject(expectedExon);

        Location expectedChromosomeGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeGeneLocation.setId(storedGeneLocation.getId());
        expectedChromosomeGeneLocation.setObject(expectedChromosome);
        expectedChromosomeGeneLocation.setSubject(expectedGene);

        Location expectedChromosomeGeneLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeGeneLocation1.setId(storedGeneLocation1.getId());
        expectedChromosomeGeneLocation1.setObject(expectedChromosome);

        Location expectedChromosomeGeneLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeGeneLocation2.setId(storedGeneLocation2.getId());
        expectedChromosomeGeneLocation2.setObject(expectedChromosome);

        Location expectedChromosomeTranscriptLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation.setId(storedTranscriptLocation.getId());
        expectedChromosomeTranscriptLocation.setObject(expectedChromosome);
        expectedChromosomeTranscriptLocation.setSubject(expectedTranscript);

        Location expectedChromosomeTranscriptLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation1.setId(storedTranscriptLocation1.getId());
        expectedChromosomeTranscriptLocation1.setObject(expectedChromosome);

        Location expectedChromosomeTranscriptLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation2.setId(storedTranscriptLocation2.getId());
        expectedChromosomeTranscriptLocation2.setObject(expectedChromosome);

        Location expectedChromosomeTranscriptLocation3 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedChromosomeTranscriptLocation3.setId(storedTranscriptLocation3.getId());
        expectedChromosomeTranscriptLocation3.setObject(expectedChromosome);

        expectedGene.setIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(new HashSet(Arrays.asList(new Object[] {expectedChromosomeGeneLocation, expectedOrthologue2})));
        expectedGene.setOrthologues(Collections.singleton(expectedOrthologue1));
        expectedGene.setProteins(Collections.singleton(expectedProtein));

        expectedGene1.setId(storedGene1.getId());
        expectedGene2.setId(storedGene2.getId());

        expectedTranscript.setIdentifier("trans0");
        expectedTranscript.setId(storedTranscript.getId());
        expectedTranscript.setGene(expectedGene);
        expectedGene.setTranscripts(Collections.singleton(expectedTranscript));

        expectedTranscript1.setId(storedTranscript1.getId());
        expectedTranscript2.setId(storedTranscript2.getId());
        expectedTranscript3.setId(storedTranscript3.getId());


        SimpleRelation expectedTranscriptRelation =
            (SimpleRelation) DynamicUtil.createObject(Collections.singleton(SimpleRelation.class));
        expectedTranscriptRelation.setId(storedTranscriptRelation.getId());
        expectedTranscriptRelation.setObject(expectedGene);
        expectedTranscriptRelation.setSubject(expectedTranscript);
        expectedTranscript.setObjects(new HashSet(Arrays.asList(new Object[] {
            expectedChromosomeTranscriptLocation, expectedTranscriptRelation })));
        expectedGene.setSubjects(new HashSet(Arrays.asList(new Object[] { expectedOrthologue1,
            expectedTranscriptRelation })));

        expectedExon.setIdentifier("exon1");
        expectedExon.setId(storedExon.getId());
        expectedTranscript.setExons(Collections.singleton(expectedExon));
        expectedExon.setTranscripts(Collections.singleton(expectedTranscript));
        expectedGene.setExons(Collections.singleton(expectedExon));
        expectedExon.setGene(expectedGene);

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRankedRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setObject(expectedTranscript);
        expectedExonRelation.setSubject(expectedExon);
        expectedTranscript.setSubjects(Collections.singleton(expectedExonRelation));

        expectedChromosome.setIdentifier("chr1");
        expectedChromosome.setId(storedChromosome.getId());
        expectedChromosome.addFeatures(expectedTranscript3);
        expectedChromosome.addFeatures(expectedTranscript2);
        expectedChromosome.addFeatures(expectedTranscript1);
        expectedChromosome.addFeatures(expectedTranscript);
        expectedChromosome.addFeatures(expectedGene2);
        expectedChromosome.addFeatures(expectedGene1);
        expectedChromosome.addFeatures(expectedGene);
        expectedChromosome.addFeatures(expectedExon);

        Relation expectedChromosomeRelation =
            (Relation) DynamicUtil.createObject(Collections.singleton(Relation.class));
        expectedChromosomeRelation.setId(storedExonLocation.getId());
        expectedChromosomeRelation.setObject(expectedChromosome);
        expectedChromosomeRelation.setSubject(expectedExon);

        expectedChromosome.setSubjects(new HashSet(Arrays.asList(new Object[] {
                expectedChromosomeTranscriptLocation3,
                expectedChromosomeTranscriptLocation2,
                expectedChromosomeTranscriptLocation1,
                expectedChromosomeTranscriptLocation,
                expectedChromosomeGeneLocation2,
                expectedChromosomeGeneLocation1,
                expectedChromosomeGeneLocation,
                expectedChromosomeExonLocation,
            })));
        expectedChromosome.setExons(Collections.singleton(expectedExon));
        expectedChromosome.setGenes(new HashSet(Arrays.asList(new Object[] {
            expectedGene2, expectedGene1, expectedGene})));
        expectedChromosome.setTranscripts(new HashSet(Arrays.asList(new Object[] {
            expectedTranscript3, expectedTranscript2, expectedTranscript1, expectedTranscript})));
        expectedGene.setChromosome(expectedChromosome);
        expectedTranscript.setChromosome(expectedChromosome);
        expectedExon.setChromosome(expectedChromosome);
        expectedExon.setObjects(new HashSet(Arrays.asList(new Object[] {expectedChromosomeRelation,
            expectedExonRelation})));

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
        compareItemsCollectionOrderInsensitive(expTranscriptItem, resTranscriptItem);



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

        Item expGeneItem = toItem(expectedGene);
        Item resGeneItem = toItem(resGene);
        compareItemsCollectionOrderInsensitive(expGeneItem, resGeneItem);

        q = new Query();
        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        q.addFrom(qcChromosome);
        q.addToSelect(qcChromosome);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Item expChromosomeItem = toItem(expectedChromosome);

        Chromosome resChromosome = (Chromosome) row.get(0);
        Item resChromosomeItem = toItem(resChromosome);
        compareItemsCollectionOrderInsensitive(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = new Results(q, os, os.getSequence());
        row = (ResultsRow) res.iterator().next();

        Item expExonItem = toItem(expectedExon);

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        compareItemsCollectionOrderInsensitive(expExonItem, resExonItem);

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

        storedExonRankedRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        storedExonRankedRelation.setObject(storedTranscript);
        storedExonRankedRelation.setSubject(storedExon);
        storedExonRankedRelation.setRank(new Integer(1));

        storedExonLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedExonLocation.setObject(storedChromosome);
        storedExonLocation.setSubject(storedExon);

        storedGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation.setObject(storedChromosome);
        storedGeneLocation.setSubject(storedGene);

        storedGeneLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation1.setObject(storedChromosome);
        storedGeneLocation1.setSubject(storedGene1);

        storedGeneLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedGeneLocation2.setObject(storedChromosome);
        storedGeneLocation2.setSubject(storedGene2);


        storedTranscriptLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscriptLocation.setObject(storedChromosome);
        storedTranscriptLocation.setSubject(storedTranscript);

        storedTranscriptLocation1 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscriptLocation1.setObject(storedChromosome);
        storedTranscriptLocation1.setSubject(storedTranscript1);

        storedTranscriptLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscriptLocation2.setObject(storedChromosome);
        storedTranscriptLocation2.setSubject(storedTranscript2);

        storedTranscriptLocation3 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        storedTranscriptLocation3.setObject(storedChromosome);
        storedTranscriptLocation3.setSubject(storedTranscript3);


        storedProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein.setIdentifier("Protein0");
        storedProtein.setGenes(Collections.singleton(storedGene));

        storedProtein1 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein1.setIdentifier("Protein1");

        storedProtein2 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein2.setIdentifier("Protein1");

        storedProtein3 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein3.setIdentifier("Protein3");

        storedGOTerm = (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        storedGOTerm.setIdentifier("GOTerm1");

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

        storedOverlapRelation =
            (OverlapRelation) DynamicUtil.createObject(Collections.singleton(OverlapRelation.class));

        // note: this isn't very consistent because there are no locations for these genes
        // use for testing insertSymmetricalRelationReferences()
        storedOverlapRelation.addBioEntities(storedGene1);
        storedOverlapRelation.addBioEntities(storedGene2);


        Set toStore = new HashSet(Arrays.asList(new Object[] {
                storedGene, storedGene1, storedGene2,
                storedTranscriptRelation,
                storedExonRankedRelation, storedTranscript,
                storedTranscript1,
                storedTranscript2, storedTranscript3,
                storedExon, storedExonRankedRelation,
                storedChromosome, storedOrthologue1,
                storedOrthologue2,
                storedGOTerm, storedProtein,
                storedProtein1, storedProtein2, storedProtein3,
                storedOverlapRelation,
                storedExonRankedRelation, storedExonLocation,
                storedGeneLocation, storedGeneLocation1, storedGeneLocation2,
                storedTranscriptLocation, storedTranscriptLocation1, storedTranscriptLocation2,
                storedTranscriptLocation3,
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

    private void compareItemsCollectionOrderInsensitive(Item a, Item b) throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(a.toString(), b.toString());
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        Assert.assertTrue("Item \"" + a.toString() + "\" not equal to item \"" + b.toString() + "\"",
                diff.similar());
    }
}
