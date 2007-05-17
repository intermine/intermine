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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.ChromosomeBand;
import org.flymine.model.genomic.Contig;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.LocatedSequenceFeature;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.OverlapRelation;
import org.flymine.model.genomic.PartialLocation;
import org.flymine.model.genomic.ReversePrimer;
import org.flymine.model.genomic.Supercontig;
import org.flymine.model.genomic.Transcript;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.ItemFactory;

public class CalculateLocationsTest extends TestCase {

    private ObjectStoreWriter osw;
    private Chromosome chromosome = null;
    private ChromosomeBand band = null;
    private Location bandOnChr = null;
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
        LOG.error("in tear down");
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        LOG.error("created results");
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.error("deleting: " +o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.error("committed transaction");
        osw.close();
        LOG.error("closed objectstore");
    }

    private void createOverlapTestData() throws Exception {
        Chromosome chr =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setIdentifier("X");
        chr.setLength(new Integer(1000));
        chr.setId(new Integer(101));

        Set toStore = new HashSet();

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


        Contig contig =
            (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        contig.setId(new Integer(4000));
        contig.setLength(new Integer(10));
        contig.setChromosome(chr);

        Location contigLoc = createLocation(chr, contig, "1", 1, 10, Location.class);
        contigLoc.setId(new Integer(4001));

        toStore.add(contig);
        toStore.add(contigLoc);


        toStore.addAll(Arrays.asList(exons));
        toStore.addAll(Arrays.asList(exonLocs));

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }
    }

    public void testCreateOverlapFull () throws Exception {
        createOverlapTestData();
        
        CalculateLocations cl = new CalculateLocations(osw);
        List classesToIgnore = new ArrayList();
        classesToIgnore.add("Primer=Exon");
        cl.createOverlapRelations(classesToIgnore, false);

        ///////////////////////////////

        int [][] expectedOverlaps = {
            { 1001, 1002 },
            { 1003, 1004 },
            { 1006, 1007 },
            { 1006, 1008 },
            { 1006, 1009 },
            { 1006, 1010 },
            { 1006, 1011 },
            { 1006, 1012 },
            { 1006, 1013 },
            { 1006, 1014 },
            { 1007, 1008 },
            { 1008, 1009 },
            { 1008, 1010 },
            { 1008, 1011 },
            { 1008, 1012 },
            { 1008, 1013 },
            { 1008, 1014 },
            { 1009, 1010 },
            { 1009, 1011 },
            { 1010, 1011 },
            { 1012, 1013 },
            { 1018, 1019 },
            { 1019, 1020 },
            { 4000, 1000 },
            { 4000, 1001 },
            { 4000, 1002 },
            { 4000, 3000 }
        };

        Query q = new Query();
        QueryClass qc = new QueryClass(OverlapRelation.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);

        Assert.assertEquals(expectedOverlaps.length, res.size());

        Iterator resIter = res.iterator();
      RESULTS:
        while (resIter.hasNext()) {
            OverlapRelation overlap = (OverlapRelation) resIter.next();

            Set bioEntities = overlap.getBioEntities();

            Iterator beIter = bioEntities.iterator();
            LocatedSequenceFeature lsf1 = (LocatedSequenceFeature) beIter.next();
            LocatedSequenceFeature lsf2 = (LocatedSequenceFeature) beIter.next();

            for (int i = 0; i < expectedOverlaps.length; i++) {
                if (lsf1.getId().intValue() == expectedOverlaps[i][0] &&
                    lsf2.getId().intValue() == expectedOverlaps[i][1] ||
                    lsf1.getId().intValue() == expectedOverlaps[i][1] &&
                    lsf2.getId().intValue() == expectedOverlaps[i][0]) {
                    continue RESULTS;
                }
            }

            Assert.fail("didn't find overlap " + lsf1.getId() + ", "+ lsf2.getId()
                 + " in expected results");
        }
    }
    
    public void testCreateOverlapIgnoreSelfMatches () throws Exception {
        createOverlapTestData();
        
        CalculateLocations cl = new CalculateLocations(osw);
        List classesToIgnore = new ArrayList();
        classesToIgnore.add("Primer");
        cl.createOverlapRelations(classesToIgnore, true);

        ///////////////////////////////

        int [][] expectedOverlaps = {
            { 4000, 1000 },
            { 4000, 1001 },
            { 4000, 1002 },
        };

        Query q = new Query();
        QueryClass qc = new QueryClass(OverlapRelation.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);

        Assert.assertEquals(expectedOverlaps.length, res.size());

        Iterator resIter = res.iterator();
      RESULTS:
        while (resIter.hasNext()) {
            OverlapRelation overlap = (OverlapRelation) resIter.next();

            Set bioEntities = overlap.getBioEntities();

            Iterator beIter = bioEntities.iterator();
            LocatedSequenceFeature lsf1 = (LocatedSequenceFeature) beIter.next();
            LocatedSequenceFeature lsf2 = (LocatedSequenceFeature) beIter.next();

            for (int i = 0; i < expectedOverlaps.length; i++) {
                if (lsf1.getId().intValue() == expectedOverlaps[i][0] &&
                    lsf2.getId().intValue() == expectedOverlaps[i][1] ||
                    lsf1.getId().intValue() == expectedOverlaps[i][1] &&
                    lsf2.getId().intValue() == expectedOverlaps[i][0]) {
                    continue RESULTS;
                }
            }

            Assert.fail("didn't find overlap " + lsf1.getId() + ", "+ lsf2.getId()
                 + " in expected results");
        }
    }

