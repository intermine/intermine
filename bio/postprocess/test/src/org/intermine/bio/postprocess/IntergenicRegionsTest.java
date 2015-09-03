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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.collections.IteratorUtils;
import org.intermine.metadata.Util;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.IntergenicRegion;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.Organism;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the IntergenicRegionUtil class.
 * @author Kim Rutherford
 */
public class IntergenicRegionsTest extends TestCase
{
    private ObjectStoreWriter osw;
    private Organism organism = null;
    private DataSource dataSource;

    public IntergenicRegionsTest(String arg) {
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

    public void testCreateIntergenicRegionFeatures() throws Exception {
        IntergenicRegionUtil iru = new IntergenicRegionUtil(osw);

        List chrXgeneLocList =  new ArrayList();
        Map chrXlocMap = new HashMap();
        Integer chrXId = createChrX(chrXgeneLocList, chrXlocMap, 1000);
        Iterator irIter = iru.createIntergenicRegionFeatures(new HashSet(chrXgeneLocList),
                                                             chrXlocMap, chrXId);

        {
            Set intergenicRegions = new HashSet(IteratorUtils.toList(irIter));

            assertEquals(5, intergenicRegions.size());
        }

        List chr1geneLocList =  new ArrayList();
        Map chr1locMap = new HashMap();
        Integer chr1Id = createChr1(chr1geneLocList, chr1locMap, 2000);
        irIter = iru.createIntergenicRegionFeatures(new HashSet(chr1geneLocList), chr1locMap,
                                                    chr1Id);

        {
            Set intergenicRegions = new HashSet(IteratorUtils.toList(irIter));

            assertEquals(3, intergenicRegions.size());
        }
    }

    public void testCreateIntergenicRegionFeaturesRefs() throws Exception {
        IntergenicRegionUtil iru = new IntergenicRegionUtil(osw);

        List chrXgeneLocList =  new ArrayList();
        Map chrXlocMap = new HashMap();
        createChrX(chrXgeneLocList, chrXlocMap, 3000);
        List chr1geneLocList =  new ArrayList();
        Map chr1locMap = new HashMap();
        Integer chr1Id = createChr1(chr1geneLocList, chr1locMap, 4000);

        iru.createIntergenicRegionFeatures();

        ObjectStore os = osw.getObjectStore();
        os.flushObjectById();

        Query q = new Query();

        QueryClass qc = new QueryClass(IntergenicRegion.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = os.executeSingleton(q);
        Iterator resIter = res.iterator();

        {
            Set intergenicRegions = new HashSet(IteratorUtils.toList(resIter));

            Iterator irIter = intergenicRegions.iterator();

            Set actualIdentifiers = new HashSet();

            while(irIter.hasNext()) {
                IntergenicRegion ir = (IntergenicRegion) irIter.next();

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

                int locStart = loc.getStart().intValue();
                if (locStart > 0) {
                    Integer newLoc = new Integer(locStart - 1);
                    Collection prevGeneIds;
                    if (ir.getChromosome().getId().equals(chr1Id)) {
                        prevGeneIds = getByLoc(newLoc, chr1locMap);
                    } else {
                        prevGeneIds = getByLoc(newLoc, chrXlocMap);
                    }
                    Iterator prevGeneIdsIter = prevGeneIds.iterator();

                    while (prevGeneIdsIter.hasNext()) {
                        Gene prevGene = (Gene) os.getObjectById((Integer) prevGeneIdsIter.next());

                        assertTrue(prevGene.getUpstreamIntergenicRegion() != null
                               || prevGene.getDownstreamIntergenicRegion() != null);

                        Set adjacentGenes = new HashSet(ir.getAdjacentGenes());
                        assertTrue(adjacentGenes.contains(prevGene));
                        if ("1".equals(loc.getStrand())) {
                            IntergenicRegion nextIntergenicRegion =
                                prevGene.getDownstreamIntergenicRegion();
                            Integer id = nextIntergenicRegion.getId();
                            assertEquals(id, ir.getId());
                        } else {
                            assertEquals(prevGene.getUpstreamIntergenicRegion().getId(), ir.getId());
                        }
                    }
                }

                int locEnd = loc.getEnd().intValue();
                if (locEnd < ir.getChromosome().getLength().intValue()) {
                    Integer newLoc = new Integer(locEnd + 1);
                    Collection nextGeneIds;
                   if (ir.getChromosome().getId().equals(chr1Id)) {
                     nextGeneIds = getByLoc(newLoc, chr1locMap);
                   } else {
                        nextGeneIds = getByLoc(newLoc, chrXlocMap);
                   }
                    assertTrue(nextGeneIds.size() > 0);
                    Iterator nextGeneIdsIter = nextGeneIds.iterator();

                    while (nextGeneIdsIter.hasNext()) {
                        Gene nextGene = (Gene) os.getObjectById((Integer) nextGeneIdsIter.next());

                        if ("1".equals(loc.getStrand())) {
                            assertTrue(ir.getAdjacentGenes().contains(nextGene));
                            assertEquals(nextGene.getUpstreamIntergenicRegion().getId(), ir.getId());
                        } else {
                            assertTrue(ir.getAdjacentGenes().contains(nextGene));
                            assertEquals(nextGene.getDownstreamIntergenicRegion().getId(), ir.getId());
                        }
                    }
                }
                actualIdentifiers.add(ir.getPrimaryIdentifier());
            }

            Set expectedIdentifiers =
                new HashSet(Arrays.asList(new Object[] {
                    "intergenic_region_chrX_1..100",
                    "intergenic_region_chrX_201..300",
                    "intergenic_region_chrX_401..500",
                    "intergenic_region_chrX_601..700",
                    "intergenic_region_chrX_951..1000",
                    "intergenic_region_chrI_101..300",
                    "intergenic_region_chrI_401..500",
                    "intergenic_region_chrI_901..1800",
                }));

            assertEquals(expectedIdentifiers, actualIdentifiers);
        }
    }

    private Collection getByLoc(Integer newLoc, Map chrlocMap) {
        Set chrGeneList = (Set) chrlocMap.get(newLoc);
        if (chrGeneList == null) {
            chrGeneList = new HashSet();
        }
        List retList = new ArrayList();
        Iterator iter = chrGeneList.iterator();
        while (iter.hasNext()) {
            retList.add(((Gene) iter.next()).getId());
        }
        // return IDs that will be looked up in the on disk objectstore rather than using the
        // Genes created by createChrX() and createChr1(), which have null IDs
        return retList;
    }

    private Integer createChrX(List geneLocList, Map chrXlocMap, int idStart) throws ObjectStoreException {
        Chromosome chr = DynamicUtil.createObject(Chromosome.class);
        chr.setPrimaryIdentifier("X");
        chr.setLength(new Integer(1000));
        chr.setId(new Integer(101));
        chr.setOrganism(organism);

        Set toStore = new HashSet();

        toStore.add(chr);

        int [][] geneInfo = {
            { 0, 101, 200 },
            { 1, 301, 400 },
            { 2, 501, 600 },
            { 3, 701, 900 },
            { 4, 801, 950 },
        };

        Gene[] genes = new Gene[geneInfo.length];
        Location[] geneLocs = new Location[geneInfo.length];

        for (int i = 0; i < genes.length; i++) {
            genes[i] = DynamicUtil.createObject(Gene.class);
            int geneId = geneInfo[i][0] + idStart;
            int start = geneInfo[i][1];
            int end = geneInfo[i][2];
            genes[i].setId(new Integer(geneId));
            genes[i].setLength(new Integer(end - start + 1));
            genes[i].setChromosome(chr);
            geneLocs[i] = createLocation(chr, genes[i], "1", start, end, Location.class);
            geneLocs[i].setId(new Integer(100 + geneId));
            genes[i].setChromosomeLocation(geneLocs[i]);
            Util.addToSetMap(chrXlocMap, geneLocs[i].getStart(), genes[i]);
            Util.addToSetMap(chrXlocMap, geneLocs[i].getEnd(), genes[i]);
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

    private Integer createChr1(List geneLocList, Map chr1locMap, int idStart) throws ObjectStoreException {
        Chromosome chr = DynamicUtil.createObject(Chromosome.class);
        chr.setPrimaryIdentifier("I");
        chr.setLength(new Integer(2000));
        chr.setId(new Integer(102));
        chr.setOrganism(organism);

        Set toStore = new HashSet();

        toStore.add(chr);

        int [][] geneInfo = {
            // test special case - gene starts at first base of chromosome
            { 0, 1, 100 },
            // test creating two genes with the same start and/or end base
            { 1, 301, 400 },
            { 2, 301, 400 },
            { 3, 501, 800 },
            { 4, 701, 900 },
            // test special case - gene ends at last base of chromosome
            { 5, 1801, 2000 },
        };

        Gene[] genes = new Gene[geneInfo.length];
        Location[] geneLocs = new Location[geneInfo.length];

        for (int i = 0; i < genes.length; i++) {
            genes[i] = DynamicUtil.createObject(Gene.class);
            int geneId = geneInfo[i][0] + idStart;
            int start = geneInfo[i][1];
            int end = geneInfo[i][2];
            genes[i].setId(new Integer(geneId));
            genes[i].setLength(new Integer(end - start + 1));
            genes[i].setChromosome(chr);
            geneLocs[i] = createLocation(chr, genes[i], "1", start, end, Location.class);
            geneLocs[i].setId(new Integer(100 + geneId));
            genes[i].setChromosomeLocation(geneLocs[i]);
            Util.addToSetMap(chr1locMap, geneLocs[i].getStart(), genes[i]);
            Util.addToSetMap(chr1locMap, geneLocs[i].getEnd(), genes[i]);
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

    private Location createLocation(BioEntity object, BioEntity subject, String strand,
                                    int start, int end, Class<Location> locationClass) {
        Location loc = DynamicUtil.createObject(locationClass);
        loc.setLocatedOn(object);
        loc.setFeature(subject);
        loc.setStrand(strand);
        loc.setStart(new Integer(start));
        loc.setEnd(new Integer(end));
        return loc;
    }
}
