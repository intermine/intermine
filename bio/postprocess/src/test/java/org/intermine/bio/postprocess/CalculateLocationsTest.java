package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.ReversePrimer;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.ItemFactory;

public class CalculateLocationsTest extends TestCase
{

    private ObjectStoreWriter osw;
    private Chromosome chromosome = null;
    private Model model;
    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(CalculateLocationsTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
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

    private void createOverlapTestData() throws Exception {
        Chromosome chr =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(1000));
        chr.setId(new Integer(101));

        Set<InterMineObject> toStore = new HashSet<InterMineObject>();

        toStore.add(chr);

        int [][] exonInfo = {
            { 1000, 1, 1 },
            { 1001, 2, 10 },
            { 1002, 10, 15 },
            { 1003, 16, 19 },
            { 1004, 16, 19 },
            { 1005, 20, 29 },
            { 1006, 30, 100 },
            { 1007, 30, 34 },
            { 1008, 32, 95 },
            { 1009, 38, 53 },
            { 1010, 40, 50 },
            { 1011, 44, 44 },
            { 1012, 54, 54 },
            { 1013, 54, 54 },
            { 1014, 60, 70 },
            { 1015, 120, 140 },
            { 1016, 141, 145 },
            { 1017, 146, 180 },
            { 1018, 220, 240 },
            { 1019, 240, 245 },
            { 1020, 245, 280 },
        };

        Exon[] exons = new Exon[exonInfo.length];
        Location[] exonLocs = new Location[exonInfo.length];

        for (int i = 0; i < exons.length; i++) {
            exons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            int exonId = exonInfo[i][0];
            int start = exonInfo[i][1];
            int end = exonInfo[i][2];
            exons[i].setId(new Integer(exonId));
            exons[i].setLength(new Integer(end - start + 1));
            exons[i].setChromosome(chr);
            exonLocs[i] = createLocation(chr, exons[i], "1", start, end, Location.class);
            exonLocs[i].setId(new Integer(1000 + exonId));
        }

        ReversePrimer rp =
            (ReversePrimer) DynamicUtil.createObject(Collections.singleton(ReversePrimer.class));
        rp.setId(new Integer(3000));
        rp.setLength(new Integer(100));
        rp.setChromosome(chr);

        Location rpLoc = createLocation(chr, rp, "1", 1, 100, Location.class);
        rpLoc.setId(new Integer(3001));

        toStore.add(rp);
        toStore.add(rpLoc);
        toStore.addAll(Arrays.asList(exons));
        toStore.addAll(Arrays.asList(exonLocs));

        for (InterMineObject imo : toStore) {
            osw.store(imo);
        }
    }

//    public void testCreateOverlapFull () throws Exception {
//        createOverlapTestData();
//
//        CalculateLocations cl = new CalculateLocations(osw);
//        List classesToIgnore = new ArrayList();
//        classesToIgnore.add("Primer=Exon");
//        cl.createOverlapRelations(classesToIgnore, false);
//
//        ///////////////////////////////
//
//        int [][] expectedOverlaps = {
//            { 1001, 1002 },
//            { 1003, 1004 },
//            { 1006, 1007 },
//            { 1006, 1008 },
//            { 1006, 1009 },
//            { 1006, 1010 },
//            { 1006, 1011 },
//            { 1006, 1012 },
//            { 1006, 1013 },
//            { 1006, 1014 },
//            { 1007, 1008 },
//            { 1008, 1009 },
//            { 1008, 1010 },
//            { 1008, 1011 },
//            { 1008, 1012 },
//            { 1008, 1013 },
//            { 1008, 1014 },
//            { 1009, 1010 },
//            { 1009, 1011 },
//            { 1010, 1011 },
//            { 1012, 1013 },
//            { 1018, 1019 },
//            { 1019, 1020 },
//            { 4000, 1000 },
//            { 4000, 1001 },
//            { 4000, 1002 },
//            { 4000, 3000 }
//        };
//
//        Query q = new Query();
//        QueryClass qc = new QueryClass(OverlapRelation.class);
//        q.addFrom(qc);
//        q.addToSelect(qc);
//        SingletonResults res = osw.getObjectStore().executeSingleton(q);
//
//        Assert.assertEquals(expectedOverlaps.length, res.size());
//
//        Iterator resIter = res.iterator();
//      RESULTS:
//        while (resIter.hasNext()) {
//            OverlapRelation overlap = (OverlapRelation) resIter.next();
//
//            Set bioEntities = overlap.getBioEntities();
//
//            Iterator beIter = bioEntities.iterator();
//            SequenceFeature lsf1 = (SequenceFeature) beIter.next();
//            SequenceFeature lsf2 = (SequenceFeature) beIter.next();
//
//            for (int i = 0; i < expectedOverlaps.length; i++) {
//                if (lsf1.getId().intValue() == expectedOverlaps[i][0] &&
//                    lsf2.getId().intValue() == expectedOverlaps[i][1] ||
//                    lsf1.getId().intValue() == expectedOverlaps[i][1] &&
//                    lsf2.getId().intValue() == expectedOverlaps[i][0]) {
//                    continue RESULTS;
//                }
//            }
//
//            Assert.fail("didn't find overlap " + lsf1.getId() + ", "+ lsf2.getId()
//                 + " in expected results");
//        }
//    }

//    public void testCreateOverlapIgnoreSelfMatches () throws Exception {
//        createOverlapTestData();
//
//        CalculateLocations cl = new CalculateLocations(osw);
//        List classesToIgnore = new ArrayList();
//        classesToIgnore.add("Primer");
//        cl.createOverlapRelations(classesToIgnore, true);
//
//        ///////////////////////////////
//
//        int [][] expectedOverlaps = {
//            { 4000, 1000 },
//            { 4000, 1001 },
//            { 4000, 1002 },
//        };
//
//        Query q = new Query();
//        QueryClass qc = new QueryClass(OverlapRelation.class);
//        q.addFrom(qc);
//        q.addToSelect(qc);
//        SingletonResults res = osw.getObjectStore().executeSingleton(q);
//
//        Assert.assertEquals(expectedOverlaps.length, res.size());
//
//        Iterator resIter = res.iterator();
//      RESULTS:
//        while (resIter.hasNext()) {
//            OverlapRelation overlap = (OverlapRelation) resIter.next();
//
//            Set bioEntities = overlap.getBioEntities();
//
//            Iterator beIter = bioEntities.iterator();
//            SequenceFeature lsf1 = (SequenceFeature) beIter.next();
//            SequenceFeature lsf2 = (SequenceFeature) beIter.next();
//
//            for (int i = 0; i < expectedOverlaps.length; i++) {
//                if (lsf1.getId().intValue() == expectedOverlaps[i][0] &&
//                    lsf2.getId().intValue() == expectedOverlaps[i][1] ||
//                    lsf1.getId().intValue() == expectedOverlaps[i][1] &&
//                    lsf2.getId().intValue() == expectedOverlaps[i][0]) {
//                    continue RESULTS;
//                }
//            }
//
//            Assert.fail("didn't find overlap " + lsf1.getId() + ", "+ lsf2.getId()
//                 + " in expected results");
//        }
//    }


