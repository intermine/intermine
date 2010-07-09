package org.flymine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Exon;
import org.intermine.model.bio.GOTerm;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Pathway;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

class ClassAttributes {
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

class indexFetcher implements Runnable {
    private static final Logger LOG = Logger.getLogger(indexFetcher.class);
    
    ObjectStore os;
    LinkedList<InterMineObject> indexingQueue;
    Class<? extends InterMineObject> cls;
    
    public indexFetcher(ObjectStore os, LinkedList<InterMineObject> indexingQueue, Class<? extends InterMineObject> cls) {
        this.os = os;
        this.indexingQueue = indexingQueue;
        this.cls = cls;
    }
    
    @SuppressWarnings("unchecked")
    public void run() {
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
            
            if(i % 10000 == 1) {
                LOG.info("'" + cls.getSimpleName() + "': fetched "+i+" of "+size);
            }
            
            for(InterMineObject object : row) {
                synchronized(os) { //TODO maybe sync on something else?
                    indexingQueue.add(object);
                }
            }
                     
            i++;
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time/1000);
        LOG.info("Fetched " + i + " objects of class '" + cls.getSimpleName() + "' in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds/60), seconds % 60, time % 1000)
                + " minutes");
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
    
    private static LinkedList<InterMineObject> indexingQueue = new LinkedList<InterMineObject>();
    
    private static RAMDirectory ram = null;
    private static HashSet<String> fieldNames = new HashSet<String>();
    private static HashMap<String, Float> fieldBoosts = new HashMap<String, Float>();

    /**
     * index document metadata in preparation for first search
     * @param im
     *            API for accessing object store
     */
    public static void initKeywordSearch(InterMineAPI im) {
        if (ram == null) {

            indexFromDatabase(im.getObjectStore());

            LOG.info("Field names: " + fieldNames.toString());
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
            IndexSearcher searcher = new IndexSearcher(ram);

            Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                    StopAnalyzer.ENGLISH_STOP_WORDS_SET);

            org.apache.lucene.search.Query query;

            // pass entire list of field names to the multi-field parser
            // => search through all fields
            String[] fieldNamesArray = new String[fieldNames.size()];
            fieldNames.toArray(fieldNamesArray);
            QueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_30, fieldNamesArray,
                    analyzer, fieldBoosts);
            query = queryParser.parse(queryString);

            // required to expand search terms
            query = query.rewrite(IndexReader.open(ram));
            LOG.debug("Actual query: " + query);

            TopDocs topDocs = searcher.search(query, 500);

            time = System.currentTimeMillis() - time;
            LOG.info("Found " + topDocs.totalHits + " document(s) that matched query '"
                    + queryString + "' in " + time + " milliseconds:");

            for (int i = 0; (i < MAX_HITS && i < topDocs.totalHits); i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                Integer id = Integer.valueOf(doc.get("id"));

//                // show how score was calculated
//                if (i < 2) {
//                    LOG.info("Score for #" + id + ": "
//                            + searcher.explain(query, topDocs.scoreDocs[i].doc));
//                }

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
    
    private static void indexFromDatabase(ObjectStore os) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing metadata...");

        ExecutorService fetchThreadPool = Executors.newFixedThreadPool(3);
        
        Vector<Future<?>> fetchThreads = new Vector<Future<?>>();
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, Gene.class)));
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, Exon.class)));
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, Transcript.class)));
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, Pathway.class)));
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, GOTerm.class)));
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, Protein.class)));
        fetchThreads.add(fetchThreadPool.submit(new indexFetcher(os, indexingQueue, Organism.class)));

        ram = new RAMDirectory();
        IndexWriter writer;
        try {
            SnowballAnalyzer snowballAnalyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                    StopAnalyzer.ENGLISH_STOP_WORDS_SET);
            writer = new IndexWriter(ram, snowballAnalyzer, true,
                    IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }
        
        int indexed = 0;

        //loop and index while we still have fetchers running
        while(fetchThreads.size() > 0) {
            InterMineObject object = null;
            synchronized(os) {
                object = indexingQueue.poll();
            }
            
            //nothing in the queue?
            if(object == null) {
                //purge thread list
                for (Iterator<Future<?>> iterator = fetchThreads.iterator(); iterator.hasNext();) {
                    Future<?> future = (Future<?>) iterator.next();
                    if(future.isDone()) {
                        iterator.remove();
                    }
                }
                
                //sleep a bit
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
            } else {
                Document doc = new Document();
                doc.add(new Field("id", object.getId().toString(), Field.Store.YES, Field.Index.NO));
                
                HashMap<String, String> attributes = getAttributeMapForObject(os.getModel(), object);
                for (Entry<String, String> attribute : attributes.entrySet()) {
                    addToDocument(doc, attribute.getKey(), attribute.getValue());
                }
                
                try {
                    writer.addDocument(doc);
                    indexed++;
                } catch (IOException e) {
                    LOG.error("Failed to submit #" + doc.getFieldable("id") + " to the index", e);
                }
                
                if(indexed % 10000 == 1) {
                    LOG.info("docs indexed="+indexed
                            + "; queue size="+indexingQueue.size()
                            + "; docs/ms=" + indexed*1.0F/(System.currentTimeMillis() - time)
                            + "; memory=" + Runtime.getRuntime().freeMemory()/1024
                            + "k/" + Runtime.getRuntime().maxMemory()/1024 + "k");
                }
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        int seconds = (int) Math.floor(time/1000);
        LOG.info("Indexing finished (" + indexed
                + " docs with " + fieldNames.size()
                + " unique fields and "+ fieldBoosts.size()
                + " boosts) in "
                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds/60), seconds % 60, time % 1000)
                + " minutes");
    }

