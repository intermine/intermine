package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;

/**
 * @author Julie Sullivan
 */
public class TreefamConverter extends BioFileConverter
{
    private Properties props = new Properties();
    private static final String PROP_FILE = "treefam_config.properties";
    private static final String DATASET_TITLE = "TreeFam data set";
    private static final String DATA_SOURCE_NAME = "TreeFam";
    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";
    private static final Logger LOG = Logger.getLogger(TreefamConverter.class);
    private Set<String> taxonIds = new HashSet<String>();
    protected File geneFile;
    private Map<String, GeneHolder> idsToGenes = new HashMap<String, GeneHolder>();
    private Map<String, String> identifiersToGenes = new HashMap<String, String>();
    private Set<MultiKey> synonyms = new HashSet<MultiKey>();
    private Map<String, String> organisms = new HashMap<String, String>();
    private Map<String, String[]> config = new HashMap<String, String[]>();
    protected IdResolverFactory resolverFactory;
    private IdResolver flyResolver;
    private static String evidenceRefId = null;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     * @throws ObjectStoreException can't store dataset
     */
    public TreefamConverter(ItemWriter writer, Model model)
    throws ObjectStoreException {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
        // only construct factory here so can be replaced by mock factory in tests
        resolverFactory = new FlyBaseIdResolverFactory("gene");
    }

    /**
     * Sets the list of taxonIds that should be processed
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setTreefamOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    /**
     * Read genes file
     *
     * Col0 = internal id (eg, 12345)
     * Col4 = identifier (eg, Fbgn)
     * Col8 = taxid
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     * @throws ObjectStoreException if the objects cannot be stored to the database
     */
    public void readGenes(Reader reader) throws IOException, ObjectStoreException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String bits[] = lineIter.next();
            if (bits.length < 9) {
                throw new RuntimeException("bad data file, couldn't process:" + bits);
            }
            String id = bits[0];
            String identifier = bits[4];
            String symbol = bits[6];
            String taxonId = bits[8];

            try {
                new Integer(taxonId);
            } catch (NumberFormatException e) {
                continue;
            }
            if (!taxonIds.isEmpty() && !taxonIds.contains(taxonId)) {
                // don't create gene object if gene isn't from an organism of interest
                continue;
            }

            // default value
            String identifierType = "primaryIdentifier";

            if (config.containsKey(taxonId)) {
                String[] configs = config.get(taxonId);
                String col = configs[0];
                if (col.equals("symbol")) {
                    if (symbol.contains("_")) {
                        // to handle this case:  WBGene00022038_F2
                        symbol = symbol.split("_")[0];
                    }
                    identifier = symbol;
                }
                identifierType = configs[1];
            }

