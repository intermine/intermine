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
    Map<String, ProbeHolder> holders;

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
        //Map<String, Item> probes = new HashMap<String, Item>();
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

            String chromosomeRefId = createChromosome(chromosomeIdentifier);
            Item gene = createGene(fbgn, delayedItems);
            if (gene != null) {
                Item transcript = createTranscript(transcriptIdentifier, gene.getIdentifier(),
                                                   delayedItems);
                ProbeHolder holder = getHolder(probesetIdentifier, transcript.getIdentifier(),
                                               gene.getIdentifier(), chromosomeRefId, strand);
                try {
                    Integer start = new Integer(startString);
                    Integer end = new Integer(endString);
                    if (holder.start.intValue() > start.intValue() || holder.end.intValue() == -1) {
                        holder.start = start;
                    }
                    if (holder.end.intValue() < end.intValue() || holder.end.intValue() == -1) {
                        holder.end = end;
                    }
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
        probeSet.setReference("organism", org.getIdentifier());
        //probeSet.setReference("chromosome", holder.chromosomeRefID);
        //probeSet.setReference("chromosomeLocation", );
        createLocation(holder.chromosomeRefID,
                       probeSet.getIdentifier(),
                       holder.start, holder.end,
                       holder.strand);
        probeSet.addToCollection("dataSets", dataSet);

        probeSet.setCollection("transcripts", holder.transcripts);

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
        protected String chromosomeRefID;
        protected String probesetIdentifier;
        protected Integer start = new Integer(-1);
        protected Integer end = new Integer(-1);
        protected List<String> genes = new ArrayList<String>();
        protected List<String> transcripts = new ArrayList<String>();
        protected String strand;

        /**
         * @param identifier probeset identifier
         * @param transcriptRefId id representing a transcript object
         * @param geneRefId id representing a gene object
         * @param chromosomeRefId id representing a chromosome object
         * @param strand strand, eg -1 or 1
         */
        ProbeHolder(String identifier, String transcriptRefId, String geneRefId,
                    String chromosomeRefId, String strand) {
            probesetIdentifier = identifier;
            transcripts.add(transcriptRefId);
            genes.add(geneRefId);
            this.chromosomeRefID = chromosomeRefId;
            this.strand = strand;
        }
    }

    /**
     * Holds information about the probeset until all probes have been processed and we know the
     * start and end
     * @author Julie Sullivan
     */
    public class ChromosomeLocation
    {
        protected String chromosomeRefID;
        protected Integer start = new Integer(-1);
        protected Integer end = new Integer(-1);
        protected String strand;

        /**
         * @param identifier probeset identifier
         * @param transcriptRefId id representing a transcript object
         * @param geneRefId id representing a gene object
         * @param chromosomeRefId id representing a chromosome object
         * @param strand strand, eg -1 or 1
         */
        ChromosomeLocation(String identifier, String transcriptRefId, String geneRefId,
                    String chromosomeRefId, String strand) {
            this.chromosomeRefID = chromosomeRefId;
            this.strand = strand;
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

    private String createLocation(String chromosome, String probeset, Integer start, Integer end,
                                  String strand)
    throws ObjectStoreException {
//        String refId = chromosomes.get(identifier);
//        if (refId == null) {
            Item item = createItem("Location");
            item.setAttribute("start", start.toString());
            item.setAttribute("end", end.toString());
            item.setAttribute("strand", strand);
            item.setReference("object", chromosome);
            item.setReference("subject", probeset);
            item.addToCollection("dataSets", dataSet);
            //chromosomes.put(identifier, refId);
            store(item);
        //}
        return item.getIdentifier();
    }

    private ProbeHolder getHolder(String identifier, String transcriptRefId, String geneRefId,
                                  String chromosomeRefId, String strand) {
        ProbeHolder holder = holders.get(identifier);
        if (holder == null) {
            holder = new ProbeHolder(identifier, transcriptRefId, geneRefId, chromosomeRefId,
                                     strand);
            holders.put(identifier, holder);
        }
        return holder;
    }
}

