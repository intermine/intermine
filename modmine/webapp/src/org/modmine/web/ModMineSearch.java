package org.modmine.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionProperty;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

public class ModMineSearch
{

    public static String SEARCH_KEY = "modminesearch";
    public static int MAX_HITS = 500;
    
    private static final Logger LOG = Logger.getLogger(ModMineSearch.class);
    private static RAMDirectory ram = null;
    private static Map<Integer, Integer> submissionMap = new HashMap<Integer, Integer>();
    
    public static void initModMineSearch(InterMineAPI im) {
        if (ram == null) {
            //Map<Integer, Set<String>> subProps = readSubmissionProperties(im);
            Set<Document> docs = readSubmissionsFromCache(im.getObjectStore());
            indexMetadata(docs);
        }
    }
    
    public static Map<Integer, Float> runLuceneSearch(String searchString) {
        LinkedHashMap<Integer, Float> matches = new LinkedHashMap<Integer, Float>();
        
        String queryString = searchString.replaceAll("(\\w+log\\b)", "$1ue $1");
        queryString = queryString.replaceAll("[^a-zA-Z0-9]", " ").trim();
        queryString = queryString.replaceAll("(\\w+)$", "$1 $1*");

        long time = System.currentTimeMillis();

        try {
            IndexSearcher searcher = new IndexSearcher(ram);

            Analyzer analyzer = new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);

            org.apache.lucene.search.Query query;
            QueryParser queryParser = new QueryParser("content", analyzer);
            query = queryParser.parse(queryString);

            // required to expand search terms
            query = query.rewrite(IndexReader.open(ram));
            Hits hits = searcher.search(query);
            
            time = System.currentTimeMillis() - time;
            LOG.info("Found " + hits.length() + " document(s) that matched query '"
                    + queryString + "' in " + time + " milliseconds:");

            //QueryScorer scorer = new QueryScorer(query);

            for (int i = 0; (i < MAX_HITS && i < hits.length()); i++) {
                Document doc = hits.doc(i);
                String name = doc.get("name");

                matches.put(Integer.parseInt(name), new Float(hits.score(i)));
            }
            // TODO proper error handling
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return matches;
    }


    private static Set<Document> readSubmissionsFromCache(ObjectStore os) {
        
        Set<Document> docs = new HashSet<Document>();
        
        for (DisplayExperiment exp : MetadataCache.getExperiments(os)) {
            for (Submission sub : exp.getSubmissions()) {

                Integer subId = sub.getId();
                Integer dccId = sub.getdCCid();

                Document doc = new Document();
                doc.add(new Field("name", subId.toString(), Field.Store.YES,
                        Field.Index.TOKENIZED));
                
                // submission details
                addToDocument(doc, subId, sub.getdCCid().toString());
                addToDocument(doc, subId, sub.getTitle(), 0.8F);
                addToDocument(doc, subId, sub.getDescription(), 0.2F);
                //addToDocument(doc, subId, sub.getExperimentType(), 0.5F);
                
                if (sub.getExperimentType() != null) {
                    Field f = new Field("content", sub.getExperimentType(), Field.Store.NO,
                            Field.Index.UN_TOKENIZED);
                    //f.setBoost(5.0F);
                    doc.add(f);
                }
                addToDocument(doc, subId, sub.getOrganism().getName());
                String genus = sub.getOrganism().getGenus();
                if (genus != null && genus.equals("Drosophila")) {
                    addToDocument(doc, subId, "fly", 2.0F);
                } else if (genus != null && genus.equals("Caenorhabditis")) {
                    addToDocument(doc, subId, "worm", 2.0F);
                }
                
                // experiment details
                addToDocument(doc, subId, exp.getPi());
                addToDocument(doc, subId, exp.getName());
                addToDocument(doc, subId, exp.getDescription(), 0.2F);
                addToDocument(doc, subId, exp.getProjectName());
                for (String lab : exp.getLabs()) {
                    addToDocument(doc, subId, lab);
                }
                
                // add submission properties
                for (SubmissionProperty prop : sub.getProperties()) {
                    // give higher weight to attributes of submission properties
                    for (String attValue : getAttributeValuesForObject(os.getModel(), prop)) {
                        addToDocument(doc, subId, attValue, 1.5F);
                    }
                    
                }
                // add feature types
                Map<String, Long> features = MetadataCache.getSubmissionFeatureCounts(os, dccId);
                if (features != null) {
                    for (String type: features.keySet()) {
                        addToDocument(doc, subId, type);
                    }
                }
                
                // add database repository types
                for (String db : exp.getReposited().keySet()) {
                    addToDocument(doc, subId, db);
                }
                docs.add(doc);
            }
        }
        return docs;
    }
    
    
    public static Map<Integer, Integer> getSubMap() {
        return submissionMap;
    }


    private static List<String> getAttributeValuesForObject(Model model, Object obj) {
        List<String> values = new ArrayList<String>();
        for (Class<?> cls : DynamicUtil.decomposeClass(obj.getClass())) {
            ClassDescriptor cld = model.getClassDescriptorByName(cls.getName());
            for (AttributeDescriptor att : cld.getAllAttributeDescriptors()) {
                try {
                    Object value = TypeUtil.getFieldValue(obj, att.getName());
                    LOG.info("INDEXING " + cld.getUnqualifiedName() + "." + att.getName() + " = " + value);
                    if (value != null) {
                        values.add(value.toString());
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Error introspecting a SubmissionProperty: " + obj, e);
                }
            }
        }
        return values;
    }
    
    
    private static void addToDocument(Document doc, Integer objectId, String property) {
        addToDocument(doc, objectId, property, 1.0F);
    }
    
    private static void addToDocument(Document doc, Integer objectId, String property, float boost) {
        if (!StringUtils.isBlank(property)) {
            Field f = new Field("content", property, Field.Store.NO,
                    Field.Index.TOKENIZED);
            //f.setBoost(boost);
            doc.add(f);
        }
    }
    
    private static void indexMetadata(Set<Document> docs) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing metadata.");

        ram = new RAMDirectory();
        IndexWriter writer;
        try {
            SnowballAnalyzer snowballAnalyzer =
                new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);
            writer = new IndexWriter(ram, snowballAnalyzer, true);
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }

        int indexed = 0;
        
        for (Document doc : docs) {
            try {
                writer.addDocument(doc);
                indexed++;
            } catch (IOException e) {
                LOG.error("Failed to submission " + doc.getFieldable("name")
                        + " to the index", e);
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        LOG.info("Indexed " + indexed + " out of " + docs.size() + " submissions in "
                + time + " milliseconds");
    }
    
    
    
}
