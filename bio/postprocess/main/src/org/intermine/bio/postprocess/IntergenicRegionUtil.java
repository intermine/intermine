package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.intermine.bio.util.BioQueries;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
import org.intermine.model.bio.Chromosome;
import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.DataSource;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Location;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.util.Util;

/**
 * Methods for creating feature for intergenic regions.
 *
 * @author Kim Rutherford
 */
public class IntergenicRegionUtil
{
    private ObjectStoreWriter osw = null;
    private ObjectStore os;
    private DataSet dataSet;
    private DataSource dataSource;
    private Model model;
//    private static final Logger LOG = Logger.getLogger(IntergenicRegionUtil.class);

    /**
     * Create a new IntergenicRegionUtil object that will operate on the given
     * ObjectStoreWriter.
     *
     * @param osw the ObjectStoreWriter to use when creating/changing objects
     */
    public IntergenicRegionUtil(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
        this.model = os.getModel();
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
     *
     * @throws ObjectStoreException if there is an ObjectStore problem
     * @throws IllegalAccessException if a field is missing from the model
     */
    public void createIntergenicRegionFeatures()
        throws ObjectStoreException, IllegalAccessException {

        try {
            String message = "Not performing IntergenicRegionUtil.createIntergenicRegionFeatures ";
            PostProcessUtil.checkFieldExists(model, "IntergenicRegion", "adjacentGenes", message);
            PostProcessUtil.checkFieldExists(model, "Gene", "upstreamIntergenicRegion", message);
            PostProcessUtil.checkFieldExists(model, "Gene", "downstreamIntergenicRegion", message);
        } catch (MetaDataException e) {
            return;
        }

        Results results = BioQueries.findLocationAndObjects(os, Chromosome.class, Gene.class, false,
                false, false, 1000);
        dataSet = (DataSet) DynamicUtil.createObject(Collections.singleton(DataSet.class));
        dataSet.setName("FlyMine intergenic regions");
        dataSet.setDescription("Intergenic regions created by FlyMine");
        dataSet.setVersion("" + new Date()); // current time and date
        dataSet.setUrl("http://www.flymine.org");
        dataSet.setDataSource(dataSource);

        Iterator<?> resIter = results.iterator();

        Integer previousChrId = null;
        Set<Location> locationSet = new HashSet<Location>();
        Map<Location, Set<Gene>> locToGeneMap = new HashMap<Location, Set<Gene>>();

        osw.beginTransaction();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            Integer chrId = (Integer) rr.get(0);
            Gene gene = (Gene) rr.get(1);
            Location loc = (Location) rr.get(2);

            if (previousChrId != null && !chrId.equals(previousChrId)) {
                Iterator<?> irIter = createIntergenicRegionFeatures(locationSet,
                        locToGeneMap, previousChrId);
                storeItergenicRegions(osw, irIter);
                locationSet = new HashSet<Location>();
                locToGeneMap = new HashMap<Location, Set<Gene>>();
            }

            addToLocToGeneMap(locToGeneMap, loc, gene);

            locationSet.add(loc);
            previousChrId = chrId;
        }

        if (previousChrId != null) {
            Iterator<?> irIter = createIntergenicRegionFeatures(locationSet, locToGeneMap,
                    previousChrId);
            storeItergenicRegions(osw, irIter);

            // we've created some IntergenicRegion objects so store() the DataSet
            osw.store(dataSet);
        }
        osw.commitTransaction();
    }

    private void addToLocToGeneMap(Map<Location, Set<Gene>> locToGeneMap, Location loc, Gene gene) {
        Util.addToSetMap(locToGeneMap, loc.getStart(), gene);
        Util.addToSetMap(locToGeneMap, loc.getEnd(), gene);
    }

    /**
     * Store the objects returned by createIntergenicRegionFeatures().
     */
    private void storeItergenicRegions(ObjectStoreWriter objectStoreWriter,
            Iterator<?> irIter) throws ObjectStoreException, IllegalAccessException {
        while (irIter.hasNext()) {
            SequenceFeature ir = (SequenceFeature) irIter.next();
            objectStoreWriter.store(ir);
            objectStoreWriter.store(ir.getChromosomeLocation());
            Set<Gene> adjacentGenes = (Set<Gene>) ir.getFieldValue("adjacentGenes");
            Iterator<?> adjacentGenesIter = adjacentGenes.iterator();
            while (adjacentGenesIter.hasNext()) {
                objectStoreWriter.store(adjacentGenesIter.next());
            }
        }
    }

