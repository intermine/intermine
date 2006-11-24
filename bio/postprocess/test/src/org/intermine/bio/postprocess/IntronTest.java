package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.dataloader.IntegrationWriter;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.BioEntity;
import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Transcript;
import org.flymine.model.genomic.Exon;
import org.flymine.model.genomic.Intron;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Organism;
import org.flymine.model.genomic.Synonym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.collection.CompositeCollection;

import junit.framework.TestCase;

/**
 * Tests for the IntronUtil class.
 * @author Wenyan Ji
 */
public class IntronTest extends TestCase{

    private ObjectStoreWriter osw;
    private IntegrationWriter iw;
    private Organism organism = null;
    private DataSource dataSource;

    private HashSet toStore = new HashSet();
    private HashSet locationSet1 = new HashSet();
    private HashSet locationSet2 = new HashSet();
    private HashSet locationSet3 = new HashSet();

    public IntronTest(String arg) {
        super(arg);

        organism = (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
        dataSource.setName("FlyMine");
    }

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        osw.store(organism);
        osw.store(dataSource);
    }

    public void tearDown() throws Exception {
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
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
        osw.close();
    }

    public void XtestCreateIntronFeatures() throws Exception {
        IntronUtil iru = new IntronUtil(osw);

        Transcript t1 = createTranscriptT1(100);
        int is1 = iru.createIntronFeatures(locationSet1, t1, t1.getChromosomeLocation());
        assertEquals(2, is1);

        Transcript t2 = createTranscriptT2(100);
        int is2 = iru.createIntronFeatures(locationSet2, t2, t2.getChromosomeLocation());
        assertEquals(3, is2);

        Transcript t3 = createTranscriptT3(100);
        int is3 = iru.createIntronFeatures(locationSet3, t3, t3.getChromosomeLocation());
        assertEquals(0, is3);

    }

