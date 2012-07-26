package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.StringUtil;
import org.intermine.xml.full.Item;
import org.xml.sax.SAXException;

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
    private Set<String> homologues = new HashSet<String>();
    protected File geneFile;
    private Map<String, GeneHolder> idsToGenes = new HashMap<String, GeneHolder>();
    private Map<String, String> identifiersToGenes = new HashMap<String, String>();
    private Map<String, String[]> config = new HashMap<String, String[]>();
    protected IdResolverFactory flyResolverFactory;
    private static String evidenceRefId = null;
    protected IdResolverFactory fishResolverFactory;
    private static final String ZFIN_TAXON = "7955";
    private static final String FLY_TAXON = "7227";

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
        flyResolverFactory = new FlyBaseIdResolverFactory("gene");
        fishResolverFactory = new ZfinGeneIdResolverFactory();

    }

    /**
     * Sets the list of taxonIds that should be processed.  All genes will be loaded.
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setTreefamOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtil.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + taxonIds);
    }

    /**
     * Sets the list of taxonIds of homologues that should be processed.  These homologues will only
     * be processed if they are homologues for the organisms of interest.
     *
     * @param homologues a space-separated list of taxonIds
     */
    public void setTreefamHomologues(String homologues) {
        this.homologues = new HashSet<String>(Arrays.asList(StringUtil.split(homologues, " ")));
        LOG.info("Setting list of homologues to " + homologues);
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
     * Read genes file.
     *
     * Col0 = internal id (eg, 12345)
     * Col4 = identifier (eg, Fbgn)
     * Col8 = taxid
     * @param reader reader
     * @throws IOException if the file cannot be found/read
     * @throws ObjectStoreException if the objects cannot be stored to the database
     * @throws SAXException if something goes horribly wrong
     */
    public void readGenes(Reader reader)
        throws IOException, ObjectStoreException, SAXException {
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] bits = lineIter.next();
            if (bits.length < 9) {
                throw new IllegalArgumentException("bad data file, couldn't process:" + bits[0]);
            }
            String id = bits[0];
            String identifier = bits[4];
            String symbol = bits[6];
            String taxonId = bits[8];

            if (!taxonIds.contains(taxonId) && !homologues.contains(taxonId)) {
                // don't create gene object if gene isn't from an organism of interest
                continue;
            }

            // default value
            String identifierType = "primaryIdentifier";

            if (config.containsKey(taxonId)) {
                String[] configs = config.get(taxonId);
                identifier = setIdentifier(identifier, symbol, configs[0]);
                identifierType = configs[1];
            }
            idsToGenes.put(id, new GeneHolder(identifier, identifierType, taxonId));
        }
    }

    private String getGene(String ident, String type, String taxonId)
        throws ObjectStoreException {
        String identifierType = type;
        String identifier = ident;
        if (ZFIN_TAXON.equals(taxonId) || FLY_TAXON.equals(taxonId)) {
            IdResolverFactory idResolverFactory = null;
            if (ZFIN_TAXON.equals(taxonId)) {
                idResolverFactory = fishResolverFactory;
            } else if (FLY_TAXON.equals(taxonId)) {
                idResolverFactory = flyResolverFactory;
            }
            identifier = resolveGene(idResolverFactory, taxonId, identifier);
            identifierType = "primaryIdentifier";
            if (identifier == null) {
                return null;
            }
        }
        String refId = identifiersToGenes.get(identifier);
        if (refId == null) {
            Item item = createItem("Gene");
            item.setAttribute(identifierType, identifier);
            item.setReference("organism", getOrganism(taxonId));
            refId = item.getIdentifier();
            identifiersToGenes.put(identifier, refId);
            store(item);
        }
        return refId;
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
        if (taxonIds.isEmpty()) {
            throw new NullPointerException("treefam.organisms property not set in project XML "
                    + "file");
        }
        if (homologues.isEmpty()) {
            LOG.warn("treefam.homologues property not set in project XML file");
        }
        try {
            readGenes(new FileReader(geneFile));
        } catch (IOException err) {
            throw new RuntimeException("error reading geneFile", err);
        }
        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] bits = lineIter.next();
            if (bits.length < 4) {
                continue;
            }
            String gene1id = bits[0];
            String gene2id = bits[1];
            //String taxonId = bits[2];
            String bootstrap = bits[3];

            GeneHolder holder1 = idsToGenes.get(gene1id);
            GeneHolder holder2 = idsToGenes.get(gene2id);

            // at least one of the genes has to be from an organism of interest
            if (isValidPair(holder1, holder2)) {
                processHomologues(holder1, holder2, bootstrap);
                processHomologues(holder2, holder1, bootstrap);
            }
        }
    }

    // the gene is from an organism we want
    // the homologue is from an organism we want
    private boolean isValidPair(GeneHolder holder1, GeneHolder holder2) {
        if (holder1 == null || holder2 == null) {
            return false;
        }
        if (holder1.validGene()  && (holder2.validGene() || holder2.validHomologue())) {
            return true;
        }
        if (holder2.validGene()  && (holder1.validGene() || holder1.validHomologue())) {
            return true;
        }
        return false;
    }

    private void processHomologues(GeneHolder holder1, GeneHolder holder2, String bootstrap)
        throws ObjectStoreException {

        String gene1 = getGene(holder1.identifier, holder1.identifierType, holder1.taxonId);
        String gene2 = getGene(holder2.identifier, holder2.identifierType, holder2.taxonId);

        // resolver didn't resolve
        if (gene1 == null || gene2 == null) {
            return;
        }

        Item homologue = createItem("Homologue");
        homologue.setAttribute("bootstrapScore", bootstrap);
        homologue.setReference("gene", gene1);
        homologue.setReference("homologue", gene2);
        homologue.addToCollection("evidence", getEvidence());
        String type = "orthologue";
        if (holder1.taxonId.equals(holder2.taxonId)) {
            type = "paralogue";
        }
        homologue.setAttribute("type", type);

        store(homologue);
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
        protected String identifier, taxonId, identifierType;

        /**
         * @param identifier gene identifier, eg FBgn
         * @param identifierType which field to set, eg. primaryIdentifier or symbol
         * @param taxonId organism for this gene
         */
        public GeneHolder(String identifier, String identifierType, String taxonId) {
            this.identifier = identifier;
            this.taxonId = taxonId;
            this.identifierType = identifierType;
        }

        /**
         * @return true of this organism is in list of organisms of interest
         */
        protected boolean validGene() {
            return taxonIds.contains(taxonId);
        }

        /**
         * @return true of this organism is in the homologues list
         */
        protected boolean validHomologue() {
            return homologues.contains(taxonId);
        }
    }

    private String setIdentifier(String ident, String sym, String col) {
        String identifier = ident;
        String symbol = sym;
        if ("symbol".equals(col)) {
            if (symbol.contains("_")) {
                // to handle this case:  WBGene00022038_F2
                symbol = symbol.split("_")[0];
            }
            identifier = symbol;
        }
        return identifier;
    }

    private String resolveGene(IdResolverFactory resolverFactory, String taxonId,
            String identifier) {
        IdResolver resolver = resolverFactory.getIdResolver(false);
        if (resolver == null) {
            // no id resolver available, so return the original identifier
            return identifier;
        }
        int resCount = resolver.countResolutions(taxonId, identifier);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                    + identifier + " count: " + resCount + " "
                    + resolver.resolveId(taxonId, identifier));
            return null;
        }
        return resolver.resolveId(taxonId, identifier).iterator().next();
    }
}
