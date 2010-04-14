package org.modmine.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionProperty;
import org.intermine.objectstore.ObjectStore;

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
            Map<Integer, Set<String>> subProps = readSubmissionsFromCache(im.getObjectStore());
            indexMetadata(subProps);
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
    


    private static Map<Integer, Set<String>> readSubmissionsFromCache(ObjectStore os) {
        Map<Integer, Set<String>> subProps = new HashMap<Integer, Set<String>>();
        
       
        for (DisplayExperiment exp : MetadataCache.getExperiments(os)) {
            for (Submission sub : exp.getSubmissions()) {
                Integer subId = sub.getId();
                Integer dccId = sub.getdCCid();
                
                // submission details
                addMetaData(subProps, subId, sub.getdCCid().toString());
                addMetaData(subProps, subId, sub.getTitle());
                addMetaData(subProps, subId, sub.getDescription());
                addMetaData(subProps, subId, sub.getExperimentType());
                addMetaData(subProps, subId, sub.getOrganism().getName());
                String genus = sub.getOrganism().getGenus();
                if (genus.equals("Drosophila")) {
                    addMetaData(subProps, subId, "fly");
                } else if (genus.equals("Caenorhabditis")) {
                    addMetaData(subProps, subId, "worm");
                }
                
                // experiment details
                addMetaData(subProps, subId, exp.getPi());
                addMetaData(subProps, subId, exp.getName());
                addMetaData(subProps, subId, exp.getDescription());
                addMetaData(subProps, subId, exp.getProjectName());
                for (String lab : exp.getLabs()) {
                    addMetaData(subProps, subId, lab);
                }
                
                // add submission properties
                for (SubmissionProperty prop : sub.getProperties()) {
                    // TODO reflection to find properties of subclasses as well
                    addMetaData(subProps, subId, prop.getName());
                    addMetaData(subProps, subId, prop.getType());
                }
                // add feature types
                Map<String, Long> features = MetadataCache.getSubmissionFeatureCounts(os, dccId);
                if (features != null) {
                    for (String type: features.keySet()) {
                        addMetaData(subProps, subId, type);
                    }
                }
                
                // add database repository types
                for (String db : exp.getReposited().keySet()) {
                    addMetaData(subProps, subId, db);
                }
            }
        }
        
        return subProps;
    }
    
    
    public static Map<Integer, Integer> getSubMap() {
        return submissionMap;
    }
    
    private static void addMetaData(Map<Integer, Set<String>> subProps, Integer objectId, String property) {
        Set<String> props = subProps.get(objectId);
        if (props == null) {
            props = new HashSet<String>();
            subProps.put(objectId, props);
        }
        props.add(property);
    }

    private static void addToDocument(Document doc, Integer objectId, String property) {
        addToDocument(doc, objectId, property, 1.0F);
    }
    
    private static void addToDocument(Document doc, Integer objectId, String property, float boost) {
        Field f = new Field("content", property, Field.Store.NO,
                              Field.Index.TOKENIZED);
        f.setBoost(boost);
        doc.add(f);
    }
    
    private static void indexMetadata(Map<Integer, Set<String>> subProps) {
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

        for (Integer objectId : subProps.keySet()) {
            Document doc = new Document();
            doc.add(new Field("name", objectId.toString(), Field.Store.YES,
                              Field.Index.TOKENIZED));
            StringBuffer contentBuffer = new StringBuffer();
            for (String prop : subProps.get(objectId)) {
                contentBuffer.append(' ').append(prop);
            }
            

            // normalise the text
            String content = contentBuffer.toString().replaceAll("[^a-zA-Z0-9]", " ");
            doc.add(new Field("content", content, Field.Store.NO,
                              Field.Index.TOKENIZED));
            try {
                writer.addDocument(doc);
                indexed++;
            } catch (IOException e) {
                LOG.error("Failed to submission " + objectId
                        + " to the index", e);
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        LOG.info("Indexed " + indexed + " out of " + subProps.size() + " webSearchables in "
                + time + " milliseconds");
    }
    
    
    
}
