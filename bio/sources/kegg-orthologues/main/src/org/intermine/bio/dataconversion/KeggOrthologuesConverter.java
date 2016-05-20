package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;

/**
 * Create orthologues based on KEGG pathways.
 *
 * @author Julie Sullivan
 */
public class KeggOrthologuesConverter extends BioFileConverter
{
    protected static final Logger LOG = Logger.getLogger(KeggOrthologuesConverter.class);
    private static final String PROP_FILE = "kegg_config.properties";
    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";
    private static final String DATASET_TITLE = "KEGG orthologues data set";
    private static final String DATA_SOURCE_NAME = "GenomeNet";
    private static String evidenceRefId = null;
    //eg.             HSA: 7358(UGDH)
    private static final String REGULAR_EXPRESSION = "\\w\\w\\w[:]\\s.+";
    private static final Pattern HOMOLOGUE_PATTERN = Pattern.compile(REGULAR_EXPRESSION);
    private Map<String, String[]> config = new HashMap<String, String[]>();
    private Set<String> taxonIds = new HashSet<String>();
    private Map<String, String> identifiersToGenes = new HashMap<String, String>();

    protected IdResolver rslv;

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public KeggOrthologuesConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
    }

    /**
     * Sets the list of taxonIds that should be imported
     *
     * @param taxonIds a space-separated list of taxonIds
     */
    public void setKeggOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
        LOG.info("Setting list of organisms to " + this.taxonIds);
    }

    private void readConfig() {
        Properties props = new Properties();
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
            String organism = attributes[0];

            if (config.get(organism) == null) {
                String[] configs = new String[2];
                configs[1] = "primaryIdentifier";
                config.put(organism, configs);
            }
            if ("taxonId".equals(attributes[1])) {
                String[] bits = config.get(organism);
                bits[0] = value;
            } else if ("identifier".equals(attributes[1])) {
                String[] bits = config.get(organism);
                bits[1] = value;
            } else {
                String msg = "Problem processing properties '" + PROP_FILE + "' on line " + key
                    + ".  This line has not been processed.";
                LOG.error(msg);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        // init resolver
        if (rslv == null) {
            rslv = IdResolverService.getFlyIdResolver();
            rslv = IdResolverService.getWormIdResolver();
        }

        Set<String> homologues = new HashSet<String>();
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        while ((line = br.readLine()) != null) {
            // process gene
            if (line.length() > 12) {
                String choppedLine = line.substring(12);
                Matcher matcher = HOMOLOGUE_PATTERN.matcher(choppedLine);
                if (matcher.matches()) {
                    // chop off beginning indentation
                    processLine(choppedLine, homologues);
                }
            }
            // end of entry, process all homologues
            if (line.startsWith("///")) {
                for (String identifier : homologues) {
                    processHomologues(identifier, homologues);
                }
                // reset list
                homologues = new HashSet<String>();
            }
        }
    }

    private void processLine(String line, Set<String> homologues)
        throws ObjectStoreException {
        // split taxon abbreviation and identifier, eg. HSA: 7358(UGDH)
        String[] bits = line.split(" ");
        if (bits.length < 2) {
            // not a valid gene line
            return;
        }
        // get `HSA` from `HSA: 7358(UGDH)`
        String organism = bits[0].substring(0, 3);
        String[] organismConfig = config.get(organism);
        if (organismConfig == null) {
            return;
        }
        String taxonId = organismConfig[0];
        String identifierType = organismConfig[1];
        if (!taxonIds.isEmpty() && !taxonIds.contains(taxonId)) {
            // don't create gene object if gene isn't from an organism of interest
            return;
        }
        // default value
        if (identifierType == null) {
            identifierType = "primaryIdentifier";
        }
        for (int i = 1; i < bits.length; i++) {
            String geneRefId = getGene(identifierType, formatIdentifier(bits[i]), taxonId);
            if (geneRefId != null) {
                homologues.add(geneRefId);
            }
        }
    }

    // for this gene, loop through other genes and make homologue pair
    private void processHomologues(String identifier, Set<String> homologues)
        throws ObjectStoreException {
        for (String homologue : homologues) {
            if (!homologue.equals(identifier)) {
                processHomologue(identifier, homologue);
            }
        }
    }

    private String formatIdentifier(String s) {
        String identifier = s;
        if (identifier.startsWith("Dmel_")) {
            identifier = identifier.substring(5);
        }
        if (identifier.contains("(")) {
            identifier = identifier.substring(0, identifier.indexOf('('));
        }
        return identifier;
    }

    // save homologue pair
    private void processHomologue(String gene1, String gene2)
        throws ObjectStoreException {
        Item homologue = createItem("Homologue");
        homologue.setReference("gene", gene1);
        homologue.setReference("homologue", gene2);
        homologue.addToCollection("evidence", getEvidence());
        homologue.setAttribute("type", "homologue");
        try {
            store(homologue);
        } catch (ObjectStoreException e) {
            throw new ObjectStoreException(e);
        }
    }

    private String getGene(String identifierType, String id, String taxonId)
        throws ObjectStoreException {
        String identifier = id;

        if (rslv != null && rslv.hasTaxon(taxonId)) {
            identifier = resolveGene(identifier, taxonId);
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
            try {
                store(gene);
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


    private String resolveGene(String originalId, String taxonId) {
        String primaryIdentifier = null;
        int resCount = rslv.countResolutions(taxonId, originalId);
        if (resCount != 1) {
            LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring "
                    + "gene: " + originalId + " for organism " + taxonId + " count: " + resCount
                    + " found ids: " + rslv.resolveId(taxonId, originalId) + ".");
        } else {
            primaryIdentifier =
                    rslv.resolveId(taxonId, originalId).iterator().next();
            LOG.info("RESOLVER found gene " + primaryIdentifier
                    + " for original id: " + originalId);
        }
        return primaryIdentifier;
    }

}
