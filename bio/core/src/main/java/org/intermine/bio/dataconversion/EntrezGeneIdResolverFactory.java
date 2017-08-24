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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    private static final Logger LOG = Logger.getLogger(EntrezGeneIdResolverFactory.class);
    private String propKey = "resolver.file.rootpath"; // set in .intermine/MINE.properties
    private String resolverFileSymbo = "entrez";

    private String propFile = "entrezIdResolver_config.properties";
    private Map<String, String> configXref = new HashMap<String, String>();
    private Map<String, String> configNonxref = new HashMap<String, String>();
    private Map<String, String> configPrefix = new HashMap<String, String>();
    private Map<String, String> configStrains = new HashMap<String, String>();
    private Set<String> ignoredTaxonIds = new HashSet<String>();

    /**
     * Constructor read pid configuration
     */
    public EntrezGeneIdResolverFactory() {
        this.clsCol = this.defaultClsCol;
        readConfig(propFile);
    }

    /**
     * Return an IdResolver by taxon id, if not already built then create it.
     * @param taxonId string representing taxonomic identifier
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
     * @param taxonIds strings representing taxonomic identifiers
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(Set<String> taxonIds) {
        if (taxonIds == null || taxonIds.isEmpty()) {
            return null;
        }
        return getIdResolver(taxonIds, true);
    }

    /**
     * Return an IdResolver by taxon id, if not already built then create it.  If failOnError
     * set to false then swallow any exceptions and return null.  Allows code to
     * continue if no resolver can be set up.
     * @param failOnError if false swallow any exceptions and return null
     * @param taxonId string representing taxonomic identifier
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
     *
     * @param taxonIds strings representing taxonomic identifiers
     * @param failOnError if false swallow any exceptions and return null
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(Set<String> taxonIds, boolean failOnError) {
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
     * @param taxonId string representing taxonomic identifier
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void createIdResolver(String taxonId) {
        // Don't pass null to asList - java bug (SUN already fixed it???)
        if (taxonId == null) {
            createIdResolver(new HashSet<String>());
        } else {
            createIdResolver(new HashSet<String>(Arrays.asList(taxonId)));
        }
    }

    /**
     * Build an IdResolver from Entrez Gene gene_info file.
     *
     * @param taxonIds list of taxon IDs
     */
    protected void createIdResolver(Set<String> taxonIds) {
        if (taxonIds == null || taxonIds.isEmpty()) {
            LOG.info("Taxon ids can not be null.");
            return;
        }
        taxonIds.removeAll(ignoredTaxonIds);
        LOG.info("Ignore taxons: " + ignoredTaxonIds + ", remain taxons: " + taxonIds);

        if (resolver != null
                && resolver.hasTaxonsAndClassName(taxonIds, this.clsCol
                        .iterator().next())) {
            return;
        }
        if (resolver == null) {
            if (clsCol.size() > 1) { // Not the case, Entrez has gene only
                resolver = new IdResolver();
            } else {
                resolver = new IdResolver(clsCol.iterator().next());
            }
        }

        try {
            boolean isCachedIdResolverRestored = restoreFromFile();
            if (!isCachedIdResolverRestored || (isCachedIdResolverRestored
                    && !resolver.hasTaxonsAndClassName(taxonIds, this.clsCol.iterator().next()))) {
                String resolverFileRoot =
                        PropertiesUtil.getProperties().getProperty(propKey);

                // File path not set in MINE.properties
                if (StringUtils.isBlank(resolverFileRoot)) {
                    String message = "Resolver data file root path is not specified";
                    LOG.warn(message);
                    return;
                }

                LOG.info("Creating id resolver from data file and caching it.");
                String resolverFileName = resolverFileRoot.trim() + "/" + resolverFileSymbo;
                File f = new File(resolverFileName);
                if (f.exists()) {
                    createFromFile(f, taxonIds);
                    resolver.writeToFile(new File(idResolverCachedFileName));
                } else {
                    LOG.warn("Resolver file does not exist: " + resolverFileName);
                }
            } else {
                LOG.info("Using previously cached id resolver file: " + idResolverCachedFileName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populate the ID resolver from a tab delimited file
     *
     * @param taxonIds list of taxon IDs
     * @param f the file
     * @throws IOException if we can't read from the file
     */
    protected void createFromFile(File f, Set<String> taxonIds) throws IOException {
        // in ncbi gene_info, some organisms use strain taxon id, e.g.yeast
        Map<String, String> newTaxonIds = getStrain(taxonIds);
        LOG.info("New taxons: " + newTaxonIds.keySet() + ", original taxons: "
                + newTaxonIds.values());


        NcbiGeneInfoParser parser = new NcbiGeneInfoParser(new BufferedReader(new FileReader(f)),
                new HashSet<String>(newTaxonIds.keySet()));
        Map<String, Set<GeneInfoRecord>> records = parser.getGeneInfoRecords();
        if (records == null) {
            throw new IllegalArgumentException("Failed to read any records from gene_info file.");
        }

        // Some species are not found in gene_info
        if (newTaxonIds.size() > records.size()) {
            Set<String> taxonIdsCopy = new HashSet<String>(newTaxonIds.keySet());
            taxonIdsCopy.removeAll(records.keySet());
            if (taxonIdsCopy.size() > 0) {
                LOG.warn("No records in gene_info file for species: "
                        + taxonIdsCopy);
            }
        }

        for (String newTaxon : records.keySet()) {
            // resolver still uses original taxon
            if (resolver.hasTaxonAndClassName(newTaxonIds.get(newTaxon),
                    this.clsCol.iterator().next())) {
                continue;
            }
            Set<GeneInfoRecord> genes = records.get(newTaxon);
            // use original taxon id in resolver
            // no need to lookup strain in converter
            processGenes(newTaxonIds.get(newTaxon), genes);
        }
    }

    private void processGenes(String taxonId, Set<GeneInfoRecord> genes) {
        for (GeneInfoRecord record : genes) {
            String primaryIdentifier = null;

            if (configXref.containsKey(taxonId)) {
                String config = configXref.get(taxonId);
                if (record.xrefs.get(config) != null) {
                    String prefix = configPrefix.get(taxonId); // eg. RGD:
                    primaryIdentifier = record.xrefs.get(config).iterator().next();
                    if (StringUtils.isNotEmpty(prefix)) {
                        primaryIdentifier = prefix + primaryIdentifier;
                    }
                } else {
                    LOG.info("Gene " + record.entrez + " does not have xref pattern: " + config);
                    continue;
                }
            } else if (configNonxref.containsKey(taxonId)) {
                String config = configNonxref.get(taxonId);
                if (("locusTag").equalsIgnoreCase(config)) {
                    primaryIdentifier = record.locusTag;
                }
            } else {
                primaryIdentifier = record.entrez;
            }

            resolver.addMainIds(taxonId, primaryIdentifier,
                    Collections.singleton(primaryIdentifier));
            resolver.addMainIds(taxonId, primaryIdentifier, record.getMainIds());
            resolver.addSynonyms(taxonId, primaryIdentifier,
                    flattenCollections(record.xrefs.values()));
            resolver.addSynonyms(taxonId, primaryIdentifier, record.synonyms);
        }
    }

    /**
     * Read pid configurations from entrezIdResolver_config.properties in resources dir
     * @param properties name of properties file
     */
    protected void readConfig(String properties) {
        Properties entrezConfig = new Properties();
        try {
            entrezConfig.load(getClass().getClassLoader().getResourceAsStream(properties));
        } catch (IOException e) {
            throw new RuntimeException("I/O Problem loading properties '"
                    + properties + "'", e);
        }

        for (Map.Entry<Object, Object> entry : entrezConfig.entrySet()) {
            if ("taxon.ignored".equals(entry.getKey())) {  // taxon to ignore
                if (entry.getValue() != null || !((String) entry.getValue()).trim().isEmpty()) {
                    String[] ignoredTaxons = ((String) entry.getValue()).trim().split("\\s*,\\s*");
                    ignoredTaxonIds.addAll(Arrays.asList(ignoredTaxons));
                }
            } else if (entry.getKey().toString().contains("strains")) { // use strain
                if (entry.getValue() != null || !((String) entry.getValue()).trim().isEmpty()) {
                    configStrains.put(
                            entry.getKey().toString().split("\\.")[0],
                            ((String) entry.getValue()).trim());
                }

            } else {
                String key = (String) entry.getKey(); // e.g. 10090.primaryIdentifier.xref
                String value = ((String) entry.getValue()).trim(); // e.g. ZFIN
                String[] attributes = key.split("\\.");
                if (attributes.length == 0) {
                    throw new RuntimeException("Problem loading properties '"
                            + properties + "' on line " + key);
                }

                String taxonId = attributes[0];
                if ("xref".equals(attributes[2])) {
                    configXref.put(taxonId, value);
                } else if ("prefix".equals(attributes[2])) {
                    configPrefix.put(taxonId, value);
                } else if ("nonxref".equals(attributes[2])) {
                    configNonxref.put(taxonId, value);
                }
            }
        }
    }

    /**
     * Merge all sets in a collection to a single set
     *
     * @param colOfCols a collection of HashSet
     * @return a set of strings
     */
    private static Set<String> flattenCollections(Collection<Set<String>> colOfCols) {
        Set<String> all = new HashSet<String>();
        for (Set<String> col : colOfCols) {
            all.addAll(col);
        }
        return all;
    }

    /**
     * Get strain taxons.
     *
     * @param taxonIds list of taxon IDs
     * @return a map from taxon ID for a strain to taxon ID for the main organism
     */
    protected Map<String, String> getStrain(Set<String> taxonIds) {
        Map<String, String> newTaxons = new HashMap<String, String>();
        for (String taxon : taxonIds) {
            if (configStrains.containsKey(taxon)) {
                newTaxons.put(configStrains.get(taxon), taxon);
            } else {
                newTaxons.put(taxon, taxon);
            }
        }
        return newTaxons;
    }

    @Override
    protected void createIdResolver() {
        // Not implemented. TaxonId is needed as argument
    }


    /** used for testing
     * @return xrefs from config
     * **/
    protected Map<String, String> getXrefs() {
        return Collections.unmodifiableMap(configXref);
    }

    /** used for testing
     * @return configNonxref from config
     * **/
    protected Map<String, String> getNonXrefs() {
        return configNonxref;
    }

    /** used for testing
     * @return configPrefix from config
     * **/
    protected Map<String, String> getPrefixes() {
        return Collections.unmodifiableMap(configPrefix);
    }

    /** used for testing
     * @return configStrains from config
     * **/
    protected Map<String, String> getStrains() {
        return configStrains;
    }

    /** used for testing
     * @return ignoredTaxonIds
     * **/
    protected Set<String> getIgnoredTaxonIds() {
        return ignoredTaxonIds;
    }
}
