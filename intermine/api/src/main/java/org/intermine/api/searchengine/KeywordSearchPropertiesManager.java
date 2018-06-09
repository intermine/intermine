package org.intermine.api.searchengine;

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
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * A manager class to handle all the configuration properties from keyword_search.properties file
 */
public class KeywordSearchPropertiesManager {

    private static final Logger LOG = Logger.getLogger(KeywordSearchPropertiesManager.class);

    private static KeywordSearchPropertiesManager keywordSearchPropertiesManager;

    /**
     * maximum number of items to be displayed on a page
     */
    public static final int PER_PAGE = 100;

    private Properties properties = null;

    private String tempDirectory = null;
    private Map<Class<? extends InterMineObject>, String[]> specialReferences;
    private Set<Class<? extends InterMineObject>> ignoredClasses;
    private Map<Class<? extends InterMineObject>, Set<String>> ignoredFields;
    private Map<ClassDescriptor, Float> classBoost;
    private Vector<KeywordSearchFacetData> facets;
    private boolean debugOutput;
    private Map<String, String> attributePrefixes = null;

    private String solrUrl;

    private KeywordSearchPropertiesManager(ObjectStore objectStore){

        parseProperties(objectStore);

    }

    public static KeywordSearchPropertiesManager getInstance(ObjectStore objectStore){
        if (keywordSearchPropertiesManager == null){
            keywordSearchPropertiesManager = new KeywordSearchPropertiesManager(objectStore);
        }
        return keywordSearchPropertiesManager;
    }

    private synchronized void parseProperties(ObjectStore os) {
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
        ClassLoader classLoader = KeywordSearchPropertiesManager.class.getClassLoader();
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
                    } else if ("index.solrurl".equals(key) && !StringUtils.isBlank(value)) {
                        solrUrl = value;
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

    private void addAttributePrefix(String classAndAttribute, String prefix) {
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

    /**
     * recurse into class descriptor and add all subclasses to ignoredClasses
     * @param ignoredClassMap
     *            set of classes
     * @param cld
     *            super class descriptor
     */
    @SuppressWarnings("unchecked")
    private void addCldToIgnored(Set<Class<? extends InterMineObject>> ignoredClassMap,
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

    public Properties getProperties() {
        return properties;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public Map<Class<? extends InterMineObject>, String[]> getSpecialReferences() {
        return specialReferences;
    }

    public Set<Class<? extends InterMineObject>> getIgnoredClasses() {
        return ignoredClasses;
    }

    public Map<Class<? extends InterMineObject>, Set<String>> getIgnoredFields() {
        return ignoredFields;
    }

    public Map<ClassDescriptor, Float> getClassBoost() {
        return classBoost;
    }

    public Vector<KeywordSearchFacetData> getFacets() {
        return facets;
    }

    public boolean isDebugOutput() {
        return debugOutput;
    }

    public Map<String, String> getAttributePrefixes() {
        return attributePrefixes;
    }

    public String getSolrUrl() {
        return solrUrl;
    }
}
