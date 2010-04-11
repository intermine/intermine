package org.modmine.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.PathQuery;

public class ModMineSearch
{

    public static String SEARCH_KEY = "modminesearch";
    public static int MAX_HITS = 100;
    
    private static final Logger LOG = Logger.getLogger(ModMineSearch.class);
    private static RAMDirectory ram = null;
    private static Map<Integer, Integer> submissionMap = new HashMap<Integer, Integer>();
    
    public static void initModMineSearch(InterMineAPI im) {
        if (ram == null) {
            Map<Integer, Set<String>> subProps = readSubmissionProperties(im);
            indexMetadata(subProps);
        }
    }
    
    public static Map<Integer, Float> runLuceneSearch(String searchString) {
        Map<Integer, Float> matches = new HashMap<Integer, Float>();
        
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

            for (int i = 0; i < MAX_HITS; i++) {
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
    
    
    private static Map<Integer, Set<String>> readSubmissionProperties(InterMineAPI im) {
        Map<Integer, Set<String>> subProps = new HashMap<Integer, Set<String>>();
        
        PathQuery query = new PathQuery(im.getModel());
        query.setView("Submission.id, Submission.DCCid, Submission.title, Submission.project.title, Submission.project.name, Submission.project.surnamePI, Submission.lab.name, Submission.experiment.name, Submission.organism.genus, Submission.organism.species, Submission.properties.type, Submission.properties.name");

        Profile superUser = im.getProfileManager().getSuperuserProfile();
        PathQueryExecutor executor = im.getPathQueryExecutor(superUser);
        ExportResultsIterator resIter = executor.execute(query);
        while (resIter.hasNext()) {
            List<ResultElement> row = resIter.next();
            Integer objectId = (Integer) row.get(0).getField();
            if (!subProps.containsKey(objectId)) {
                for (int i = 1; i < 10; i++) {
                    addMetaData(subProps, objectId, row.get(i).toString());
                }
                String genus = (String) row.get(8).getField();
                if (genus.equals("Drosophila")) {
                    addMetaData(subProps, objectId, "fly");
                } else if (genus.equals("Caenorhabditis")) {
                    addMetaData(subProps, objectId, "worm");
                }
                
                submissionMap.put(objectId, (Integer) row.get(1).getField());
            }
            addMetaData(subProps, objectId, row.get(10).getField().toString());
            addMetaData(subProps, objectId, row.get(11).getField().toString());
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
