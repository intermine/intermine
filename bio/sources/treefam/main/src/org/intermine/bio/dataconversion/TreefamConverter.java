package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.DataConverter;
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
    private static final String DATASET_TITLE = "TreeFam dataset";
    private static final String DATA_SOURCE_NAME = "TreeFam";
    private static final Logger LOG = Logger.getLogger(TreefamConverter.class);
    private Set<String> taxonIds = new HashSet();
    protected File geneFile;
    private Map<String, GeneHolder> idsToGenes = new HashMap();
    private Map<String, Item> identifiersToGenes = new HashMap();
    private Set<String> synonyms = new HashSet();
    private Map<String, String> organisms = new HashMap();
    private Map<String, String[]> config = new HashMap();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public TreefamConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
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

//            `IDX` int(11) NOT NULL default '0' COMMENT 'Gene index',
//            `ID` varchar(30) NOT NULL default '' COMMENT 'Unique sequence ID',
//            `TID` varchar(30) NOT NULL default '' COMMENT 'Transcript ID',
//            `TVER` tinyint(3) unsigned NOT NULL default '0' COMMENT 'Version of TID',
//            `GID` varchar(30) NOT NULL default '' COMMENT 'Gene ID',
//            `GVER` tinyint(3) unsigned NOT NULL default '0' COMMENT 'Version of GID',
//            `SYMBOL` varchar(30) NOT NULL default '' COMMENT 'gene symbol',
//            `DISP_ID` varchar(45) NOT NULL default '' COMMENT 'display name (obsolete)',
//            `TAX_ID` int(10) unsigned NOT NULL default '0' COMMENT 'Taxonomy ID',
//            `DESC` text NOT NULL COMMENT 'Description',


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

            Item gene = getGene(identifierType, identifier, taxonId);
            idsToGenes.put(id, new GeneHolder(id, identifier, gene));
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
     * @see DataConverter#process
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
                    Item gene1 = holder1.getGene();
                    Item gene2 = holder2.getGene();
                    processHomologues(gene1, gene2, bootstrap);
                    processHomologues(gene2, gene1, bootstrap);
                    holder1.setHomologue(true);
                    holder2.setHomologue(true);
                }
            }

        }
        for (Map.Entry<String, Item> entry : identifiersToGenes.entrySet()) {
            Item gene = entry.getValue();
            try {
                store(gene);
                getSynonym(gene.getIdentifier(), "identifier", entry.getKey());
            } catch (ObjectStoreException e) {
                throw new ObjectStoreException(e);
            }
        }
    }


// TODO only store homologues, idsToGenes is has duplicate gene objects
//        for (Map.Entry<String, GeneHolder> entry : idsToGenes.entrySet()) {
//            GeneHolder holder = entry.getValue();
//            if (holder.isHomologue()) {
//                Item item = holder.getGene();
//                try {
//                    store(item);
//                    getSynonym(item.getIdentifier(), "identifier", holder.getIdentifier());
//                } catch (ObjectStoreException e) {
//                    throw new ObjectStoreException(e);
//                }
//            }
//        }

    private void processHomologues(Item gene1, Item gene2, String bootstrap)
    throws ObjectStoreException {
        Item homologue = createItem("Homologue");
        homologue.setAttribute("bootstrapScore", bootstrap);
        homologue.setReference("gene", gene1);
        homologue.setReference("homologue", gene2);
        String type = "orthologue";
        String refId = gene1.getReference("organism").getRefId();
        if (gene2.getReference("organism").getRefId().equals(refId)) {
            type = "inParalogue";
        }
        homologue.setAttribute("type", type);
        gene1.addToCollection("homologues", homologue);
        try {
            store(homologue);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }

    private Item getGene(String identifierType, String identifier, String taxonId)
    throws ObjectStoreException {
        Item gene = identifiersToGenes.get(identifier);
        if (gene != null) {
            return gene;
        }
        gene = createItem("Gene");

        gene.setAttribute(identifierType, identifier);
        gene.setReference("organism", getOrganism(taxonId));
        identifiersToGenes.put(identifier, gene);
        return gene;
    }

    private void getSynonym(String subjectId, String type, String value)
    throws ObjectStoreException {
        String key = subjectId + type + value;
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

    /**
     * temporary object that holds the id, identifier, and gene for each record
     * @author julie
     *
     */
    public class GeneHolder
    {
        boolean isHomologue = false;
        String id = null;
        String identifier = null;
        Item gene = null;

        /**
         * @param id internal treefam database id, an integer
         * @param identifier gene identifier, eg FBgn
         * @param gene gene object
         */
        public GeneHolder(String id, String identifier, Item gene) {
            this.id = id;
            this.identifier = identifier;
            this.gene = gene;
        }

        /**
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the identifier
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         * @param identifier the identifier to set
         */
        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        /**
         * @return the gene
         */
        public Item getGene() {
            return gene;
        }

        /**
         * @param gene the gene to set
         */
        public void setGene(Item gene) {
            this.gene = gene;
        }

        /**
         * @return the isHomologue
         */
        public boolean isHomologue() {
            return isHomologue;
        }

        /**
         * @param isHomologue the isHomologue to set
         */
        public void setHomologue(boolean isHomologue) {
            this.isHomologue = isHomologue;
        }

    }

}