//    @SuppressWarnings("unchecked")
//	private static void indexClassFromDatabase(ObjectStore os, IndexWriter writer, Class<?> classToIndex) {
//        long time = System.currentTimeMillis();
//        int indexed = 0;
//
//        LOG.info("Indexing class '" + classToIndex.getSimpleName() + "'...");
//        
//        Query q = new Query();
//        QueryClass qcGene = new QueryClass(classToIndex);
//        q.addFrom(qcGene);
//        q.addToSelect(qcGene);
//
//        Results results = os.execute(q);
//
//        Iterator<ResultsRow<InterMineObject>> it = results.iterator();
//        int i = 0;
//        LOG.info("Query returned " + results.size() + " results");
//        
//        while (it.hasNext()) {
//            ResultsRow<InterMineObject> row = it.next();
//            
//            if(i % 10000 == 1) {
//            	LOG.info("docs indexed="+indexed
//            			+ "; speed=" + (System.currentTimeMillis() - time)*1.0F/indexed + " ms/doc"
//            			+ "; free memory=" + Runtime.getRuntime().freeMemory()/1024 + "k/"
//            			+ Runtime.getRuntime().maxMemory()/1024 + "k");
//            }
//            
//            for(InterMineObject object : row) {
//                Document doc = new Document();
//                doc.add(new Field("id", object.getId().toString(), Field.Store.YES, Field.Index.NO));
//                
//                HashMap<String, String> attributes = getAttributeMapForObject(os.getModel(), object);
//                for (Entry<String, String> attribute : attributes.entrySet()) {
//					addToDocument(doc, attribute.getKey(), attribute.getValue());
//                }
//                
//                try {
//                    writer.addDocument(doc);
//                    indexed++;
//                } catch (IOException e) {
//                    LOG.error("Failed to submit #" + doc.getFieldable("id") + " to the index", e);
//                }
//            }
//            
//            i++;
//        }
//
//        time = System.currentTimeMillis() - time;
//        int seconds = (int) Math.floor(time/1000);
//        LOG.info("Indexed " + indexed + " documents for class '" + classToIndex.getSimpleName() + "' in "
//                + String.format("%02d:%02d.%03d", (int) Math.floor(seconds/60), seconds % 60, time % 1000)
//                + " minutes");
//    }
    
    //simple caching of attributes
    static HashMap<Class<?>, Vector<ClassAttributes>> decomposedClassesCache = new HashMap<Class<?>, Vector<ClassAttributes>>();
    private static Vector<ClassAttributes> getClassAttributes(Model model, Class<?> baseClass) {
        Vector<ClassAttributes> attributes = decomposedClassesCache.get(baseClass);
        
        if (attributes == null) {
            LOG.info("decomposedClassesCache: No entry for " + baseClass + ", adding...");
            attributes = new Vector<ClassAttributes>();
            
            for (Class<?> cls : DynamicUtil.decomposeClass(baseClass)) {
                ClassDescriptor cld = model.getClassDescriptorByName(cls.getName());
                attributes.add(new ClassAttributes(cld.getUnqualifiedName(), cld.getAllAttributeDescriptors()));
            }
            
            decomposedClassesCache.put(baseClass, attributes);
        }
        
        return attributes;
    }

    private static HashMap<String, String> getAttributeMapForObject(Model model, Object obj) {
        HashMap<String, String> values = new HashMap<String, String>();
        Vector<ClassAttributes> decomposedClassAttributes = getClassAttributes(model, obj.getClass());

        for (ClassAttributes classAttributes : decomposedClassAttributes) {
            for(AttributeDescriptor att : classAttributes.getAttributes()) {
                try {
                    if("java.lang.String".equals(att.getType())) {                  
                        Object value = TypeUtil.getFieldValue(obj, att.getName());
                        
                        // ignore null values
                        if (value != null) {
                            values.put((classAttributes.getClassName() + "_" + att.getName()).toLowerCase(),
                                    String.valueOf(value));
                        }
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Error introspecting a SubmissionProperty: " + obj, e);
                }
            }
        }

        return values;
    }

    private static void addToDocument(Document doc,
            String fieldName, String value) {
        if (!StringUtils.isBlank(fieldName) && !StringUtils.isBlank(value)) {
//            LOG.debug("ADDED FIELD: " + fieldName + " = " + value);

            Field f = new Field(fieldName, value, Field.Store.NO, Field.Index.ANALYZED);
            doc.add(f);
            fieldNames.add(fieldName);
        }
    }

}