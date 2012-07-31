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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 *
 * @author julie sullivan
 *
 */
public class UniprotConfig
{
    private static final Logger LOG = Logger.getLogger(UniprotConfig.class);
    private static final String PROP_FILE = "uniprot_config.properties";
    private List<String> featureTypes = new ArrayList<String>();
    private List<String> xrefs = new ArrayList<String>();
    private Map<String, ConfigEntry> entries = new HashMap<String, ConfigEntry>();
    private String geneDesignation = "gene designation";
    private Map<String, String> strains = new HashMap<String, String>();

    /**
     * read configuration file
     */
    public UniprotConfig() {
        readConfig();
    }

    /**
     * if NULL, all feature types will be loaded.
     * @return list of feature types.  will return EMPTY if "feature.types" attribute
     * not set in property file.
     */
    public List<String> getFeatureTypes() {
        return featureTypes;
    }

    /**
     * @return list of cross references.  will return null if "crossReferences.dbs" attribute
     * not set in property file.
     */
    public List<String> getCrossReferences() {
        return xrefs;
    }

    /**
     * @param taxonId taxonid
     * @return the unique identifier for genes of this organism, eg. primaryIdentifier
     */
    public String getUniqueIdentifier(String taxonId) {
        ConfigEntry entry = entries.get(taxonId);
        if (entry == null) {
            return null;
        }
        return entry.getUniqueIdentifier();
    }

    /**
     * @param taxonId taxonid of the strain
     * @return the taxonId to use
     */
    public String getStrain(String taxonId) {
        return strains.get(taxonId);
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
            String taxonId = attributes[0];

            if ("feature".equals(taxonId)) {
                String[] types = value.split("[, ]+");
                featureTypes.addAll(Arrays.asList(types));
                continue;
            }

            if ("crossReference".equals(taxonId)) {
                String[] types = value.split("[, ]+");
                xrefs.addAll(Arrays.asList(types));
                continue;
            }

            ConfigEntry configEntry = getConfig(taxonId);

            if ("uniqueField".equals(attributes[1])) {
                configEntry.setUniqueIdentifier(value);
            } else if ("gene-designation".equals(attributes[1])) {
                geneDesignation = value;
            } else if ("strain".equals(attributes[1])) {
                configEntry.setStrain(value);
                strains.put(value, taxonId);
            } else if (attributes.length == 3) {
                configEntry.addIdentifier(attributes[1], attributes[2], value);
            } else {
                LOG.error("Problem processing properties '" + PROP_FILE + "' on line "
                        + key + ".  This line has not been processed.");
            }
        }
    }

    private ConfigEntry getConfig(String taxonId) {
        ConfigEntry configEntry = entries.get(taxonId);
        if (configEntry == null) {
            configEntry = new ConfigEntry();
            entries.put(taxonId, configEntry);
        }
        return configEntry;
    }

    /**
     * @param taxonId organism for this gene
     * @return list of fields to be set for genes from this organism
     */
    public Set<String> getGeneIdentifierFields(String taxonId) {
        ConfigEntry configEntry = entries.get(taxonId);
        if (configEntry == null) {
            return null;
        }
        return configEntry.getIdentifierFields();
    }

    /**
     * @param taxonId organism for this gene
     * @param identifier eg primaryIdentifier or secondaryIdentifier
     * @return how to set this identifier, eg datasource or variable
     */
    protected String getIdentifierMethod(String taxonId, String identifier) {
        ConfigEntry configEntry = entries.get(taxonId);
        if (configEntry == null) {
            return null;
        }
        return configEntry.getIdentifierMethod(identifier);
    }

    /**
     * @param taxonId organism for this gene
     * @param identifier eg primaryIdentifier or secondaryIdentifier
     * @return what value to use with method, eg "FlyBase" or "ORF"
     */
    protected String getIdentifierValue(String taxonId, String identifier) {
        ConfigEntry configEntry = entries.get(taxonId);
        if (configEntry == null) {
            return null;
        }
        return configEntry.getIdentifierValue(identifier);
    }

    /**
     * Set the gene designation string.
     *
     * @param geneDesignation string to use to get the gene identifier
     */
    public void setGeneDesignation(String geneDesignation) {
        this.geneDesignation = geneDesignation;
    }

    /**
     * Get the gene designation for this gene.  Default value is "gene designation".  Worm uses
     * "gene ID".
     *
     * @return the gene designation string
     */
    public String getGeneDesignation() {
        return geneDesignation;
    }

    /**
     * class representing an organism in the uniprot config file
     *
     * @author julie sullivan
     */
    public class ConfigEntry
    {
        private String uniqueIdentifier = null;
        private Map<String, IdentifierConfig> identifiers = new HashMap<String, IdentifierConfig>();
        private String strain = null;

        /**
         * eg. primaryIdentifier
         * @return the uniqueIdentifier
         */
        protected String getUniqueIdentifier() {
            return uniqueIdentifier;
        }
        /**
         * @param uniqueIdentifier the uniqueIdentifier to set
         */
        protected void setUniqueIdentifier(String uniqueIdentifier) {
            this.uniqueIdentifier = uniqueIdentifier;
        }

        /**
         * @return the strain
         */
        public String getStrain() {
            return strain;
        }
        /**
         * @param strain the strain to set
         */
        public void setStrain(String strain) {
            this.strain = strain;
        }

        /**
         * example:
         *
         * 6239.primaryIdentifier.dbref = WormBase
         * 6239.secondaryIdentifier.name = ORF
         *
         * [taxonId].[identifier].[type] = [value]
         *
         * @param identifier which identifier to use, eg primaryIdentifier or secondaryIdentifier
         * @param method how to get the identifier value, eg datasource or variable
         * @param value name of datasource or variable to use
         */
        protected void addIdentifier(String identifier, String method, String value) {
            IdentifierConfig identifierConfig = new IdentifierConfig(method, value);
            identifiers.put(identifier, identifierConfig);
        }

        /**
         * which fields in gene object we are setting, eg. primaryIdentifier or secondaryIdentifier
         * @return list of gene identifier fields to be set
         */
        protected Set<String> getIdentifierFields() {
            return identifiers.keySet();
        }

        /**
         * @param identifier eg primaryIdentifier or secondaryIdentifier
         * @return how to set this identifier, eg datasource or variable
         */
        protected String getIdentifierMethod(String identifier) {
            IdentifierConfig identifierType = identifiers.get(identifier);
            if (identifierType == null) {
                return null;
            }
            return identifierType.getMethod();
        }

        /**
        * @param identifier eg primaryIdentifier or secondaryIdentifier
        * @return what value to use with method, eg "FlyBase" or "ORF"
        */
        protected String getIdentifierValue(String identifier) {
            IdentifierConfig identifierType = identifiers.get(identifier);
            if (identifierType == null) {
                return null;
            }
            return identifierType.getValue();
        }
    }

    /**
     * class representing a line in the uniprot config file
     * @author julie sullivan
     */
    public class IdentifierConfig
    {

        private String method;
        private String value;

        /**
         * @param method method used to obtain gene identifiers, eg. datasource/variable
         * @param value which value to use with listed method, eg. "FlyBase", "ORF"
         */
        public IdentifierConfig(String method, String value) {
            this.method = method;
            this.value = value;
        }

        /**
         * @return the method
         */
        public String getMethod() {
            return method;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
    }
}
