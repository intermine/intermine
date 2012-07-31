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
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.xml.full.Item;


/**
 * Class to convert a flat file of identifiers from an Ensembl biomart to genes
 * @author Julie Sullivan
 */
public class EnsemblIdentifiersConverter extends BioFileConverter
{
    private static final Logger LOG = Logger.getLogger(EnsemblIdentifiersConverter.class);
    private static final String PROP_FILE = "ensembl-identifiers_config.properties";
    private Set<String> taxonIds;
    private static final String DATASET_TITLE = "Ensembl identifiers data set";
    private static final String DATA_SOURCE_NAME = "Ensembl";
    private Set<String> genes = new HashSet<String>();
    protected IdResolver resolver;
    private Map<String, Config> configs = new HashMap<String, Config>();

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @param model the Model
     */
    public EnsemblIdentifiersConverter(ItemWriter writer, Model model) {
        super(writer, model, DATA_SOURCE_NAME, DATASET_TITLE);
        readConfig();
    }

    /**
     * @param taxonIds taxon ID to process
     */
    public void setEnsemblOrganisms(String taxonIds) {
        this.taxonIds = new HashSet<String>(Arrays.asList(StringUtils.split(taxonIds, " ")));
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
            config.addIdentifierField(attributes[1], value);
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

        if (!taxonIds.contains(fileName)) {
            // this file contains an organism not listed in the project XML file
            return;
        }

        Iterator<String[]> lineIter = FormattedTextParser.parseTabDelimitedReader(reader);
        while (lineIter.hasNext()) {
            String[] line = lineIter.next();
            // skip header
            if (!line[0].startsWith("Ensembl")) {
                parseGene(line, fileName);
            }
        }
    }

    private void parseGene(String[] line, String taxonId)
        throws ObjectStoreException {
        String ensembl = line[0];
        String name = line[1];
        String entrez = line[2];

        String newIdentifier = ensembl;
        if ("7227".equals(taxonId)) {
            newIdentifier = resolveGene(taxonId, ensembl);
            if (newIdentifier == null) {
                return;
            }
        }

        if (!genes.contains(newIdentifier)) {
            Config config = getConfig(taxonId);
            if (config == null) {
                throw new IllegalArgumentException("no config found");
            }
            Item item = createItem("Gene");
            String identifierField = config.getIdentifierField("ensembl");
            if (StringUtils.isNotEmpty(identifierField) && StringUtils.isNotEmpty(ensembl)) {
                item.setAttribute(identifierField, ensembl);
            }
            identifierField = config.getIdentifierField("name");
            if (StringUtils.isNotEmpty(identifierField) && StringUtils.isNotEmpty(name)) {
                item.setAttribute(identifierField, name);
            }
            identifierField = config.getIdentifierField("entrez");
            if (StringUtils.isNotEmpty(identifierField) && StringUtils.isNotEmpty(entrez)) {
                item.setAttribute(identifierField, entrez);
            }
            item.setReference("organism", getOrganism(taxonId));
            store(item);
            genes.add(newIdentifier);
        }
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

    /**
     * Represents config for a taxon ID
     * @author Julie Sullivan
     */
    protected class Config
    {
        private Map<String, String> dataColumnToIdentifier = new HashMap<String, String>();

        /**
         * @param columnType what type of column, eg. ensembl or name
         * @return which identifier this column of data should populate
         */
        protected String getIdentifierField(String columnType) {
            return dataColumnToIdentifier.get(columnType);
        }

        /**
         * @param dataColumn which column of data
         * @param identifierColumn which gene.identifier field
         */
        protected void addIdentifierField(String dataColumn, String identifierColumn) {
            dataColumnToIdentifier.put(dataColumn, identifierColumn);
        }
    }
}
