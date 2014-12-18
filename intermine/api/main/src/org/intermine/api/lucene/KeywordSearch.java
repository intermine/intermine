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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.intermine.api.InterMineAPI;
import org.intermine.api.data.Objects;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.modelproduction.MetadataManager.LargeObjectOutputStream;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.util.ObjectPipe;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.PathFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;

/**
 * Allows for full-text searches over all metadata using the apache lucene
 * engine.
 *
 * Main entry point: contains methods for creating indices, restoring saved indices and
 * running searches over them.
 *
 * @author nils
 */

public final class KeywordSearch
{
    private static final String LUCENE_INDEX_DIR = "keyword_search_index";

    /**
     * maximum number of hits returned
     */
    public static final int MAX_HITS = 500;

    /**
     * maximum number of items to be displayed on a page
     */
    public static final int PER_PAGE = 100;

    private static final Logger LOG = Logger.getLogger(KeywordSearch.class);

    private static IndexReader reader = null;
    private static BoboIndexReader boboIndexReader = null;
    private static ObjectPipe<Document> indexingQueue = new ObjectPipe<Document>(100000);
    private static LuceneIndexContainer index = null;

    private static Properties properties = null;
    private static String tempDirectory = null;
    private static Map<Class<? extends InterMineObject>, String[]> specialReferences;
    private static Set<Class<? extends InterMineObject>> ignoredClasses;
    private static Map<Class<? extends InterMineObject>, Set<String>> ignoredFields;
    private static Map<ClassDescriptor, Float> classBoost;
    private static Vector<KeywordSearchFacetData> facets;
    private static boolean debugOutput;
    private static Map<String, String> attributePrefixes = null;

    private KeywordSearch() {
        //don't
    }

