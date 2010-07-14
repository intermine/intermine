package org.intermine.bio.search;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.tools.ant.util.OutputStreamFunneler;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.BioEntity;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Transcript;
import org.intermine.modelproduction.MetadataManager;
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
import org.postgresql.largeobject.BlobOutputStream;

class ClassAttributes
{
    String className;
    Set<AttributeDescriptor> attributes;

    public ClassAttributes(String className, Set<AttributeDescriptor> attributes) {
        super();
        this.className = className;
        this.attributes = attributes;
    }

    public String getClassName() {
        return className;
    }

    public Set<AttributeDescriptor> getAttributes() {
        return attributes;
    }
}

class LuceneIndexContainer implements Serializable {
    private static final long serialVersionUID = 1L;
    private RAMDirectory ram = new RAMDirectory();
    private HashSet<String> fieldNames = new HashSet<String>();
    private HashMap<String, Float> fieldBoosts = new HashMap<String, Float>();
    
    public RAMDirectory getRam() {
        return ram;
    }

    public void setRam(RAMDirectory ram) {
        this.ram = ram;
    }

    public HashSet<String> getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(HashSet<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    public HashMap<String, Float> getFieldBoosts() {
        return fieldBoosts;
    }

    public void setFieldBoosts(HashMap<String, Float> fieldBoosts) {
        this.fieldBoosts = fieldBoosts;
    }
    
    @Override
    public String toString() {
        return "INDEX [[" + ram.sizeInBytes()/1024/1024 + " MB"
            + ", fields = " + fieldNames + ""
            + ", boosts = " + fieldBoosts + ""
            + "]]";
    }
}

class DocumentFetcher implements Runnable
{
    private static final Logger LOG = Logger.getLogger(DocumentFetcher.class);

    ObjectStore os;
    LinkedList<Document> indexingQueue; //FIXME
    Class<? extends InterMineObject> cls;
    ClassDescriptor clsDescriptor;
    String[] references;

    HashMap<Integer, Document> documents = new HashMap<Integer, Document>();
    HashSet<String> fieldNames = new HashSet<String>();
    HashMap<Class<?>, Vector<ClassAttributes>> decomposedClassesCache = new HashMap<Class<?>, Vector<ClassAttributes>>();

    public DocumentFetcher(ObjectStore os, LinkedList<Document> indexingQueue,
            Class<? extends InterMineObject> cls) {
        this(os, indexingQueue, cls, null);
    }

    public DocumentFetcher(ObjectStore os, LinkedList<Document> indexingQueue,
            Class<? extends InterMineObject> cls, String[] references) {
        this.os = os;
        this.indexingQueue = indexingQueue;
        this.cls = cls;
        this.references = references;

        clsDescriptor = os.getModel().getClassDescriptorByName(cls.getName());
    }

