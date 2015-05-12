package org.intermine.api.lucene;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;

/**
 * The search and indexing configuration.
 *
 * This class is the structured version of the data captured in the
 * key word search properties file.
 *
 * @author Alex Kalderimis
 *
 */
public class Configuration
{

    private static final Logger LOG = Logger.getLogger(Configuration.class);

    private static final Set<String> BOOLEAN_PROPS = new HashSet<String>(Arrays.asList(
        "index.temp.directory", "debug", "delete.index"
    ));

    private Map<ClassDescriptor, String[]> specialReferences
        = new HashMap<ClassDescriptor, String[]>();
    private HashSet<Class<? extends InterMineObject>> ignoredClasses
        = new HashSet<Class<? extends InterMineObject>>();
    private HashMap<ClassDescriptor, Float> classBoost
        = new HashMap<ClassDescriptor, Float>();
    private HashMap<Class<? extends InterMineObject>, Set<String>> ignoredFields
        = new HashMap<Class<? extends InterMineObject>, Set<String>>();
    private Vector<KeywordSearchFacetData> facets
        = new Vector<KeywordSearchFacetData>();
    private HashMap<String, String> attributePrefixes = new HashMap<String, String>();
    private boolean debugOutput = true;
    private String tempDirectory;
    private Model model;

    private boolean shouldDelete;

    /**
     * Construct a new set of configuration.
     * @param model The data model
     * @param options The options in raw property form.
     */
    public Configuration(Model model, Properties options) {
        this.model = model;
        parseProperties(options);
    }

    /**
     * @return The special references.
     */
    public Map<ClassDescriptor, String[]> getSpecialReferences() {
        return specialReferences;
    }

    /** @return the ignored classes **/
    public HashSet<Class<? extends InterMineObject>> getIgnoredClasses() {
        return ignoredClasses;
    }

    /** @return the class boosts */
    public HashMap<ClassDescriptor, Float> getClassBoost() {
        return classBoost;
    }

    /** @return the ignored fields **/
    public HashMap<Class<? extends InterMineObject>, Set<String>> getIgnoredFields() {
        return ignoredFields;
    }

    /** @return the facets **/
    public Vector<KeywordSearchFacetData> getFacets() {
        return facets;
    }

    /** @return the attribute prefixes **/
    public HashMap<String, String> getAttributePrefixes() {
        return attributePrefixes;
    }

    /** @return are we in debug mode? **/
    public boolean isDebugOutput() {
        return debugOutput;
    }

    /** @return the configured temp directory location **/
    public String getTempDirectory() {
        return tempDirectory;
    }

    /** @return whether we should delete the index when we are done. **/
    public boolean shouldDelete() {
        return shouldDelete;
    }

    // Private Methods

    private void parseProperties(Properties options) {
        for (Map.Entry<Object, Object> entry : options.entrySet()) {
            String key = (String) entry.getKey();
            String value = ((String) entry.getValue()).trim();
            handleConfiguration(key, value);
        }
        tempDirectory = options.getProperty("index.temp.directory", "");
        debugOutput = Boolean.parseBoolean(options.getProperty("debug", "true"));
        shouldDelete = Boolean.parseBoolean(options.getProperty("delete.index", "true"));
        debugConfiguredState();
    }

    private void debugConfiguredState() {
        LOG.debug("Indexing - Ignored classes:");
        for (Class<? extends InterMineObject> class1 : ignoredClasses) {
            LOG.debug("- " + class1.getSimpleName());
        }

        LOG.info("Indexing - Special References:");
        for (Entry<ClassDescriptor, String[]> specialReference
                : specialReferences.entrySet()) {
            LOG.info("- " + specialReference.getKey().getUnqualifiedName() + " = "
                    + Arrays.toString(specialReference.getValue()));
        }

        LOG.debug("Indexing - Facets:");
        for (KeywordSearchFacetData facet : facets) {
            LOG.debug("- field = " + facet.getField()
                    + ", name = " + facet.getName()
                    + ", type = " + facet.getType());
        }

        LOG.debug("Indexing with and without attribute prefixes:");
        for (String clsAndAttribute : attributePrefixes.keySet()) {
            LOG.debug("- class and attribute: " + clsAndAttribute + " with prefix: "
                        + attributePrefixes.get(clsAndAttribute));
        }

        LOG.debug("Search - Debug mode: " + debugOutput);
    }

