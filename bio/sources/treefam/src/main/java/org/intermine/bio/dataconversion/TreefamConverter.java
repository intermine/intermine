package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2020 FlyMine
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
import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.metadata.StringUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
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
    private static String evidenceRefId = null;
    private static final String DEFAULT_IDENTIFIER_TYPE = "primaryIdentifier";
    private static final String DEFAULT_HOMOLOGUE_TYPE = "orthologue";
    private static final String DEFAULT_IDENTIFIER_COLUMN = "geneid";
    protected IdResolver rslv;
    private Set<MultiKey> homologuePairs = new HashSet<MultiKey>();

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
     * Process the text file
     * @param reader the Reader
     * @throws Exception if something goes wrong
     */
    @Override
    public void process(Reader reader) throws Exception {
        if (geneFile == null) {
            throw new BuildException("geneFile property not set");
        }
        if (taxonIds.isEmpty()) {
            throw new BuildException("treefam.organisms property not set in project XML file");
        }
        if (homologues.isEmpty()) {
            LOG.info("treefam.homologues property not set in project XML file");
        }

        createIDResolver();

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

    private void processHomologues(GeneHolder holder1, GeneHolder holder2, String bootstrap)
        throws ObjectStoreException {

        String gene1 = getGene(holder1);
        String gene2 = getGene(holder2);

        // resolver didn't resolve OR a duplicate
        // AND genes can be paralogues with themselves so don't duplicate
        if (gene1 == null || gene2 == null
                || homologuePairs.contains(new MultiKey(gene1, gene2))
                || gene1.equals(gene2)) {
            return;
        }

        Item homologue = createItem("Homologue");
        homologue.setAttribute("bootstrapScore", bootstrap);
        homologue.setReference("gene", gene1);
        homologue.setReference("homologue", gene2);
        homologue.addToCollection("evidence", getEvidence());
        String type = DEFAULT_HOMOLOGUE_TYPE;
        if (holder1.taxonId.equals(holder2.taxonId)) {
            type = "paralogue";
        }
        homologue.setAttribute("type", type);
        store(homologue);
        homologuePairs.add(new MultiKey(gene1, gene2));
    }

    private String getGene(GeneHolder holder)
        throws ObjectStoreException {
        String refId = identifiersToGenes.get(holder.resolvedIdentifier);
        if (refId == null) {
            Item gene = createItem("Gene");
            if (!holder.identifierType.equals(DEFAULT_IDENTIFIER_TYPE)) {
                gene.setAttribute(holder.identifierType, holder.resolvedIdentifier);
            } else {
                gene.setAttribute(DEFAULT_IDENTIFIER_TYPE, holder.resolvedIdentifier);
            }
            gene.setReference("organism", getOrganism(holder.taxonId));
            refId = gene.getIdentifier();
            identifiersToGenes.put(holder.resolvedIdentifier, refId);
            store(gene);
        }
        return refId;
    }

    private void createIDResolver() {
        Set<String> allTaxonIds = new HashSet<String>();
        allTaxonIds.addAll(taxonIds);
        allTaxonIds.addAll(homologues);
        if (rslv == null) {
            rslv = IdResolverService.getIdResolverByOrganism(allTaxonIds);
        }
        LOG.info("Taxons in resolver:" + rslv.getTaxons());
    }

    // the gene is from an organism we want
    // the homologue is from an organism we want
    private static boolean isValidPair(GeneHolder holder1, GeneHolder holder2) {
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
            String geneId = bits[4];
            String symbol = bits[6];
            String taxonId = bits[8];

            if (!taxonIds.contains(taxonId) && !homologues.contains(taxonId)) {
                // don't create gene object if gene isn't from an organism of interest
                continue;
            }

            // remove special characters
            symbol = chopSymbol(symbol);

            String identifierType = DEFAULT_IDENTIFIER_TYPE;
            String identifierColumn = DEFAULT_IDENTIFIER_COLUMN;

            if (config.containsKey(taxonId)) {
                String[] configs = config.get(taxonId);
                identifierColumn = configs[0];
                identifierType = configs[1];
            }

            String resolvedIdentifier = resolveGene(taxonId, geneId, symbol);
            if (!StringUtils.isEmpty(resolvedIdentifier)) {
                idsToGenes.put(id, new GeneHolder(geneId, symbol, resolvedIdentifier,
                        identifierColumn, identifierType, taxonId));
            }
        }
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
     * temporary object that holds the id, identifier, symbol, and gene for each record
     * @author julie
     *
     */
    public class GeneHolder
    {
        protected String identifier, symbol, taxonId, identifierType, resolvedIdentifier;
        protected String whichColumn;

        /**
         * @param identifier gene identifier, eg FBgn from geneid column
         * @param identifierType which field to set, eg. primaryIdentifier or symbol
         * @param taxonId organism for this gene
         * @param symbol gene symbol, 6th column. with special chars removed
         * @param resolvedIdentifier the identifer found by the ID resolver
         * @param whichColumn either geneid (4th col) or symbol (6th col)
         */
        public GeneHolder(String identifier, String symbol, String resolvedIdentifier,
                String whichColumn, String identifierType, String taxonId) {
            this.identifier = identifier;
            this.symbol = symbol;
            this.taxonId = taxonId;
            this.identifierType = identifierType;
            this.whichColumn = whichColumn;
            this.resolvedIdentifier = resolvedIdentifier;
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

    private static String chopSymbol(String symbol) {
        String cleanSymbol = symbol;
        if (cleanSymbol.contains("_")) {
            // to handle this case:  Y65B4BL.6_F2
            cleanSymbol = cleanSymbol.split("_")[0];
        }
        if (cleanSymbol.contains("_")) {
            // to handle this case:  Y65B4BL.6_F2
            cleanSymbol = cleanSymbol.split("_")[0];
        }
        if (cleanSymbol.contains("-")) {
            // to handle this case:  LIMS3-201
            cleanSymbol = cleanSymbol.split("-")[0];
        }
        return cleanSymbol;
    }

    private String resolveGene(String taxonId, String identifier, String symbol) {
        String id = identifier;
        if (rslv != null && rslv.hasTaxon(taxonId)) {
            int resCount = rslv.countResolutions(taxonId, identifier);
            if (resCount != 1) {
                // failed to resolve. try again!
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                         + identifier + " count: " + resCount + " Human identifier: "
                         + identifier);
                return null;
            }
            id = rslv.resolveId(taxonId, identifier).iterator().next();
        }
        return id;
    }
}
