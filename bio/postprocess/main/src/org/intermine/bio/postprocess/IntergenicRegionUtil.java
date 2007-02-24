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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.IntergenicRegion;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Synonym;

/**
 * Methods for creating feature for intergenic regions.
 * @author Kim Rutherford
 */
public class IntergenicRegionUtil
{
    private ObjectStoreWriter osw = null;
    private ObjectStore os;
    private DataSet dataSet;
    private DataSource dataSource;

    /**
     * Create a new IntergenicRegionUtil object that will operate on the given ObjectStoreWriter.
     * @param osw the ObjectStoreWriter to use when creating/changing objects
     */
    public IntergenicRegionUtil(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
        dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
        dataSource.setName("FlyMine");
        try {
            dataSource = (DataSource) os.getObjectByExample(dataSource,
                                                            Collections.singleton("name"));
        } catch (ObjectStoreException e) {
            throw new RuntimeException("unable to fetch FlyMine DataSource object", e);
        }
    }

    /**
     * Create IntergenicRegion objects
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    public void createIntergenicRegionFeatures() throws ObjectStoreException {
        Results results =
            PostProcessUtil.findLocationAndObjects(os, Chromosome.class, Gene.class, false);
        results.setBatchSize(500);

        dataSet = (DataSet) DynamicUtil.createObject(Collections.singleton(DataSet.class));
        dataSet.setTitle("FlyMine intergenic regions");
        dataSet.setDescription("Intergenic regions created by FlyMine");
        dataSet.setVersion("" + new Date()); // current time and date
        dataSet.setUrl("http://www.flymine.org");
        dataSet.setDataSource(dataSource);

        Iterator resIter = results.iterator();

        Integer previousChrId = null;
        Set locationSet = new HashSet();
        Map locToGeneMap = new HashMap();

        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer chrId = (Integer) rr.get(0);
            Gene gene = (Gene) rr.get(1);
            Location loc = (Location) rr.get(2);

            if (previousChrId != null && !chrId.equals(previousChrId)) {
                Iterator irIter = createIntergenicRegionFeatures(locationSet, locToGeneMap,
                                                                 previousChrId);
                storeItergenicRegions(osw, irIter);
                locationSet = new HashSet();
                locToGeneMap = new HashMap();
            }

            addToLocToGeneMap(locToGeneMap, loc, gene);

            locationSet.add(loc);
            previousChrId = chrId;
        }

        if (previousChrId != null) {
            Iterator irIter = createIntergenicRegionFeatures(locationSet, locToGeneMap,
                                                             previousChrId);
            storeItergenicRegions(osw, irIter);

            // we've created some IntergenicRegion objects so store() the DataSet
            osw.store(dataSet);
        }
        osw.commitTransaction();
    }

    /**
     * Add a value to a Map from keys to List of values, creating the value list as needed.
     * @param map the Map
     * @param key the key
     * @param value the value
     */
    protected static void addToListMap(Map map, Object key, Object value) {
        List valuesList = (List) map.get(key);
        if (valuesList == null) {
            valuesList = new ArrayList();
            map.put(key, valuesList);
        }
        valuesList.add(value);
    }

    private void addToLocToGeneMap(Map locToGeneMap, Location loc, Gene gene) {
        addToListMap(locToGeneMap, loc.getStart(), gene);
        addToListMap(locToGeneMap, loc.getEnd(), gene);
    }

    /**
     * Store the objects returned by createIntergenicRegionFeatures().
     */
    private void storeItergenicRegions(ObjectStoreWriter objectStoreWriter, Iterator irIter)
        throws ObjectStoreException {
        while (irIter.hasNext()) {
            IntergenicRegion ir = (IntergenicRegion) irIter.next();
            objectStoreWriter.store(ir);
            objectStoreWriter.store(ir.getChromosomeLocation());
            objectStoreWriter.store((InterMineObject) ir.getSynonyms().iterator().next());
            Set adjacentGenes = ir.getAdjacentGenes();
            Iterator adjacentGenesIter = adjacentGenes.iterator();
            while (adjacentGenesIter.hasNext()) {
                objectStoreWriter.store((InterMineObject) adjacentGenesIter.next());
            }
        }
    }

