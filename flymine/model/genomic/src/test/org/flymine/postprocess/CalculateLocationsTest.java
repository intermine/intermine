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
    private Chromosome chr = null;
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


    public void testSupercontigToChromosome() throws Exception {
        Set toStore = new HashSet(Arrays.asList(new Object[] {getChromosome(), getChromosomeBand(), getBandOnChr()}));
        Supercontig sc = (Supercontig) DynamicUtil.createObject(Collections.singleton(Supercontig.class));
        sc.setId(new Integer(104));
        Location loc = createLocation(getChromosome(), sc, 0, 1201, 1600);
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

        Location expected = createLocation(getChromosomeBand(), sc, 0, 201, 600);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Location result = (Location) ((ResultsRow) cl.findLocations(ChromosomeBand.class, Supercontig.class).next()).get(2);
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
        Location scOnChr = createLocation(getChromosome(), sc, 0, 1201, 1600);
        Location contigOnSc = createLocation(sc, c, 0, 101, 350);
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
        Location expected = createLocation(getChromosome(), c, 0, 1301, 1550);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Location result = (Location) ((ResultsRow) cl.findLocations(Chromosome.class, Contig.class).next()).get(2);
        Item resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);

        // test contig location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), c, 0, 301, 550);
        expected.setId(new Integer(0));
        expItem = FullRenderer.toItem(expected, model);
        result = (Location) ((ResultsRow) cl.findLocations(ChromosomeBand.class, Contig.class).next()).get(2);
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
        Location scOnChr = createLocation(getChromosome(), sc, 0, 1201, 1600);
        Location contigOnSc = createLocation(sc, c, 0, 101, 350);
        Location exonOnContig = createLocation(c, e, 0, 51, 150);
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
        Location expected = createLocation(getChromosome(), e, 0, 1351, 1450);
        expected.setId(new Integer(0));
        Item expItem = FullRenderer.toItem(expected, model);
        Location result = (Location) ((ResultsRow) cl.findLocations(Chromosome.class, Exon.class).next()).get(2);
        Item resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);

        // test Exon location on Supercontig
        expected = createLocation(sc, e, 0, 151, 250);
        expected.setId(new Integer(0));
        expItem = FullRenderer.toItem(expected, model);
        result = (Location) ((ResultsRow) cl.findLocations(Supercontig.class, Exon.class).next()).get(2);
        resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);

        // test Exon location on ChromosomeBand
        expected = createLocation(getChromosomeBand(), e, 0, 351, 450);
        expected.setId(new Integer(0));
        expItem = FullRenderer.toItem(expected, model);
        result = (Location) ((ResultsRow) cl.findLocations(ChromosomeBand.class, Exon.class).next()).get(2);
        resItem = FullRenderer.toItem(result, model);
        resItem.setIdentifier("0");
        assertEquals(expItem, resItem);
    }


    public void testCloneInterMineObject() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        Chromosome chr = getChromosome();
        Chromosome newChr = (Chromosome)  cl.cloneInterMineObject(chr);

        assertEquals(FullRenderer.toItem(chr, model), FullRenderer.toItem(newChr, model));
    }


    public void testOverlap() throws Exception {
        CalculateLocations cl = new CalculateLocations(osw);
        CalculateLocations.SimpleLoc parent = cl.new SimpleLoc(101, 102, 101, 200, 1);

        //   ------------>       parent
        //          |
        //          --------->   child
        CalculateLocations.SimpleLoc s1 = cl.new SimpleLoc(101, 103, 151, 250, 1);
        assertTrue(cl.overlap(s1, parent));
        assertTrue(cl.overlap(parent, s1));

        //       -------------->   parent
        //            |
        //   ---------->           child
        CalculateLocations.SimpleLoc s2 = cl.new SimpleLoc(101, 103, 51, 150, 1);
        assertTrue(cl.overlap(s2, parent));
        assertTrue(cl.overlap(parent, s2));

        //  ------------------>   parent
        //      |        |
        //      ---------->       child
        CalculateLocations.SimpleLoc s3 = cl.new SimpleLoc(101, 103, 126, 175, 1);
        assertTrue(cl.overlap(s3, parent));
        assertTrue(cl.overlap(parent, s3));

        //      -------->        parent
        //      |      |
        //   -------------->     child
        CalculateLocations.SimpleLoc s4 = cl.new SimpleLoc(101, 103, 51, 250, 1);
        assertTrue(cl.overlap(s4, parent));
        assertTrue(cl.overlap(parent, s4));

        // ------->               parent
        //
        //           ------->     child
        CalculateLocations.SimpleLoc s5 = cl.new SimpleLoc(101, 103, 251, 350, 1);
        assertFalse(cl.overlap(s5, parent));
        assertFalse(cl.overlap(parent, s5));

        //           ------->     parent
        //
        // ------->               child
        CalculateLocations.SimpleLoc s6 = cl.new SimpleLoc(101, 103, 26, 75, 1);
        assertFalse(cl.overlap(s6, parent));
        assertFalse(cl.overlap(parent, s6));
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

    private Location createLocation(BioEntity object, BioEntity subject, int strand, int start, int end) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
        loc.setObject(object);
        loc.setSubject(subject);
        loc.setStrand(new Integer(strand));
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        loc.setStartIsPartial(Boolean.FALSE);
        loc.setEndIsPartial(Boolean.FALSE);
        loc.setStrand(new Integer(1));
        return loc;
    }


//     public void testFindLocations() throws Exception {
//         insertTestData();
//         CalculateLocations cl = new CalculateLocations(osw);
//         Map locs = new HashMap();
//         Iterator iter = cl.findLocations(Chromosome.class, ChromosomeBand.class, locs);
//         ResultsRow rr = (ResultsRow) iter.next();
//         if (iter.hasNext()) {
//             fail("expected only one object to be returned");
//         }
//         //o.setId(new Integer(0));
//         //assertEquals(expected, result);
//     }

//     private void insertTestData() throws Exception {
//         InputStream is = getClass().getClassLoader().getResourceAsStream("test/CalculateLocations_src.xml");
//         XmlDataLoader dl = new XmlDataLoader(iw);
//         Source source = iw.getMainSource("genomic-test");
//         Source skelSource = iw.getSkeletonSource("genomic-test");
//         dl.processXml(is, source, skelSource);
//     }


    private Item toItem(InterMineObject o) {
        if (o.getId() == null) {
            o.setId(new Integer(0));
        }
        Item item = FullRenderer.toItem(o, model);
        item.setIdentifier("0");
        return item;
    }

    private Chromosome getChromosome() {
        if (chr == null) {
            chr = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
            chr.setName("X");
            chr.setLength(new Integer(10000));
            chr.setId(new Integer(101));
        }
        return chr;
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
