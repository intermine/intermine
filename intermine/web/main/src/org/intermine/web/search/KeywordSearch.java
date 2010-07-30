package org.intermine.web.search;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.modelproduction.MetadataManager.LargeObjectOutputStream;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.PathException;
import org.intermine.sql.Database;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;

/**
 * container class to cache class attributes
 * @author nils
 */
class ClassAttributes
{
    String className;
    Set<AttributeDescriptor> attributes;

    /**
     * constructor
     * @param className
     *            name of the class
     * @param attributes
     *            set of attributes for the class
     */
    public ClassAttributes(String className, Set<AttributeDescriptor> attributes) {
        super();
        this.className = className;
        this.attributes = attributes;
    }

    /**
     * name of the class
     * @return name of the class
     */
    public String getClassName() {
        return className;
    }

    /**
     * attributes associated with the class
     * @return attributes associated with the class
     */
    public Set<AttributeDescriptor> getAttributes() {
        return attributes;
    }
}

/**
 * container for the lucene index to hold field list
 * @author nils
 */
class LuceneIndexContainer implements Serializable
{
    private static final long serialVersionUID = 1L;
    private transient Directory directory;
    private String directoryType;
    private HashSet<String> fieldNames = new HashSet<String>();
    private HashMap<String, Float> fieldBoosts = new HashMap<String, Float>();

    /**
     * get lucene directory for this index
     * @return directory
     */
    public Directory getDirectory() {
        return directory;
    }

    /**
     * set lucene directory
     * @param directory
     *            directory
     */
    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

    /**
     * get type of directory
     * @return FSDirectory or RAMDirectory
     */
    public String getDirectoryType() {
        return directoryType;
    }

    /**
     * set type of directory
     * @param directoryType
     *            class name of lucene directory
     */
    public void setDirectoryType(String directoryType) {
        this.directoryType = directoryType;
    }

    /**
     * get list of fields in the index
     * @return fields
     */
    public HashSet<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * set list of fields in the index
     * @param fieldNames
     *            fields
     */
    public void setFieldNames(HashSet<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    /**
     * get list of boost associated with fields
     * @return boosts
     */
    public HashMap<String, Float> getFieldBoosts() {
        return fieldBoosts;
    }

    /**
     * set boost associated with fields
     * @param fieldBoosts
     *            boosts
     */
    public void setFieldBoosts(HashMap<String, Float> fieldBoosts) {
        this.fieldBoosts = fieldBoosts;
    }

    @Override
    public String toString() {
        return "INDEX [[" + directory + "" + ", fields = " + fieldNames + "" + ", boosts = "
                + fieldBoosts + "" + "]]";
    }
}

/**
 * fetched objects from database, generates documents and adds them to a queue
 * for indexing
 * @author nils
 */
class DocumentFetcher implements Runnable
{
    private static final Logger LOG = Logger.getLogger(DocumentFetcher.class);

    final ObjectStore os;
    final ConcurrentLinkedQueue<Document> indexingQueue;
    final HashMap<Class<? extends InterMineObject>, String[]> specialIndexClasses;
    final Class<? extends InterMineObject> cls;
    final boolean overwrite;

    final HashMap<Integer, Document> documents = new HashMap<Integer, Document>();
    final HashSet<String> fieldNames = new HashSet<String>();
    final HashMap<Class<?>, Vector<ClassAttributes>> decomposedClassesCache =
            new HashMap<Class<?>, Vector<ClassAttributes>>();

    /**
     * initialize the documentfetcher thread
     * @param os
     *            intermine objectstore
     * @param indexingQueue
     *            queue shared with indexer
     * @param specialIndexClasses
     *            map of classes that are not handled by the default bioentity
     *            fetcher
     * @param cls
     *            class of object to index
     * @param overwrite
     *            if false the fetcher will check if an object class is in
     *            specialProperties.keySet() before adding it
     */
    public DocumentFetcher(ObjectStore os, ConcurrentLinkedQueue<Document> indexingQueue,
            HashMap<Class<? extends InterMineObject>, String[]> specialIndexClasses,
            Class<? extends InterMineObject> cls, boolean overwrite) {
        this.os = os;
        this.indexingQueue = indexingQueue;
        this.specialIndexClasses = specialIndexClasses;
        this.cls = cls;
        this.overwrite = overwrite;
    }

