package org.intermine.api.searchengine.solr;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.intermine.api.lucene.KeywordSearch;
import org.intermine.api.lucene.KeywordSearchFacetData;
import org.intermine.api.lucene.KeywordSearchFacetType;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.ObjectPipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Solr Implementation of IndexHandler
 *
 * @author arunans23
 */
public final class SolrIndexHandler
{
    private static final Logger LOG = Logger.getLogger(SolrIndexHandler.class);

    private static final String LUCENE_INDEX_DIR = "keyword_search_index";

    /**
     * maximum number of hits returned
     */
    public static final int MAX_HITS = 500;

    /**
     * maximum number of items to be displayed on a page
     */
    public static final int PER_PAGE = 100;

    private static ObjectPipe<SolrInputDocument> indexingQueue = new ObjectPipe<SolrInputDocument>(100000);

    private static Properties properties = null;
    private static String tempDirectory = null;
    private static Map<Class<? extends InterMineObject>, String[]> specialReferences;
    private static Set<Class<? extends InterMineObject>> ignoredClasses;
    private static Map<Class<? extends InterMineObject>, Set<String>> ignoredFields;
    private static Map<ClassDescriptor, Float> classBoost;
    private static Vector<KeywordSearchFacetData> facets;
    private static boolean debugOutput;
    private static Map<String, String> attributePrefixes = null;

    private SolrIndexHandler() {

    }

    private static synchronized void parseProperties(ObjectStore os) {
        if (properties != null) {
            return;
        }

        specialReferences = new HashMap<Class<? extends InterMineObject>, String[]>();
        ignoredClasses = new HashSet<Class<? extends InterMineObject>>();
        classBoost = new HashMap<ClassDescriptor, Float>();
        ignoredFields = new HashMap<Class<? extends InterMineObject>, Set<String>>();
        facets = new Vector<KeywordSearchFacetData>();
        debugOutput = true;

        // load config file to figure out special classes
        String configFileName = "keyword_search.properties";
        ClassLoader classLoader = KeywordSearch.class.getClassLoader();
        InputStream configStream = classLoader.getResourceAsStream(configFileName);
        if (configStream != null) {
            properties = new Properties();
            try {
                properties.load(configStream);

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = (String) entry.getKey();
                    String value = ((String) entry.getValue()).trim();

                    if ("index.ignore".equals(key) && !StringUtils.isBlank(value)) {
                        String[] ignoreClassNames = value.split("\\s+");

                        for (String className : ignoreClassNames) {
                            ClassDescriptor cld = os.getModel().getClassDescriptorByName(className);

                            if (cld == null) {
                                LOG.error("Unknown class in config file: " + className);
                            } else {
                                addCldToIgnored(ignoredClasses, cld);
                            }
                        }
                    } else  if ("index.ignore.fields".equals(key) && !StringUtils.isBlank(value)) {
                        String[] ignoredPaths = value.split("\\s+");

                        for (String ignoredPath : ignoredPaths) {
                            if (StringUtils.countMatches(ignoredPath, ".") != 1) {
                                LOG.error("Fields to ignore specified by 'index.ignore.fields'"
                                        + " should contain Class.field, e.g. Company.name");
                            } else {
                                String clsName = ignoredPath.split("\\.")[0];
                                String fieldName = ignoredPath.split("\\.")[1];

                                ClassDescriptor cld =
                                        os.getModel().getClassDescriptorByName(clsName);
                                if (cld != null) {
                                    FieldDescriptor fld = cld.getFieldDescriptorByName(fieldName);
                                    if (fld != null) {
                                        addToIgnoredFields(ignoredFields, cld, fieldName);
                                    } else {
                                        LOG.error("Field name '" + fieldName + "' not found for"
                                                + " class '" + clsName + "' specified in"
                                                + "'index.ignore.fields'");
                                    }
                                } else {
                                    LOG.error("Class name specified in 'index.ignore.fields'"
                                            + " not found: " + clsName);
                                }
                            }
                        }
                    } else if (key.startsWith("index.references.")) {
                        String classToIndex = key.substring("index.references.".length());
                        ClassDescriptor cld = os.getModel().getClassDescriptorByName(classToIndex);
                        if (cld != null) {
                            Class<? extends InterMineObject> cls =
                                    (Class<? extends InterMineObject>) cld.getType();

                            // special fields (references to follow) come as
                            // a
                            // space-separated list
                            String[] specialFields;
                            if (!StringUtils.isBlank(value)) {
                                specialFields = value.split("\\s+");
                            } else {
                                specialFields = null;
                            }

                            specialReferences.put(cls, specialFields);
                        } else {
                            LOG.error("keyword_search.properties: classDescriptor for '"
                                    + classToIndex + "' not found!");
                        }
                    } else if (key.startsWith("index.facet.single.")) {
                        String facetName = key.substring("index.facet.single.".length());
                        String facetField = value;
                        facets.add(new KeywordSearchFacetData(facetField, facetName,
                                KeywordSearchFacetType.SINGLE));
                    } else if (key.startsWith("index.facet.multi.")) {
                        String facetName = key.substring("index.facet.multi.".length());
                        String facetField = value;
                        facets.add(new KeywordSearchFacetData(facetField, facetName,
                                KeywordSearchFacetType.MULTI));
                    } else if (key.startsWith("index.facet.path.")) {
                        String facetName = key.substring("index.facet.path.".length());
                        String[] facetFields = value.split(" ");
                        facets.add(new KeywordSearchFacetData(facetFields, facetName,
                                KeywordSearchFacetType.PATH));
                    } else if (key.startsWith("index.boost.")) {
                        String classToBoost = key.substring("index.boost.".length());
                        ClassDescriptor cld = os.getModel().getClassDescriptorByName(classToBoost);
                        if (cld != null) {
                            classBoost.put(cld, Float.valueOf(value));
                        } else {
                            LOG.error("keyword_search.properties: classDescriptor for '"
                                    + classToBoost + "' not found!");
                        }
                    } else if (key.startsWith("index.prefix")) {
                        String classAndAttribute = key.substring("index.prefix.".length());
                        addAttributePrefix(classAndAttribute, value);
                    } else if ("search.debug".equals(key) && !StringUtils.isBlank(value)) {
                        debugOutput =
                                "1".equals(value) || "true".equals(value.toLowerCase())
                                        || "on".equals(value.toLowerCase());
                    }

                    tempDirectory = properties.getProperty("index.temp.directory", "");
                }
            } catch (IOException e) {
                LOG.error("keyword_search.properties: errow while loading file '" + configFileName
                        + "'", e);
            }
        } else {
            LOG.error("keyword_search.properties: file '" + configFileName + "' not found!");
        }

