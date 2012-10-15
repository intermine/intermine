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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.util.PropertiesUtil;

/**
 * ID resolver for Entrez genes.
 *
 * @author Richard Smith
 * @author Fengyuan Hu
 */
public class EntrezGeneIdResolverFactory extends IdResolverFactory
{
    protected static final Logger LOG = Logger.getLogger(EntrezGeneIdResolverFactory.class);
    private final String propName = "resolver.entrez.file"; // set in .intermine/MINE.properties

    private static final String PROP_FILE = "entrezIdResolver_config.properties";
    private Properties props = new Properties();
    private Map<String, String> config_xref = new HashMap<String, String>();
    private Map<String, String> config_prefix = new HashMap<String, String>();

    /**
     * Constructor read pid configuration
     */
    public EntrezGeneIdResolverFactory() {
        readConfig();
    }
    /**
     * Return an IdResolver by taxon id, if not already built then create it.
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(String taxonId) {
        if (taxonId == null) {
            return null;
        }
        return getIdResolver(taxonId, true);
    }

    /**
     * Return an IdResolver by a list of taxon id, if not already built then create it.
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(Collection<String> taxonIds) {
        if (taxonIds == null | taxonIds.isEmpty()) {
            return null;
        }
        return getIdResolver(taxonIds, true);
    }

    /**
     * Return an IdResolver by taxon id, if not already built then create it.  If failOnError
     * set to false then swallow any exceptions and return null.  Allows code to
     * continue if no resolver can be set up.
     * @param failOnError if false swallow any exceptions and return null
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(String taxonId, boolean failOnError) {
        if (!caughtError) {
            try {
                createIdResolver(taxonId);
            } catch (Exception e) {
                this.caughtError = true;
                if (failOnError) {
                    throw new RuntimeException(e);
                }
            }
        }
        return resolver;
    }

    /**
     * Return an IdResolver by a list of taxon ids, if not already built then create it.
     * If failOnError set to false then swallow any exceptions and return null.  Allows code to
     * continue if no resolver can be set up.
     * @param failOnError if false swallow any exceptions and return null
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(Collection<String> taxonIds, boolean failOnError) {
        if (!caughtError) {
            try {
                createIdResolver(taxonIds);
            } catch (Exception e) {
                this.caughtError = true;
                if (failOnError) {
                    throw new RuntimeException(e);
                }
            }
        }
        return resolver;
    }

    /**
     * Build an IdResolver from Entrez Gene gene_info file
     * @return an IdResolver for Entrez Gene
     */
    protected void createIdResolver(String taxonId) {
        // Don't pass null to asList - java bug (SUN already fixed it???)
        if (taxonId == null) {
            createIdResolver(new HashSet<String>());
        } else {
            createIdResolver(Arrays.asList(taxonId));
        }
    }

    /**
     * Build an IdResolver from Entrez Gene gene_info file
     * @return an IdResolver for Entrez Gene
     */
    protected void createIdResolver(Collection<String> taxonIds) {

        if (resolver == null) {
            resolver = new IdResolver(clsName);
        }

        Properties props = PropertiesUtil.getProperties();
        String fileName = props.getProperty(propName);

        // File path not set in MINE.properties
        if (StringUtils.isBlank(fileName)) {
            String message = "Entrez gene resolver has no file name specified, set " + propName
                + " to the location of the gene_info file.";
            LOG.warn(message);
            return;
        }

        BufferedReader reader;
        try {
            FileReader fr = new FileReader(new File(fileName));
            reader = new BufferedReader(fr);
            createFromFile(reader, taxonIds);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Failed to open gene_info file: "
                    + fileName, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading from gene_info file: "
                    + fileName, e);
        }
    }


    @Override
    // Not implemented. TaxonId is needed as argument
    protected void createIdResolver() {
    }

    private void createFromFile(BufferedReader reader,
            Collection<String> taxonIds) throws IOException {

        NcbiGeneInfoParser parser = new NcbiGeneInfoParser(reader,
                new HashSet<String>(taxonIds));
        Map<String, Set<GeneInfoRecord>> records = parser.getGeneInfoRecords();
        if (records == null) {
            throw new IllegalArgumentException("Failed to read any records from gene_info file.");
        }

        for (String taxonId : taxonIds) {
            if (!records.containsKey(taxonId)) {
                LOG.warn("No records in gene_info file for taxon: "
                        + taxonId);
            }

            if (resolver.hasTaxon(taxonId)) {
                continue;
            }

            for (GeneInfoRecord record : records.get(taxonId)) {
                // primaryIdentifier set according to configuration
                String primaryIdentifier;
                if (record.xrefs.get(config_xref.get(taxonId)) != null) {
                    primaryIdentifier =
                            (config_prefix.get(taxonId) != null ? config_prefix
                                    .get(taxonId) : "")
                                    + record.xrefs.get(config_xref.get(taxonId))
                                            .iterator().next();
                } else {
                    primaryIdentifier = record.entrez;
                }

                resolver.addMainIds(taxonId, primaryIdentifier, record.getMainIds());
                resolver.addSynonyms(taxonId, primaryIdentifier,
                        flattenCollections(record.xrefs.values()));
                resolver.addSynonyms(taxonId, primaryIdentifier, record.synonyms);
            }
        }
    }

//    private Set<String> lowerCase(Set<String> input) {
//        Set<String> lower = new HashSet<String>();
//        for (String s : input) {
//            lower.add(s.toLowerCase());
//        }
//        return lower;
//    }

    /**
     * Read pid configurations from entrezIdResolver_config.properties in resources dir
     */
    private void readConfig() {
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(
                    PROP_FILE));
        } catch (IOException e) {
            throw new RuntimeException("I/O Problem loading properties '"
                    + PROP_FILE + "'", e);
        }

        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey(); // e.g. 10090.xref
            String value = ((String) entry.getValue()).trim(); // e.g. ZFIN

            String[] attributes = key.split("\\.");
            if (attributes.length == 0) {
                throw new RuntimeException("Problem loading properties '"
                        + PROP_FILE + "' on line " + key);
            }

            String taxonId = attributes[0];
            if ("xref".equals(attributes[1])) {
                config_xref.put(taxonId, value);
            } else if ("prefix".equals(attributes[1])) {
                config_prefix.put(taxonId, value);
            }

        }
    }

    /**
     * Merge all sets in a collection to a single set
     * @param colOfCols a collection of HashSet
     * @return a set of strings
     */
    private Set<String> flattenCollections(Collection<Set<String>> colOfCols) {
        Set<String> all = new HashSet<String>();
        for (Set<String> col : colOfCols) {
            all.addAll(col);
        }
        return all;
    }
}