    public HashSet<String> getFieldNames() {
        return fieldNames;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        try {
            long time = System.currentTimeMillis();
            LOG.info("Fetching class '" + cls.getSimpleName() + "'...");

            Query q = new Query();
            QueryClass qcGene = new QueryClass(cls);
            q.addFrom(qcGene);
            q.addToSelect(qcGene);

            Results results = os.execute(q);

            Iterator<ResultsRow<InterMineObject>> it = results.iterator();
            int i = 0;
            int size = results.size();
            LOG.info("Query returned " + size + " results");

            while (it.hasNext()) {
                ResultsRow<InterMineObject> row = it.next();

                if (i % 10000 == 1) {
                    LOG.info("'" + cls.getSimpleName() + "': fetched " + i + " of " + size);
                }

                for (InterMineObject object : row) {
                    Document doc = getDocumentForObject(object);
                    if (references == null) {
                        synchronized (os) {
                            indexingQueue.add(doc);
                        }
                    } else {
                        documents.put(object.getId(), doc);
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
                    + "' in "
                    + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                            time % 1000) + " minutes");

            if (references != null) {
                for(int j = 0; j < references.length; j++) {
                    String reference = references[j];
                    time = System.currentTimeMillis();
                    LOG.info(cls.getSimpleName() + "::" + reference + " - Querying...");

                    Query qc = getPathQuery(cls, reference);
                    Results resultsc = os.execute(qc);
                    int sizec = resultsc.size();

                    LOG.info(cls.getSimpleName() + "::" + reference + " - Query returned " + sizec
                            + " results");

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
                            
                            //speed - submit completed documents already if we are at last reference
                            if((j == references.length - 1) && previousId != docId) {
                                synchronized (os) {
                                    indexingQueue.add(documents.remove(previousId));
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
                }

                synchronized (os) {
                    LOG.info("Adding remaining " + documents.size() + " documents to queue");
                    indexingQueue.addAll(documents.values());
                }
            }
        } catch (Exception e) {
            LOG.warn(null, e);
        }
    }

    private Document getDocumentForObject(InterMineObject object) {
        Document doc = new Document();
        doc.add(new Field("id", object.getId().toString(), Field.Store.YES, Field.Index.NO));        
        doc.add(new Field("Category", clsDescriptor.getUnqualifiedName(), Field.Store.YES, Field.Index.ANALYZED));
        
        if(object instanceof BioEntity && ((BioEntity) object).getOrganism() != null) {
            doc.add(new Field("Organism", ((BioEntity) object).getOrganism().getShortName(), Field.Store.YES, Field.Index.ANALYZED));
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
        Vector<ClassAttributes> decomposedClassAttributes = getClassAttributes(model, obj
                .getClass());

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

    // private Query getCollectionQuery(Class<? extends InterMineObject>
    // mainCls, String collectionName) {
    // Query q = new Query();
    // QueryClass qcMain = new QueryClass(mainCls);
    // QueryField qfMainId = new QueryField(qcMain, "id");
    // q.addToSelect(qfMainId);
    // q.addFrom(qcMain);
    //
    // ClassDescriptor classDescriptor =
    // os.getModel().getClassDescriptorByName(mainCls.getName());
    // Class<?> classInCollection =
    // classDescriptor.getCollectionDescriptorByName(collectionName).getReferencedClassDescriptor().getType();
    //        
    // QueryClass qcInCollection = new QueryClass(classInCollection);
    // q.addToSelect(qcInCollection);
    // q.addFrom(qcInCollection);
    // QueryCollectionReference col = new QueryCollectionReference(qcMain,
    // collectionName);
    // ContainsConstraint cc = new ContainsConstraint(col,
    // ConstraintOp.CONTAINS, qcInCollection);
    // q.setConstraint(cc);
    //        
    // q.addToOrderBy(qfMainId);
    //       
    // return q;
    // }

    private Query getPathQuery(Class<? extends InterMineObject> mainCls, String pathString)
            throws PathException {
        Query q = new Query();
        ConstraintSet constraints = new ConstraintSet(ConstraintOp.AND);

        org.intermine.pathquery.Path path = new org.intermine.pathquery.Path(os.getModel(),
                pathString);
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
                q.addToOrderBy(topId); //important for optimization in run()
            } else {
                String fieldName = fieldNames.get(i - 1);

                LOG.info(fieldName + " -> " + classDescriptor);

                if (parentClassDescriptor.getReferenceDescriptorByName(fieldName, true) != null) {
                    LOG.info(fieldName + " is OBJECT");
                    QueryObjectReference objectReference = new QueryObjectReference(
                            parentQueryClass, fieldName);
                    ContainsConstraint cc = new ContainsConstraint(objectReference,
                            ConstraintOp.CONTAINS, queryClass);
                    constraints.addConstraint(cc);
                } else if (parentClassDescriptor.getCollectionDescriptorByName(fieldName, true) != null) {
                    LOG.info(fieldName + " is COLLECTION");
                    QueryCollectionReference collectionReference = new QueryCollectionReference(
                            parentQueryClass, fieldName);
                    ContainsConstraint cc = new ContainsConstraint(collectionReference,
                            ConstraintOp.CONTAINS, queryClass);
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
    // public static final String SEARCH_KEY = "modminesearch";

    /**
     * maximum number of hits returned
     */
    public static final int MAX_HITS = 500;

    private static final Logger LOG = Logger.getLogger(KeywordSearch.class);
    private static final String[] BOOLEAN_WORDS = {"AND", "OR", "NOT"};

    private static LinkedList<Document> indexingQueue = new LinkedList<Document>();
    private static LuceneIndexContainer index = null;

    /**
     * loads or creates the lucene index
     * @param im
     *            API for accessing object store
     */
    public static void initKeywordSearch(InterMineAPI im) {
        if (index == null) {
            //try to load index from database first
            loadIndexFromDatabase(im.getObjectStore());
            
            //still nothing, create new one
            if(index == null) {
                createIndex(im.getObjectStore());
            }
        }
    }
    
    public static void saveIndexToDatabase(ObjectStore os) {
        if(index == null) {
            createIndex(os);
        }     

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
//            PipedOutputStream out = new PipedOutputStream();
//            PipedInputStream in = new PipedInputStream(out);
//            BufferedOutputStream outb = new BufferedOutputStream(out);
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            
            try {
                LOG.info("memory 1="
                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                        + Runtime.getRuntime().maxMemory() / 1024 + "k");
                
                LOG.info("Serializing index...");
                objectStream.writeObject(index);

                LOG.info("memory 2="
                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                        + Runtime.getRuntime().maxMemory() / 1024 + "k");
                
                index = null;
                System.gc();

                LOG.info("memory 3="
                        + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                        + Runtime.getRuntime().maxMemory() / 1024 + "k");

                LOG.info("Saving index to database...");
                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();                
                MetadataManager.storeBinary(db, MetadataManager.SEARCH_INDEX, byteStream.toByteArray());
    
                LOG.info("Successfully saved search index to database: " + index);
            } catch (IOException e) {
                LOG.error(null, e);
            } catch (SQLException e) {
                LOG.error(null, e);
            } finally {
                objectStream.close();
            }
        } catch (IOException e) {
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
            IndexSearcher searcher = new IndexSearcher(index.getRam());

            Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                    StopAnalyzer.ENGLISH_STOP_WORDS_SET);

            org.apache.lucene.search.Query query;

            // pass entire list of field names to the multi-field parser
            // => search through all fields
            String[] fieldNamesArray = new String[index.getFieldNames().size()];
            index.getFieldNames().toArray(fieldNamesArray);
            QueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_30, fieldNamesArray,
                    analyzer, index.getFieldBoosts());
            query = queryParser.parse(queryString);

            // required to expand search terms
            query = query.rewrite(IndexReader.open(index.getRam()));
            LOG.debug("Actual query: " + query);

            TopDocs topDocs = searcher.search(query, 500);
            // Filter filter = new TermsFilter();
            // searcher.search(query, filter, collector);

            time = System.currentTimeMillis() - time;
            LOG.info("Found " + topDocs.totalHits + " document(s) that matched query '"
                    + queryString + "' in " + time + " milliseconds:");

            for (int i = 0; (i < MAX_HITS && i < topDocs.totalHits); i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                Integer id = Integer.valueOf(doc.get("id"));

                // show how score was calculated
                if (i < 5) {
                    Explanation explanation = searcher.explain(query, topDocs.scoreDocs[i].doc);
                    LOG.info("Score for #" + id + ": " + explanation);
                }

                matches.put(id, new Float(topDocs.scoreDocs[i].score));
            }
        } catch (ParseException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        } catch (IOException e) {
            // just return an empty list
            LOG.info("Exception caught, returning no results", e);
        }
        return matches;
    }

    private static String parseQueryString(String queryString) {
        queryString = queryString.replaceAll("\\b(\\s+)\\+(\\s+)\\b", "$1AND$2");

        // to support partial matches we have to add a asterisk to the end of
        // every word, taking care of keywords, quoted phrases and hyphenated
        // terms
        String queryStringNew = queryString;

        // find all words without special characters around them
        Pattern pattern = Pattern.compile("(?<!(\\w-|[:.]))\\b(\\w+)\\b(?![.:*-])");
        // remove all quoted terms
        Matcher matcher = pattern.matcher(queryString.replaceAll("\"[^\"]+\"", ""));
        HashSet<String> words = new HashSet<String>();
        while (matcher.find()) {
            String word = matcher.group(2);

            // ignore words that are boolean keywords
            boolean isKeyword = false;
            for (int i = 0; i < BOOLEAN_WORDS.length; i++) {
                if (BOOLEAN_WORDS[i].equals(word)) {
                    isKeyword = true;
                    break;
                }
            }

            if (isKeyword) {
                continue;
            }

            // only allow partial matches for words >= 3 characters
            if (word.length() > 2) {
                words.add(word);
            }
        }

        // finally replace all words by (word word*) -- separate from main loop
        // to avoid
        // issues with duplicates
        for (String word : words) {
            queryStringNew = queryStringNew.replaceAll("\\b(" + Pattern.quote(word) + ")\\b",
                    "($1 $1*)");
        }

        queryString = queryStringNew; // apply changes
        return queryString;
    }

    private static void loadIndexFromDatabase(ObjectStore os) {
        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            try {
                InputStream is = MetadataManager.retrieveBLOBInputStream(db,
                        MetadataManager.SEARCH_INDEX);

                if (is != null) {
                    ObjectInputStream objectInput = new ObjectInputStream(is);

                    try {
                        Object object = objectInput.readObject();

                        if (object instanceof LuceneIndexContainer) {
                            index = (LuceneIndexContainer)object;
                            LOG.info("Successfully restored search index from database: " + index);
                        }
                    } finally {
                        objectInput.close();
                    }
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Could not load search index", e);
            } catch (SQLException e) {
                LOG.error("Could not load search index", e);
            } catch (IOException e) {
                LOG.error("Could not load search index", e);
            }
        }
    }

    private static void createIndex(ObjectStore os) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing metadata...");

        ExecutorService fetchThreadPool = Executors.newFixedThreadPool(3);

        HashMap<Future<?>, DocumentFetcher> fetchThreads = new HashMap<Future<?>, DocumentFetcher>();

//        addFetcherThread(os, fetchThreadPool, fetchThreads, Gene.class, new String[] {
//            "Gene.pathways", "Gene.omimDiseases", "Gene.goAnnotation.ontologyTerm"});
//        addFetcherThread(os, fetchThreadPool, fetchThreads, Exon.class, null);
        addFetcherThread(os, fetchThreadPool, fetchThreads, Transcript.class, null);
        addFetcherThread(os, fetchThreadPool, fetchThreads, Pathway.class, null);
        addFetcherThread(os, fetchThreadPool, fetchThreads, GOTerm.class, null);
        addFetcherThread(os, fetchThreadPool, fetchThreads, Protein.class, null);
        addFetcherThread(os, fetchThreadPool, fetchThreads, Organism.class, null);

        index = new LuceneIndexContainer();
        IndexWriter writer;
        try {
            SnowballAnalyzer snowballAnalyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                    StopAnalyzer.ENGLISH_STOP_WORDS_SET);
            writer = new IndexWriter(index.getRam(), snowballAnalyzer, true,
                    IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }

        int indexed = 0;

        // loop and index while we still have fetchers running
        while (true) {
            Document doc = null;
            synchronized (os) {
                doc = indexingQueue.poll();
            }

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
                    break;
                }
            } else {
                try {
                    writer.addDocument(doc);
                    indexed++;
                } catch (IOException e) {
                    LOG.error("Failed to submit #" + doc.getFieldable("id") + " to the index", e);
                }

                if (indexed % 10000 == 1) {
                    purgeFetcherThreads(fetchThreads);

                    LOG.info("docs indexed=" + indexed + "; queue=" + indexingQueue.size()
                            + "; threads=" + fetchThreads.size() + "; docs/ms=" + indexed * 1.0F
                            / (System.currentTimeMillis() - time) + "; memory="
                            + Runtime.getRuntime().freeMemory() / 1024 + "k/"
                            + Runtime.getRuntime().maxMemory() / 1024 + "k");
                }
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time / 1000);
        LOG.info("Indexing finished in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds / 60), seconds % 60,
                        time % 1000) + " minutes");

        LOG.info("INDEXING: " + index);
    }

    private static void addFetcherThread(ObjectStore os, ExecutorService fetchThreadPool,
            HashMap<Future<?>, DocumentFetcher> fetchThreads, Class<? extends InterMineObject> cls,
            String[] collections) {
        DocumentFetcher fetcher = new DocumentFetcher(os, indexingQueue, cls, collections);
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