    public void testFixPartials() throws Exception {
        Contig contig1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        contig1.setLength(new Integer(1000));
        contig1.setId(new Integer(105));

        Contig contig2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        contig2.setLength(new Integer(1500));
        contig2.setId(new Integer(106));

        Exon exon1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon1.setId(new Integer(107));

        Exon exon2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon2.setId(new Integer(108));

        Location exon1OnContig1 = createLocation(contig1, exon1, "1", 51, 250, Location.class);
        exon1OnContig1.setId(new Integer(1010));
        Location exon2OnContig1 = createLocation(contig1, exon2, "1", 701, 1000, Location.class);
        exon2OnContig1.setId(new Integer(1011));
        Location exon2OnContig2 = createLocation(contig2, exon2, "1", 1, 20, Location.class);
        exon2OnContig2.setId(new Integer(1012));

        Object [] objects = new Object[] {
            contig1, contig2,
                exon1, exon2,
                exon1OnContig1, exon2OnContig1, exon2OnContig2
                };

        Set toStore = new HashSet(Arrays.asList(objects));

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }

        CalculateLocations cl = new CalculateLocations(osw);
        cl.fixPartials();

        ObjectStore os = osw.getObjectStore();

        ///////////////////////////////


        Location resExon1OnContig1 = (Location) os.getObjectById(new Integer(1010));

        Assert.assertTrue((resExon1OnContig1 instanceof Location) &&
                   ! (resExon1OnContig1 instanceof PartialLocation));

        Item resExon1OnContig1Item = itemFactory.makeItem(resExon1OnContig1);

        Location expectedResExon1OnContig1 =
            (Location) createLocation(contig1, exon1, "1", 51, 250, Location.class);

        expectedResExon1OnContig1.setId(new Integer(1010));
        expectedResExon1OnContig1.setStartIsPartial(Boolean.FALSE);
        expectedResExon1OnContig1.setEndIsPartial(Boolean.FALSE);

        Item expectedResExon1OnContig1Item = itemFactory.makeItem(expectedResExon1OnContig1);

        Assert.assertEquals(expectedResExon1OnContig1Item, resExon1OnContig1Item);



        ///////////////////////////////////////


        PartialLocation resExon2OnContig1 = (PartialLocation) os.getObjectById(new Integer(1011));
        Item resExon2OnContig1Item = itemFactory.makeItem(resExon2OnContig1);

        PartialLocation expectedResExon2OnContig1 =
            (PartialLocation) createLocation(contig1, exon2, "1", 701, 1000, PartialLocation.class);

        expectedResExon2OnContig1.setSubjectStart(new Integer(1));
        expectedResExon2OnContig1.setSubjectEnd(new Integer(300));
        expectedResExon2OnContig1.setId(new Integer(1011));
        expectedResExon2OnContig1.setStartIsPartial(Boolean.FALSE);
        expectedResExon2OnContig1.setEndIsPartial(Boolean.TRUE);

        Item expectedResExon2OnContig1Item = itemFactory.makeItem(expectedResExon2OnContig1);

        Assert.assertEquals(expectedResExon2OnContig1Item, resExon2OnContig1Item);


        /////////////////////////////////////////


        PartialLocation resExon2OnContig2 = (PartialLocation) os.getObjectById(new Integer(1012));
        Item resExon2OnContig2Item = itemFactory.makeItem(resExon2OnContig2);

        PartialLocation expectedResExon2OnContig2 =
            (PartialLocation) createLocation(contig2, exon2, "1", 1, 20, PartialLocation.class);

        expectedResExon2OnContig2.setSubjectStart(new Integer(301));
        expectedResExon2OnContig2.setSubjectEnd(new Integer(320));
        expectedResExon2OnContig2.setId(new Integer(1012));
        expectedResExon2OnContig2.setStartIsPartial(Boolean.TRUE);
        expectedResExon2OnContig2.setEndIsPartial(Boolean.FALSE);

        Item expectedResExon2OnContig2Item = itemFactory.makeItem(expectedResExon2OnContig2);