    @SuppressWarnings("unchecked")
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
        for (Entry<Class<? extends InterMineObject>, String[]> specialReference : specialReferences
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

    /**
     * loads or creates the lucene index
     * @param im API for accessing object store
     * @param path path to store the fsdirectory in
     */
    public static synchronized void initKeywordSearch(InterMineAPI im, String path) {
        try {

            if (index == null) {
                // try to load index from database first
                index = loadIndexFromDatabase(im.getObjectStore(), path);
            }

            if (index == null) {
                LOG.error("lucene index missing!");
                return;
            }

            if (properties == null) {
                parseProperties(im.getObjectStore());
            }

            if (reader == null) {
                reader = IndexReader.open(index.getDirectory(), true);
            }

            if (boboIndexReader == null) {
                // prepare faceting
                HashSet<FacetHandler<?>> facetHandlers = new HashSet<FacetHandler<?>>();
                facetHandlers.add(new SimpleFacetHandler("Category"));
                for (KeywordSearchFacetData facet : facets) {
                    if (facet.getType().equals(KeywordSearchFacetType.MULTI)) {
                        facetHandlers.add(new MultiValueFacetHandler(facet.getField()));
                    } else if (facet.getType().equals(KeywordSearchFacetType.PATH)) {
                        facetHandlers.add(new PathFacetHandler("path_"
                                + facet.getName().toLowerCase()));
                    } else {
                        facetHandlers.add(new SimpleFacetHandler(facet.getField()));
                    }
                }

                boboIndexReader = BoboIndexReader.getInstance(reader, facetHandlers);

                LOG.debug("Fields:"
                        + Arrays.toString(boboIndexReader.getFieldNames(FieldOption.ALL)
                                .toArray()));
                LOG.debug("Indexed fields:"
                        + Arrays.toString(boboIndexReader.getFieldNames(FieldOption.INDEXED)
                                .toArray()));
            }
        } catch (CorruptIndexException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void writeObjectToDB(ObjectStore os, String key, Object object)
        throws IOException, SQLException {
        LOG.debug("Saving stream to database...");
        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();

        LargeObjectOutputStream streamOut = null;
        GZIPOutputStream gzipStream = null;
        ObjectOutputStream objectStream = null;

        try {
            streamOut = MetadataManager.storeLargeBinary(db, key);
            gzipStream = new GZIPOutputStream(new BufferedOutputStream(streamOut));
            objectStream = new ObjectOutputStream(gzipStream);

            LOG.debug("GZipping and serializing object...");
            objectStream.writeObject(object);
        } finally {
            if (objectStream != null) {
                objectStream.flush();
                objectStream.close();
            }
            if (gzipStream != null) {
                gzipStream.finish();
                gzipStream.flush();
                gzipStream.close();
            }
            if (streamOut != null) {
                streamOut.close();
            }
        }

    }

    /**
     * writes index and associated directory to the database using the metadatamanager.
     *
     * @param os intermine objectstore
     * @param classKeys map of classname to key field descriptors (from InterMineAPI)
     */
    public static void saveIndexToDatabase(ObjectStore os,
            Map<String, List<FieldDescriptor>> classKeys) {
        try {
            if (index == null) {
                createIndex(os, classKeys);
            }

            LOG.debug("Deleting previous search index dirctory blob from db...");
            long startTime = System.currentTimeMillis();
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            boolean blobExisted = MetadataManager.deleteLargeBinary(db,
                    MetadataManager.SEARCH_INDEX);
            if (blobExisted) {
                LOG.debug("Deleting previous search index blob from db took: "
                        + (System.currentTimeMillis() - startTime) + ".");
            } else {
                LOG.debug("No previous search index blob found in db");
            }

            LOG.debug("Saving search index information to database...");
            writeObjectToDB(os, MetadataManager.SEARCH_INDEX, index);
            LOG.debug("Successfully saved search index information to database.");

            // if we have a FSDirectory we need to zip and save that separately
            if ("FSDirectory".equals(index.getDirectoryType())) {
                ZipOutputStream zipOut = null;
                final int bufferSize = 2048;

                try {
                    LOG.debug("Zipping up FSDirectory...");

                    LOG.debug("Deleting previous search index dirctory blob from db...");
                    startTime = System.currentTimeMillis();
                    blobExisted = MetadataManager.deleteLargeBinary(db,
                            MetadataManager.SEARCH_INDEX_DIRECTORY);
                    if (blobExisted) {
                        LOG.debug("Deleting previous search index directory blob from db took: "
                                + (System.currentTimeMillis() - startTime) + ".");
                    } else {
                        LOG.debug("No previous search index directory blob found in db");
                    }
                    LargeObjectOutputStream streamOut =
                            MetadataManager.storeLargeBinary(db,
                                    MetadataManager.SEARCH_INDEX_DIRECTORY);

                    zipOut = new ZipOutputStream(streamOut);

                    byte[] data = new byte[bufferSize];

                    // get a list of files from current directory
                    File dir = ((FSDirectory) index.getDirectory()).getFile();
                    String[] files = dir.list();

                    for (int i = 0; i < files.length; i++) {
                        File file = new File(dir.getAbsolutePath() + File.separator + files[i]);
                        LOG.debug("Getting length of file: " + file.getName());
                        long fileLength = file.length();
                        LOG.debug("Zipping file: " + file.getName() + " (" + file.length() / 1024
                                / 1024 + " MB)");

                        FileInputStream fi = new FileInputStream(file);
                        BufferedInputStream fileInput = new BufferedInputStream(fi, bufferSize);

                        try {
                            ZipEntry entry = new ZipEntry(files[i]);
                            zipOut.putNextEntry(entry);

                            long total = fileLength / bufferSize;
                            long progress = 0;

                            int count;
                            while ((count = fileInput.read(data, 0, bufferSize)) != -1) {
                                zipOut.write(data, 0, count);
                                progress++;
                                if (progress % 1000 == 0) {
                                    LOG.debug("Written " + progress + " of " + total
                                            + " batches for file: " + file.getName());
                                }
                            }
                        } finally {
                            LOG.debug("Closing file: " + file.getName() + "...");
                            fileInput.close();
                        }
                        LOG.debug("Finished storing file: " + file.getName());
                    }
                } catch (IOException e) {
                    LOG.error("Error storing index", e);
                } finally {
                    if (zipOut != null) {
                        zipOut.close();
                    }
                }
            } else if ("RAMDirectory".equals(index.getDirectoryType())) {
                LOG.debug("Saving RAM directory to database...");
                writeObjectToDB(os, MetadataManager.SEARCH_INDEX_DIRECTORY, index.getDirectory());
                LOG.debug("Successfully saved RAM directory to database.");
            }
        } catch (IOException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        } catch (SQLException e) {
            LOG.error(null, e);
            throw new RuntimeException("Index creation failed: ", e);
        }
    }

    /**
     * perform a keyword search over all document metadata fields with lucene
     * @param searchString
     *            string to search for
     * @return map of document IDs with their respective scores
     * @deprecated Use runBrowseSearch instead.
     */
    @Deprecated
    public static Map<Integer, Float> runLuceneSearch(String searchString) {
        LinkedHashMap<Integer, Float> matches = new LinkedHashMap<Integer, Float>();

        String queryString = parseQueryString(searchString);

        long time = System.currentTimeMillis();

        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher(reader);

            Analyzer analyzer = new WhitespaceAnalyzer();
            org.apache.lucene.search.Query query;

            // pass entire list of field names to the multi-field parser
            // => search through all fields
            String[] fieldNamesArray = new String[index.getFieldNames().size()];
            index.getFieldNames().toArray(fieldNamesArray);
            QueryParser queryParser =
                    new MultiFieldQueryParser(Version.LUCENE_30, fieldNamesArray, analyzer, index
                            .getFieldBoosts());
            query = queryParser.parse(queryString);

            // required to expand search terms
            query = query.rewrite(reader);
            LOG.debug("Actual query: " + query);

            TopDocs topDocs = searcher.search(query, 500);
            // Filter filter = new TermsFilter();
            // searcher.search(query, filter, collector);

            LOG.debug("Found " + topDocs.totalHits + " document(s) that matched query '"
                    + queryString + "'");

            for (int i = 0; (i < MAX_HITS && i < topDocs.totalHits); i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                Integer id = Integer.valueOf(doc.get("id"));

                matches.put(id, new Float(topDocs.scoreDocs[i].score));
            }
        } catch (ParseException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        } catch (IOException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        } finally {
            try {
                if (searcher != null) {
                    searcher.close();
                }
            } catch (IOException e) {
                LOG.warn("Error closing searcher", e);
            }
        }

        LOG.info("Lucene search finished in " + (System.currentTimeMillis() - time) + " ms");

        return matches;
    }

    /**
     * @param result search result
     * @param facetVector facets for search results
     * @param facetValues values for facets
     * @return search result for given facet
     */
    public static Vector<KeywordSearchFacet> parseFacets(BrowseResult result,
            Vector<KeywordSearchFacetData> facetVector, Map<String, String> facetValues) {
        long time = System.currentTimeMillis();
        Vector<KeywordSearchFacet> searchResultsFacets = new Vector<KeywordSearchFacet>();
        for (KeywordSearchFacetData facet : facetVector) {
            FacetAccessible boboFacet = result.getFacetMap().get(facet.getField());
            if (boboFacet != null) {
                searchResultsFacets.add(new KeywordSearchFacet(facet.getField(), facet
                        .getName(), facetValues.get(facet.getField()), boboFacet
                        .getFacets()));
            }
        }
        LOG.debug("Parsing " + searchResultsFacets.size() + " facets took "
                + (System.currentTimeMillis() - time) + " ms");
        return searchResultsFacets;
    }

    /**
     * @param browseHits search results
     * @param objMap object map
     * @return matching object
     */
    public static Vector<KeywordSearchHit> getSearchHits(BrowseHit[] browseHits,
            Map<Integer, InterMineObject> objMap) {
        long time = System.currentTimeMillis();
        Vector<KeywordSearchHit> searchHits = new Vector<KeywordSearchHit>();
        for (BrowseHit browseHit : browseHits) {
            try {
                Document doc = browseHit.getStoredFields();
                if (doc == null) {
                    LOG.error("doc is null for browseHit " + browseHit);
                } else {
                    Integer id          = Integer.valueOf(doc.getFieldable("id").stringValue());
                    InterMineObject obj = objMap.get(id);
                    searchHits.add(new KeywordSearchHit(browseHit.getScore(), doc, obj));
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        LOG.debug("Creating list of search hits took " + (System.currentTimeMillis() - time)
                + " ms");
        return searchHits;
    }

    /**
     * @param browseHits the query results.
     *
     * @return set of IDs found in the search results
     */
    public static Set<Integer> getObjectIds(BrowseHit[] browseHits) {
        long time = System.currentTimeMillis();
        Set<Integer> objectIds = new HashSet<Integer>();
        for (BrowseHit browseHit : browseHits) {
            try {
                Document doc = browseHit.getStoredFields();
                if (doc != null) {
                    objectIds.add(Integer.valueOf(doc.getFieldable("id").stringValue()));
                }
            } catch (NumberFormatException e) {
                LOG.info("Invalid id '" + browseHit.getField("id") + "' for hit '"
                        + browseHit + "'", e);
            }
        }
        LOG.debug("Getting IDs took " + (System.currentTimeMillis() - time) + " ms");
        return objectIds;
    }

    /**
     * Run a browse search and get back both search results and facet information.
     * @param im The InterMine state object.
     * @param searchString The search input.
     * @param offset An offset.
     * @param facetValues The facets selected.
     * @param ids A collection of objects to restrict the search to.
     * @return An object which provides access to hits and facets.
     * @throws ObjectStoreException If we can't fetch objects.
     */
    public static ResultsWithFacets runBrowseWithFacets(
            InterMineAPI im,
            String searchString,
            int offset,
            Map<String, String> facetValues,
            List<Integer> ids)
        throws ObjectStoreException {
        // last parameter used only when creating lists
        BrowseResult results = runBrowseSearch(searchString, offset, facetValues, ids, 0);
        Collection<KeywordSearchFacet> searchResultsFacets = Collections.emptySet();
        Collection<KeywordSearchHit> searchHits = Collections.emptySet();
        int totalHits = 0;
        if (results != null) {
            totalHits = results.getNumHits();
            LOG.debug("Browse found " + totalHits + " hits");
            BrowseHit[] browseHits = results.getHits();
            Set<Integer> objectIds = getObjectIds(browseHits);
            Map<Integer, InterMineObject> objMap = Objects.getObjects(im, objectIds);
            searchHits = getSearchHits(browseHits, objMap);
            searchResultsFacets = parseFacets(results, facets, facetValues);
            results.close();
        }
        return new ResultsWithFacets(searchHits, searchResultsFacets, totalHits);
    }

    /**
     * perform a keyword search using bobo-browse for faceting and pagination
     * @param searchString string to search for
     * @param offset display offset
     * @param facetValues map of 'facet field name' to 'value to restrict field to' (optional)
     * @param ids ids to research the search to (for search in list)
     * @param listSize size of the list (used only when creating one)
     * @return bobo browse result or null if failed
     */
    public static BrowseResult runBrowseSearch(String searchString, int offset,
            Map<String, String> facetValues, List<Integer> ids, int listSize) {
        return runBrowseSearch(searchString, offset, facetValues, ids, true, 0);
    }

    /**
     * perform a keyword search using bobo-browse for faceting and pagination
     * @param searchString string to search for
     * @param offset display offset
     * @param facetValues map of 'facet field name' to 'value to restrict field to' (optional)
     * @param ids ids to research the search to (for search in list)
     * @param pagination if TRUE only return 100
     * @param listSize siza of a list of results being created
     * @return bobo browse result or null if failed
     */
    public static BrowseResult runBrowseSearch(String searchString, int offset,
            Map<String, String> facetValues, List<Integer> ids, boolean pagination, int listSize) {
        BrowseResult result = null;
        if (index == null) {
            return result;
        }
        long time = System.currentTimeMillis();
        String queryString = parseQueryString(searchString);

        try {
            Analyzer analyzer = new WhitespaceAnalyzer();

            // pass entire list of field names to the multi-field parser
            // => search through all fields
            String[] fieldNamesArray = new String[index.getFieldNames().size()];

            index.getFieldNames().toArray(fieldNamesArray);
            QueryParser queryParser =
                    new MultiFieldQueryParser(Version.LUCENE_30, fieldNamesArray, analyzer);
            queryParser.setDefaultOperator(Operator.AND);
            queryParser.setAllowLeadingWildcard(true);
            org.apache.lucene.search.Query query = queryParser.parse(queryString);

            // required to expand search terms
            query = query.rewrite(reader);

            if (debugOutput) {
                LOG.debug("Rewritten query: " + query);
            }

            // initialize request
            BrowseRequest browseRequest = new BrowseRequest();
            if (debugOutput) {
                browseRequest.setShowExplanation(true);
            }
            browseRequest.setQuery(query);
            browseRequest.setFetchStoredFields(true);

            if (ids != null && !ids.isEmpty()) {
                TermsFilter idFilter = new TermsFilter(); //we may want fieldcachetermsfilter

                for (int id : ids) {
                    idFilter.addTerm(new Term("id", Integer.toString(id)));
                }

                browseRequest.setFilter(idFilter);
            }

            // pagination
            browseRequest.setOffset(offset);
            if (pagination) {
                // used on keywordsearch results page
                browseRequest.setCount(PER_PAGE);
            } else {
                // when creating lists from results
                // this check should be not necessary and reproduces previous behaviour
                if (listSize == 0) {
                    listSize = 10000;
                }
                browseRequest.setCount(listSize);
            }

            // add faceting selections
            for (Entry<String, String> facetValue : facetValues.entrySet()) {
                if (facetValue != null) {
                    BrowseSelection browseSelection = new BrowseSelection(facetValue.getKey());
                    browseSelection.addValue(facetValue.getValue());
                    browseRequest.addSelection(browseSelection);
                }
            }

            // order faceting results by hits
            FacetSpec orderByHitsSpec = new FacetSpec();
            orderByHitsSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
            browseRequest.setFacetSpec("Category", orderByHitsSpec);
            for (KeywordSearchFacetData facet : facets) {
                browseRequest.setFacetSpec(facet.getField(), orderByHitsSpec);
            }

            LOG.debug("Prepared browserequest in " + (System.currentTimeMillis() - time) + " ms");
            time = System.currentTimeMillis();

            // execute query and return result
            Browsable browser = null;
            try {
                browser = new BoboBrowser(boboIndexReader);
                result = browser.browse(browseRequest);
            } finally {
                if (browser != null) {
                    browser.close();
                }
            }

            if (debugOutput) {
                for (int i = 0; i < result.getHits().length && i < 5; i++) {
                    Explanation expl = result.getHits()[i].getExplanation();
                    if (expl != null) {
                        LOG.debug(result.getHits()[i].getStoredFields().getFieldable("id")
                                + " - score explanation: " + expl.toString());
                    }
                }
            }
        } catch (ParseException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        } catch (IOException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        } catch (BrowseException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        }

        LOG.debug("Bobo browse finished in " + (System.currentTimeMillis() - time) + " ms");

        return result;
    }



    private static String parseQueryString(String qs) {
        String queryString = qs;
        // keep strings separated by spaces together
        queryString = queryString.replaceAll("\\b(\\s+)\\+(\\s+)\\b", "$1AND$2");
        // i don't know
        queryString = queryString.replaceAll("(^|\\s+)'(\\b[^']+ [^']+\\b)'(\\s+|$)", "$1\"$2\"$3");
        // escape special characters, see http://lucene.apache.org/java/2_9_0/queryparsersyntax.html
        final String[] specialCharacters = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[",
            "]", "^", "~", "?", ":", "\\"};
        for (String s : specialCharacters) {
            if (queryString.contains(s)) {
                queryString = queryString.replace(s, "*");
            }
        }
        return toLowerCase(queryString);
    }

    private static String toLowerCase(String s) {
        StringBuffer sb = new StringBuffer();
        String[] bits = s.split(" ");
        for (String b : bits) {
            // booleans have to stay UPPER
            if ("OR".equalsIgnoreCase(b) || "AND".equalsIgnoreCase(b)
                    || "NOT".equalsIgnoreCase(b)) {
                sb.append(b.toUpperCase() + " ");
            } else {
                sb.append(b.toLowerCase() + " ");
            }
        }
        return sb.toString().trim();
    }

    private static LuceneIndexContainer loadIndexFromDatabase(ObjectStore os, String path) {
        LOG.debug("Attempting to restore search index from database...");
        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            LuceneIndexContainer ret = null;
            try {
                ret = restoreIndex(db);

                if (ret != null) {
                    String indexDirectoryType = ret.getDirectoryType();
                    Directory dir = restoreSearchDirectory(indexDirectoryType, path, db);
                    if (dir == null) {
                        LOG.error("Could not load directory");
                        return null;
                    }
                    ret.setDirectory(dir);
                    return ret;
                }

            } catch (ClassNotFoundException e) {
                LOG.error("Could not load search index", e);
            } catch (SQLException e) {
                LOG.error("Could not load search index", e);
            } catch (IOException e) {
                LOG.error("Could not load search index", e);
            }
        } else {
            LOG.error("ObjectStore is of wrong type!");
        }
        return null;
    }

    private static Directory restoreSearchDirectory(String dirType, String path, Database db)
        throws SQLException, IOException, FileNotFoundException, ClassNotFoundException {
        InputStream is;
        LOG.debug("Attempting to restore search directory from database...");
        is = MetadataManager.readLargeBinary(db, MetadataManager.SEARCH_INDEX_DIRECTORY);


        if (is != null) {
            try {
                if ("FSDirectory".equals(dirType)) {
                    return readFSDirectory(path, is);
                } else if ("RAMDirectory".equals(dirType)) {
                    return readRAMDirectory(is);
                } else {
                    LOG.warn("Unknown directory type specified: " + dirType);
                }
            } finally {
                is.close();
            }

        } else {
            LOG.warn("Could not find search directory!");
        }
        return null;
    }

    private static FSDirectory readFSDirectory(String path, InputStream is)
        throws IOException, FileNotFoundException {
        long time = System.currentTimeMillis();
        final int bufferSize = 2048;
        File directoryPath = new File(path + File.separator + LUCENE_INDEX_DIR);
        LOG.debug("Directory path: " + directoryPath);

        // make sure we start with a new index
        if (directoryPath.exists()) {
            String[] files = directoryPath.list();
            for (int i = 0; i < files.length; i++) {
                LOG.info("Deleting old file: " + files[i]);
                new File(directoryPath.getAbsolutePath() + File.separator
                        + files[i]).delete();
            }
        } else {
            directoryPath.mkdir();
        }

        ZipInputStream zis = new ZipInputStream(is);
        ZipEntry entry;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                LOG.info("Extracting: " + entry.getName() + " (" + entry.getSize()
                        + " MB)");

                FileOutputStream fos =
                        new FileOutputStream(directoryPath.getAbsolutePath()
                                + File.separator + entry.getName());
                BufferedOutputStream bos =
                        new BufferedOutputStream(fos, bufferSize);

                int count;
                byte[] data = new byte[bufferSize];

                try {
                    while ((count = zis.read(data, 0, bufferSize)) != -1) {
                        bos.write(data, 0, count);
                    }
                } finally {
                    bos.flush();
                    bos.close();
                }
            }
        } finally {
            zis.close();
        }

        FSDirectory directory = FSDirectory.open(directoryPath);

        LOG.info("Successfully restored FS directory from database in "
                + (System.currentTimeMillis() - time) + " ms");
        return directory;
    }

    private static RAMDirectory readRAMDirectory(InputStream is)
        throws IOException, ClassNotFoundException {
        long time = System.currentTimeMillis();
        GZIPInputStream gzipInput = new GZIPInputStream(is);
        ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

        try {
            Object object = objectInput.readObject();

            if (object instanceof FSDirectory) {
                RAMDirectory directory = (RAMDirectory) object;

                time = System.currentTimeMillis() - time;
                LOG.info("Successfully restored RAM directory"
                        + " from database in " + time + " ms");
                return directory;
            }
        } finally {
            objectInput.close();
            gzipInput.close();
        }
        return null;
    }

    private static LuceneIndexContainer restoreIndex(Database db)
        throws IOException, ClassNotFoundException, SQLException {

        long time = System.currentTimeMillis();
        InputStream is = MetadataManager.readLargeBinary(db, MetadataManager.SEARCH_INDEX);

        if (is == null) {
            LOG.warn("No search index stored in this DB.");
            return null;
        } else {
            GZIPInputStream gzipInput = new GZIPInputStream(is);
            ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

            try {
                Object object = objectInput.readObject();

                if (object instanceof LuceneIndexContainer) {

                    LOG.info("Successfully restored search index information"
                            + " from database in " + (System.currentTimeMillis() - time)
                            + " ms");
                    LOG.debug("Index: " + index);
                    return (LuceneIndexContainer) object;
                } else {
                    LOG.warn("Object from DB has wrong class:"
                            + object.getClass().getName());
                    return null;
                }
            } finally {
                objectInput.close();
                gzipInput.close();
                is.close();
            }
        }
    }

    private static File createIndex(ObjectStore os, Map<String, List<FieldDescriptor>> classKeys)
        throws IOException {
        long time = System.currentTimeMillis();
        File tempFile = null;
        LOG.debug("Creating keyword search index...");

        parseProperties(os);

        LOG.info("Starting fetcher thread...");
        InterMineObjectFetcher fetchThread =
                new InterMineObjectFetcher(os, classKeys, indexingQueue, ignoredClasses,
                        ignoredFields, specialReferences, classBoost, facets, attributePrefixes);
        fetchThread.start();

        // index the docs queued by the fetchers
        LOG.info("Preparing indexer...");
        index = new LuceneIndexContainer();
        try {
            tempFile = makeTempFile(tempDirectory);
        } catch (IOException e) {
            String tmpDir = System.getProperty("java.io.tmpdir");
            LOG.warn("Failed to create temp directory " + tempDirectory + " trying " + tmpDir
                    + " instead", e);
            try {
                tempFile = makeTempFile(tmpDir);
            } catch (IOException ee) {
                LOG.warn("Failed to create temp directory in " + tmpDir, ee);
                throw ee;
            }
        }

        LOG.info("Index directory: " + tempFile.getAbsolutePath());

        IndexWriter writer;
        writer = new IndexWriter(index.getDirectory(), new WhitespaceAnalyzer(), true,
                 IndexWriter.MaxFieldLength.UNLIMITED); //autocommit = false?
        writer.setMergeFactor(10); //10 default, higher values = more parts
        writer.setRAMBufferSizeMB(64); //flush to disk when docs take up X MB

        int indexed = 0;

        // loop and index while we still have fetchers running
        LOG.debug("Starting to index...");
        while (indexingQueue.hasNext()) {
            Document doc = indexingQueue.next();

            // nothing in the queue?
            if (doc != null) {
                try {
                    writer.addDocument(doc);
                    indexed++;
                } catch (IOException e) {
                    LOG.error("Failed to submit #" + doc.getFieldable("id") + " to the index",
                            e);
                }

                if (indexed % 10000 == 1) {
                    LOG.info("docs indexed=" + indexed + "; thread state="
                            + fetchThread.getState() + "; docs/ms=" + indexed * 1.0F
                            / (System.currentTimeMillis() - time) + "; memory="
                            + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                            + Runtime.getRuntime().maxMemory() / 1024 + "k" + "; time="
                            + (System.currentTimeMillis() - time) + "ms");
                }
            }
        }
        if (fetchThread.getException() != null) {
            try {
                writer.close();
            } catch (Exception e) {
                LOG.error("Error closing writer while handling exception.", e);
            }
            throw new RuntimeException("Indexing failed.", fetchThread.getException());
        }
        index.getFieldNames().addAll(fetchThread.getFieldNames());
        LOG.debug("Indexing done, optimizing index files...");
        try {
            writer.optimize();
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while optimizing and closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info("Indexing of " + indexed + " documents finished in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                        time % 1000) + " minutes");
        return tempFile;
    }

    private static File makeTempFile(String tempDir) throws IOException {
        LOG.debug("Creating search index tmp dir: " + tempDir);
        File tempFile = File.createTempFile("search_index", "", new File(tempDir));
        if (!tempFile.delete()) {
            throw new IOException("Could not delete temp file");
        }

        index.setDirectory(FSDirectory.open(tempFile));
        index.setDirectoryType("FSDirectory");

        // make sure we start with a new index
        if (tempFile.exists()) {
            String[] files = tempFile.list();
            for (int i = 0; i < files.length; i++) {
                LOG.info("Deleting old file: " + files[i]);
                new File(tempFile.getAbsolutePath() + File.separator + files[i]).delete();
            }
        } else {
            tempFile.mkdir();
        }
        return tempFile;
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
     * get list of facet fields and names
     * @return map of internal fieldname -> displayed name
     */
    public static Vector<KeywordSearchFacetData> getFacets() {
        return facets;
    }

    /**
     * delete the directory used for the index (used in postprocessing)
     */
    public static void deleteIndexDirectory() {
        if (index != null && "FSDirectory".equals(index.getDirectoryType())) {
            File tempFile = ((FSDirectory) index.getDirectory()).getFile();
            LOG.info("Deleting index directory: " + tempFile.getAbsolutePath());

            if (tempFile.exists()) {
                String[] files = tempFile.list();
                for (int i = 0; i < files.length; i++) {
                    LOG.debug("Deleting index file: " + files[i]);
                    new File(tempFile.getAbsolutePath() + File.separator + files[i]).delete();
                }
                tempFile.delete();
                LOG.warn("Deleted index directory!");
            } else {
                LOG.warn("Index directory does not exist!");
            }

            index = null;
        }
    }

    /**
     * set all the variables to NULL
     */
    public static void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                LOG.error("Not able to free Lucene index file.");
                e.printStackTrace();
            }
        }
        reader = null;
        if (boboIndexReader != null) {
            try {
                boboIndexReader.close();
            } catch (IOException e) {
                LOG.error("Not able to close bobo Index Reader (Lucene).");
                e.printStackTrace();
            }
        }
        boboIndexReader = null;
        indexingQueue = null;
        index = null;
        properties = null;
        tempDirectory = null;
        specialReferences = null;
        ignoredClasses = null;
        ignoredFields = null;
        classBoost = null;
        facets = null;
        attributePrefixes = null;
    }
}