    private void handleConfiguration(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key MUST not be null");
        }
        if ("index.ignore".equals(key) && !StringUtils.isBlank(value)) {
            handleIgnore(value);
        } else  if ("index.ignore.fields".equals(key) && !StringUtils.isBlank(value)) {
            handleIgnoreFields(value);
        } else if (key.startsWith("index.references.")) {
            handleIndexReferences(key, value);
        } else if (key.startsWith("index.facet.single.")) {
            handleSingleFacet(key, value);
        } else if (key.startsWith("index.facet.multi.")) {
            handleMultiFacet(key, value);
        } else if (key.startsWith("index.facet.path.")) {
            handlePathFacet(key, value);
        } else if (key.startsWith("index.boost.")) {
            handleBoost(key, value);
        } else if (key.startsWith("index.prefix")) {
            handlePrefix(key, value);
        } else if (!BOOLEAN_PROPS.contains(key)) {
            LOG.error("Cannot handle configuration - bad config?: " + key + " = " + value);
        }
    }

    private void handlePrefix(String key, String value) {
        String classAndAttribute = key.substring("index.prefix.".length());
        addAttributePrefix(classAndAttribute, value);
    }

    private void handleBoost(String key, String value) {
        String classToBoost = key.substring("index.boost.".length());
        ClassDescriptor cld = model.getClassDescriptorByName(classToBoost);
        if (cld != null) {
            classBoost.put(cld, Float.valueOf(value));
        } else {
            LOG.error("keyword_search.properties: classDescriptor for '"
                    + classToBoost + "' not found!");
        }
    }

    private void handlePathFacet(String key, String value) {
        String facetName = key.substring("index.facet.path.".length());
        String[] facetFields = value.split(" ");
        facets.add(new KeywordSearchFacetData(facetFields, facetName,
                KeywordSearchFacetType.PATH));
    }

    private void handleMultiFacet(String key, String value) {
        String facetName = key.substring("index.facet.multi.".length());
        String facetField = value;
        facets.add(new KeywordSearchFacetData(facetField, facetName,
                KeywordSearchFacetType.MULTI));
    }

    private void handleSingleFacet(String key, String value) {
        String facetName = key.substring("index.facet.single.".length());
        String facetField = value;
        facets.add(new KeywordSearchFacetData(facetField, facetName,
                KeywordSearchFacetType.SINGLE));
    }

    private void handleIndexReferences(String key, String value) {
        String classToIndex = key.substring("index.references.".length());
        ClassDescriptor cld = model.getClassDescriptorByName(classToIndex);
        if (cld != null) {
            // special fields (references to follow) come as a space-separated list
            // eg: index.references.Employee = department address
            String[] specialFields;
            if (!StringUtils.isBlank(value)) {
                specialFields = value.split("\\s+");
            } else {
                specialFields = null;
            }

            specialReferences.put(cld, specialFields);
        } else {
            LOG.error("keyword_search.properties: classDescriptor for '"
                    + classToIndex + "' not found!");
        }
    }

    private void handleIgnoreFields(String value) {
        String[] ignoredPaths = value.split("\\s+");

        for (String ignoredPath : ignoredPaths) {
            if (StringUtils.countMatches(ignoredPath, ".") != 1) {
                LOG.error("Fields to ignore specified by 'index.ignore.fields'"
                        + " should contain Class.field, e.g. Company.name");
            } else {
                String clsName = ignoredPath.split("\\.")[0];
                String fieldName = ignoredPath.split("\\.")[1];

                ClassDescriptor cld =
                    model.getClassDescriptorByName(clsName);
                if (cld != null) {
                    FieldDescriptor fld = cld.getFieldDescriptorByName(fieldName);
                    if (fld != null) {
                        ignoreField(cld, fieldName);
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
    }

    private void handleIgnore(String value) {
        String[] ignoreClassNames = value.split("\\s+");

        for (String className : ignoreClassNames) {
            ClassDescriptor cld = model.getClassDescriptorByName(className);

            if (cld == null) {
                LOG.error("Unknown class in config file: " + className);
            } else {
                ignoreClass(cld);
            }
        }
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

    private void ignoreField(ClassDescriptor cld, String fieldName) {
        addToIgnoredFields(ignoredFields, cld, fieldName);
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

    private void ignoreClass(ClassDescriptor cld) {
        addCldToIgnored(ignoredClasses, cld);
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