        Assert.assertEquals(expectedResExon2OnContig2Item, resExon2OnContig2Item);
    }

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

        exon1.setTranscripts(new HashSet(Arrays.asList(new Object [] {trans1})));
        exon2.setTranscripts(new HashSet(Arrays.asList(new Object [] {trans1})));

        // the location of exon3 should be ignored by createSpanningLocations() because trans2
        // already has a location
        exon3.setTranscripts(new HashSet(Arrays.asList(new Object [] {trans2})));

        trans1.setGene(gene);
        trans2.setGene(gene);

        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), gene, trans1, trans2,
                                                    exon1, exon2, exon3,
                                                    exon1OnChr, exon2OnChr, trans2OnChr
                                                }));

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }

        CalculateLocations cl = new CalculateLocations(osw);
        cl.createSpanningLocations(Transcript.class, Exon.class, "exons");
        cl.createSpanningLocations(Gene.class, Transcript.class, "transcripts");

        ObjectStore os = osw.getObjectStore();
        Transcript resTrans1 = (Transcript) os.getObjectById(new Integer(201));

        Assert.assertEquals(1, resTrans1.getObjects().size());
        Location resTrans1Location = (Location) resTrans1.getObjects().iterator().next();
        Assert.assertEquals(51, resTrans1Location.getStart().intValue());
        Assert.assertEquals(250, resTrans1Location.getEnd().intValue());

        Transcript resTrans2 = (Transcript) os.getObjectById(new Integer(202));

        Assert.assertEquals(1, resTrans2.getObjects().size());
        Location resTrans2Location = (Location) resTrans2.getObjects().iterator().next();
        Assert.assertEquals(61, resTrans2Location.getStart().intValue());
        Assert.assertEquals(300, resTrans2Location.getEnd().intValue());

        Gene resGene = (Gene) os.getObjectById(new Integer(301));
        Assert.assertEquals(1, resGene.getObjects().size());
        Location resGeneLocation = (Location) resGene.getObjects().iterator().next();
        Assert.assertEquals(51, resGeneLocation.getStart().intValue());
        Assert.assertEquals(300, resGeneLocation.getEnd().intValue());
    }

    public void testSupercontigToChromosome() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));

        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));

        sc.setId(new Integer(104));
        Location loc = createLocation(getChromosome(), sc, "1", 1201, 1600, Location.class);
        toStore.add(sc);
        toStore.add(loc);

        //Source source = iw.getMainSource("genomic-test");
        //Source skelSource = iw.getSkeletonSource("genomic-test");
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            InterMineObject object = (InterMineObject) i.next();
            osw.store(object);
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        Location expected = createLocation(getChromosomeBand(), sc, "1", 201, 600, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                             Supercontig.class, true);
        Iterator iter = results.iterator();
        Location result = (Location) ((ResultsRow) iter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testRevSupercontigToChromosome() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Location loc = createLocation(getChromosome(), sc, "-1", 1201, 1600, Location.class);
        toStore.add(sc);
        toStore.add(loc);

        //Source source = iw.getMainSource("genomic-test");
        //Source skelSource = iw.getSkeletonSource("genomic-test");
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            InterMineObject object = (InterMineObject) i.next();
            osw.store(object);
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        Location expected = createLocation(getChromosomeBand(), sc, "-1", 201, 600, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        ChromosomeBand.class, Supercontig.class, true);
        Iterator iter = results.iterator();
        Location result = (Location) ((ResultsRow) iter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");

        Assert.assertEquals(expItem, resItem);
    }

    public void testContigToSupercontig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        sc.setId(new Integer(104));
        c.setId(new Integer(105));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "1", 101, 350, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);

        //Source source = iw.getMainSource("genomic-test");
        //Source skelSource = iw.getSkeletonSource("genomic-test");
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        // test contig location on chromosome
        Location expected = createLocation(getChromosome(), c, "1", 1301, 1550, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                               Chromosome.class,
                                                               Contig.class, true);
        Iterator chrContigIter = results.iterator();
        Location result = (Location) ((ResultsRow) chrContigIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test contig location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), c, "1", 301, 550, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                 Contig.class, true);
        Iterator chrBandContigIter = results.iterator();

        result = (Location) ((ResultsRow) chrBandContigIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testRevContigToSupercontig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        sc.setId(new Integer(104));
        c.setId(new Integer(105));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "1", 101, 350, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);

        //Source source = iw.getMainSource("genomic-test");
        //Source skelSource = iw.getSkeletonSource("genomic-test");
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        // test contig location on chromosome
        Location expected = createLocation(getChromosome(), c, "1", 1301, 1550, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Chromosome.class,
                                                        Contig.class, true);
        Iterator chrContigIter = results.iterator();
        Location result = (Location) ((ResultsRow) chrContigIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test contig location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), c, "1", 301, 550, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Contig.class, true);
        Iterator chrBandContigIter = results.iterator();
        result = (Location) ((ResultsRow) chrBandContigIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testFeatureToContig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c.setId(new Integer(105));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(106));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "1", 101, 350, Location.class);
        Location exonOnContig = createLocation(c, e, "1", 51, 150, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);
        toStore.add(exonOnContig);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1351, 1450, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                                    Chromosome.class, Exon.class, true);
        Iterator chrExonIter = results.iterator();
        Location result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 151, 250, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                 Exon.class, true);
        Iterator supercontigExonIter = results.iterator();
        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 351, 450, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                 Exon.class, true);
        Iterator chrBandExonIter = results.iterator();

        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testFeatureToContigWithPartialLocations() throws Exception {
        //  --------------------------> chromosome
        //    --------------------->    supercontig
        //        ----->----->
        //         con1  con2
        //           --->
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 241, 250, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "1", 1, 60, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1541, 1610, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                                    Chromosome.class, Exon.class,
                                                                    true);

        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 341, 410, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                       Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 541, 610, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                       Exon.class, true);
        Iterator chrBandExonIter = results.iterator();

        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testRevFeatureToContigWithPartialLocations() throws Exception {
        //  --------------------------> chromosome
        //    --------------------->    supercontig
        //        ----->----->
        //         con1  con2
        //           <---
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "-1", 241, 250, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "-1", 1, 60, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "-1", 1541, 1610, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                               Chromosome.class, Exon.class,
                                                               true);

        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "-1", 341, 410, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                       Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "-1", 541, 610, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                       Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testFeatureToRevContigsWithPartialLocations() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));


        //  --------------------------> chromosome
        //    --------------------->    supercontig
        //       <-----<-----
        //         con1  con2
        //           <---
        //           exon
        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "-1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "-1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 1, 20, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "1", 41, 70, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "-1", 1531, 1580, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                              Chromosome.class, Exon.class,
                                                              true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "-1", 331, 380, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                 Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "-1", 531, 580, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                       Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }


    public void testRevFeatureToRevContigsWithPartialLocations() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));


        //  --------------------------> chromosome
        //    --------------------->    supercontig
        //       <-----<-----
        //         con1  con2
        //           --->
        //           exon
        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "-1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "-1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "-1", 1, 20, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "-1", 41, 70, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1531, 1580, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                               Chromosome.class, Exon.class,
                                                               true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 331, 380, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                       Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 531, 580, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                       Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }


    public void testFeatureToOneRevContigWithPartialLocations() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {
            getChromosome(), getChromosomeBand(), getBandOnChr()
                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));


        //  --------------------------> chromosome
        //    --------------------->    supercontig
        //       -----><-----
        //        con1  con2
        //           --->
        //           exon
        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "-1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 191, 250, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "-1", 41, 70, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1491, 1580, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                               Chromosome.class, Exon.class,
                                                               true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 1491-1200, 1580-1200, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                 Exon.class, true);
Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 1491-1000, 1580-1000, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                 Exon.class, true);
Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testRevFeatureToOneRevContigWithPartialLocations() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {
            getChromosome(), getChromosomeBand(), getBandOnChr()
                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));


        //  --------------------------> chromosome
        //    --------------------->    supercontig
        //       -----><-----
        //        con1  con2
        //           <---
        //           exon
        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "-1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 191, 250, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "-1", 41, 70, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1491, 1580, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        Chromosome.class, Exon.class, true);

        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 1491-1200, 1580-1200, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                 Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 1491-1000, 1580-1000, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                 Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }


    public void testRevSCNormalFeatureToContigWithPartialLocations() throws Exception {
        //  --------------------------> chromosome
        //    <---------------------    supercontig
        //        <-----<-----
        //         con1  con2
        //           <---
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 241, 250, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "1", 1, 60, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "-1", 1491, 1560, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        Chromosome.class, Exon.class,
                                                        true);

        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 341, 410, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                 Exon.class, true);
Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "-1", 1491-1000, 1560-1000, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                 Exon.class, true);
Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testRevSCRevFeatureToContigWithPartialLocations() throws Exception {
        //  --------------------------> chromosome
        //    <---------------------    supercontig
        //        <-----<-----
        //         con1  con2
        //           --->
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "-1", 241, 250, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "-1", 1, 60, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1491, 1560, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        Chromosome.class, Exon.class,
                                                        true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "-1", 341, 410, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 1491-1000, 1560-1000, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testRevSCNormalFeatureToRevContigWithPartialLocations() throws Exception {
        //  --------------------------> chromosome
        //    <---------------------    supercontig
        //        ----->----->
        //         con1  con2
        //           --->
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "-1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "-1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 1, 30, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "1", 61, 70, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1541, 1580, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        Chromosome.class, Exon.class,
                                                        true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "-1", 321, 360, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 1541-1000, 1580-1000, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }


    public void testRevSCRevFeatureToRevContigWithPartialLocations() throws Exception {
        //  --------------------------> chromosome
        //    <---------------------    supercontig
        //        ----->----->
        //         con1  con2
        //           <---
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(105));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(106));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1900, Location.class);
        scOnChr.setId(new Integer(170));
        Location contig1OnSc = createLocation(sc, c1, "-1", 101, 350, Location.class);
        contig1OnSc.setId(new Integer(201));
        Location contig2OnSc = createLocation(sc, c2, "-1", 351, 420, Location.class);
        contig2OnSc.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "-1", 1, 30, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "-1", 61, 70, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contig1OnSc);
        toStore.add(contig2OnSc);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "-1", 1541, 1580 , Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        Chromosome.class, Exon.class,
                                                        true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 321, 360, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "-1", 1541-1000, 1580-1000, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                        Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testFeatureToContigWithPartialLocationsDisjointSuperContigs() throws Exception {
        //  --------------------------> chromosome
        //    --------->----------->    supercontigs
        //        ----->----->
        //         con1  con2
        //           --->
        //           exon
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));
        Supercontig sc1 =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc1.setId(new Integer(104));
        Supercontig sc2 =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc2.setId(new Integer(105));

        Contig c1 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c1.setId(new Integer(106));
        c1.setLength(new Integer(250));
        Contig c2 = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c2.setId(new Integer(107));
        c2.setLength(new Integer(70));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(108));
        Location sc1OnChr = createLocation(getChromosome(), sc1, "1", 1201, 1400, Location.class);
        sc1OnChr.setId(new Integer(170));
        Location sc2OnChr = createLocation(getChromosome(), sc2, "1", 1401, 1900, Location.class);
        sc2OnChr.setId(new Integer(171));
        Location contig1OnSc1 = createLocation(sc1, c1, "1", 101, 200, Location.class);
        contig1OnSc1.setId(new Integer(201));
        Location contig2OnSc2 = createLocation(sc2, c2, "1", 1, 200, Location.class);
        contig2OnSc2.setId(new Integer(202));
        PartialLocation exonOnContig1 =
            (PartialLocation) createLocation(c1, e, "1", 51, 100, PartialLocation.class);
        exonOnContig1.setId(new Integer(203));
        exonOnContig1.setStartIsPartial(Boolean.FALSE);
        exonOnContig1.setEndIsPartial(Boolean.TRUE);
        exonOnContig1.setSubjectStart(new Integer(1));
        exonOnContig1.setSubjectEnd(new Integer(10));
        PartialLocation exonOnContig2 =
            (PartialLocation) createLocation(c2, e, "1", 1, 160, PartialLocation.class);
        exonOnContig2.setId(new Integer(204));
        exonOnContig2.setStartIsPartial(Boolean.TRUE);
        exonOnContig2.setEndIsPartial(Boolean.FALSE);
        exonOnContig2.setSubjectStart(new Integer(11));
        exonOnContig2.setSubjectEnd(new Integer(70));
        toStore.add(sc1);
        toStore.add(sc2);
        toStore.add(c1);
        toStore.add(c2);
        toStore.add(e);
        toStore.add(sc1OnChr);
        toStore.add(sc2OnChr);
        toStore.add(contig1OnSc1);
        toStore.add(contig2OnSc2);
        toStore.add(exonOnContig1);
        toStore.add(exonOnContig2);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }



        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        ResultsRow rr;

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1351, 1560, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results= PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                       Chromosome.class, Exon.class,
                                                       true);
        Iterator chrExonIter = results.iterator();

        rr = (ResultsRow) chrExonIter.next();

        Location result = (Location) rr.get(2);
        Assert.assertTrue(! (result instanceof PartialLocation));
        Assert.assertFalse(chrExonIter.hasNext());
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon locations on Supercontig
        expected = createLocation(sc1, e, "1", 151, 200, PartialLocation.class);
        expected.setId(new Integer(0));
        ((PartialLocation) expected).setEndIsPartial(Boolean.TRUE);
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        Assert.assertTrue(supercontigExonIter.hasNext());

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertTrue(result instanceof PartialLocation);

        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        expected = createLocation(sc2, e, "1", 1, 160, PartialLocation.class);
        expected.setId(new Integer(0));
        ((PartialLocation) expected).setStartIsPartial(Boolean.TRUE);
        expItem = itemFactory.makeItem(expected);

        rr = (ResultsRow) supercontigExonIter.next();
        result = (Location) rr.get(2);

        Assert.assertTrue(result instanceof PartialLocation);

        Assert.assertFalse(supercontigExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);


        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 351, 560, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        Assert.assertTrue(chrBandExonIter.hasNext());
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        Assert.assertTrue(! (result instanceof PartialLocation));
        Assert.assertFalse(chrBandExonIter.hasNext());
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }


    // Chromosome ------------------>
    // Supercontig   ------------->
    // Contig          <---------
    // Exon                  <--
    public void testFeatureToRevContig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c.setId(new Integer(105));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(106));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "-1", 101, 350, Location.class);
        Location exonOnContig = createLocation(c, e, "1", 71, 100, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);
        toStore.add(exonOnContig);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);

        cl.createLocations();

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "-1", 1451, 1480, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                        Chromosome.class, Exon.class,
                                                        true);
        Iterator chrExonIter = results.iterator();

        Location result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on Supercontig
        expected = createLocation(sc, e, "-1", 251, 280, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();
        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "-1", 451, 480, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    // Chromosome ------------------>
    // Supercontig   <-------------
    // Contig          --------->
    // Exon               -->
    public void testFeatureToRevContigAndRevSupercontig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c.setId(new Integer(105));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(106));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "-1", 101, 350, Location.class);
        Location exonOnContig = createLocation(c, e, "1", 71, 100, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);
        toStore.add(exonOnContig);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);

        cl.createLocations();

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "1", 1321, 1350, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Chromosome.class, Exon.class, true);
        Iterator chrExonIter = results.iterator();
        Location result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on Supercontig
        expected = createLocation(sc, e, "-1", 251, 280, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();
        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "1", 321, 350, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    // Chromosome ------------------>
    // Supercontig   <-------------
    // Contig          <---------
    // Exon 1                <--
    // Exon 1            -->
    public void testFeatureToContigRevSupercontig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c.setId(new Integer(105));
        Exon e1 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e1.setId(new Integer(106));
        Exon e2 = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e2.setId(new Integer(107));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "1", 101, 350, Location.class);
        Location exon1OnContig = createLocation(c, e1, "1", 21, 30, Location.class);
        Location exon2OnContig = createLocation(c, e2, "-1", 71, 100, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(e1);
        toStore.add(e2);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);
        toStore.add(exon1OnContig);
        toStore.add(exon2OnContig);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);

        cl.createLocations();

        // test Exon 1 location on Chromosome
        Location expected = createLocation(getChromosome(), e1, "-1", 1471, 1480, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results =
            PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Chromosome.class, Exon.class, true);
        Iterator chrExonIter = results.iterator();
        Location result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon 2 location on Chromosome
        expected = createLocation(getChromosome(), e2, "1", 1401, 1430, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon 1 location on Supercontig
        expected = createLocation(sc, e1, "1", 121, 130, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();
        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon 2 location on Supercontig
        expected = createLocation(sc, e2, "-1", 171, 200, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon 1 location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e1, "-1", 471, 480, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon 2 location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e2, "1", 401, 430, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    // Chromosome ------------------>
    // Supercontig   <-------------
    // Contig          --------->
    // Exon               <--
    public void testRevFeatureToRevContigAndRevSupercontig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c.setId(new Integer(105));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(106));
        Location scOnChr = createLocation(getChromosome(), sc, "-1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "-1", 101, 350, Location.class);
        Location exonOnContig = createLocation(c, e, "-1", 71, 100, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(e);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);
        toStore.add(exonOnContig);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);

        cl.createLocations();

        // test Exon location on Chromosome
        Location expected = createLocation(getChromosome(), e, "-1", 1321, 1350, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Chromosome.class,
                                                        Exon.class, true);
        Iterator chrExonIter = results.iterator();

        Location result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on Supercontig
        expected = createLocation(sc, e, "1", 251, 280, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), Supercontig.class,
                                                Exon.class, true);
        Iterator supercontigExonIter = results.iterator();

        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);

        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, "-1", 321, 350, Location.class);
        expected.setId(new Integer(0));
        expItem = itemFactory.makeItem(expected);
        results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(), ChromosomeBand.class,
                                                Exon.class, true);
        Iterator chrBandExonIter = results.iterator();

        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }

    public void testCloneInterMineObject() throws Exception {
        Chromosome chr = getChromosome();
        Chromosome newChr =
            (Chromosome) CalculateLocations.cloneInterMineObject(chr, Chromosome.class);

        Assert.assertEquals(itemFactory.makeItem(chr), itemFactory.makeItem(newChr));
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


    public void testCreateTransformedLocation() throws Exception {
        Chromosome chr = getChromosome();
        int chrId = chr.getId().intValue();
        BioEntity parent = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int parentId = 101;
        parent.setId(new Integer(parentId));
        BioEntity child = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int childId = 102;
        child.setId(new Integer(childId));

        //  ------------------>   parent
        //      |        |
        //      ---------->       child
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, "1");
        CalculateLocations.SimpleLoc childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, "1");
        Location res = cl.createTransformedLocation(parentOnChr, childOnParent, chr, child);

        Location exp1 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp1.setStart(new Integer(251));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.FALSE);
        exp1.setEndIsPartial(Boolean.FALSE);
        exp1.setStrand("1");
        exp1.setObject(chr);
        exp1.setSubject(child);
        Assert.assertEquals(toItem(exp1), toItem(res));

        //  <------------------   parent
        //      |        |
        //     <----------       child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, "-1");
        childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, "1");
        res = cl.createTransformedLocation(parentOnChr, childOnParent, chr, child);

        Location exp2 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp2.setStart(new Integer(151));
        exp2.setEnd(new Integer(250));
        exp2.setStartIsPartial(Boolean.FALSE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setStrand("-1");
        exp2.setObject(chr);
        exp2.setSubject(child);
        Assert.assertEquals(toItem(exp2), toItem(res));

        //  ------------------>  parent
        //      |        |
        //     <----------       child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, "1");
        childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, "-1");
        res = cl.createTransformedLocation(parentOnChr, childOnParent, chr, child);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(251));
        exp3.setEnd(new Integer(350));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand("-1");
        exp3.setObject(chr);
        exp3.setSubject(child);
        Assert.assertEquals(toItem(exp3), toItem(res));

        //  <-----------------   parent
        //      |        |
        //      ---------->      child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, "-1");
        childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, "-1");
        res = cl.createTransformedLocation(parentOnChr, childOnParent, chr, child);

        Location exp4 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp4.setStart(new Integer(151));
        exp4.setEnd(new Integer(250));
        exp4.setStartIsPartial(Boolean.FALSE);
        exp4.setEndIsPartial(Boolean.FALSE);
        exp4.setStrand("1");
        exp4.setObject(chr);
        exp4.setSubject(child);
        Assert.assertEquals(toItem(exp4), toItem(res));
    }


    public void testCreateLocationNormal() throws Exception {
        BioEntity parent = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int parentId = 101;
        parent.setId(new Integer(parentId));
        BioEntity child = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int childId = 102;
        child.setId(new Integer(childId));

        //  ------------------>   parent
        //      |        |
        //      ---------->       child
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "1");
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, "1");
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(51));
        exp3.setEnd(new Integer(150));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand("1");
        exp3.setObject(parent);
        exp3.setSubject(child);
        Assert.assertEquals(toItem(exp3), toItem(res));

        //   ------------>        parent
        //          |
        //          --------->    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, "1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(151));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.FALSE);
        exp1.setEndIsPartial(Boolean.TRUE);
        exp1.setSubjectStart(new Integer(1));
        exp1.setSubjectEnd(new Integer(200));
        exp1.setStrand("1");
        exp1.setObject(parent);
        exp1.setSubject(child);
        Assert.assertEquals(toItem(exp1), toItem(res));

        //       -------------->   parent
        //            |
        //   ---------->           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, "1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(1));
        exp2.setEnd(new Integer(50));
        exp2.setStartIsPartial(Boolean.TRUE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setSubjectStart(new Integer(51));
        exp2.setSubjectEnd(new Integer(100));
        exp2.setStrand("1");
        exp2.setObject(parent);
        exp2.setSubject(child);
        Assert.assertEquals(toItem(exp2), toItem(res));

        //      -------->        parent
        //      |      |
        //   -------------->     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, "1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(51));
        exp4.setSubjectEnd(new Integer(250));
        exp4.setStrand("1");
        exp4.setObject(parent);
        exp4.setSubject(child);
        Assert.assertEquals(toItem(exp4), toItem(res));
    }

    public void testCreateLocationPartialChildStart() throws Exception {
        BioEntity parent = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int parentId = 101;
        parent.setId(new Integer(parentId));
        BioEntity child = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int childId = 102;
        child.setId(new Integer(childId));

        //  ------------------>   parent
        //      |        |
        //      ---------->       child
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "1");
        CalculateLocations.SimpleLoc childOnChr =
            cl.new SimpleLoc(parentId, childId, 151, 250, "1", true, false);
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 =
            (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp3.setStart(new Integer(51));
        exp3.setEnd(new Integer(150));
        exp3.setStartIsPartial(Boolean.TRUE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand("1");
        exp3.setObject(parent);
        exp3.setSubject(child);
        Assert.assertEquals(toItem(exp3), toItem(res));

        //   ------------>        parent
        //          |
        //          --------->    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, "1", true, false);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(151));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.TRUE);
        exp1.setEndIsPartial(Boolean.TRUE);
        exp1.setSubjectStart(new Integer(1));
        exp1.setSubjectEnd(new Integer(200));
        exp1.setStrand("1");
        exp1.setObject(parent);
        exp1.setSubject(child);
        Assert.assertEquals(toItem(exp1), toItem(res));

        //       -------------->   parent
        //            |
        //   ---------->           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, "1", true, false);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(1));
        exp2.setEnd(new Integer(50));
        exp2.setStartIsPartial(Boolean.TRUE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setSubjectStart(new Integer(51));
        exp2.setSubjectEnd(new Integer(100));
        exp2.setStrand("1");
        exp2.setObject(parent);
        exp2.setSubject(child);
        Assert.assertEquals(toItem(exp2), toItem(res));

        //      -------->        parent
        //      |      |
        //   -------------->     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, "1", true, false);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(51));
        exp4.setSubjectEnd(new Integer(250));
        exp4.setStrand("1");
        exp4.setObject(parent);
        exp4.setSubject(child);
        Assert.assertEquals(toItem(exp4), toItem(res));
    }

    public void testCreateLocationPartialChildEnd() throws Exception {
        // XXX - write me
    }

    public void testCreateLocationPartialParentStart() throws Exception {
        // XXX - write me
    }

    public void testCreateLocationPartialParentEnd() throws Exception {
        // XXX - write me
    }

    public void testCreateLocationParentRev() throws Exception {
        BioEntity parent = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int parentId = 101;
        parent.setId(new Integer(parentId));
        BioEntity child = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int childId = 102;
        child.setId(new Integer(childId));

        //  <------------------   parent
        //      |        |
        //      ---------->       child
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "-1");
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, "1");
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(151));
        exp3.setEnd(new Integer(250));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand("-1");
        exp3.setObject(parent);
        exp3.setSubject(child);
        Assert.assertEquals(toItem(exp3), toItem(res));

        //   <------------        parent
        //          |
        //          --------->    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, "-1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, "1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(1));
        exp1.setEnd(new Integer(200));
        exp1.setStartIsPartial(Boolean.TRUE);
        exp1.setEndIsPartial(Boolean.FALSE);
        exp1.setSubjectStart(new Integer(1));
        exp1.setSubjectEnd(new Integer(200));
        exp1.setStrand("-1");
        exp1.setObject(parent);
        exp1.setSubject(child);
        Assert.assertEquals(toItem(exp1), toItem(res));

        //       <--------------   parent
        //            |
        //   ---------->           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "-1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, "1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(251));
        exp2.setEnd(new Integer(300));
        exp2.setStartIsPartial(Boolean.FALSE);
        exp2.setEndIsPartial(Boolean.TRUE);
        exp2.setSubjectStart(new Integer(51));
        exp2.setSubjectEnd(new Integer(100));
        exp2.setStrand("-1");
        exp2.setObject(parent);
        exp2.setSubject(child);
        Assert.assertEquals(toItem(exp2), toItem(res));

        //     <--------         parent
        //      |      |
        //   -------------->     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, "-1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, "1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(51));
        exp4.setSubjectEnd(new Integer(250));
        exp4.setStrand("-1");
        exp4.setObject(parent);
        exp4.setSubject(child);
        Assert.assertEquals(toItem(exp4), toItem(res));
    }

    public void testCreateLocationChildRev() throws Exception {
        BioEntity parent = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int parentId = 101;
        parent.setId(new Integer(parentId));
        BioEntity child = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int childId = 102;
        child.setId(new Integer(childId));

        //  ------------------>   parent
        //      |        |
        //     <----------        child
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "1");
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, "-1");
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(51));
        exp3.setEnd(new Integer(150));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand("-1");
        exp3.setObject(parent);
        exp3.setSubject(child);
        Assert.assertEquals(toItem(exp3), toItem(res));

        //   ------------>       parent
        //          |
        //         <---------    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, "-1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(151));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.FALSE);
        exp1.setEndIsPartial(Boolean.TRUE);
        exp1.setSubjectStart(new Integer(51));
        exp1.setSubjectEnd(new Integer(250));
        exp1.setStrand("-1");
        exp1.setObject(parent);
        exp1.setSubject(child);
        Assert.assertEquals(toItem(exp1), toItem(res));

        //       -------------->   parent
        //            |
        //   <---------           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, "-1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(1));
        exp2.setEnd(new Integer(50));
        exp2.setStartIsPartial(Boolean.TRUE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setSubjectStart(new Integer(1));
        exp2.setSubjectEnd(new Integer(50));
        exp2.setStrand("-1");
        exp2.setObject(parent);
        exp2.setSubject(child);
        Assert.assertEquals(toItem(exp2), toItem(res));

        //      -------->        parent
        //      |      |
        //   <--------------     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, "1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, "-1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(101));
        exp4.setSubjectEnd(new Integer(300));
        exp4.setStrand("-1");
        exp4.setObject(parent);
        exp4.setSubject(child);
        Assert.assertEquals(toItem(exp4), toItem(res));
    }


    public void testCreateLocationBothRev() throws Exception {
        BioEntity parent = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int parentId = 101;
        parent.setId(new Integer(parentId));
        BioEntity child = (BioEntity) DynamicUtil.createObject(Collections.singleton(BioEntity.class));
        int childId = 102;
        child.setId(new Integer(childId));

        //  <------------------   parent
        //      |        |
        //     <----------        child
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parentOnChr= cl.new SimpleLoc(parentId, childId,101, 500, "-1");
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, "-1");
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(251));
        exp3.setEnd(new Integer(350));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand("1");
        exp3.setObject(parent);
        exp3.setSubject(child);
        Assert.assertEquals(toItem(exp3), toItem(res));

        //   <------------       parent
        //          |
        //         <---------    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 350, "-1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, "-1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(1));
        exp1.setEnd(new Integer(100));
        exp1.setStartIsPartial(Boolean.TRUE);
        exp1.setEndIsPartial(Boolean.FALSE);
        exp1.setSubjectStart(new Integer(151));
        exp1.setSubjectEnd(new Integer(250));
        exp1.setStrand("1");
        exp1.setObject(parent);
        exp1.setSubject(child);
        Assert.assertEquals(toItem(exp1), toItem(res));

        //       <--------------  parent
        //            |
        //   <---------           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, "-1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, "-1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(251));
        exp2.setEnd(new Integer(300));
        exp2.setStartIsPartial(Boolean.FALSE);
        exp2.setEndIsPartial(Boolean.TRUE);
        exp2.setSubjectStart(new Integer(1));
        exp2.setSubjectEnd(new Integer(50));
        exp2.setStrand("1");
        exp2.setObject(parent);
        exp2.setSubject(child);
        Assert.assertEquals(toItem(exp2), toItem(res));

        //     <--------         parent
        //      |      |
        //   <--------------     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, "-1");
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 550, "-1");
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(251));
        exp4.setSubjectEnd(new Integer(450));
        exp4.setStrand("1");
        exp4.setObject(parent);
        exp4.setSubject(child);
        Assert.assertEquals(toItem(exp4), toItem(res));
    }

    public void testCreateTransformedLocations() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome()}));
        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        sc.setId(new Integer(104));
        c.setId(new Integer(105));
        Location scOnChr = createLocation(getChromosome(), sc, "1", 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, "1", 101, 350, Location.class);
        toStore.add(sc);
        toStore.add(c);
        toStore.add(scOnChr);
        toStore.add(contigOnSc);

        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);

        cl.createTransformedLocations(Supercontig.class, Chromosome.class, Contig.class);

        // test contig location on chromosome
        Location expected = createLocation(getChromosome(), c, "1", 1301, 1550, Location.class);
        expected.setId(new Integer(0));
        Item expItem = itemFactory.makeItem(expected);
        Results results = PostProcessUtil.findLocationAndObjects(osw.getObjectStore(),
                                                               Chromosome.class,
                                                               Contig.class, true);
        Iterator chrContigIter = results.iterator();
        Location result = (Location) ((ResultsRow) chrContigIter.next()).get(2);
        Item resItem = itemFactory.makeItem(result);
        resItem.setIdentifier("0");
        Assert.assertEquals(expItem, resItem);
    }


