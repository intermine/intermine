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
import java.util.ArrayList;
import java.io.InputStream;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.ResultsRow;
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

public class CalculateLocationsTest extends TestCase {

    private ObjectStoreWriter osw;
    private IntegrationWriter iw;
    private Chromosome chromosome = null;
    private ChromosomeBand band = null;
    private Location bandOnChr = null;
    private Model model;

    private static final Logger LOG = Logger.getLogger(CalculateLocationsTest.class);

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        model = Model.getInstanceByName("genomic");
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
        //osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.error("deleting: " +o.getId());
            osw.delete(o);
        }
        //osw.commitTransaction();
        LOG.error("committed transaction");
        osw.close();
        LOG.error("closed objectstore");
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

        Location exon1OnContig1 = createLocation(contig1, exon1, 1, 51, 250, Location.class);
        exon1OnContig1.setId(new Integer(1010));
        Location exon2OnContig1 = createLocation(contig1, exon2, 1, 701, 1000, Location.class);
        exon2OnContig1.setId(new Integer(1011));
        Location exon2OnContig2 = createLocation(contig2, exon2, 1, 1, 20, Location.class);
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

        assert((resExon1OnContig1 instanceof Location) &&
               ! (resExon1OnContig1 instanceof PartialLocation));

        Item resExon1OnContig1Item = FullRenderer.toItem(resExon1OnContig1, model);

        Location expectedResExon1OnContig1 =
            (Location) createLocation(contig1, exon1, 1, 51, 250, Location.class); 
        
        expectedResExon1OnContig1.setId(new Integer(1010));
        expectedResExon1OnContig1.setStartIsPartial(Boolean.FALSE);
        expectedResExon1OnContig1.setEndIsPartial(Boolean.FALSE);

        Item expectedResExon1OnContig1Item = FullRenderer.toItem(expectedResExon1OnContig1, model);

        assertEquals(expectedResExon1OnContig1Item, resExon1OnContig1Item);

        

        ///////////////////////////////////////

        
        PartialLocation resExon2OnContig1 = (PartialLocation) os.getObjectById(new Integer(1011));
        Item resExon2OnContig1Item = FullRenderer.toItem(resExon2OnContig1, model);

        PartialLocation expectedResExon2OnContig1 =
            (PartialLocation) createLocation(contig1, exon2, 1, 701, 1000, PartialLocation.class); 

        expectedResExon2OnContig1.setSubjectStart(new Integer(1));
        expectedResExon2OnContig1.setSubjectEnd(new Integer(300));
        expectedResExon2OnContig1.setId(new Integer(1011));
        expectedResExon2OnContig1.setStartIsPartial(Boolean.FALSE);
        expectedResExon2OnContig1.setEndIsPartial(Boolean.TRUE);

        Item expectedResExon2OnContig1Item = FullRenderer.toItem(expectedResExon2OnContig1, model);

        assertEquals(expectedResExon2OnContig1Item, resExon2OnContig1Item);

        
        /////////////////////////////////////////


        PartialLocation resExon2OnContig2 = (PartialLocation) os.getObjectById(new Integer(1012));
        Item resExon2OnContig2Item = FullRenderer.toItem(resExon2OnContig2, model);

        PartialLocation expectedResExon2OnContig2 =
            (PartialLocation) createLocation(contig2, exon2, 1, 1, 20, PartialLocation.class); 

        expectedResExon2OnContig2.setSubjectStart(new Integer(301));
        expectedResExon2OnContig2.setSubjectEnd(new Integer(320));
        expectedResExon2OnContig2.setId(new Integer(1012));
        expectedResExon2OnContig2.setStartIsPartial(Boolean.TRUE);
        expectedResExon2OnContig2.setEndIsPartial(Boolean.FALSE);

        Item expectedResExon2OnContig2Item = FullRenderer.toItem(expectedResExon2OnContig2, model);

        assertEquals(expectedResExon2OnContig2Item, resExon2OnContig2Item);
    }

    public void testSupercontigToChromosome() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {
                                                    getChromosome(), getChromosomeBand(),
                                                    getBandOnChr()
                                                }));

        Supercontig sc =
            (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));

        sc.setId(new Integer(104));
        Location loc = createLocation(getChromosome(), sc, 1, 1201, 1600, Location.class);
        toStore.add(sc);
        toStore.add(loc);

        //Source source = iw.getMainSource("genomic-test");
        //Source skelSource = iw.getSkeletonSource("genomic-test");
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        Location expected = createLocation(getChromosomeBand(), sc, 1, 201, 600, Location.class);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Iterator iter = CalculateLocations.findLocations(osw.getObjectStore(), ChromosomeBand.class,
                                         Supercontig.class);
        Location result = (Location) ((ResultsRow) iter.next()).get(2);
        Item resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);
    }

    public void testReversedSupercontigToChromosome() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Location loc = createLocation(getChromosome(), sc, -1, 1201, 1600, Location.class);
        toStore.add(sc);
        toStore.add(loc);

        //Source source = iw.getMainSource("genomic-test");
        //Source skelSource = iw.getSkeletonSource("genomic-test");
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        CalculateLocations cl = new CalculateLocations(osw);
        cl.createLocations();

        Location expected = createLocation(getChromosomeBand(), sc, -1, 201, 600, Location.class);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Iterator iter = CalculateLocations.findLocations(osw.getObjectStore(),
                                         ChromosomeBand.class, Supercontig.class);
        Location result = (Location) ((ResultsRow) iter.next()).get(2);
        Item resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");

        assertEquals(expItem, resItem);
    }

    public void testContigToSupercontig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        sc.setId(new Integer(104));
        c.setId(new Integer(105));
        Location scOnChr = createLocation(getChromosome(), sc, 1, 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, 1, 101, 350, Location.class);
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
        Location expected = createLocation(getChromosome(), c, 1, 1301, 1550, Location.class);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Iterator chrContigIter = CalculateLocations.findLocations(osw.getObjectStore(), Chromosome.class, Contig.class);
        Location result = (Location) ((ResultsRow) chrContigIter.next()).get(2);
        Item resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);

        // test contig location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), c, 1, 301, 550, Location.class);
        expected.setId(new Integer(0));
        expItem = FullRenderer.toItem(expected, model);
        Iterator chrBandContigIter =
            CalculateLocations.findLocations(osw.getObjectStore(), ChromosomeBand.class, Contig.class);
        result = (Location) ((ResultsRow) chrBandContigIter.next()).get(2);
        resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);
    }

    public void testFeatureToContig() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Contig c = (Contig) DynamicUtil.createObject(Collections.singleton(Contig.class));
        c.setId(new Integer(105));
        Exon e = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        e.setId(new Integer(106));
        Location scOnChr = createLocation(getChromosome(), sc, 1, 1201, 1600, Location.class);
        Location contigOnSc = createLocation(sc, c, 1, 101, 350, Location.class);
        Location exonOnContig = createLocation(c, e, 1, 51, 150, Location.class);
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
        Location expected = createLocation(getChromosome(), e, 1, 1351, 1450, Location.class);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Iterator chrExonIter = CalculateLocations.findLocations(osw.getObjectStore(), Chromosome.class, Exon.class);
        Location result = (Location) ((ResultsRow) chrExonIter.next()).get(2);
        Item resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);

        // test Exon location on Supercontig
        expected = createLocation(sc, e, 1, 151, 250, Location.class);
        expected.setId(new Integer(0));
        expItem = FullRenderer.toItem(expected, model);
        Iterator supercontigExonIter =
            CalculateLocations.findLocations(osw.getObjectStore(), Supercontig.class, Exon.class);
        result = (Location) ((ResultsRow) supercontigExonIter.next()).get(2);
        resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);

        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, 1, 351, 450, Location.class);
        expected.setId(new Integer(0));
        expItem = FullRenderer.toItem(expected, model);
        Iterator chrBandExonIter =
            CalculateLocations.findLocations(osw.getObjectStore(), ChromosomeBand.class, Exon.class);
        result = (Location) ((ResultsRow) chrBandExonIter.next()).get(2);
        resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);
    }


    public void testCloneInterMineObject() throws Exception {
        Chromosome chr = getChromosome();
        Chromosome newChr =
            (Chromosome) CalculateLocations.cloneInterMineObject(chr, Chromosome.class);

        assertEquals(FullRenderer.toItem(chr, model), FullRenderer.toItem(newChr, model));
    }


    public void testOverlap() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parent = cl.new SimpleLoc(101, 102, 101, 200, 1);

        //   ------------>       parent
        //          |
        //          --------->   child
        CalculateLocations.SimpleLoc s1 = cl.new SimpleLoc(101, 103, 151, 250, 1);
        assertTrue(CalculateLocations.overlap(s1, parent));
        assertTrue(CalculateLocations.overlap(parent, s1));

        //       -------------->   parent
        //            |
        //   ---------->           child
        CalculateLocations.SimpleLoc s2 = cl.new SimpleLoc(101, 103, 51, 150, 1);
        assertTrue(CalculateLocations.overlap(s2, parent));
        assertTrue(CalculateLocations.overlap(parent, s2));

        //  ------------------>   parent
        //      |        |
        //      ---------->       child
        CalculateLocations.SimpleLoc s3 = cl.new SimpleLoc(101, 103, 126, 175, 1);
        assertTrue(CalculateLocations.overlap(s3, parent));
        assertTrue(CalculateLocations.overlap(parent, s3));

        //      -------->        parent
        //      |      |
        //   -------------->     child
        CalculateLocations.SimpleLoc s4 = cl.new SimpleLoc(101, 103, 51, 250, 1);
        assertTrue(CalculateLocations.overlap(s4, parent));
        assertTrue(CalculateLocations.overlap(parent, s4));

        // ------->               parent
        //
        //           ------->     child
        CalculateLocations.SimpleLoc s5 = cl.new SimpleLoc(101, 103, 251, 350, 1);
        assertFalse(CalculateLocations.overlap(s5, parent));
        assertFalse(CalculateLocations.overlap(parent, s5));

        //           ------->     parent
        //
        // ------->               child
        CalculateLocations.SimpleLoc s6 = cl.new SimpleLoc(101, 103, 26, 75, 1);
        assertFalse(CalculateLocations.overlap(s6, parent));
        assertFalse(CalculateLocations.overlap(parent, s6));
    }


    public void testCreateChromosomeLocation() throws Exception {
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
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, 1);
        CalculateLocations.SimpleLoc childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, 1);
        Location res = cl.createChromosomeLocation(parentOnChr, childOnParent, chr, child);

        Location exp1 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp1.setStart(new Integer(251));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.FALSE);
        exp1.setEndIsPartial(Boolean.FALSE);
        exp1.setStrand(new Integer(1));
        exp1.setObject(chr);
        exp1.setSubject(child);
        assertEquals(toItem(exp1), toItem(res));

        //  <------------------   parent
        //      |        |
        //      ---------->       child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, -1);
        childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, 1);
        res = cl.createChromosomeLocation(parentOnChr, childOnParent, chr, child);

        Location exp2 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp2.setStart(new Integer(151));
        exp2.setEnd(new Integer(250));
        exp2.setStartIsPartial(Boolean.FALSE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setStrand(new Integer(1));
        exp2.setObject(chr);
        exp2.setSubject(child);
        assertEquals(toItem(exp2), toItem(res));

        //  ------------------>  parent
        //      |        |
        //     <----------       child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, 1);
        childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, -1);
        res = cl.createChromosomeLocation(parentOnChr, childOnParent, chr, child);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(251));
        exp3.setEnd(new Integer(350));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand(new Integer(-1));
        exp3.setObject(chr);
        exp3.setSubject(child);
        assertEquals(toItem(exp3), toItem(res));

        //  <-----------------   parent
        //      |        |
        //     <----------       child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(chrId, parentId, 101, 400, -1);
        childOnParent = cl.new SimpleLoc(parentId, childId, 151, 250, -1);
        res = cl.createChromosomeLocation(parentOnChr, childOnParent, chr, child);

        Location exp4 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp4.setStart(new Integer(151));
        exp4.setEnd(new Integer(250));
        exp4.setStartIsPartial(Boolean.FALSE);
        exp4.setEndIsPartial(Boolean.FALSE);
        exp4.setStrand(new Integer(-1));
        exp4.setObject(chr);
        exp4.setSubject(child);
        assertEquals(toItem(exp4), toItem(res));
    }


    public void testCreateLocationNormal() throws Exception {
        Chromosome chr = getChromosome();
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
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, 1);
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, 1);
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(51));
        exp3.setEnd(new Integer(150));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand(new Integer(1));
        exp3.setObject(parent);
        exp3.setSubject(child);
        assertEquals(toItem(exp3), toItem(res));

        //   ------------>        parent
        //          |
        //          --------->    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, 1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, 1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(151));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.FALSE);
        exp1.setEndIsPartial(Boolean.TRUE);
        exp1.setSubjectStart(new Integer(1));
        exp1.setSubjectEnd(new Integer(200));
        exp1.setStrand(new Integer(1));
        exp1.setObject(parent);
        exp1.setSubject(child);
        assertEquals(toItem(exp1), toItem(res));

        //       -------------->   parent
        //            |
        //   ---------->           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, 1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, 1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(1));
        exp2.setEnd(new Integer(50));
        exp2.setStartIsPartial(Boolean.TRUE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setSubjectStart(new Integer(51));
        exp2.setSubjectEnd(new Integer(100));
        exp2.setStrand(new Integer(1));
        exp2.setObject(parent);
        exp2.setSubject(child);
        assertEquals(toItem(exp2), toItem(res));

        //      -------->        parent
        //      |      |
        //   -------------->     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, 1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, 1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(51));
        exp4.setSubjectEnd(new Integer(250));
        exp4.setStrand(new Integer(1));
        exp4.setObject(parent);
        exp4.setSubject(child);
        assertEquals(toItem(exp4), toItem(res));
    }


    public void testCreateLocationParentReversed() throws Exception {
        Chromosome chr = getChromosome();
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
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, -1);
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, 1);
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(151));
        exp3.setEnd(new Integer(250));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand(new Integer(-1));
        exp3.setObject(parent);
        exp3.setSubject(child);
        assertEquals(toItem(exp3), toItem(res));

        //   <------------        parent
        //          |
        //          --------->    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, -1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, 1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(1));
        exp1.setEnd(new Integer(200));
        exp1.setStartIsPartial(Boolean.TRUE);
        exp1.setEndIsPartial(Boolean.FALSE);
        exp1.setSubjectStart(new Integer(1));
        exp1.setSubjectEnd(new Integer(200));
        exp1.setStrand(new Integer(-1));
        exp1.setObject(parent);
        exp1.setSubject(child);
        assertEquals(toItem(exp1), toItem(res));

        //       <--------------   parent
        //            |
        //   ---------->           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, -1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, 1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(251));
        exp2.setEnd(new Integer(300));
        exp2.setStartIsPartial(Boolean.FALSE);
        exp2.setEndIsPartial(Boolean.TRUE);
        exp2.setSubjectStart(new Integer(51));
        exp2.setSubjectEnd(new Integer(100));
        exp2.setStrand(new Integer(-1));
        exp2.setObject(parent);
        exp2.setSubject(child);
        assertEquals(toItem(exp2), toItem(res));

        //     <--------         parent
        //      |      |
        //   -------------->     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, -1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, 1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(51));
        exp4.setSubjectEnd(new Integer(250));
        exp4.setStrand(new Integer(-1));
        exp4.setObject(parent);
        exp4.setSubject(child);
        assertEquals(toItem(exp4), toItem(res));
    }

    public void testCreateLocationChildReversed() throws Exception {
        Chromosome chr = getChromosome();
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
        CalculateLocations.SimpleLoc parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, 1);
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, -1);
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(51));
        exp3.setEnd(new Integer(150));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand(new Integer(-1));
        exp3.setObject(parent);
        exp3.setSubject(child);
        assertEquals(toItem(exp3), toItem(res));

        //   ------------>       parent
        //          |
        //         <---------    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 450, 1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, -1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(151));
        exp1.setEnd(new Integer(350));
        exp1.setStartIsPartial(Boolean.FALSE);
        exp1.setEndIsPartial(Boolean.TRUE);
        exp1.setSubjectStart(new Integer(51));
        exp1.setSubjectEnd(new Integer(250));
        exp1.setStrand(new Integer(-1));
        exp1.setObject(parent);
        exp1.setSubject(child);
        assertEquals(toItem(exp1), toItem(res));

        //       -------------->   parent
        //            |
        //   <---------           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, 1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, -1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(1));
        exp2.setEnd(new Integer(50));
        exp2.setStartIsPartial(Boolean.TRUE);
        exp2.setEndIsPartial(Boolean.FALSE);
        exp2.setSubjectStart(new Integer(1));
        exp2.setSubjectEnd(new Integer(50));
        exp2.setStrand(new Integer(-1));
        exp2.setObject(parent);
        exp2.setSubject(child);
        assertEquals(toItem(exp2), toItem(res));

        //      -------->        parent
        //      |      |
        //   <--------------     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, 1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 400, -1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(101));
        exp4.setSubjectEnd(new Integer(300));
        exp4.setStrand(new Integer(-1));
        exp4.setObject(parent);
        exp4.setSubject(child);
        assertEquals(toItem(exp4), toItem(res));
    }


    public void testCreateLocationBothReversed() throws Exception {
        Chromosome chr = getChromosome();
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
        CalculateLocations.SimpleLoc parentOnChr= cl.new SimpleLoc(parentId, childId,101, 500, -1);
        CalculateLocations.SimpleLoc childOnChr = cl.new SimpleLoc(parentId, childId, 151, 250, -1);
        Location res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        Location exp3 = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        exp3.setStart(new Integer(251));
        exp3.setEnd(new Integer(350));
        exp3.setStartIsPartial(Boolean.FALSE);
        exp3.setEndIsPartial(Boolean.FALSE);
        exp3.setStrand(new Integer(1));
        exp3.setObject(parent);
        exp3.setSubject(child);
        assertEquals(toItem(exp3), toItem(res));

        //   <------------       parent
        //          |
        //         <---------    child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 350, -1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 251, 500, -1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp1 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp1.setStart(new Integer(1));
        exp1.setEnd(new Integer(100));
        exp1.setStartIsPartial(Boolean.TRUE);
        exp1.setEndIsPartial(Boolean.FALSE);
        exp1.setSubjectStart(new Integer(151));
        exp1.setSubjectEnd(new Integer(250));
        exp1.setStrand(new Integer(1));
        exp1.setObject(parent);
        exp1.setSubject(child);
        assertEquals(toItem(exp1), toItem(res));

        //       <--------------  parent
        //            |
        //   <---------           child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 400, -1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 150, -1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp2 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp2.setStart(new Integer(251));
        exp2.setEnd(new Integer(300));
        exp2.setStartIsPartial(Boolean.FALSE);
        exp2.setEndIsPartial(Boolean.TRUE);
        exp2.setSubjectStart(new Integer(1));
        exp2.setSubjectEnd(new Integer(50));
        exp2.setStrand(new Integer(1));
        exp2.setObject(parent);
        exp2.setSubject(child);
        assertEquals(toItem(exp2), toItem(res));

        //     <--------         parent
        //      |      |
        //   <--------------     child
        cl = new CalculateLocations(osw);
        parentOnChr = cl.new SimpleLoc(parentId, childId, 101, 300, -1);
        childOnChr = cl.new SimpleLoc(parentId, childId, 51, 550, -1);
        res = cl.createLocation(parent, parentOnChr, child, childOnChr);

        PartialLocation exp4 = (PartialLocation) DynamicUtil.createObject(Collections.singleton(PartialLocation.class));
        exp4.setStart(new Integer(1));
        exp4.setEnd(new Integer(200));
        exp4.setStartIsPartial(Boolean.TRUE);
        exp4.setEndIsPartial(Boolean.TRUE);
        exp4.setSubjectStart(new Integer(251));
        exp4.setSubjectEnd(new Integer(450));
        exp4.setStrand(new Integer(1));
        exp4.setObject(parent);
        exp4.setSubject(child);
        assertEquals(toItem(exp4), toItem(res));
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
//         Item expItem2 = FullRenderer.toItem(expBand, model);
//         Query q2 = new Query();
//         QueryClass qc2 = new QueryClass(ChromosomeBand.class);
//         q2.addToSelect(qc2);
//         q2.addFrom(qc2);
//         SingletonResults sr2 = new SingletonResults(q2, os, os.getSequence());
//         ChromosomeBand result2 = (ChromosomeBand) sr2.iterator().next();
//         Item resItem2 = FullRenderer.toItem(result2, model);
//         resItem2.setIdentifier("0");
//         assertEquals(expItem2, resItem2);

//         Chromosome expChr = getChromosome();
//         expChr.setSubjects(new ArrayList(Collections.singleton(loc)));
//         expChr.setId(new Integer(0));
//         Item expItem1 = FullRenderer.toItem(expChr, model);
//         Query q = new Query();
//         QueryClass qc = new QueryClass(Chromosome.class);
//         q.addToSelect(qc);
//         q.addFrom(qc);
//         SingletonResults sr = new SingletonResults(q, os, os.getSequence());
//         Chromosome result = (Chromosome) sr.iterator().next();
//         Item resItem = FullRenderer.toItem(result, model);
//         resItem.setIdentifier("0");
//         assertEquals(expItem1, resItem);

// //         osw.flushObjectById();
// //         os.flushObjectById();

// //         ChromosomeBand expBand = getChromosomeBand();
// //         expBand.setObjects(new ArrayList(Collections.singleton(loc)));
// //         expBand.setId(new Integer(0));
// //         Item expItem2 = FullRenderer.toItem(expBand, model);
// //         Query q2 = new Query();
// //         QueryClass qc2 = new QueryClass(ChromosomeBand.class);
// //         q2.addToSelect(qc2);
// //         q2.addFrom(qc2);
// //         SingletonResults sr2 = new SingletonResults(q2, os, os.getSequence());
// //         ChromosomeBand result2 = (ChromosomeBand) sr2.iterator().next();
// //         Item resItem2 = FullRenderer.toItem(result2, model);
// //         resItem2.setIdentifier("0");
// //         assertEquals(expItem2, resItem2);
//     }

    private Location createLocation(BioEntity object, BioEntity subject, int strand,
                                    int start, int end, Class locationClass) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(locationClass));
        loc.setObject(object);
        loc.setSubject(subject);
        loc.setStrand(new Integer(strand));
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStartIsPartial(Boolean.FALSE);
        loc.setEndIsPartial(Boolean.FALSE);
        loc.setStrand(new Integer(strand));
        return loc;
    }



    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = FullRenderer.toItem(o, model);
        item.setIdentifier("0");
        return item;
    }

    private Chromosome getChromosome() {
        if (chromosome == null) {
            chromosome = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
            chromosome.setName("X");
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
            bandOnChr.setStrand(new Integer(1));
            bandOnChr.setStart(new Integer(1001));
            bandOnChr.setEnd(new Integer(2000));
            bandOnChr.setId(new Integer(103));
        }
        return bandOnChr;
    }
}
