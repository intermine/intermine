package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
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
public class AffyProbeHumanConverter extends FileConverter
{
    protected static final Logger LOG = Logger.getLogger(AffyProbeHumanConverter.class);

    protected Item dataSource, dataSet, org;
    protected Map<String, String> bioentities = new HashMap();
    private static final String TAXON_ID = "9606";
    private Map<String, Item> synonyms = new HashMap<String, Item>();
    private Map<String, String> chromosomes = new HashMap<String, String>();
    private Map<String, ProbeSetHolder> holders = new HashMap();
    List<Item> delayedItems = new ArrayList<Item>();
    public IdResolver affyResolver = new IdResolver("probeset");

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the data model
     * @throws ObjectStoreException if an error occurs in storing
     */
    public AffyProbeHumanConverter(ItemWriter writer, Model model)
        throws ObjectStoreException {
        super(writer, model);


	try {
		BufferedReader in = new BufferedReader(new FileReader("/shared/data/affy-probes/human/AffyResolve.txt"));
		in.readLine();
		String readString;	
		while ((readString = in.readLine()) != null) {
			String[] tmp = readString.split("\\t");		
			if (tmp.length>=2 && !tmp[1].equals("")) 
			{
				affyResolver.addSynonyms("9606",tmp[1],new HashSet(Arrays.asList(new String[] {tmp[0]})));
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException(e);
	}

        dataSource = createItem("DataSource");
        dataSource.setAttribute("name", "Ensembl");
        store(dataSource);

        org = createItem("Organism");
        org.setAttribute("taxonId", TAXON_ID);
        store(org);

    }

    /**
     * Read each line from flat file.
     *
     * {@inheritDoc}
     */
    public void process(Reader reader)
    throws Exception {

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        boolean hasDataset = false;

        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
//            if (!hasDataset) {
//                createDataset(line[0]);
//                hasDataset = true;
//            }


		System.out.println(line);
            String probesetIdentifier = line[0];
            String transcriptIdentifier = line[2];
            String fbgn = line[1];
            String chromosomeIdentifier = line[3];
            String startString = line[4];
            String endString = line[5];
            String strand = line[6];

            String chromosomeRefId = createChromosome(chromosomeIdentifier);
            String geneRefId = createGene(fbgn);
            if (geneRefId != null) {
                String transcriptRefId = createBioentity("Transcript", transcriptIdentifier,
                                                          geneRefId);
                ProbeSetHolder holder = getHolder(probesetIdentifier);
                holder.transcripts.add(transcriptRefId);
                holder.genes.add(geneRefId);
                //holder.datasets.add(dataSet.getIdentifier());
                try {
                    Integer start = new Integer(startString);
                    Integer end = new Integer(endString);
                    holder.addLocation(chromosomeRefId, start, end, strand);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("bad start/end values");
                }
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
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
	if (affyResolver.countResolutions("9606",holder.probesetIdentifier)>0) probeSet.setAttribute("focusIdentifier",affyResolver.resolveId("9606",holder.probesetIdentifier).iterator().next());
        probeSet.setAttribute("name", holder.probesetIdentifier);
        probeSet.setReference("organism", org.getIdentifier());
        //probeSet.setCollection("dataSets", holder.datasets);
        probeSet.setCollection("transcripts", holder.transcripts);
        probeSet.setCollection("locations", holder.createLocations(probeSet.getIdentifier()));
        probeSet.setCollection("genes", holder.genes);
        createSynonym(probeSet.getIdentifier(), "identifier", holder.probesetIdentifier);
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
        protected List<String> genes = new ArrayList();
        protected List<String> transcripts = new ArrayList();
        private List<String> locations = new ArrayList<String>();
        protected Map<String, LocationHolder> locationHolders = new HashMap();
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
         * @return reference list of location objects
         * @throws ObjectStoreException if something goes wrong storing locations
         */
        protected List<String> createLocations(String probeSetRefId)
        throws ObjectStoreException {
            for (LocationHolder holder : locationHolders.values()) {
                String location = createLocation(holder, probeSetRefId);
                locations.add(location);
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
        return createBioentity("Gene", id, null);
    }

    private String createBioentity(String type, String identifier, String geneRefId)
    throws ObjectStoreException {
        String refId = bioentities.get(identifier);
        if (refId == null) {
            Item bioentity = createItem(type);
            bioentity.setAttribute("primaryIdentifier", identifier);
            bioentity.setReference("organism", org.getIdentifier());
            if ("Transcript".equals(type)) {
                bioentity.setReference("gene", geneRefId);
            }
            //bioentity.addToCollection("dataSets", dataSet);
            refId = bioentity.getIdentifier();
            store(bioentity);
            bioentities.put(identifier, refId);
            createSynonym(refId, "identifier", identifier);
        }
        return refId;
    }

    private Item createSynonym(String subjectId, String type, String value) {
        String key = subjectId + type + value;
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (!synonyms.containsKey(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            //syn.addToCollection("dataSets", dataSet);
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
        //item.addToCollection("dataSets", dataSet);
        store(item);
        return item.getIdentifier();
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