        LOG.debug("Indexing - Ignored classes:");
        for (Class<? extends InterMineObject> class1 : ignoredClasses) {
            LOG.debug("- " + class1.getSimpleName());
        }

        LOG.debug("Indexing - Special References:");
        for (Map.Entry<Class<? extends InterMineObject>, String[]> specialReference : specialReferences
                .entrySet()) {
            LOG.debug("- " + specialReference.getKey() + " = "
                    + Arrays.toString(specialReference.getValue()));
        }

        LOG.debug("Indexing - Facets:");
        for (KeywordSearchFacetData facet : facets) {
            LOG.debug("- field = " + facet.getField() + ", name = " + facet.getName() + ", type = "
                    + facet.getType().toString());
        }

        LOG.debug("Indexing with and without attribute prefixes:");
        if (attributePrefixes != null) {
            for (String clsAndAttribute : attributePrefixes.keySet()) {
                LOG.debug("- class and attribute: " + clsAndAttribute + " with prefix: "
                        + attributePrefixes.get(clsAndAttribute));
            }
        }

        LOG.info("Search - Debug mode: " + debugOutput);
        LOG.info("Indexing - Temp Dir: " + tempDirectory);
    }

    private static void addAttributePrefix(String classAndAttribute, String prefix) {
        if (StringUtils.isBlank(classAndAttribute) || classAndAttribute.indexOf(".") == -1
                || StringUtils.isBlank(prefix)) {
            LOG.warn("Invalid search.prefix configuration: '" + classAndAttribute + "' = '"
                    + prefix + "'. Should be className.attributeName = prefix.");
        } else {
            if (attributePrefixes == null) {
                attributePrefixes = new HashMap<String, String>();
            }
            attributePrefixes.put(classAndAttribute, prefix);
        }
    }

    public static void createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys)
            throws IOException {
        long time = System.currentTimeMillis();
        LOG.debug("Creating keyword search index...");

        String solrUrlString = "http://localhost:8983/solr/intermine";
        SolrClient solrClient = new HttpSolrClient.Builder(solrUrlString).build();

        parseProperties(os);

        LOG.info("Starting fetcher thread...");
        SolrObjectHandler fetchThread =
                new SolrObjectHandler(os, classKeys, indexingQueue, ignoredClasses,
                        ignoredFields, specialReferences, classBoost, facets, attributePrefixes);
        fetchThread.start();

        int indexed = 0;

        List<SolrInputDocument> solrInputDocuments = new ArrayList<SolrInputDocument>();

        // loop and index while we still have fetchers running
        LOG.debug("Starting to index...");
        while (indexingQueue.hasNext()) {
            SolrInputDocument doc = indexingQueue.next();

            solrInputDocuments.add(doc);

            indexed++;

            if (indexed % 10000 == 1) {
                LOG.info("docs indexed=" + indexed + "; thread state="
                        + fetchThread.getState() + "; docs/ms=" + indexed * 1.0F
                        / (System.currentTimeMillis() - time) + "; memory="
                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                        + Runtime.getRuntime().maxMemory() / 1024 + "k" + "; time="
                        + (System.currentTimeMillis() - time) + "ms");
            }


        }

        try {
            UpdateResponse response = solrClient.add(solrInputDocuments);

            solrClient.commit();
        } catch (SolrServerException e) {

            LOG.error("Error while commiting the solrinputdocuments to the solrclient.", e);

            e.printStackTrace();
        }



        if (fetchThread.getException() != null) {
            try {

            } catch (Exception e) {
                LOG.error("Error closing writer while handling exception.", e);
            }
            throw new RuntimeException("Indexing failed.", fetchThread.getException());
        }
//        index.getFieldNames().addAll(fetchThread.getFieldNames());
//        LOG.debug("Indexing done, optimizing index files...");
        try {

        } catch (Exception e) {
            LOG.error("IOException while optimizing and closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info("Indexing of " + indexed + " documents finished in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                time % 1000) + " minutes");
    }

    private static void addToIgnoredFields(
            Map<Class<? extends InterMineObject>, Set<String>> ignoredFieldMap, ClassDescriptor cld,
            String fieldName) {
        if (cld == null) {
            LOG.error("ClassDesriptor was null when attempting to add an ignored field.");
        } else if (InterMineObject.class.isAssignableFrom(cld.getType())) {
            Set<ClassDescriptor> clds = new HashSet<ClassDescriptor>();
            clds.add(cld);
            for (ClassDescriptor subCld : cld.getSubDescriptors()) {
                clds.add(subCld);
            }

            for (ClassDescriptor ignoreCld : clds) {
                Set<String> fields = ignoredFieldMap.get(ignoreCld.getType());
                @SuppressWarnings("unchecked")
                Class<? extends InterMineObject> cls =
                        (Class<? extends InterMineObject>) ignoreCld.getType();
                if (fields == null) {
                    fields = new HashSet<String>();
                    ignoredFieldMap.put(cls, fields);
                }
                fields.add(fieldName);
            }
        } else {
            LOG.error("cld " + cld + " is not IMO!");
        }
    }

    /**
     * recurse into class descriptor and add all subclasses to ignoredClasses
     * @param ignoredClassMap
     *            set of classes
     * @param cld
     *            super class descriptor
     */
    @SuppressWarnings("unchecked")
    private static void addCldToIgnored(Set<Class<? extends InterMineObject>> ignoredClassMap,
                                        ClassDescriptor cld) {
        if (cld == null) {
            LOG.error("cld is null!");
        } else if (InterMineObject.class.isAssignableFrom(cld.getType())) {
            ignoredClassMap.add((Class<? extends InterMineObject>) cld.getType());

            for (ClassDescriptor subCld : cld.getSubDescriptors()) {
                addCldToIgnored(ignoredClassMap, subCld);
            }
        } else {
            LOG.error("cld " + cld + " is not IMO!");
        }
    }
}
