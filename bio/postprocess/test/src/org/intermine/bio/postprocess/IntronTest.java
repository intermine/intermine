package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.collections.IteratorUtils;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Intron;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the IntronUtil class.
 * @author Wenyan Ji
 */
public class IntronTest extends TestCase{

    private ObjectStoreWriter osw;
    private Organism organism = null;
    private DataSource dataSource;
    private HashSet locationSet1 = new HashSet();
    private HashSet locationSet2 = new HashSet();
    private HashSet locationSet3 = new HashSet();

    public IntronTest(String arg) {
        super(arg);

        organism = DynamicUtil.createObject(Organism.class);
        dataSource = DynamicUtil.createObject(DataSource.class);
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
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
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
        int is1 = iru.createIntronFeatures(locationSet1, t1, t1.getChromosomeLocation(), t1.getGene());
        assertEquals(2, is1);

        Transcript t2 = createTranscriptT2(100);
        int is2 = iru.createIntronFeatures(locationSet2, t2, t2.getChromosomeLocation(), t2.getGene());
        assertEquals(3, is2);

        Transcript t3 = createTranscriptT3(100);
        int is3 = iru.createIntronFeatures(locationSet3, t3, t3.getChromosomeLocation(), t3.getGene());
        assertEquals(0, is3);

    }

    public void testCreateIntronFeaturesRef() throws Exception {
        IntronUtil iru = new IntronUtil(osw);

        createTranscriptT1(100);
        createTranscriptT2(100);

        iru.createIntronFeatures();

        ObjectStore os = osw.getObjectStore();
        os.flushObjectById();

        Query q = new Query();

        QueryClass qc = new QueryClass(Intron.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = os.executeSingleton(q);
        Iterator resIter = res.iterator();

        Set introns = new HashSet(IteratorUtils.toList(resIter));

        Iterator irIter = introns.iterator();

        Set actualIdentifiers = new HashSet();

        while(irIter.hasNext()) {
            Intron ir = (Intron) irIter.next();

            assertNotNull(ir.getChromosome());
            assertNotNull(ir.getOrganism());
            assertNotNull(ir.getLength());

            assertTrue(ir.getLength().intValue() > 0);
            assertEquals(1, ir.getDataSets().size());

            Location loc = ir.getChromosomeLocation();
            assertNotNull(loc);
            assertNotNull(loc.getStart());
            assertNotNull(loc.getEnd());
            assertNotNull(loc.getStrand());
            assertEquals(1, loc.getDataSets().size());

            Set<Gene> genes = ir.getGenes();
            assertFalse(genes.isEmpty());
            assertEquals(1, genes.size());

//            assertEquals(1, ir.getSynonyms().size());
//            Synonym synonym = (Synonym) ir.getSynonyms().iterator().next();
//            assertEquals(ir.getPrimaryIdentifier(), synonym.getValue());
//
            actualIdentifiers.add(ir.getPrimaryIdentifier());
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
        Set<InterMineObject> toStore = new HashSet<InterMineObject>();
        locationSet1 = new HashSet();

        Chromosome chr = DynamicUtil.createObject(Chromosome.class);
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(10000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);
        toStore.add(chr);

        Gene gene = DynamicUtil.createObject(Gene.class);
        gene.setPrimaryIdentifier("FBgn00001");
        gene.setLength(new Integer(10000));
        gene.setId(new Integer(1001));
        gene.setOrganism(organism);
        toStore.add(gene);

        Transcript t1 = DynamicUtil.createObject(Transcript.class);
        t1.setLength(new Integer(1000));
        t1.setId(new Integer(10));
        t1.setPrimaryIdentifier("ENST00000306601");
        t1.setOrganism(organism);
        t1.setChromosome(chr);
        t1.setGene(gene);
        Location location = createLocation(chr, t1, "1", 1, 1000);
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
            exons[i] = DynamicUtil.createObject(Exon.class);
            int exonId = exonInfo[i][0] + idStart;
            int start = exonInfo[i][1];
            int end = exonInfo[i][2];
            exons[i].setId(new Integer(exonId));
            exons[i].setLength(new Integer(end - start + 1));
            exons[i].setChromosome(chr);
            exonLocs[i] = createLocation(chr, exons[i], "1", start, end);
            exonLocs[i].setId(new Integer(exonId + 100));
            exons[i].setChromosomeLocation(exonLocs[i]);
            // System.out.println("exonLocs[" + i + "]" + exonLocs[i]);
            locationSet1.add(exonLocs[i]);
        }
        t1.setExons(new HashSet(Arrays.asList(exons)));
        toStore.addAll(Arrays.asList(exons));
        toStore.addAll(Arrays.asList(exonLocs));

        for (InterMineObject o : toStore) {
            osw.store(o);
        }

        return t1;

    }



    private Transcript createTranscriptT2(int idStart) throws ObjectStoreException {
        Set toStore = new HashSet();
        locationSet2 = new HashSet();

        Chromosome chr = DynamicUtil.createObject(Chromosome.class);
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(10000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);
        toStore.add(chr);

        Gene gene = DynamicUtil.createObject(Gene.class);
        gene.setPrimaryIdentifier("FBgn00002");
        gene.setLength(new Integer(10000));
        gene.setId(new Integer(1002));
        gene.setOrganism(organism);
        toStore.add(gene);

        Transcript t2 = DynamicUtil.createObject(Transcript.class);
        t2.setLength(new Integer(1050));
        t2.setId(new Integer(11));
        t2.setPrimaryIdentifier("ENST00000306610");
        t2.setOrganism(organism);
        t2.setChromosome(chr);
        t2.setGene(gene);
        Location location = createLocation(chr, t2, "1", 1, 1150);
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
            exons[i] = DynamicUtil.createObject(Exon.class);
            int exonId = exonInfo[i][0] + idStart;
            int start = exonInfo[i][1];
            int end = exonInfo[i][2];
            exons[i].setId(new Integer(exonId));
            exons[i].setLength(new Integer(end - start + 1));
            exons[i].setChromosome(chr);
            exonLocs[i] = createLocation(chr, exons[i], "1", start, end);
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

        Chromosome chr = DynamicUtil.createObject(Chromosome.class);
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(10000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);
        toStore.add(chr);

        Transcript t3 = DynamicUtil.createObject(Transcript.class);
        t3.setLength(new Integer(1000));
        t3.setId(new Integer(10));
        t3.setPrimaryIdentifier("ENST00000306001");
        t3.setOrganism(organism);
        t3.setChromosome(chr);
        Location location = createLocation(chr, t3, "1", 1, 1000);
        t3.setChromosomeLocation(location);
        toStore.add(location);
        toStore.add(t3);

        Exon exon = DynamicUtil.createObject(Exon.class);
        exon.setLength(new Integer(1000));
        exon.setId(new Integer(140));
        exon.setOrganism(organism);
        exon.setChromosome(chr);
        Location exonLoc = createLocation(chr, exon, "1", 1, 1000);
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

    private Location createLocation(BioEntity object, BioEntity subject, String strand,
                                    int start, int end) {
        Location loc = DynamicUtil.createObject(Location.class);
        loc.setLocatedOn(object);
        loc.setFeature(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        return loc;
    }
}
