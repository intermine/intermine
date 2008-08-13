package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;

/**
 *
 * @author Julie Sullivan
 */
public class Drosophila2ProbeConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(Drosophila2ProbeConverter.class);

    protected Item dataSource, dataSet, org;
    protected Map<String, Item> bioentities = new HashMap<String, Item>();
    protected IdResolverFactory resolverFactory;
    private static final String TAXON_ID = "7227";
    private Map<String, Item> synonyms = new HashMap<String, Item>();
    private Map<String, String> chromosomes = new HashMap<String, String>();
    private Map<String, ProbeHolder> holders;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the data model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public Drosophila2ProbeConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model);

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Affymetrix");
        store(dataSource);

        org = createItem("Organism");
        org.setAttribute("taxonId", TAXON_ID);
        store(org);

        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory();
    }


    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        List<Item> delayedItems = new ArrayList<Item>();
        holders = new HashMap<String, ProbeHolder>();
        boolean hasDataset = false;

        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            if (!hasDataset) {
                createDataset(line[0]);
                hasDataset = true;
            }

            String probesetIdentifier = line[1];
            String transcriptIdentifier = line[2];
            String fbgn = line[3];
            String chromosomeIdentifier = line[4];
            String startString = line[5];
            String endString = line[6];
            String strand = line[7];
            String hitCount = line[8];
            String[] hitStrings = hitCount.split(" ");
            Integer hits = new Integer(0);
            try {
                hits = new Integer(hitStrings[3]);
            } catch (Exception e) {
                // don't process this data
            }
            String chromosomeRefId = createChromosome(chromosomeIdentifier);
            Item gene = createGene(fbgn, delayedItems);
            if (gene != null) {
                Item transcript = createTranscript(transcriptIdentifier, gene.getIdentifier(),
                                                   delayedItems);
                ProbeHolder holder = getHolder(probesetIdentifier, transcript.getIdentifier(),
                                               gene.getIdentifier(), hits);
                try {
                    Integer start = new Integer(startString);
                    Integer end = new Integer(endString);
                    holder.setLocation(chromosomeRefId, start, end, strand);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("bad start/end values");
                }
            }
        }
        for (ProbeHolder holder : holders.values()) {
            storeProbeSet(holder, delayedItems);
        }

        for (Item item : delayedItems) {
            store(item);
        }
    }

    private void storeProbeSet(ProbeHolder holder, List<Item> delayedItems)
    throws ObjectStoreException  {
        Item probeSet = createItem("ProbeSet");
        probeSet.setAttribute("primaryIdentifier", holder.probesetIdentifier);
        probeSet.setAttribute("name", holder.probesetIdentifier);
        probeSet.setAttribute("hits", holder.hits.toString());
        probeSet.setReference("organism", org.getIdentifier());
        probeSet.addToCollection("dataSets", dataSet);
        probeSet.setCollection("transcripts", holder.transcripts);
        probeSet.setCollection("locations", holder.createLocations(probeSet.getIdentifier()));
        probeSet.setCollection("genes", holder.genes);
        createSynonym(probeSet.getIdentifier(), "identifier", holder.probesetIdentifier,
                      delayedItems);

        store(probeSet);
    }

    /**
     * Holds information about the probeset until all probes have been processed and we know the
     * start and end
     * @author Julie Sullivan
     */
    public class ProbeHolder
    {
        protected String probesetIdentifier;
        protected Integer hits;
        protected List<String> genes = new ArrayList<String>();
        protected List<String> transcripts = new ArrayList<String>();
        private List<String> locations = new ArrayList<String>();
        protected Map<String, LocationHolder> locationHolders = new HashMap();

        /**
         * @param identifier probeset identifier
         * @param transcriptRefId id representing a transcript object
         * @param geneRefId id representing a gene object
         * @param hits number of hits, 13 or 14
         */
        public ProbeHolder(String identifier, String transcriptRefId, String geneRefId,
                           Integer hits) {
            probesetIdentifier = identifier;
            transcripts.add(transcriptRefId);
            this.hits = hits;
            genes.add(geneRefId);
        }

        /**
         * @param chromosomeRefId id representing a chromosome object
         * @param start start of location
         * @param end end of location
         * @param strand strand, eg -1 or 1
         */
        protected void setLocation(String chromosomeRefId, Integer start, Integer end,
                                   String strand) {
            LocationHolder location = locationHolders.get(chromosomeRefId);
            if (location == null) {
                location = new LocationHolder(chromosomeRefId, start, end, strand);
                locationHolders.put(chromosomeRefId, location);
            } else {
                location.setLocation(start, end, strand);
            }
        }

        /**
         * when all of the probes for this probeset have been processed, create and store all
         * related locations
         * @param probeSetRefId id representing probeset object
         * @return reference list of location objects
         * @throws ObjectStoreException if something goes wrong storing locations
         */
        protected List<String> createLocations(String probeSetRefId)
        throws ObjectStoreException {
            for (LocationHolder holder : locationHolders.values()) {
                locations.add(createLocation(holder, probeSetRefId));
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

        /**
         * set the location for these coordinates.  some probes are located on multiple chromosomes
         * so we have to keep track of several locations at once.
         * @param start start location
         * @param end end location
         * @param strand strand, eg -1
         */
        protected void setLocation(Integer start, Integer end, String strand) {
            if (this.start.intValue() == -1 && this.end.intValue() == -1) {
                if (start.intValue() != -1) {
                    this.start = start;
                }
                if (end.intValue() != -1) {
                    this.end = end;
                }
                this.strand = strand;
            } else {
                if (start.intValue() < this.start.intValue() && !start.equals(new Integer(-1))) {
                    this.start = start;
                }
                if (end.intValue() > this.end.intValue() && !end.equals(new Integer(-1))) {
                    this.end = end;
                }
            }
        }
    }


    private Item createGene(String id, List<Item> delayedItems)
    throws ObjectStoreException {
        String identifier = id;

        IdResolver resolver = resolverFactory.getIdResolver();
        int resCount = resolver.countResolutions(TAXON_ID, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + identifier + " count: " + resCount + " FBgn: "
                     + resolver.resolveId(TAXON_ID, identifier));
            return null;
        }
        identifier = resolver.resolveId(TAXON_ID, identifier).iterator().next();

        Item bioentity = bioentities.get(identifier);
        if (bioentity == null) {
            bioentity = createItem("Gene");
            bioentity.setReference("organism", org.getIdentifier());
            bioentity.setAttribute("primaryIdentifier", identifier);
            bioentity.addToCollection("dataSets", dataSet);
            bioentities.put(identifier, bioentity);
            store(bioentity);
            createSynonym(bioentity.getIdentifier(), "identifier", identifier, delayedItems);
        }
        return bioentity;
    }


    private Item createTranscript(String id, String geneRefId, List<Item> delayedItems)
    throws ObjectStoreException {
        String identifier = id;
        Item bioentity = bioentities.get(identifier);
        if (bioentity == null) {
            bioentity = createItem("Transcript");
            bioentity.setAttribute("secondaryIdentifier", identifier);
            bioentity.setReference("organism", org.getIdentifier());
            bioentity.setReference("gene", geneRefId);
            bioentity.addToCollection("dataSets", dataSet);
            bioentities.put(identifier, bioentity);
            store(bioentity);
            createSynonym(bioentity.getIdentifier(), "identifier", identifier, delayedItems);
        }
        return bioentity;
    }
    private Item createSynonym(String subjectId, String type, String value,
                               List<Item> delayedItems) {
        String key = subjectId + type + value;
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (!synonyms.containsKey(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            syn.setReference("source", dataSource.getIdentifier());
            synonyms.put(key, syn);
            delayedItems.add(syn);
            return syn;
        }
        return null;
    }

    private void createDataset(String array)
    throws ObjectStoreException  {
        dataSet = createItem("DataSet");
        dataSet.setReference("dataSource", dataSource.getIdentifier());
        dataSet.setAttribute("title", "Affymetrix array: " + array);
        store(dataSet);
    }

    private String createChromosome(String identifier)
    throws ObjectStoreException {
        String refId = chromosomes.get(identifier);
        if (refId == null) {
            Item item = createItem("Chromosome");
            item.setAttribute("primaryIdentifier", identifier);
            item.setReference("organism", org.getIdentifier());
            chromosomes.put(identifier, item.getIdentifier());
            store(item);
            refId = item.getIdentifier();
        }
        return refId;
    }

    private String createLocation(LocationHolder holder, String probeset)
    throws ObjectStoreException {
        Item item = createItem("Location");
        item.setAttribute("start", holder.start.toString());
        item.setAttribute("end", holder.end.toString());
        if (holder.strand != null) {
            item.setAttribute("strand", holder.strand);
        } else {
            LOG.error("probeset " + probeset + " has no strand");
        }
        item.setReference("object", holder.chromosomeRefID);
        item.setReference("subject", probeset);
        item.addToCollection("dataSets", dataSet);
        store(item);
        return item.getIdentifier();
    }

    private ProbeHolder getHolder(String identifier, String transcriptRefId, String geneRefId,
                                  Integer hits) {
        ProbeHolder holder = holders.get(identifier);
        if (holder == null) {
            holder = new ProbeHolder(identifier, transcriptRefId, geneRefId, hits);
            holders.put(identifier, holder);
        } else {
            if (hits.compareTo(holder.hits) > 0) {
                holder.hits = hits;
            }
            holder.genes.add(geneRefId);
        }
        return holder;
    }
}