    /**
     * Return an iterator over a Set of IntergenicRegion objects that don't
     * overlap the Locations in the locationSet argument. The caller must call
     * ObjectStoreWriter.store() on the IntergenicRegion, its chromosomeLocation.  Doesn't create
     * synonyms anymore.
     *
     * @param locationSet a set of Locations for the Genes on a particular chromosome
     * @param locToGeneMap a Map from Location.start to Gene and Location.end to Gene,
     *            used by this method to find the next and previous Genes for
     *            setting the IntergenicRegion.upstreamGene and downstreamGene
     *            fields
     * @param chrId the ID of the Chromosome that the Locations refer to
     * @return an Iterator over IntergenicRegion objects
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    protected Iterator<?> createIntergenicRegionFeatures(Set<Location> locationSet,
            final Map<Location, Set<Gene>> locToGeneMap, Integer chrId)
        throws ObjectStoreException {
        final Chromosome chr = (Chromosome) os.getObjectById(chrId);

        // do nothing if chromosome has no length set
        if (chr.getLength() == null) {
            return new HashSet<Location>().iterator();
        }
        final BitSet bs = new BitSet(chr.getLength().intValue() + 1);

        Iterator<?> locationIter = locationSet.iterator();

        while (locationIter.hasNext()) {
            Location location = (Location) locationIter.next();
            bs.set(location.getStart().intValue(), location.getEnd().intValue() + 1);
        }

        return new Iterator<Object>() {
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

                if (nextSetBit == -1 || bs.nextClearBit(nextSetBit) > chr.getLength().intValue()) {
                    prevEndPos = -1;
                } else {
                    prevEndPos = intergenicEnd;
                }

                int newLocStart = nextIntergenicStart;
                int newLocEnd = intergenicEnd;

                Class<? extends FastPathObject> igCls =
                    os.getModel().getClassDescriptorByName("IntergenicRegion").getType();
                SequenceFeature intergenicRegion = (SequenceFeature) DynamicUtil
                .createObject(Collections.singleton(igCls));
                Location location = (Location) DynamicUtil.createObject(
                        Collections.singleton(Location.class));
                location.setStart(new Integer(newLocStart));
                location.setEnd(new Integer(newLocEnd));
                location.setStrand("1");

                location.setFeature(intergenicRegion);
                location.setLocatedOn(chr);
                location.addDataSets(dataSet);
                intergenicRegion.setChromosomeLocation(location);
                intergenicRegion.setChromosome(chr);
                intergenicRegion.setOrganism(chr.getOrganism());
                intergenicRegion.addDataSets(dataSet);

                int length = location.getEnd().intValue() - location.getStart().intValue() + 1;
                intergenicRegion.setLength(new Integer(length));

                String primaryIdentifier = "intergenic_region_chr"
                        + chr.getPrimaryIdentifier() + "_"
                        + location.getStart() + ".." + location.getEnd();
                intergenicRegion.setPrimaryIdentifier(primaryIdentifier);

                Set<Gene> adjacentGenes = new HashSet<Gene>();

                Set<Gene> nextGenes = locToGeneMap.get(new Integer(newLocEnd + 1));
                if (nextGenes != null) {
                    Iterator<?> nextGenesIter = nextGenes.iterator();

                    while (nextGenesIter.hasNext()) {
                        Gene nextGene = (Gene) nextGenesIter.next();
                        String strand = null;
                        if (nextGene.getChromosomeLocation() != null) {
                            strand = nextGene.getChromosomeLocation().getStrand();
                        }
                        if (strand != null) {
                            if ("1".equals(strand)) {
                                nextGene.setFieldValue("upstreamIntergenicRegion",
                                        intergenicRegion);
                            } else {
                                nextGene.setFieldValue("downstreamIntergenicRegion",
                                        intergenicRegion);
                            }
                        }
                        adjacentGenes.add(nextGene);
                    }
                }

                Set<Gene> prevGenes = locToGeneMap.get(new Integer(newLocStart - 1));
                if (prevGenes != null) {
                    Iterator<?> prevGenesIter = prevGenes.iterator();

                    while (prevGenesIter.hasNext()) {
                        Gene prevGene = (Gene) prevGenesIter.next();
                        String strand = null;
                        if (prevGene.getChromosomeLocation() != null) {
                            strand = prevGene.getChromosomeLocation().getStrand();
                        }
                        if (strand != null) {
                            if ("1".equals(strand)) {
                                prevGene.setFieldValue("downstreamIntergenicRegion",
                                        intergenicRegion);
                            } else {
                                prevGene.setFieldValue("upstreamIntergenicRegion",
                                        intergenicRegion);
                            }
                        }
                        adjacentGenes.add(prevGene);
                    }
                }
                intergenicRegion.setFieldValue("adjacentGenes", adjacentGenes);
                return intergenicRegion;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove() not implemented");
            }
        };
    }

}