// method removed - should test some other way
//     public void testUpdateCollections() throws Exception {
//         // Chromosome and ChromosomeBand in db with empty subjects/objects collections
//         Chromosome chr = getChromosome();
//         ChromosomeBand band = getChromosomeBand();
//         Set toStore = new HashSet(Arrays.asList(new Object[] {chr, band}));
//         Iterator i = toStore.iterator();
//         while (i.hasNext()) {
//             osw.store((InterMineObject) i.next());
//         }

//         // store updated objects
//         Location loc = getBandOnChr();
//         CalculateLocations cl = new CalculateLocations(osw);
//         toStore = cl.updateCollections(chr, band, loc);;
//         i = toStore.iterator();
//         while (i.hasNext()) {
//             osw.store((InterMineObject) i.next());
//         }

//         ObjectStore os = osw.getObjectStore();
//         ChromosomeBand expBand = getChromosomeBand();
//         expBand.setObjects(new ArrayList(Collections.singleton(loc)));
//         expBand.setId(new Integer(0));
//         Item expItem2 = itemFactory.makeItem(expBand);
//         Query q2 = new Query();
//         QueryClass qc2 = new QueryClass(ChromosomeBand.class);
//         q2.addToSelect(qc2);
//         q2.addFrom(qc2);
//         SingletonResults sr2 = os.executeSingleton(q2);
//         ChromosomeBand result2 = (ChromosomeBand) sr2.iterator().next();
//         Item resItem2 = itemFactory.makeItem(result2);
//         resItem2.setIdentifier("0");
//         assertEquals(expItem2, resItem2);