    /**
     * get list of fields contained in the fetched documents
     * @return fields
     */
    public HashSet<String> getFieldNames() {
        return fieldNames;
    }

    /**
     * fetch objects from database, create documents and add them to the queue
     */
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            long time = System.currentTimeMillis();
            LOG.info("Fetching class '" + cls.getSimpleName() + "'...");

            HashSet<String> specialClassNames = new HashSet<String>();
            for (Class<? extends InterMineObject> specialClass : specialIndexClasses.keySet()) {
                specialClassNames.add(specialClass.getName());
            }
            LOG.info("Special Class Names = " + specialClassNames);

            String[] references = specialIndexClasses.get(cls);
            LOG.info("References = " + Arrays.toString(references));

            HashMap<Class<? extends InterMineObject>, Boolean> ignoredClasses;
            ignoredClasses = new HashMap<Class<? extends InterMineObject>, Boolean>();

            Query q = new Query();
            QueryClass qcGene = new QueryClass(cls);
            q.addFrom(qcGene);
            q.addToSelect(qcGene);

            Results results = os.execute(q);

            Iterator<ResultsRow<InterMineObject>> it = results.iterator();
            int i = 0;
            int ignored = 0;
            int size = results.size();
            LOG.info("Query returned " + size + " results");

            while (it.hasNext()) {
                ResultsRow<InterMineObject> row = it.next();

                if (i % 10000 == 1) {
                    LOG.info("'" + cls.getSimpleName() + "': fetched " + i + " of " + size);
                }

                for (InterMineObject object : row) {
                    if (!overwrite) {
                        boolean ignore = false;
                        Boolean cachedIgnore = ignoredClasses.get(object.getClass());

                        if (cachedIgnore != null) {
                            ignore = cachedIgnore.booleanValue();
                        } else {
                            LOG.info("'" + cls.getSimpleName()
                                    + "': checking whether to ignore class " + object.getClass());

                            for (Class<?> objectClass : DynamicUtil.decomposeClass(object
                                    .getClass())) {
                                ClassDescriptor cld =
                                        os.getModel().getClassDescriptorByName(
                                                objectClass.getName());

                                LOG.info("'" + cls.getSimpleName()
                                        + "': checking decomposed part of " + object.getClass()
                                        + " -> " + objectClass + " -- super="
                                        + cld.getSuperclassNames());

                                if (specialClassNames.contains(objectClass.getName())) {
                                    ignore = true;
                                } else if (cld.getSuperclassNames() != null) {
                                    if (cld.getSuperclassNames().removeAll(specialClassNames)) {
                                        ignore = true;
                                    }
                                } else {
                                    LOG.info("'" + cls.getSimpleName()
                                            + "': class CLD has no superclassnames - "
                                            + object.getClass());
                                }
                            }

                            // cache result
                            LOG.info("'" + cls.getSimpleName() + "': putting ignore=" + ignore
                                    + " into cache for class " + object.getClass());
                            ignoredClasses.put(object.getClass(), Boolean.valueOf(ignore));
                        }

                        if (ignore) {
                            ignored++;

                            if (ignored % 10000 == 1) {
                                LOG.info("'" + cls.getSimpleName() + "': ignored " + ignored
                                        + " out of " + i);
                            }

                            continue;
                        }
                    }

                    // if this takes too long we can cache it...
                    Class<?> objectClass =
                            DynamicUtil.decomposeClass(object.getClass()).iterator().next();
                    ClassDescriptor classDescriptor =
                            os.getModel().getClassDescriptorByName(objectClass.getName());

                    Document doc = getDocumentForObject(object, classDescriptor);
                    if (doc != null) {
                        if (references == null) {
                            indexingQueue.add(doc);
                        } else {
                            documents.put(object.getId(), doc);
                        }
                    }
                }

                i++;
            }

            time = System.currentTimeMillis() - time;
            int seconds = (int) Math.floor(time / 1000);
            LOG.info("Fetched "
                    + i
                    + " objects of class '"
                    + cls.getSimpleName()
                    + "' ("
                    + ignored
                    + " ignored) in "
                    + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                            time % 1000) + " minutes");