    public void testCreateIntronFeaturesRef() throws Exception {
        IntronUtil iru = new IntronUtil(osw);

        Transcript t1 = createTranscriptT1(100);
        Transcript t2 = createTranscriptT2(100);

        iru.createIntronFeatures();

        ObjectStore os = osw.getObjectStore();
        os.flushObjectById();

        Query q = new Query();

        QueryClass qc = new QueryClass(Intron.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        Iterator resIter = res.iterator();


        Set introns = new HashSet(IteratorUtils.toList(resIter));

        Iterator irIter = introns.iterator();

        Set actualIdentifiers = new HashSet();

        while(irIter.hasNext()) {
            Intron ir = (Intron) irIter.next();

            assertNotNull(ir.getChromosome());
            assertNotNull(ir.getOrganism());
            assertNotNull(ir.getLength());
            System.out.println("length " + ir.getLength().intValue());
            assertTrue(ir.getLength().intValue() > 0);
            assertEquals(1, ir.getEvidence().size());

            Location loc = ir.getChromosomeLocation();
            assertNotNull(loc);
            assertNotNull(loc.getStart());
            assertNotNull(loc.getEnd());
            assertNotNull(loc.getStrand());
            assertNotNull(loc.getPhase());
            assertNotNull(loc.getStartIsPartial());
            assertNotNull(loc.getEndIsPartial());
            assertEquals(1, loc.getEvidence().size());


            assertEquals(1, ir.getSynonyms().size());
            Synonym synonym = (Synonym) ir.getSynonyms().iterator().next();
            assertEquals(ir.getIdentifier(), synonym.getValue());
            assertEquals("identifier", synonym.getType());
            assertEquals("FlyMine", synonym.getSource().getName());

            actualIdentifiers.add(ir.getIdentifier());
        }

        Set expectedIdentifiers =
            new HashSet(Arrays.asList(new Object[] {
                    "intron_chrX_201..300",
                    "intron_chrX_501..600",
                    "intron_chrX_501..700",
            }));

        assertEquals(expectedIdentifiers, actualIdentifiers);

    }


    private Transcript createTranscriptT1(int idStart) throws ObjectStoreException {
        Set toStore = new HashSet();
        locationSet1 = new HashSet();

        Chromosome chr = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setIdentifier("X");
        chr.setLength(new Integer(10000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);
        toStore.add(chr);

        Transcript t1 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        t1.setLength(new Integer(1000));
        t1.setId(new Integer(10));
        t1.setIdentifier("ENST00000306601");
        t1.setOrganism(organism);
        t1.setExonCount(new Integer(3));
        t1.setChromosome(chr);
        Location location = createLocation(chr, t1, 1, 1, 1000);
        t1.setChromosomeLocation(location);
        toStore.add(location);
        toStore.add(t1);

        int[][] exonInfo = {
            {20, 1, 200},
            {21, 301, 500},
            {22, 601, 1000},
        };

        Exon[] exons = new Exon[exonInfo.length];
        Location[] exonLocs = new Location[exonInfo.length];
        for (int i = 0; i < exons.length; i++) {
            exons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            int exonId = exonInfo[i][0] + idStart;
            int start = exonInfo[i][1];
            int end = exonInfo[i][2];
            exons[i].setId(new Integer(exonId));
            exons[i].setLength(new Integer(end - start + 1));
            exons[i].setChromosome(chr);
            exonLocs[i] = createLocation(chr, exons[i], 1, start, end);
            exonLocs[i].setId(new Integer(exonId + 100));
            exons[i].setChromosomeLocation(exonLocs[i]);
            // System.out.println("exonLocs[" + i + "]" + exonLocs[i]);
            locationSet1.add(exonLocs[i]);
        }
        t1.setExons(new HashSet(Arrays.asList(exons)));
        toStore.addAll(Arrays.asList(exons));
        toStore.addAll(Arrays.asList(exonLocs));

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }

        return t1;

    }



    private Transcript createTranscriptT2(int idStart) throws ObjectStoreException {
        Set toStore = new HashSet();
        locationSet2 = new HashSet();

        Chromosome chr = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setIdentifier("X");
        chr.setLength(new Integer(10000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);
        toStore.add(chr);

        Transcript t2 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        t2.setLength(new Integer(1050));
        t2.setId(new Integer(11));
        t2.setIdentifier("ENST00000306610");
        t2.setOrganism(organism);
        t2.setExonCount(new Integer(3));
        t2.setChromosome(chr);
        Location location = createLocation(chr, t2, 1, 1, 1150);
        t2.setChromosomeLocation(location);
        toStore.add(location);
        toStore.add(t2);

        int[][] exonInfo = {
            {20, 1, 200},
            {21, 301, 500},
            {23, 701, 1150},
        };

        Exon[] exons = new Exon[exonInfo.length];
        Location[] exonLocs = new Location[exonInfo.length];
        for (int i = 0; i < exons.length; i++) {
            exons[i] = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
            int exonId = exonInfo[i][0] + idStart;
            int start = exonInfo[i][1];
            int end = exonInfo[i][2];
            exons[i].setId(new Integer(exonId));
            exons[i].setLength(new Integer(end - start + 1));
            exons[i].setChromosome(chr);
            exonLocs[i] = createLocation(chr, exons[i], 1, start, end);
            exonLocs[i].setId(new Integer(exonId + 100));
            //System.out.println("exonLocs[" + i + "]" + exonLocs[i]);
            exons[i].setChromosomeLocation(exonLocs[i]);
            locationSet2.add(exonLocs[i]);
        }
        t2.setExons(new HashSet(Arrays.asList(exons)));
        toStore.addAll(Arrays.asList(exons));
        toStore.addAll(Arrays.asList(exonLocs));

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }

        return t2;
    }
    /**
     * special test case for which only one exon in the transcript,
     * then no intron is created in this case
     */

    private Transcript createTranscriptT3(int idStart) throws ObjectStoreException {
        Set toStore = new HashSet();
        locationSet3 = new HashSet();

        Chromosome chr = (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setIdentifier("X");
        chr.setLength(new Integer(10000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);
        toStore.add(chr);

        Transcript t3 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        t3.setLength(new Integer(1000));
        t3.setId(new Integer(10));
        t3.setIdentifier("ENST00000306001");
        t3.setOrganism(organism);
        t3.setExonCount(new Integer(1));
        t3.setChromosome(chr);
        Location location = createLocation(chr, t3, 1, 1, 1000);
        t3.setChromosomeLocation(location);
        toStore.add(location);
        toStore.add(t3);

        Exon exon = (Exon) DynamicUtil.createObject(Collections.singleton(Exon.class));
        exon.setLength(new Integer(1000));
        exon.setId(new Integer(140));
        exon.setOrganism(organism);
        exon.setChromosome(chr);
        Location exonLoc = createLocation(chr, exon, 1, 1, 1000);
        exon.setChromosomeLocation(exonLoc);
        toStore.add(location);
        toStore.add(exon);

        locationSet3.add(exonLoc);


        toStore.add(exon);
        toStore.add(exonLoc);

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }

        return t3;
    }

    private Location createLocation(BioEntity object, BioEntity subject, int strand,
                                    int start, int end) {
        Location loc = (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
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
}