    public void testCreateSpanningLocations() throws Exception {
        Exon exon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon1.setId(new Integer(107));
        Exon exon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon2.setId(new Integer(108));
        Exon exon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon3.setId(new Integer(109));

        Location exon1OnChr = createLocation(getChromosome(), exon1, "1", 51, 100, Location.class);
        exon1OnChr.setId(new Integer(1010));
        Location exon2OnChr = createLocation(getChromosome(), exon2, "1", 201, 250, Location.class);
        exon2OnChr.setId(new Integer(1011));
        Location exon3OnChr = createLocation(getChromosome(), exon3, "1", 201, 400, Location.class);
        exon3OnChr.setId(new Integer(1012));

        Transcript trans1 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        trans1.setId(new Integer(201));

        Transcript trans2 =
            (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        trans2.setId(new Integer(202));

        Location trans2OnChr = createLocation(getChromosome(), trans2, "1", 61, 300, Location.class);

        Gene gene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        gene.setId(new Integer(301));

        exon1.setTranscripts(new HashSet<Transcript>(Arrays.asList(new Transcript [] {trans1})));
        exon2.setTranscripts(new HashSet<Transcript>(Arrays.asList(new Transcript [] {trans1})));

        // the location of exon3 should be ignored by createSpanningLocations() because trans2
        // already has a location
        exon3.setTranscripts(new HashSet<Transcript>(Arrays.asList(new Transcript [] {trans2})));

        trans1.setGene(gene);
        trans2.setGene(gene);

        Set<InterMineObject> toStore =
            new HashSet<InterMineObject>(Arrays.asList(new InterMineObject[] {
                    getChromosome(), gene, trans1, trans2,
                    exon1, exon2, exon3,
                    exon1OnChr, exon2OnChr, trans2OnChr
            }));

        for (InterMineObject imo : toStore) {
            osw.store(imo);
        }

        CalculateLocations cl = new CalculateLocations(osw);
        cl.createSpanningLocations("Transcript", "Exon", "exons");
        cl.createSpanningLocations("Gene", "Transcript", "transcripts");

        ObjectStore os = osw.getObjectStore();
        Transcript resTrans1 = (Transcript) os.getObjectById(new Integer(201));

        Assert.assertEquals(1, resTrans1.getLocations().size());
        Location resTrans1Location = (Location) resTrans1.getLocations().iterator().next();
        Assert.assertEquals(51, resTrans1Location.getStart().intValue());
        Assert.assertEquals(250, resTrans1Location.getEnd().intValue());

        Transcript resTrans2 = (Transcript) os.getObjectById(new Integer(202));

        Assert.assertEquals(1, resTrans2.getLocations().size());
        Location resTrans2Location = (Location) resTrans2.getLocations().iterator().next();
        Assert.assertEquals(61, resTrans2Location.getStart().intValue());
        Assert.assertEquals(300, resTrans2Location.getEnd().intValue());

        Gene resGene = (Gene) os.getObjectById(new Integer(301));
        Assert.assertEquals(1, resGene.getLocations().size());
        Location resGeneLocation = (Location) resGene.getLocations().iterator().next();
        Assert.assertEquals(51, resGeneLocation.getStart().intValue());
        Assert.assertEquals(300, resGeneLocation.getEnd().intValue());
    }


    public void testOverlap() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parent = cl.new SimpleLoc(101, 102, 101, 200, "1");

        //   ------------>       parent
        //          |
        //          --------->   child
        CalculateLocations.SimpleLoc s1 = cl.new SimpleLoc(101, 103, 151, 250, "1");
        Assert.assertTrue(CalculateLocations.overlap(s1, parent));
        Assert.assertTrue(CalculateLocations.overlap(parent, s1));

        //       -------------->   parent
        //            |
        //   ---------->           child
        CalculateLocations.SimpleLoc s2 = cl.new SimpleLoc(101, 103, 51, 150, "1");
        Assert.assertTrue(CalculateLocations.overlap(s2, parent));
        Assert.assertTrue(CalculateLocations.overlap(parent, s2));

        //  ------------------>   parent
        //      |        |
        //      ---------->       child
        CalculateLocations.SimpleLoc s3 = cl.new SimpleLoc(101, 103, 126, 175, "1");
        Assert.assertTrue(CalculateLocations.overlap(s3, parent));
        Assert.assertTrue(CalculateLocations.overlap(parent, s3));

        //      -------->        parent
        //      |      |
        //   -------------->     child
        CalculateLocations.SimpleLoc s4 = cl.new SimpleLoc(101, 103, 51, 250, "1");
        Assert.assertTrue(CalculateLocations.overlap(s4, parent));
        Assert.assertTrue(CalculateLocations.overlap(parent, s4));

        // ------->               parent
        //
        //           ------->     child
        CalculateLocations.SimpleLoc s5 = cl.new SimpleLoc(101, 103, 251, 350, "1");
        Assert.assertFalse(CalculateLocations.overlap(s5, parent));
        Assert.assertFalse(CalculateLocations.overlap(parent, s5));

        //           ------->     parent
        //
        // ------->               child
        CalculateLocations.SimpleLoc s6 = cl.new SimpleLoc(101, 103, 26, 75, "1");
        Assert.assertFalse(CalculateLocations.overlap(s6, parent));
        Assert.assertFalse(CalculateLocations.overlap(parent, s6));
    }


