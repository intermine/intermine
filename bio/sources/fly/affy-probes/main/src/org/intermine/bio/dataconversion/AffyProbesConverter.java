package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 *
 * @author Julie Sullivan
 */
public class AffyProbesConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(AffyProbesConverter.class);

    protected String dataSource, dataSet;
    private String orgRefId;
    protected Map<String, String> bioentities = new HashMap<String, String>();
    private static final String TAXON_FLY = "7227";
    private static final String DATASET_PREFIX = "Affymetrix array: ";
    private Map<String, String> chromosomes = new HashMap<String, String>();
    private Map<String, ProbeSetHolder> holders = new HashMap<String, ProbeSetHolder>();
    private List<Item> delayedItems = new LinkedList<Item>();
    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the data model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public AffyProbesConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model, null, null);

        dataSource = getDataSource("Ensembl");
        orgRefId = getOrganism(TAXON_FLY);
    }

    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    @Override
    public void process(Reader reader)
        throws Exception {
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);

        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            dataSet = getDataSet(DATASET_PREFIX + line[0], dataSource);

            String probesetIdentifier = line[1];
            String transcriptIdentifier = line[2];
            String fbgn = line[3];
//            String chromosomeIdentifier = line[4];
//            String startString = line[5];
//            String endString = line[6];
//            String strand = line[7];
//
//            String chromosomeRefId = createChromosome(chromosomeIdentifier);
            String geneRefId = createGene(fbgn);
            if (geneRefId != null) {
                String transcriptRefId = createBioentity("Transcript", transcriptIdentifier,
                                                         geneRefId);
                ProbeSetHolder holder = getHolder(probesetIdentifier);
                holder.transcripts.add(transcriptRefId);
                holder.genes.add(geneRefId);
                holder.datasets.add(dataSet);
//                try {
//                    Integer start = new Integer(startString);
//                    Integer end = new Integer(endString);
//                    holder.addLocation(chromosomeRefId, start, end, strand);
//                } catch (NumberFormatException e) {
//                    LOG.error("bad start/end values " + startString + " and " + endString);
//                }
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        for (ProbeSetHolder holder : holders.values()) {
            storeProbeSet(holder);
        }

        for (Item item : delayedItems) {
            store(item);
        }
    }

    private void storeProbeSet(ProbeSetHolder holder)
        throws ObjectStoreException  {
        Item probeSet = createItem("ProbeSet");
        probeSet.setAttribute("primaryIdentifier", holder.probesetIdentifier);
        probeSet.setAttribute("name", holder.probesetIdentifier);
        probeSet.setReference("organism", orgRefId);
        probeSet.setCollection("dataSets", holder.datasets);
        probeSet.setCollection("transcripts", holder.transcripts);
        probeSet.setCollection("genes", holder.genes);
//        probeSet.setCollection("locations", holder.createLocations(probeSet.getIdentifier(),
//                holder.datasets));
        store(probeSet);
    }

    /**
     * Holds information about the probeset until all probes have been processed and we know the
     * start and end
     * @author Julie Sullivan
     */
    public class ProbeSetHolder
    {
        protected String probesetIdentifier;
        protected List<String> genes = new ArrayList<String>();
        private List<String> locations = new ArrayList<String>();
        protected List<String> transcripts = new ArrayList<String>();
        protected Map<String, LocationHolder> locationHolders
            = new HashMap<String, LocationHolder>();
        protected List<String> datasets = new ArrayList<String>();

        /**
         * @param identifier probeset identifier
         */
        public ProbeSetHolder(String identifier) {
            probesetIdentifier = identifier;
        }

        /**
         * @param chromosomeRefId id representing a chromosome object
         * @param start start of location
         * @param end end of location
         * @param strand strand, eg -1 or 1
         */
        protected void addLocation(String chromosomeRefId, Integer start, Integer end,
                                   String strand) {
            String key = chromosomeRefId + "|" + start.toString() + "|"
                + end.toString() + "|" + strand;
            if (locationHolders.get(key) == null) {
                LocationHolder location = new LocationHolder(chromosomeRefId, start, end, strand);
                locationHolders.put(key, location);
            }
        }

        /**
         * when all of the probes for this probeset have been processed, create and store all
         * related locations
         * @param probeSetRefId id representing probeset object
         * @param dataSets list of IDs reresenting dataset objects
         * @return reference list of location objects
         * @throws ObjectStoreException if something goes wrong storing locations
         */
        protected List<String> createLocations(String probeSetRefId, List<String> dataSets)
            throws ObjectStoreException {
            for (LocationHolder holder : locationHolders.values()) {
                locations.add(createLocation(holder, probeSetRefId, dataSets));
            }
            return locations;
        }
    }

    /**
     * holds information about a location
     */
    public class LocationHolder
    {
        protected Integer start = new Integer(-1);
        protected Integer end = new Integer(-1);
        protected String strand;
        protected String chromosomeRefID;

        /**
         * @param chromosomeRefId id representing a chromosome object
         * @param start start of location
         * @param end end of location
         * @param strand strand, eg -1 or 1
         */
        public LocationHolder(String chromosomeRefId, Integer start, Integer end, String strand) {
            this.chromosomeRefID = chromosomeRefId;
            this.start = start;
            this.end = end;
            this.strand = strand;
        }
    }

    private String createGene(String id)
        throws ObjectStoreException {
        if (rslv == null || !rslv.hasTaxon(TAXON_FLY)) {
            return null;
        }
        String identifier = id;
        int resCount = rslv.countResolutions(TAXON_FLY, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + identifier + " count: " + resCount + " FBgn: "
                     + rslv.resolveId(TAXON_FLY, identifier));
            return null;
        }
        identifier = rslv.resolveId(TAXON_FLY, identifier).iterator().next();
        return createBioentity("Gene", identifier, null);
    }

    private String createBioentity(String type, String identifier, String geneRefId)
        throws ObjectStoreException {
        String refId = bioentities.get(identifier);
        if (refId == null) {
            Item bioentity = createItem(type);
            bioentity.setAttribute("primaryIdentifier", identifier);
            bioentity.setReference("organism", orgRefId);
            if ("Transcript".equals(type)) {
                bioentity.setReference("gene", geneRefId);
            }
            bioentity.addToCollection("dataSets", dataSet);
            refId = bioentity.getIdentifier();
            store(bioentity);
            bioentities.put(identifier, refId);
        }
        return refId;
    }

    private String createChromosome(String identifier)
        throws ObjectStoreException {
        String refId = chromosomes.get(identifier);
        if (refId == null) {
            Item item = createItem("Chromosome");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", orgRefId);
            chromosomes.put(identifier, item.getIdentifier());
            store(item);
            refId = item.getIdentifier();
        }
        return refId;
    }

    private String createLocation(LocationHolder holder, String probeset, List<String> dataSets) {
        String strand = null;
        if (holder.strand != null) {
            strand = holder.strand;
        } else {
            LOG.warn("probeset " + probeset + " has no strand");
        }
        Item location = makeLocation(holder.chromosomeRefID, probeset, holder.start.toString(),
                holder.end.toString(), strand, false);
        location.setCollection("dataSets", dataSets);
        delayedItems.add(location);
        return location.getIdentifier();
    }

    private ProbeSetHolder getHolder(String identifier) {
        ProbeSetHolder holder = holders.get(identifier);
        if (holder == null) {
            holder = new ProbeSetHolder(identifier);
            holders.put(identifier, holder);
        }
        return holder;
    }
}

