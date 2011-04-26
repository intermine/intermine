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
import java.io.FileNotFoundException;
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
import org.intermine.bio.dataconversion.BioFileConverter;
import org.intermine.bio.dataconversion.FlyBaseIdResolverFactory;
import org.intermine.bio.dataconversion.IdResolver;
import org.intermine.bio.dataconversion.IdResolverFactory;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 *
 * @author
 */
public class EnsemblComparaConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(EnsemblComparaConverter.class);
    private static final String PROP_FILE = "ensembl-compara_config.properties";
    private static final String EVIDENCE_CODE_ABBR = "AA";
    private static final String EVIDENCE_CODE_NAME = "Amino acid sequence comparison";
    private Set<String> taxonIds;
    private Set<String> homologues;
    private static final String DATASET_TITLE = "Ensembl Compara data set";
    private static final String DATA_SOURCE_NAME = "Ensembl";
    private Map<String, String> genes = new HashMap<String, String>();
    protected IdResolver resolver;
    private Map<String, Config> configs = new HashMap<String, Config>();
    private static String evidenceRefId = null;
    protected enum ColumnType { ensembl, entrez, name }

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public EnsemblComparaConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
        IdResolverFactory resolverFactory = new FlyBaseIdResolverFactory("gene");
        resolver = resolverFactory.getIdResolver(false);
    }

    public void setEnsemblComparaOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
    }

    public void setEnsemblComparaHomologues(String taxonIds) {
        this.homologues = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
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
            if (attributes.length != 2) {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                                           + key);
            }

            String taxonId = attributes[0];
            Config config = configs.get(taxonId);
            if (config == null) {
                config = new Config();
                configs.put(taxonId, config);
            }
            String fieldName = attributes[1];
            if ("primaryIdentifier".equals(fieldName)) {
                config.primaryIdentifier = value;
            } else if ("secondaryIdentifier".equals(fieldName)) {
                config.secondaryIdentifier = value;
            } else if ("symbol".equals(fieldName)) {
                config.symbol = value;
            } else {
                throw new RuntimeException("Problem loading properties '" + PROP_FILE + "' on line "
                        + fieldName);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(Reader reader) throws Exception {
        if (taxonIds == null || taxonIds.isEmpty()) {
            throw new IllegalArgumentException("No organism data provided for Ensembl Compara");
        }
        File file = getCurrentFile();
        if (file == null) {
            throw new FileNotFoundException("No valid data files found.");
        }
        String fileName = file.getName();
        String[] bits = fileName.split("_");
        boolean processFile = false;
        for (String bit : bits) {
            if (taxonIds.contains(bit)) {
                processFile = true;
            } else if (!homologues.contains(bit)) {
                // this file contains an organism not listed in the project XML file
                return;
            }
        }
        if (!processFile) {
            // this file contains organisms listed in homologues collection, but not genes
            return;
        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            if (line.length <= 8) {
                throw new RuntimeException("Invalid line, should be 8 columns but is '"
                        + line + "' instead");
            }
            String refId1 = parseGene(line, 0);
            String refId2 = parseGene(line, 4);

            if (refId1 == null || refId2 == null) {
                continue;
            }

            // store homologues
            processHomologue(refId1, refId2);
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

    private String parseGene(String[] line, int index)
        throws ObjectStoreException {
        int i = index;
        String taxonId = line[i++];
        String ensembl = line[i++];
        String name = line[i++];
        String entrez = line[i];

        String newIdentifier = ensembl;
        if ("7227".equals(taxonId)) {
            newIdentifier = resolveGene(taxonId, ensembl);
            if (newIdentifier == null) {
                return null;
            }
        }
        String refId = genes.get(newIdentifier);
        if (refId == null) {
            Config config = getConfig(taxonId);
            if (config == null) {
                throw new IllegalArgumentException("no config found");
            }
            Item item = createItem("Gene");
            if (config.primaryIdentifier != null) {
                String identifier = getIdentifier(config.primaryIdentifier, ensembl, name, entrez);
                item.setAttribute("primaryIdentifier", identifier);
            }
            if (config.secondaryIdentifier != null) {
                String identifier = getIdentifier(
                            config.secondaryIdentifier, ensembl, name, entrez);
                item.setAttribute("secondaryIdentifier", identifier);
            }
            if (config.symbol != null) {
                String identifier = getIdentifier(config.symbol, ensembl, name, entrez);
                item.setAttribute("symbol", identifier);
            }
            item.setReference("organism", getOrganism(taxonId));
            store(item);
            refId = item.getIdentifier();
            genes.put(newIdentifier, refId);
        }
        return refId;
    }

    private String getIdentifier(String identifierField, String ensembl, String name,
            String entrez) {
        if (identifierField.equals(ColumnType.ensembl)) {
            return ensembl;
        }
        if (identifierField.equals(ColumnType.name)) {
            return name;
        }
        if (identifierField.equals(ColumnType.entrez)) {
            return entrez;
        }
        throw new IllegalArgumentException("invalid config: " + identifierField);
    }

    private Config getConfig(String taxonId) {
        Config config = configs.get(taxonId);
        if (config == null) {
            config = configs.get("default");
        }
        return config;
    }

    private String resolveGene(String taxonId, String identifier) {
        String id = identifier;
        if ("7227".equals(taxonId) && resolver != null) {
            int resCount = resolver.countResolutions(taxonId, identifier);
            if (resCount != 1) {
                LOG.info("RESOLVER: failed to resolve gene to one identifier, ignoring gene: "
                        + identifier + " count: " + resCount + " FBgn: "
                        + resolver.resolveId(taxonId, identifier));
                return null;
            }
            id = resolver.resolveId(taxonId, identifier).iterator().next();
        }
        return id;
    }

    public class Config
    {
        protected String primaryIdentifier = null;
        protected String secondaryIdentifier = null;
        protected String symbol = null;
    }
}