//         Chromosome expChr = getChromosome();
//         expChr.setSubjects(new ArrayList(Collections.singleton(loc)));
//         expChr.setId(new Integer(0));
//         Item expItem1 = itemFactory.makeItem(expChr);
//         Query q = new Query();
//         QueryClass qc = new QueryClass(Chromosome.class);
//         q.addToSelect(qc);
//         q.addFrom(qc);
//         SingletonResults sr = os.executeSingleton(q);
//         Chromosome result = (Chromosome) sr.iterator().next();
//         Item resItem = itemFactory.makeItem(result);
//         resItem.setIdentifier("0");
//         assertEquals(expItem1, resItem);

// //         osw.flushObjectById();
// //         os.flushObjectById();

// //         ChromosomeBand expBand = getChromosomeBand();
// //         expBand.setObjects(new ArrayList(Collections.singleton(loc)));
// //         expBand.setId(new Integer(0));
// //         Item expItem2 = itemFactory.makeItem(expBand);
// //         Query q2 = new Query();
// //         QueryClass qc2 = new QueryClass(ChromosomeBand.class);
// //         q2.addToSelect(qc2);
// //         q2.addFrom(qc2);
// //         SingletonResults sr2 = os.executeSingleton(q2);
// //         ChromosomeBand result2 = (ChromosomeBand) sr2.iterator().next();
// //         Item resItem2 = itemFactory.makeItem(result2);
// //         resItem2.setIdentifier("0");
// //         assertEquals(expItem2, resItem2);
//    }

    private Location createLocation(BioEntity object, BioEntity subject, String strand,
                                    int start, int end, Class locationClass) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(locationClass));
        loc.setObject(object);
        loc.setSubject(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStartIsPartial(Boolean.FALSE);
        loc.setEndIsPartial(Boolean.FALSE);
        loc.setStrand(strand);
        return loc;
    }



    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = itemFactory.makeItem(o);
        item.setIdentifier("0");
        return item;
    }

    private Chromosome getChromosome() {
        if (chromosome == null) {
            chromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
            chromosome.setIdentifier("X");
            chromosome.setLength(new Integer(10000));
            chromosome.setId(new Integer(101));
        }
        return chromosome;
    }

    private ChromosomeBand getChromosomeBand() {
        if (band == null) {
            band = (ChromosomeBand) DynamicUtil.createObject(Collections.singleton(ChromosomeBand.class));
            band.setChromosome(getChromosome());
            band.setStain("blue");
            band.setId(new Integer(102));
        }
        return band;
    }

    private Location getBandOnChr() {
        if (bandOnChr == null) {
            bandOnChr = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
            bandOnChr.setObject(getChromosome());
            bandOnChr.setSubject(getChromosomeBand());
            bandOnChr.setStrand("0");
            bandOnChr.setStart(new Integer(1001));
            bandOnChr.setEnd(new Integer(2000));
            bandOnChr.setId(new Integer(103));
        }
        return bandOnChr;
    }
}
