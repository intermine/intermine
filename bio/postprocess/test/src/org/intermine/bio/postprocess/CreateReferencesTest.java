package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import org.flymine.model.genomic.OverlapRelation;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.RankedRelation;
import org.flymine.model.genomic.Relation;
import org.flymine.model.genomic.SimpleRelation;
import org.flymine.model.genomic.ThreePrimeUTR;
import org.flymine.model.genomic.Transcript;
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
    private GOTerm storedGOTerm = null;
    private OverlapRelation storedOverlapRelation = null;

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
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
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

        QueryField qf2 = new QueryField(qcGene, "primaryIdentifier");
        SimpleConstraint sc2 =
            new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("gene1"));
        q.setConstraint(sc2);

        Results res = os.execute(q);
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
        MRNA storedMRNA1 = (MRNA) DynamicUtil.createObject(Collections.singleton(MRNA.class));
        storedMRNA1.setPrimaryIdentifier("mrna1");
        storedMRNA1.setId(new Integer(1000));
        MRNA storedMRNA2 = (MRNA) DynamicUtil.createObject(Collections.singleton(MRNA.class));
        storedMRNA2.setPrimaryIdentifier("mrna2");
        storedMRNA2.setId(new Integer(1001));

        ThreePrimeUTR storedUTR1 =
            (ThreePrimeUTR) DynamicUtil.createObject(Collections.singleton(ThreePrimeUTR.class));
        storedUTR1.setPrimaryIdentifier("utr1-threePrimeUTR");
        storedUTR1.setmRNA(storedMRNA1);
        FivePrimeUTR storedUTR2 =
            (FivePrimeUTR) DynamicUtil.createObject(Collections.singleton(FivePrimeUTR.class));
        storedUTR2.setPrimaryIdentifier("utr2-fivePrimeUTR");
        storedUTR2.setmRNA(storedMRNA1);

        ThreePrimeUTR storedUTR3  =
            (ThreePrimeUTR) DynamicUtil.createObject(Collections.singleton(ThreePrimeUTR.class));
        storedUTR3.setPrimaryIdentifier("utr3-threePrimeUTR");
        storedUTR3.setmRNA(storedMRNA2);
        FivePrimeUTR storedUTR4 =
            (FivePrimeUTR) DynamicUtil.createObject(Collections.singleton(FivePrimeUTR.class));
        storedUTR4.setPrimaryIdentifier("utr4-fivePrimeUTR");
        storedUTR4.setmRNA(storedMRNA2);

        ThreePrimeUTR stored3UTR =
            (ThreePrimeUTR) DynamicUtil.createObject(Collections.singleton(ThreePrimeUTR.class));
        stored3UTR.setPrimaryIdentifier("utr1-threePrimeUTR-orig");
        storedMRNA1.setThreePrimeUTR(stored3UTR);
        FivePrimeUTR stored5UTR =
            (FivePrimeUTR) DynamicUtil.createObject(Collections.singleton(FivePrimeUTR.class));
        stored5UTR.setPrimaryIdentifier("utr2-fivePrimeUTR-orig");
        storedMRNA1.setFivePrimeUTR(stored5UTR);

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    storedMRNA1,
                                                    storedMRNA2,
                                                    storedUTR1,
                                                    storedUTR2,
                                                    storedUTR3,
                                                    storedUTR4,
                                                    stored3UTR,
                                                    stored5UTR
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

        Assert.assertEquals(storedUTR1.getSecondaryIdentifier(), dbMRNA1.getThreePrimeUTR().getSecondaryIdentifier());
        Assert.assertEquals(storedUTR2.getSecondaryIdentifier(), dbMRNA1.getFivePrimeUTR().getSecondaryIdentifier());
        Assert.assertEquals(storedUTR3.getSecondaryIdentifier(), dbMRNA2.getThreePrimeUTR().getSecondaryIdentifier());
        Assert.assertEquals(storedUTR4.getSecondaryIdentifier(), dbMRNA2.getFivePrimeUTR().getSecondaryIdentifier());
    }

    private void compareChromosomeLSFResultsToExpected() throws Exception {
        osw.flushObjectById();
        Exon expectedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        expectedExon.setPrimaryIdentifier("exon1");
        expectedExon.setId(storedExon.getId());

        Chromosome expectedChromosome =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        expectedChromosome.setPrimaryIdentifier("chr1");
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

        expectedChromosome.setSubjects(Collections.singleton((Relation) expectedExonRelation));

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

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Chromosome resChromosome = (Chromosome) row.get(0);
        Item resChromosomeItem = toItem(resChromosome);
        compareItemsCollectionOrderInsensitive(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = os.execute(q);
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
        expectedGene1.setPrimaryIdentifier("gene1");
        expectedGene1.setId(storedGene1.getId());
        expectedGene1.setObjects(Collections.singleton((Relation) expectedGeneLocation1));

        Location expectedGeneLocation2 =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedGeneLocation2.setId(storedGeneLocation2.getId());

        Gene expectedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene2.setPrimaryIdentifier("gene2");
        expectedGene2.setId(storedGene2.getId());
        expectedGene2.setObjects(Collections.singleton((Relation) expectedGeneLocation2));

        Transcript expectedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript1.setPrimaryIdentifier("trans1");
        expectedTranscript1.setId(storedTranscript1.getId());

        Transcript expectedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript2.setPrimaryIdentifier("trans2");
        expectedTranscript2.setId(storedTranscript2.getId());

        Transcript expectedTranscript3 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript3.setPrimaryIdentifier("trans3");
        expectedTranscript3.setId(storedTranscript3.getId());

        Protein expectedProtein1 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein1.setPrimaryIdentifier("protein1");
        expectedProtein1.setId(storedProtein1.getId());

        Protein expectedProtein2 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein2.setPrimaryIdentifier("protein2");
        expectedProtein2.setId(storedProtein2.getId());

        Protein expectedProtein3 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein3.setPrimaryIdentifier("protein3");
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

        QueryField qf1 = new QueryField(qcGene, "primaryIdentifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("gene1"));
        q.setConstraint(sc1);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Gene resGene1 = (Gene) row.get(0);
        Item resGene1Item = toItem(resGene1);

        Assert.assertEquals(2, resGene1.getProteins().size());

        compareItemsCollectionOrderInsensitive(expGene1Item, resGene1Item);


        q = new Query();
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "primaryIdentifier");
        SimpleConstraint sc2 =
            new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("gene2"));
        q.setConstraint(sc2);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Gene resGene2 = (Gene) row.get(0);
        Item resGene2Item = toItem(resGene2);

        Assert.assertEquals(1, resGene2.getProteins().size());

        compareItemsCollectionOrderInsensitive(expGene2Item, resGene2Item);
    }

    private void compareGeneTranscriptResultsToExpected() throws Exception {
        osw.flushObjectById();

        Protein expectedProtein =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        expectedProtein.setId(storedProtein.getId());

        Location expectedGeneLocation =
            (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        expectedGeneLocation.setId(storedGeneLocation.getId());

        Gene expectedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        expectedGene.setPrimaryIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(new HashSet(Arrays.asList(new Object[] {expectedGeneLocation})));
        expectedGene.setProteins(Collections.singleton(expectedProtein));

        Transcript expectedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        expectedTranscript.setPrimaryIdentifier("trans0");
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
        expectedGene.setSubjects(new HashSet(Arrays.asList(new Object[] { expectedTranscriptRelation })));

        RankedRelation expectedExonRelation =
            (RankedRelation) DynamicUtil.createObject(Collections.singleton(RankedRelation.class));
        expectedExonRelation.setId(storedExonRankedRelation.getId());
        expectedExonRelation.setRank(new Integer(1));
        expectedExonRelation.setObject(expectedTranscript);
        expectedTranscript.setSubjects(Collections.singleton((Relation) expectedExonRelation));

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

        QueryField qf1 = new QueryField(qcTranscript, "primaryIdentifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("trans0"));
        q.setConstraint(sc1);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Transcript resTranscript = (Transcript) row.get(0);
        Item resTranscriptItem = toItem(resTranscript);
        compareItemsCollectionOrderInsensitive(expTranscriptItem, resTranscriptItem);



        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "primaryIdentifier");
        SimpleConstraint sc2 =
            new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("gene0"));
        q.setConstraint(sc2);

        res = os.execute(q);
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

        expectedGene.setPrimaryIdentifier("gene0");
        expectedGene.setId(storedGene.getId());
        expectedGene.setObjects(new HashSet(Arrays.asList(new Object[] {expectedChromosomeGeneLocation })));
        expectedGene.setProteins(Collections.singleton(expectedProtein));

        expectedGene1.setId(storedGene1.getId());
        expectedGene2.setId(storedGene2.getId());

        expectedTranscript.setPrimaryIdentifier("trans0");
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
        expectedGene.setSubjects(new HashSet(Arrays.asList(new Object[] { expectedTranscriptRelation })));

        expectedExon.setPrimaryIdentifier("exon1");
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
        expectedTranscript.setSubjects(Collections.singleton((Relation) expectedExonRelation));

        expectedChromosome.setPrimaryIdentifier("chr1");
        expectedChromosome.setId(storedChromosome.getId());
        expectedChromosome.addFeatures(expectedTranscript);
        expectedChromosome.addFeatures(expectedGene);

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

        QueryField qf1 = new QueryField(qcTranscript, "primaryIdentifier");
        SimpleConstraint sc1 =
            new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("trans0"));
        q.setConstraint(sc1);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Item expTranscriptItem = toItem(expectedTranscript);

        Transcript resTranscript = (Transcript) row.get(0);
        Item resTranscriptItem = toItem(resTranscript);
        compareItemsCollectionOrderInsensitive(expTranscriptItem, resTranscriptItem);



        q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);

        QueryField qf2 = new QueryField(qcGene, "primaryIdentifier");
        SimpleConstraint sc2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS,
                                                    new QueryValue("gene0"));
        q.setConstraint(sc2);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Gene resGene = (Gene) row.get(0);

        Item expGeneItem = toItem(expectedGene);
        Item resGeneItem = toItem(resGene);
        compareItemsCollectionOrderInsensitive(expGeneItem, resGeneItem);

        q = new Query();
        QueryClass qcChromosome = new QueryClass(Chromosome.class);
        q.addFrom(qcChromosome);
        q.addToSelect(qcChromosome);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Item expChromosomeItem = toItem(expectedChromosome);

        Chromosome resChromosome = (Chromosome) row.get(0);
        Item resChromosomeItem = toItem(resChromosome);
        compareItemsCollectionOrderInsensitive(expChromosomeItem, resChromosomeItem);


        q = new Query();
        QueryClass qcExon = new QueryClass(Exon.class);
        q.addFrom(qcExon);
        q.addToSelect(qcExon);

        res = os.execute(q);
        row = (ResultsRow) res.iterator().next();

        Item expExonItem = toItem(expectedExon);

        Exon resExon = (Exon) row.get(0);
        Item resExonItem = toItem(resExon);
        compareItemsCollectionOrderInsensitive(expExonItem, resExonItem);

    }

    private void createData() throws Exception {
        osw.flushObjectById();

        storedChromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        storedChromosome.setPrimaryIdentifier("chr1");

        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setPrimaryIdentifier("gene0");

        // used by testInsertCollectionField1
        storedGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene1.setPrimaryIdentifier("gene1");

        storedGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene2.setPrimaryIdentifier("gene2");

        storedTranscript =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript.setPrimaryIdentifier("trans0");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set
        storedTranscript.setGene(storedGene);

        storedTranscript1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript1.setPrimaryIdentifier("trans1");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set

        storedTranscript2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript2.setPrimaryIdentifier("trans2");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set

        storedTranscript3 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript3.setPrimaryIdentifier("trans3");
        // currently the gene reference in Transcript is set before post-processing but the
        // transcripts reference in Gene isn't set

        storedExon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        storedExon.setPrimaryIdentifier("exon1");

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
        storedProtein.setPrimaryIdentifier("Protein0");
        storedProtein.setGenes(Collections.singleton(storedGene));

        storedProtein1 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein1.setPrimaryIdentifier("Protein1");

        storedProtein2 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein2.setPrimaryIdentifier("Protein1");

        storedProtein3 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein3.setPrimaryIdentifier("Protein3");

        storedGOTerm = (GOTerm) DynamicUtil.createObject(Collections.singleton(GOTerm.class));
        storedGOTerm.setIdentifier("GOTerm1");

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
                storedChromosome,
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

    private void compareItemsCollectionOrderInsensitive(Item exp, Item act) throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = new Diff(exp.toString(), act.toString());
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
        Assert.assertTrue("Expected Item \"" + exp.toString() + "\" not equal to actual item \""
                          + act.toString() + "\"", diff.similar());
    }
}