    /**
     * Return an iterator over a Set of IntergenicRegion objects that don't overlap the Locations
     * in the locationSet argument.  The caller must call ObjectStoreWriter.store() on the
     * IntergenicRegion, its chromosomeLocation and the synonym in the synonyms collection.
     * @param locationSet a set of Locations for the Genes on a particular chromosome
     * @param locToGeneMap a Map from Location.start to Gene and Location.end to Gene, used by this
     * method to find the next and previous Genes for setting the IntergenicRegion.upstreamGene and
     * downstreamGene fields
     * @param chrId the ID of the Chromosome that the Locations refer to
     * @return an Iterator over IntergenicRegion objects
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    protected Iterator createIntergenicRegionFeatures(Set locationSet, final Map locToGeneMap,
                                                      Integer chrId)
        throws ObjectStoreException {
        final Chromosome chr = (Chromosome) os.getObjectById(chrId);

        // do nothing if chromosome has no length set
        if (chr.getLength() == null) {
            return new HashSet().iterator();
        }
        final BitSet bs = new BitSet(chr.getLength().intValue() + 1);

        Iterator locationIter = locationSet.iterator();

        while (locationIter.hasNext()) {
            Location location = (Location) locationIter.next();
            bs.set(location.getStart().intValue(), location.getEnd().intValue() + 1);
        }

        return new Iterator() {
            int prevEndPos = 0;

            {
                if (bs.nextClearBit(prevEndPos) == -1) {
                    prevEndPos = -1;
                }
            }

            public boolean hasNext() {
                return prevEndPos != -1;
            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                int nextIntergenicStart = bs.nextClearBit(prevEndPos + 1);
                int intergenicEnd;
                int nextSetBit = bs.nextSetBit(nextIntergenicStart);

                if (nextSetBit == -1) {
                    intergenicEnd = chr.getLength().intValue();
                } else {
                    intergenicEnd = nextSetBit - 1;
                }

                if (nextSetBit == -1
                    || bs.nextClearBit(nextSetBit) > chr.getLength().intValue()) {
                    prevEndPos = -1;
                } else {
                    prevEndPos = intergenicEnd;
                }

                int newLocStart = nextIntergenicStart;
                int newLocEnd = intergenicEnd;

                IntergenicRegion intergenicRegion = (IntergenicRegion)
                    DynamicUtil.createObject(Collections.singleton(IntergenicRegion.class));
                Location location =
                    (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
                Synonym synonym =
                    (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
                location.setStart(new Integer(newLocStart));
                location.setEnd(new Integer(newLocEnd));
                location.setStrand(new Integer(1));
                location.setPhase(new Integer(0));
                location.setStartIsPartial(Boolean.FALSE);
                location.setEndIsPartial(Boolean.FALSE);
                location.setSubject(intergenicRegion);
                location.setObject(chr);
                location.addEvidence(dataSet);
                intergenicRegion.setChromosomeLocation(location);
                intergenicRegion.setChromosome(chr);
                intergenicRegion.setOrganism(chr.getOrganism());
                intergenicRegion.addSynonyms(synonym);
                intergenicRegion.addEvidence(dataSet);
                synonym.addEvidence(dataSet);
                synonym.setSource(dataSource);
                synonym.setSubject(intergenicRegion);
                synonym.setType("identifier");
                int length = location.getEnd().intValue() - location.getStart().intValue() + 1;
                intergenicRegion.setLength(new Integer(length));

                String identifier = "intergenic_region_chr" + chr.getIdentifier()
                    + "_" + location.getStart() + ".." + location.getEnd();
                intergenicRegion.setIdentifier(identifier);

                Set adjacentGenes = new HashSet();

                List nextGenes = (List) locToGeneMap.get(new Integer(newLocEnd + 1));
                if (nextGenes != null) {
                    Iterator nextGenesIter = nextGenes.iterator();

                    while (nextGenesIter.hasNext()) {
                        Gene nextGene = (Gene) nextGenesIter.next();
                        Integer strand = nextGene.getChromosomeLocation().getStrand();
                        if (strand != null) {
                            if (strand.intValue() == 1) {
                                nextGene.setUpstreamIntergenicRegion(intergenicRegion);
                            } else {
                                nextGene.setDownstreamIntergenicRegion(intergenicRegion);
                            }
                        }
                        adjacentGenes.add(nextGene);
                    }
                }

                List prevGenes = (List) locToGeneMap.get(new Integer(newLocStart - 1));
                if (prevGenes != null) {
                    Iterator prevGenesIter = prevGenes.iterator();

                    while (prevGenesIter.hasNext()) {
                        Gene prevGene = (Gene) prevGenesIter.next();
                        Integer strand = prevGene.getChromosomeLocation().getStrand();
                        if (strand != null) {
                            if (strand.intValue() == 1) {
                                prevGene.setDownstreamIntergenicRegion(intergenicRegion);
                            } else {
                                prevGene.setUpstreamIntergenicRegion(intergenicRegion);
                            }
                        }
                        adjacentGenes.add(prevGene);
                    }
                }

                synonym.setValue(intergenicRegion.getIdentifier());

                intergenicRegion.setAdjacentGenes(adjacentGenes);

                return intergenicRegion;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove() not implemented");
            }
        };
    }

}