            if (references != null) {
                for (int j = 0; j < references.length; j++) {
                    String reference = references[j];
                    time = System.currentTimeMillis();
                    LOG.info(cls.getSimpleName() + "::" + reference + " - Querying...");

                    Query qc = getPathQuery(cls, reference);
                    ((ObjectStoreInterMineImpl) os).goFaster(qc);
                    try {
                        Results resultsc = os.execute(qc);
                        int sizec = resultsc.size();

                        LOG.info(cls.getSimpleName() + "::" + reference + " - Query returned "
                                + sizec + " results");

                        Iterator<ResultsRow<?>> itc = resultsc.iterator();

                        int k = 0;
                        int previousId = -1;
                        while (itc.hasNext()) {
                            ResultsRow<?> row = itc.next();

                            Integer docId = (Integer) row.get(0);
                            InterMineObject collectionObject = (InterMineObject) row.get(1);

                            Document doc = documents.get(docId);
                            if (doc != null) {
                                addObjectToDocument(collectionObject, doc);

                                // speed - submit completed documents already
                                // if we are at last reference
                                if ((j == references.length - 1) && previousId != docId) {
                                    Document docOld = documents.remove(previousId);

                                    if (docOld != null) {
                                        indexingQueue.add(docOld);
                                    } else {
                                        LOG.warn("docOld is null! (id = " + previousId + ")");
                                    }
                                }
                            } else {
                                LOG.warn(cls.getSimpleName() + "::" + reference
                                        + " - Did not find document #" + docId);
                            }

                            if (k % 10000 == 1) {
                                LOG.info(cls.getSimpleName() + "::" + reference + " - Fetched " + k
                                        + " of " + sizec);
                            }

                            previousId = docId;

                            k++;
                        }

                        time = System.currentTimeMillis() - time;
                        LOG.info(cls.getSimpleName()
                                + "::"
                                + reference
                                + " - Fetched "
                                + k
                                + " objects in "
                                + String.format("%02d:%02d.%03d", (int) Math.floor(time / 60000),
                                        time / 1000 % 60, time % 1000) + " minutes");
                    } finally {
                        ((ObjectStoreInterMineImpl) os).releaseGoFaster(qc);
                    }
                }

                LOG.info("Adding remaining " + documents.size() + " documents to queue");
                indexingQueue.addAll(documents.values());
            }
        } catch (Exception e) {
            LOG.warn(null, e);
        }
    }

    private Document getDocumentForObject(InterMineObject object, ClassDescriptor classDescriptor) {
        Document doc = new Document();
        doc.add(new Field("id", object.getId().toString(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field("Category", classDescriptor.getUnqualifiedName(), Field.Store.YES,
                Field.Index.NOT_ANALYZED));

        if (object instanceof BioEntity && ((BioEntity) object).getOrganism() != null) {
            doc.add(new Field("Organism", ((BioEntity) object).getOrganism().getShortName(),
                    Field.Store.YES, Field.Index.NOT_ANALYZED));
        }

        addObjectToDocument(object, doc);

        return doc;
    }

    private void addObjectToDocument(InterMineObject object, Document doc) {
        HashMap<String, String> attributes = getAttributeMapForObject(os.getModel(), object);
        for (Entry<String, String> attribute : attributes.entrySet()) {
            addToDocument(doc, attribute.getKey(), attribute.getValue());
        }
    }

    private HashMap<String, String> getAttributeMapForObject(Model model, Object obj) {
        HashMap<String, String> values = new HashMap<String, String>();
        Vector<ClassAttributes> decomposedClassAttributes =
                getClassAttributes(model, obj.getClass());

        for (ClassAttributes classAttributes : decomposedClassAttributes) {
            for (AttributeDescriptor att : classAttributes.getAttributes()) {
                try {
                    if ("java.lang.String".equals(att.getType())) {
                        Object value = TypeUtil.getFieldValue(obj, att.getName());

                        // ignore null values
                        if (value != null) {
                            values.put((classAttributes.getClassName() + "_" + att.getName())
                                    .toLowerCase(), String.valueOf(value));
                        }
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Error introspecting an object: " + obj, e);
                }
            }
        }

        return values;
    }

    private void addToDocument(Document doc, String fieldName, String value) {
        if (!StringUtils.isBlank(fieldName) && !StringUtils.isBlank(value)) {
            Field f = new Field(fieldName, value, Field.Store.NO, Field.Index.ANALYZED);
            doc.add(f);
            fieldNames.add(fieldName);
        }
    }

    // simple caching of attributes
    private Vector<ClassAttributes> getClassAttributes(Model model, Class<?> baseClass) {
        Vector<ClassAttributes> attributes = decomposedClassesCache.get(baseClass);

        if (attributes == null) {
            LOG.info("decomposedClassesCache: No entry for " + baseClass + ", adding...");
            attributes = new Vector<ClassAttributes>();

            for (Class<?> cls : DynamicUtil.decomposeClass(baseClass)) {
                ClassDescriptor cld = model.getClassDescriptorByName(cls.getName());
                attributes.add(new ClassAttributes(cld.getUnqualifiedName(), cld
                        .getAllAttributeDescriptors()));
            }

            decomposedClassesCache.put(baseClass, attributes);
        }

        return attributes;
    }

    private Query getPathQuery(Class<? extends InterMineObject> mainCls, String pathString)
        throws PathException {
        Query q = new Query();
        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

        org.intermine.pathquery.Path path =
                new org.intermine.pathquery.Path(os.getModel(), pathString);
        List<ClassDescriptor> classDescriptors = path.getElementClassDescriptors();
        List<String> fieldNames = path.getElements();

        ClassDescriptor parentClassDescriptor = null;
        QueryClass parentQueryClass = null;

        for (int i = 0; i < classDescriptors.size(); i++) {
            ClassDescriptor classDescriptor = classDescriptors.get(i);

            Class<?> classInCollection = classDescriptor.getType();

            QueryClass queryClass = new QueryClass(classInCollection);
            q.addFrom(queryClass);

            if (i == 0) {
                // first class
                LOG.info("TOP: " + classDescriptor);

                QueryField topId = new QueryField(queryClass, "id");
                q.addToSelect(topId);
                q.addToOrderBy(topId); // important for optimization in run()
            } else {
                String fieldName = fieldNames.get(i - 1);

                LOG.info(fieldName + " -> " + classDescriptor);

                if (parentClassDescriptor.getReferenceDescriptorByName(fieldName, true) != null) {
                    LOG.info(fieldName + " is OBJECT");
                    QueryObjectReference objectReference =
                            new QueryObjectReference(parentQueryClass, fieldName);
                    ContainsConstraint cc =
                            new ContainsConstraint(objectReference, ConstraintOp.CONTAINS,
                                    queryClass);
                    constraints.addConstraint(cc);
                } else if (parentClassDescriptor.getCollectionDescriptorByName(fieldName, true) != null) {
                    LOG.info(fieldName + " is COLLECTION");
                    QueryCollectionReference collectionReference =
                            new QueryCollectionReference(parentQueryClass, fieldName);
                    ContainsConstraint cc =
                            new ContainsConstraint(collectionReference, ConstraintOp.CONTAINS,
                                    queryClass);
                    constraints.addConstraint(cc);
                } else {
                    LOG.warn("Unknown field '" + parentClassDescriptor.getUnqualifiedName()
                            + "'::'" + fieldName + "' in path '" + pathString + "'!");
                }
            }

            parentClassDescriptor = classDescriptor;
            parentQueryClass = queryClass;
        }

        q.setConstraint(constraints);
        q.addToSelect(parentQueryClass); // select last class

        return q;
    }
}

/**
 * allows for full-text searches over all metadata using the apache lucene
 * engine
 * @author nils
 */
public class KeywordSearch
{
    private static final String LUCENE_INDEX_PATH = "lucene_index";

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
    private static ConcurrentLinkedQueue<Document> indexingQueue =
            new ConcurrentLinkedQueue<Document>();
    private static LuceneIndexContainer index = null;

    /**
     * loads or creates the lucene index
     * @param im
     *            API for accessing object store
     */
    public static void initKeywordSearch(InterMineAPI im) {
        if (index == null) {
            // try to load index from database first
            loadIndexFromDatabase(im.getObjectStore());

            // still nothing, create new one
            // TODO: we may just want to error out here
            if (index == null) {
                createIndex(im.getObjectStore());
            }
        }

        try {
            if (reader == null) {
                reader = IndexReader.open(index.getDirectory(), true);
            }

            if (boboIndexReader == null) {
                // prepare faceting
                SimpleFacetHandler categoryFacet = new SimpleFacetHandler("Category");
                SimpleFacetHandler organismFacet = new SimpleFacetHandler("Organism");
                List<FacetHandler<?>> facetHandlers =
                        Arrays.asList(new FacetHandler<?>[] {categoryFacet, organismFacet});

                boboIndexReader = BoboIndexReader.getInstance(reader, facetHandlers);
            }
        } catch (CorruptIndexException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void writeObjectToDB(ObjectStore os, String key, Object object)
        throws IOException, SQLException {
        LOG.info("Saving stream to database...");
        Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
        LargeObjectOutputStream streamOut = MetadataManager.storeLargeBinary(db, key);

        GZIPOutputStream gzipStream = new GZIPOutputStream(new BufferedOutputStream(streamOut));
        ObjectOutputStream objectStream = new ObjectOutputStream(gzipStream);

        LOG.info("GZipping and serializing object...");
        objectStream.writeObject(object);

        objectStream.flush();
        gzipStream.finish();
        gzipStream.flush();

        streamOut.close();
    }

    /**
     * writes index and associated directory to the database using the
     * metadatamanager
     * @param os
     *            intermine objectstore
     */
    public static void saveIndexToDatabase(ObjectStore os) {
        if (index == null) {
            createIndex(os);
        }

        try {
            LOG.info("Saving search index information to database...");
            writeObjectToDB(os, MetadataManager.SEARCH_INDEX, index);
            LOG.info("Successfully saved search index information to database.");

            // if we have a FSDirectory we need to zip and save that separately
            if ("FSDirectory".equals(index.getDirectoryType())) {
                final int bufferSize = 2048;

                try {
                    LOG.info("Zipping up FSDirectory...");

                    LOG.info("Saving stream to database...");
                    Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
                    LargeObjectOutputStream streamOut =
                            MetadataManager.storeLargeBinary(db,
                                    MetadataManager.SEARCH_INDEX_DIRECTORY);

                    ZipOutputStream zipOut =
                            new ZipOutputStream(new BufferedOutputStream(streamOut));

                    byte data[] = new byte[bufferSize];

                    // get a list of files from current directory
                    File fsDirectory = ((FSDirectory) index.getDirectory()).getFile();
                    String files[] = fsDirectory.list();

                    for (int i = 0; i < files.length; i++) {
                        File file =
                                new File(fsDirectory.getAbsolutePath() + File.separator + files[i]);
                        LOG.info("Zipping file: " + file.getName() + " (" + file.length() / 1024
                                + " KB)");

                        FileInputStream fi = new FileInputStream(file);
                        BufferedInputStream fileInput = new BufferedInputStream(fi, bufferSize);

                        try {
                            ZipEntry entry = new ZipEntry(files[i]);
                            entry.setSize(file.length());
                            zipOut.putNextEntry(entry);

                            int count;
                            while ((count = fileInput.read(data, 0, bufferSize)) != -1) {
                                zipOut.write(data, 0, count);
                            }
                        } finally {
                            fileInput.close();
                        }
                    }

                    zipOut.close();
                    LOG.info("Successfully saved search directory to database!");
                } catch (IOException e) {
                    LOG.error(null, e);
                }
            } else if ("RAMDirectory".equals(index.getDirectoryType())) {
                LOG.info("Saving search directory to database...");
                writeObjectToDB(os, MetadataManager.SEARCH_INDEX_DIRECTORY, index.getDirectory());
                LOG.info("Successfully saved search directory to database.");
            }
        } catch (IOException e) {
            LOG.error(null, e);
        } catch (SQLException e) {
            LOG.error(null, e);
        }
    }

    /**
     * perform a keyword search over all document metadata fields with lucene
     * @param searchString
     *            string to search for
     * @return map of document IDs with their respective scores
     */
    public static Map<Integer, Float> runLuceneSearch(String searchString) {
        LinkedHashMap<Integer, Float> matches = new LinkedHashMap<Integer, Float>();

        String queryString = parseQueryString(searchString);

        long time = System.currentTimeMillis();

        try {
            IndexSearcher searcher = new IndexSearcher(reader);

            Analyzer analyzer =
                    new SnowballAnalyzer(Version.LUCENE_30, "English",
                            StopAnalyzer.ENGLISH_STOP_WORDS_SET);

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

            LOG.info("Found " + topDocs.totalHits + " document(s) that matched query '"
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
        }

        LOG.info("Lucene search finished in " + (System.currentTimeMillis() - time) + " ms");

        return matches;
    }

    /**
     * perform a keyword search using bobo-browse for faceting and pagination
     * @param searchString
     *            string to search for
     * @param offset
     *            display offset
     * @param categories
     *            category facets to search in
     * @param organisms
     *            organism facets to search in
     * @return bobo browse result or null if failed
     */
    public static BrowseResult runBrowseSearch(String searchString, int offset,
            String[] categories, String[] organisms) {
        BrowseResult result = null;
        long time = System.currentTimeMillis();

        String queryString = parseQueryString(searchString);

        try {
            Analyzer analyzer =
                    new SnowballAnalyzer(Version.LUCENE_30, "English",
                            StopAnalyzer.ENGLISH_STOP_WORDS_SET);

            org.apache.lucene.search.Query query;

            // pass entire list of field names to the multi-field parser
            // => search through all fields
            String[] fieldNamesArray = new String[index.getFieldNames().size()];
            index.getFieldNames().toArray(fieldNamesArray);
            QueryParser queryParser =
                    new MultiFieldQueryParser(Version.LUCENE_30, fieldNamesArray, analyzer, index
                            .getFieldBoosts());
            query = queryParser.parse(queryString);

            LOG.info("Parsed query in " + (System.currentTimeMillis() - time) + " ms");
            time = System.currentTimeMillis();

            // required to expand search terms
            query = query.rewrite(reader);

            LOG.info("Rewrote query in " + (System.currentTimeMillis() - time) + " ms");
            time = System.currentTimeMillis();

            // initialize request
            BrowseRequest browseRequest = new BrowseRequest();
            browseRequest.setQuery(query);
            browseRequest.setFetchStoredFields(true);

            // pagination
            browseRequest.setOffset(offset);
            browseRequest.setCount(PER_PAGE);

            // add faceting selections
            if (categories != null && categories.length > 0) {
                BrowseSelection browseSelection = new BrowseSelection("Category");
                for (int i = 0; i < categories.length; i++) {
                    String string = categories[i];
                    browseSelection.addValue(string);
                }
                browseRequest.addSelection(browseSelection);
            }

            if (organisms != null && organisms.length > 0) {
                BrowseSelection browseSelection = new BrowseSelection("Organism");
                for (int i = 0; i < organisms.length; i++) {
                    String string = organisms[i];
                    browseSelection.addValue(string);
                }
                browseRequest.addSelection(browseSelection);
            }

            // order faceting results by hits
            FacetSpec orderByHitsSpec = new FacetSpec();
            orderByHitsSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
            browseRequest.setFacetSpec("Category", orderByHitsSpec);
            browseRequest.setFacetSpec("Organism", orderByHitsSpec);

            LOG.info("Prepared browserequest in " + (System.currentTimeMillis() - time) + " ms");
            time = System.currentTimeMillis();

            // execute query and return result
            Browsable browser = new BoboBrowser(boboIndexReader);
            result = browser.browse(browseRequest);
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

        LOG.info("Bobo browse finished in " + (System.currentTimeMillis() - time) + " ms");

        return result;
    }

    private static String parseQueryString(String queryString) {
        queryString = queryString.replaceAll("\\b(\\s+)\\+(\\s+)\\b", "$1AND$2");
        queryString = queryString.replaceAll("(^|\\s+)'(\\b[^']+ [^']+\\b)'(\\s+|$)", "$1\"$2\"$3");

        return queryString;
    }

    private static void loadIndexFromDatabase(ObjectStore os) {
        long time = System.currentTimeMillis();
        LOG.info("Attempting to restore search index from database...");
        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            try {
                InputStream is = MetadataManager.readLargeBinary(db, MetadataManager.SEARCH_INDEX);

                if (is != null) {
                    GZIPInputStream gzipInput = new GZIPInputStream(is);
                    ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

                    try {
                        Object object = objectInput.readObject();

                        if (object instanceof LuceneIndexContainer) {
                            index = (LuceneIndexContainer) object;

                            LOG.info("Successfully restored search index information"
                                    + " from database in " + (System.currentTimeMillis() - time)
                                    + " ms");
                            LOG.info("Index: " + index);
                        } else {
                            LOG.warn("Object from DB has wrong class:"
                                    + object.getClass().getName());
                        }
                    } finally {
                        objectInput.close();
                        gzipInput.close();
                    }
                } else {
                    LOG.warn("IS is null");
                }

                if (index != null) {
                    time = System.currentTimeMillis();
                    LOG.info("Attempting to restore search directory from database...");
                    is =
                            MetadataManager.readLargeBinary(db,
                                    MetadataManager.SEARCH_INDEX_DIRECTORY);

                    if (is != null) {
                        if ("FSDirectory".equals(index.getDirectoryType())) {
                            final int bufferSize = 2048;
                            File directoryPath = new File(LUCENE_INDEX_PATH);

                            // make sure we start with a new index
                            if (directoryPath.exists()) {
                                String files[] = directoryPath.list();
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
                            while ((entry = zis.getNextEntry()) != null) {
                                LOG.info("Extracting: " + entry.getName() + " (" + entry.getSize()
                                        + " MB)");

                                FileOutputStream fos =
                                        new FileOutputStream(directoryPath.getAbsolutePath()
                                                + File.separator + entry.getName());
                                BufferedOutputStream bos =
                                        new BufferedOutputStream(fos, bufferSize);

                                int count;
                                byte data[] = new byte[bufferSize];

                                while ((count = zis.read(data, 0, bufferSize)) != -1) {
                                    bos.write(data, 0, count);
                                }

                                bos.flush();
                                bos.close();
                            }

                            FSDirectory directory = FSDirectory.open(directoryPath);
                            // RAMDirectory ramDirectory = new
                            // RAMDirectory(directory);
                            index.setDirectory(directory);

                            LOG.info("Successfully restored FS directory from database in "
                                    + (System.currentTimeMillis() - time) + " ms");
                            time = System.currentTimeMillis();
                        } else if ("RAMDirectory".equals(index.getDirectoryType())) {
                            GZIPInputStream gzipInput = new GZIPInputStream(is);
                            ObjectInputStream objectInput = new ObjectInputStream(gzipInput);

                            try {
                                Object object = objectInput.readObject();

                                if (object instanceof FSDirectory) {
                                    RAMDirectory directory = (RAMDirectory) object;
                                    index.setDirectory(directory);

                                    time = System.currentTimeMillis() - time;
                                    LOG.info("Successfully restored RAM directory"
                                            + " from database in " + time + " ms");
                                }
                            } finally {
                                objectInput.close();
                                gzipInput.close();
                            }
                        } else {
                            LOG.warn("Unknown directory type specified: "
                                    + index.getDirectoryType());
                        }

                        LOG.info("Directory: " + index.getDirectory());
                    } else {
                        LOG.warn("index is null!");
                    }
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
    }

    @SuppressWarnings("unchecked")
    private static void createIndex(ObjectStore os) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing metadata...");

        // special classes are the classes we index in addition to bioentities
        // and/or
        // classes which have special references that need to be indexed with
        // the objects
        HashMap<Class<? extends InterMineObject>, String[]> specialIndexClasses =
                new HashMap<Class<? extends InterMineObject>, String[]>();

        // load config file to figure out special classes
        String configFileName = "keyword_search.properties";
        ClassLoader classLoader = KeywordSearch.class.getClassLoader();
        InputStream configStream = classLoader.getResourceAsStream(configFileName);
        if (configStream != null) {
            Properties properties = new Properties();
            try {
                properties.load(configStream);

                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    String key = (String) entry.getKey();
                    String value = (String) entry.getValue();

                    if (key.startsWith("index.class.")) {
                        String classToIndex = key.substring("index.class.".length());
                        ClassDescriptor cld = os.getModel().getClassDescriptorByName(classToIndex);
                        if (cld != null) {
                            Class<? extends InterMineObject> cls =
                                    (Class<? extends InterMineObject>) cld.getType();

                            // special fields (references to follow) come as a
                            // space-separated list
                            String[] specialFields;
                            if (!StringUtils.isBlank(value)) {
                                specialFields = value.split("\\s+");
                                for (int i = 0; i < specialFields.length; i++) {
                                    specialFields[i] = classToIndex + "." + specialFields[i];
                                }
                            } else {
                                specialFields = null;
                            }

                            specialIndexClasses.put(cls, specialFields);
                        } else {
                            LOG.error("keyword_search.properties: classDescriptor for '"
                                    + classToIndex + "' not found!");
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error("keyword_search.properties: errow while loading file '" + configFileName
                        + "'", e);
            }
        } else {
            LOG.error("keyword_search.properties: file '" + configFileName + "' not found!");
        }

        for (Entry<Class<? extends InterMineObject>, String[]> e : specialIndexClasses.entrySet()) {
            LOG.info("Indexing additional class: " + e.getKey() + ", special fields = "
                    + Arrays.toString(e.getValue()));
        }

        // fetching happens in multiple threads since the bottleneck is
        // generally the DB
        ExecutorService fetchThreadPool = Executors.newFixedThreadPool(5);
        HashMap<Future<?>, DocumentFetcher> fetchThreads =
                new HashMap<Future<?>, DocumentFetcher>();

        // start threads for special classes
        for (Class<? extends InterMineObject> specialClass : specialIndexClasses.keySet()) {
            addFetcherThread(os, fetchThreadPool, fetchThreads, specialIndexClasses, specialClass,
                    true);
        }

        // start general thread for all bioentities
        addFetcherThread(os, fetchThreadPool, fetchThreads, specialIndexClasses, BioEntity.class,
                false);

        // index the docs queued by the fetchers
        index = new LuceneIndexContainer();
        try {
            File directoryPath = new File(LUCENE_INDEX_PATH);
            index.setDirectory(FSDirectory.open(directoryPath));
            index.setDirectoryType("FSDirectory");

            // make sure we start with a new index
            if (directoryPath.exists()) {
                String files[] = directoryPath.list();
                for (int i = 0; i < files.length; i++) {
                    LOG.info("Deleting old file: " + files[i]);
                    new File(directoryPath.getAbsolutePath() + File.separator + files[i]).delete();
                }
            }
        } catch (IOException e) {
            LOG.error("Could not create index directory, using RAM!", e);
            index.setDirectory(new RAMDirectory());
            index.setDirectoryType("RAMDirectory");
        }

        IndexWriter writer;
        try {
            SnowballAnalyzer snowballAnalyzer =
                    new SnowballAnalyzer(Version.LUCENE_30, "English",
                            StopAnalyzer.ENGLISH_STOP_WORDS_SET);
            writer =
                    new IndexWriter(index.getDirectory(), snowballAnalyzer, true,
                            IndexWriter.MaxFieldLength.UNLIMITED);

            int indexed = 0;

            // loop and index while we still have fetchers running
            while (true) {
                Document doc = null;
                doc = indexingQueue.poll();

                // nothing in the queue?
                if (doc == null) {
                    if (fetchThreads.size() > 0) {
                        // purge thread list
                        purgeFetcherThreads(fetchThreads);

                        // sleep a bit
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        // only stop if the queue is actually empty
                        if (indexingQueue.isEmpty()) {
                            break;
                        }
                    }
                } else {
                    try {
                        writer.addDocument(doc);
                        indexed++;
                    } catch (IOException e) {
                        LOG.error("Failed to submit #" + doc.getFieldable("id") + " to the index",
                                e);
                    }

                    if (indexed % 10000 == 1) {
                        purgeFetcherThreads(fetchThreads);

                        LOG.info("docs indexed=" + indexed + "; threads=" + fetchThreads.size()
                                + "; docs/ms=" + indexed * 1.0F
                                / (System.currentTimeMillis() - time) + "; memory="
                                + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                                + Runtime.getRuntime().maxMemory() / 1024 + "k");
                    }
                }
            }

            LOG.info("Indexing done, optimizing index files...");

            try {
                writer.optimize();
                writer.close();
            } catch (IOException e) {
                LOG.error("IOException while optimizing and closing IndexWriter", e);
            }

            time = System.currentTimeMillis() - time;
            int seconds = (int) Math.floor(time / 1000);
            LOG.info("Indexing of "
                    + indexed
                    + " documents finished in "
                    + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                            time % 1000) + " minutes");
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }
    }

    private static void addFetcherThread(ObjectStore os, ExecutorService fetchThreadPool,
            HashMap<Future<?>, DocumentFetcher> fetchThreads,
            HashMap<Class<? extends InterMineObject>, String[]> specialIndexClasses,
            Class<? extends InterMineObject> cls, boolean overwrite) {
        DocumentFetcher fetcher =
                new DocumentFetcher(os, indexingQueue, specialIndexClasses, cls, overwrite);
        fetchThreads.put(fetchThreadPool.submit(fetcher), fetcher);
    }

    private static void purgeFetcherThreads(HashMap<Future<?>, DocumentFetcher> fetchThreads) {
        for (Iterator<Future<?>> iterator = fetchThreads.keySet().iterator(); iterator.hasNext();) {
            Future<?> future = iterator.next();
            if (future.isDone()) {
                index.getFieldNames().addAll(fetchThreads.get(future).getFieldNames());
                iterator.remove();
            }
        }
    }
}