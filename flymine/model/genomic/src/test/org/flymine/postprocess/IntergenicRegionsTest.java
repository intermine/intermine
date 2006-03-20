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

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
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
import org.flymine.model.genomic.ChromosomeBand;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.IntergenicRegion;
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
 * Tests for the IntergenicRegionUtil class.
 * @author Kim Rutherford
 */
public class IntergenicRegionsTest extends TestCase
{
    private ObjectStoreWriter osw;
    private IntegrationWriter iw;
    private Organism organism = null;
    private DataSource dataSource;

    public IntergenicRegionsTest(String arg) {
        super(arg);

        organism = (Organism) DynamicUtil.createObject(Collections.singleton(Organism.class));
        dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
        dataSource.setName("FlyMine");
    }

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
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


    public void testCreateIntergenicRegionFeatures() throws Exception {
        IntergenicRegionUtil iru = new IntergenicRegionUtil(osw);

        List chrXgeneLocList =  new ArrayList();
        Map chrXlocMap = new HashMap();
        Integer chrXId = createChrX(chrXgeneLocList, chrXlocMap);
        Iterator irIter = iru.createIntergenicRegionFeatures(new HashSet(chrXgeneLocList),
                                                             chrXlocMap, chrXId);

        {
            Set intergenicRegions = new HashSet(IteratorUtils.toList(irIter));

            assertEquals(5, intergenicRegions.size());
        }

        List chr1geneLocList =  new ArrayList();
        Map chr1locMap = new HashMap();
        Integer chr1Id = createChr1(chr1geneLocList, chr1locMap);
        irIter = iru.createIntergenicRegionFeatures(new HashSet(chr1geneLocList), chr1locMap,
                                                    chr1Id);

        {
            Set intergenicRegions = new HashSet(IteratorUtils.toList(irIter));

            assertEquals(3, intergenicRegions.size());
        }


        iru.createIntergenicRegionFeatures();

        Query q = new Query();

        QueryClass qc = new QueryClass(IntergenicRegion.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        Iterator resIter = res.iterator();

        {
            Set intergenicRegions = new HashSet(IteratorUtils.toList(resIter));

            assertEquals(8, intergenicRegions.size());

            irIter = intergenicRegions.iterator();

            Set actualIdentifiers = new HashSet();

            while(irIter.hasNext()) {
                IntergenicRegion ir = (IntergenicRegion) irIter.next();

                assertNotNull(ir.getChromosome());
                assertNotNull(ir.getOrganism());
                assertNotNull(ir.getLength());
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

                int locStart = loc.getStart().intValue();
                if (locStart > 0) {
                    Integer newLoc = new Integer(locStart - 1);
                    List chr1List = (List) chr1locMap.get(newLoc);
                    if (chr1List == null) {
                        chr1List = Collections.EMPTY_LIST;
                    }
                    List chr2List = (List) chrXlocMap.get(newLoc);
                    if (chr2List == null) {
                        chr2List = Collections.EMPTY_LIST;
                    }
                    Collection prevGenes = new CompositeCollection(new Collection[] {
                            chr1List, chr2List                            
                    });
                    assertTrue(prevGenes.size() > 0);
                    Iterator prevGenesIter = prevGenes.iterator();
                    
                    while (prevGenesIter.hasNext()) {
                        Gene prevGene = (Gene) prevGenesIter.next();

                        if (loc.getStrand().intValue() == 1) {
                            assertTrue(ir.getAdjacentGenes().contains(prevGene));
                            assertEquals(prevGene.getDownstreamIntergenicRegion().getId(), ir.getId());
                        } else {
                            assertTrue(ir.getAdjacentGenes().contains(prevGene));
                            assertEquals(prevGene.getUpstreamIntergenicRegion().getId(), ir.getId());
                        }
                    }
                }

                int locEnd = loc.getEnd().intValue();
                if (locEnd < ir.getChromosome().getLength().intValue()) {
                    Integer newLoc = new Integer(locEnd + 1);
                    List chr1List = (List) chr1locMap.get(newLoc);
                    if (chr1List == null) {
                        chr1List = Collections.EMPTY_LIST;
                    }
                    List chr2List = (List) chrXlocMap.get(newLoc);
                    if (chr2List == null) {
                        chr2List = Collections.EMPTY_LIST;
                    }
                    Collection nextGenes = new CompositeCollection(new Collection[] {
                        chr1List, chr2List
                    });
                    assertTrue(nextGenes.size() > 0);
                    Iterator nextGenesIter = nextGenes.iterator();
                    
                    while (nextGenesIter.hasNext()) {
                        Gene nextGene = (Gene) nextGenesIter.next();

                        if (loc.getStrand().intValue() == 1) {
                            assertTrue(ir.getAdjacentGenes().contains(nextGene));
                            assertEquals(nextGene.getUpstreamIntergenicRegion().getId(), ir.getId());
                        } else {
                            assertTrue(ir.getAdjacentGenes().contains(nextGene));
                            assertEquals(nextGene.getDownstreamIntergenicRegion().getId(), ir.getId());
                        }
                    }
                }

                assertEquals(1, ir.getSynonyms().size());
                Synonym synonym = (Synonym) ir.getSynonyms().iterator().next();
                assertEquals(ir.getIdentifier(), synonym.getValue());
                assertEquals("identifier", synonym.getType());
                assertEquals("FlyMine", synonym.getSource().getName());

                actualIdentifiers.add(ir.getIdentifier());
            }

            Set expectedIdentifiers =
                new HashSet(Arrays.asList(new Object[] {
                    "integenic_region_chrX_1..100",
                    "integenic_region_chrX_201..300",
                    "integenic_region_chrX_401..500",
                    "integenic_region_chrX_601..700",
                    "integenic_region_chrX_951..1000",
                    "integenic_region_chrI_101..300",
                    "integenic_region_chrI_401..500",
                    "integenic_region_chrI_901..1800",
                }));

            assertEquals(expectedIdentifiers, actualIdentifiers);
        }
    }

    private Integer createChrX(List geneLocList, Map chrXlocMap) throws ObjectStoreException {
        Chromosome chr =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setIdentifier("X");
        chr.setLength(new Integer(1000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);

        Set toStore = new HashSet();

        toStore.add(chr);

        int [][] geneInfo = {
            { 1000, 101, 200 },
            { 1001, 301, 400 },
            { 1002, 501, 600 },
            { 1003, 701, 900 },
            { 1004, 801, 950 },
        };

        Gene[] genes = new Gene[geneInfo.length];
        Location[] geneLocs = new Location[geneInfo.length];

        for (int i = 0; i < genes.length; i++) {
            genes[i] = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
            int geneId = geneInfo[i][0];
            int start = geneInfo[i][1];
            int end = geneInfo[i][2];
            genes[i].setId(new Integer(geneId));
            genes[i].setLength(new Integer(end - start + 1));
            genes[i].setChromosome(chr);
            geneLocs[i] = createLocation(chr, genes[i], 1, start, end, Location.class);
            geneLocs[i].setId(new Integer(1000 + geneId));
            genes[i].setChromosomeLocation(geneLocs[i]);
            IntergenicRegionUtil.addToListMap(chrXlocMap, geneLocs[i].getStart(), genes[i]);
            IntergenicRegionUtil.addToListMap(chrXlocMap, geneLocs[i].getEnd(), genes[i]);
        }

        toStore.addAll(Arrays.asList(genes));
        geneLocList.addAll(Arrays.asList(geneLocs));
        toStore.addAll(geneLocList);

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }

        return chr.getId();
    }

    private Integer createChr1(List geneLocList, Map chr1locMap) throws ObjectStoreException {
        Chromosome chr =
            (Chromosome) DynamicUtil.createObject(Collections.singleton(Chromosome.class));
        chr.setIdentifier("I");
        chr.setLength(new Integer(2000));
        chr.setId(new Integer(102));
        chr.setOrganism(organism);

        Set toStore = new HashSet();

        toStore.add(chr);

        int [][] geneInfo = {
            // test special case - gene starts at first base of chromosome
            { 5000, 1, 100 },
            // test creating two genes with the same start and/or end base
            { 5001, 301, 400 },
            { 5002, 301, 400 },
            { 5003, 501, 800 },
            { 5004, 701, 900 },
            // test special case - gene ends at last base of chromosome
            { 5005, 1801, 2000 },
        };

        Gene[] genes = new Gene[geneInfo.length];
        Location[] geneLocs = new Location[geneInfo.length];

        for (int i = 0; i < genes.length; i++) {
            genes[i] = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
            int geneId = geneInfo[i][0];
            int start = geneInfo[i][1];
            int end = geneInfo[i][2];
            genes[i].setId(new Integer(geneId));
            genes[i].setLength(new Integer(end - start + 1));
            genes[i].setChromosome(chr);
            geneLocs[i] = createLocation(chr, genes[i], 1, start, end, Location.class);
            geneLocs[i].setId(new Integer(1000 + geneId));
            genes[i].setChromosomeLocation(geneLocs[i]);
            IntergenicRegionUtil.addToListMap(chr1locMap, geneLocs[i].getStart(), genes[i]);
            IntergenicRegionUtil.addToListMap(chr1locMap, geneLocs[i].getEnd(), genes[i]);
        }

        toStore.addAll(Arrays.asList(genes));
        geneLocList.addAll(Arrays.asList(geneLocs));
        toStore.addAll(geneLocList);

        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }

        return chr.getId();
    }

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
}