    public void testSetChromosomeLocationsAndLengths() throws Exception {
        Chromosome chr1 = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr1.setPrimaryIdentifier("1");
        chr1.setId(new Integer(101));
        Chromosome chr2 = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr1.setPrimaryIdentifier("2");
        chr1.setId(new Integer(102));

        Exon exon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon1.setId(new Integer(107));
        exon1.setLength(new Integer(1000));
        Exon exon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon2.setId(new Integer(108));
        Exon exon3 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon3.setId(new Integer(109));

        // exon 2 has two chromosome locations, shouldn't get chromosome[Location] references
        Location exon1OnChr = createLocation(chr1, exon1, "1", 51, 100, Location.class);
        exon1OnChr.setId(new Integer(1010));
        Location exon2OnChr = createLocation(chr2, exon2, "1", 201, 250, Location.class);
        exon2OnChr.setId(new Integer(1011));
        Location exon2OnChrDup = createLocation(chr1, exon2, "1", 501, 550, Location.class);
        exon2OnChrDup.setId(new Integer(1012));
        Location exon3OnChr = createLocation(chr2, exon3, "1", 601, 650, Location.class);
        exon3OnChr.setId(new Integer(1013));

        Set<InterMineObject> toStore =
            new HashSet<InterMineObject>(Arrays.asList(new InterMineObject[] {
                    chr1, chr2, exon1, exon2, exon3,
                    exon1OnChr, exon2OnChr, exon2OnChrDup, exon3OnChr
            }));

        for (InterMineObject imo : toStore) {
            osw.store(imo);
        }

        CalculateLocations cl = new CalculateLocations(osw);
        cl.setChromosomeLocationsAndLengths();

        ObjectStore os = osw.getObjectStore();
        Exon resExon1 = (Exon) os.getObjectById(new Integer(107));
        Exon resExon2 = (Exon) os.getObjectById(new Integer(108));
        Exon resExon3 = (Exon) os.getObjectById(new Integer(109));

        assertEquals(chr1.getId(), resExon1.getChromosome().getId());
        assertEquals(exon1OnChr.getId(), resExon1.getChromosomeLocation().getId());

        assertNull(resExon2.getChromosome());
        assertNull(resExon2.getChromosomeLocation());

        assertEquals(chr2.getId(), resExon3.getChromosome().getId());
        assertEquals(exon3OnChr.getId(), resExon3.getChromosomeLocation().getId());

        // exon1 has length set so should stay as 1000, exon3 should get length 50 set from location
        assertEquals(new Integer(1000), resExon1.getLength());
        assertEquals(new Integer(50), resExon3.getLength());
        // nothing done to exon2
        assertNull(resExon2.getLength());
    }


    private Location createLocation(BioEntity object, BioEntity subject, String strand,
                                    int start, int end, Class<?> locationClass) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(locationClass));
        loc.setLocatedOn(object);
        loc.setFeature(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStrand(strand);
        return loc;
    }

    private Chromosome getChromosome() {
        if (chromosome == null) {
            chromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
            chromosome.setPrimaryIdentifier("X");
            chromosome.setLength(new Integer(10000));
            chromosome.setId(new Integer(101));
        }
        return chromosome;
    }
}