            String geneRefId = getGene(identifierType, identifier, taxonId);
            if (geneRefId != null) {
                idsToGenes.put(id, new GeneHolder(identifier, geneRefId, taxonId));
            }
        }
    }

    /**
     * Set the gene input file.
     *
     * @param geneFile gene input file
     */
    public void setGeneFile(File geneFile) {
        this.geneFile = geneFile;
    }

    private void readConfig() {
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Problem loading properties '" + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry: props.entrySet()) {

            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();

            String[] attributes = key.split("\\.");
            if (attributes.length == 0) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                                           + key);
            }
            String taxonId = attributes[0];
            String col = attributes[1];

            String[] configs = new String[2];
            configs[0] = col;
            configs[1] = value;

            config.put(taxonId, configs);
        }
    }

    /**
     * Process the text file
     * @param reader the Reader
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (geneFile == null) {
            throw new NullPointerException("geneFile property not set");
        }
        try {
            readGenes(new FileReader(geneFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading geneFile", err);
        }
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String bits[] = lineIter.next();
            if (bits.length < 4) {
                continue;
            }
            String gene1id = bits[0];
            String gene2id = bits[1];
            //String taxonId = bits[2];
            String bootstrap = bits[3];

            GeneHolder holder1 = idsToGenes.get(gene1id);
            if (holder1 != null) {
                GeneHolder holder2 = idsToGenes.get(gene2id);
                if (holder2 != null) {
                    processHomologues(holder1, holder2, bootstrap);
                    processHomologues(holder2, holder1, bootstrap);
                }
            }

        }
    }

    private void processHomologues(GeneHolder holder1, GeneHolder holder2, String bootstrap)
    throws ObjectStoreException {

        String gene1 = holder1.getGeneRefId();
        String gene2 = holder2.getGeneRefId();

        Item homologue = createItem("Homologue");
        homologue.setAttribute("bootstrapScore", bootstrap);
        homologue.setReference("gene", gene1);
        homologue.setReference("homologue", gene2);
        homologue.addToCollection("evidence", getEvidence());
        String type = "orthologue";
        if (holder1.getTaxonId().equals(holder2.getTaxonId())) {
            type = "paralogue";
        }
        homologue.setAttribute("type", type);
        //gene1.addToCollection("homologues", homologue);
        try {
            store(homologue);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }

    private String getGene(String identifierType, String id, String taxonId)
    throws ObjectStoreException {
        String identifier = id;
        if (taxonId.equals("7227")) {
            identifier = resolveGene(identifier);
            if (identifier == null) {
                return null;
            }
        }
        String refId = identifiersToGenes.get(identifier);
        if (refId == null) {
            Item gene = createItem("Gene");
            refId = gene.getIdentifier();
            gene.setAttribute(identifierType, identifier);
            gene.setReference("organism", getOrganism(taxonId));
            identifiersToGenes.put(identifier, refId);
            getSynonym(refId, "identifier", identifier);
            try {
                store(gene);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

        }
        return refId;
    }

    private void getSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
        MultiKey key = new MultiKey(subjectId, type, value);
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (!synonyms.contains(key)) {
            Item syn = createItem("Synonym");
            syn.setReference("subject", subjectId);
            syn.setAttribute("type", type);
            syn.setAttribute("value", value);
            synonyms.add(key);
            try {
                store(syn);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }

    private String getOrganism(String taxonId)
    throws ObjectStoreException {
        String refId = organisms.get(taxonId);
        if (refId == null) {
            Item item = createItem("Organism");
            item.setAttribute("taxonId", taxonId);
            refId = item.getIdentifier();
            organisms.put(taxonId, refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
        return refId;
    }

    private String getEvidence()
    throws ObjectStoreException {

        if (evidenceRefId == null) {

            Item item = createItem("OrthologueEvidenceCode");
            item.setAttribute("abbreviation", EVIDENCE_CODE_ABBR);
            item.setAttribute("name", EVIDENCE_CODE_NAME);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
            String refId = item.getIdentifier();

            item = createItem("OrthologueEvidence");
            item.setReference("evidenceCode", refId);
            try {
                store(item);
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }

            evidenceRefId = item.getIdentifier();
        }
        return evidenceRefId;
    }

    /**
     * temporary object that holds the id, identifier, and gene for each record
     * @author julie
     *
     */
    public class GeneHolder
    {
        String identifier = null;
        String geneRefId = null;
        String taxonId = null;

        /**
         * @param identifier gene identifier, eg FBgn
         * @param geneRefId id representing the gene object
         * @param taxonId organism for this gene
         */
        public GeneHolder(String identifier, String geneRefId, String taxonId) {
            this.identifier = identifier;
            this.geneRefId = geneRefId;
            this.taxonId = taxonId;
        }


        /**
         * @return the identifier
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         * @return the id representing the gene object
         */
        public String getGeneRefId() {
            return geneRefId;
        }

        /**
         * @return taxonid
         */
        public String getTaxonId() {
            return taxonId;
        }
    }

    private String resolveGene(String identifier) {
        // we only have a resolver for dmel for now
        String taxonId = "7227";
        flyResolver = resolverFactory.getIdResolver(false);
        if (flyResolver == null) {
            // no id resolver available, so return the original identifier
            return identifier;
        }
        int resCount = flyResolver.countResolutions(taxonId, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                     + identifier + " count: " + resCount + " FBgn: "
                     + flyResolver.resolveId(taxonId, identifier));
            return null;
        }
        return flyResolver.resolveId(taxonId, identifier).iterator().next();
    }
}
