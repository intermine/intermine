package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
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
import org.intermine.model.bio.Antibody;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Strain;
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionProperty;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * allows for full-text searches over all metadata using the apache lucene
 * engine
 * @author nils
 */
public class ModMineSearch
{
    // public static final String SEARCH_KEY = "modminesearch";

    /**
     * maximum number of hits returned
     */
    public static final int MAX_HITS = 500;

    private static final Logger LOG = Logger.getLogger(ModMineSearch.class);
    private static final String[] BOOLEAN_WORDS = {"AND", "OR", "NOT"};

    private static RAMDirectory ram = null;
    private static HashSet<String> fieldNames = new HashSet<String>();
    private static HashMap<String, Float> fieldBoosts = new HashMap<String, Float>();

    /**
     * index document metadata in preparation for first search
     * @param im
     *            API for accessing object store
     */
    public static synchronized void initModMineSearch(InterMineAPI im) {
        if (ram == null) {

            // Map<Integer, Set<String>> subProps =
            // readSubmissionProperties(im);
            Set<Document> docs = readSubmissionsFromCache(im.getObjectStore());
            indexMetadata(docs);

            LOG.debug("Field names: " + fieldNames.toString());
            LOG.debug("Boosts: " + fieldBoosts.toString());
        }
    }

    
    private static String prepareQueryString(String formInput)
    {
        // to lowercase the search string terms but not the operators
        // TODO it should probably go into the parseQueryString method
        String[] result = formInput.split("\\s");
        StringBuffer newString = new StringBuffer();
        for (String token : result) {
            if (token.equalsIgnoreCase("AND") ||
                    token.equalsIgnoreCase("OR") ||
                    token.equalsIgnoreCase("NOT"))
            {
                newString.append(token.toUpperCase() + " ");
            }
            else {
                newString.append(token.toLowerCase() + " ");
            }
        }
        LOG.debug("QUERYSTRING " + newString.toString());        
        return newString.toString();
    }

    
    /**
     * perform a keyword search over all document metadata fields with lucene
     * @param searchString
     *            string to search for
     * @return map of document IDs with their respective scores
     */
    public static Map<Integer, Float> runLuceneSearch(String searchString) {
        LinkedHashMap<Integer, Float> matches = new LinkedHashMap<Integer, Float>();        
        String queryString = parseQueryString(prepareQueryString(searchString));

        long time = System.currentTimeMillis();

        try {
            IndexSearcher searcher = new IndexSearcher(ram);
            Analyzer analyzer = new WhitespaceAnalyzer();
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
                String name = doc.get("name");

                // show how score was calculated
                if (i < 2) {
                    LOG.debug("Score for " + name + ": "
                            + searcher.explain(query, topDocs.scoreDocs[i].doc));
                }

                matches.put(Integer.parseInt(name), new Float(topDocs.scoreDocs[i].score));
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

    private static Set<Document> readSubmissionsFromCache(ObjectStore os) {
        long time = System.currentTimeMillis();
        LOG.info("Creating documents from metadata...");
        Set<Document> docs = new HashSet<Document>();

        for (DisplayExperiment exp : MetadataCache.getExperiments(os)) {
            for (Submission sub : exp.getSubmissions()) {

                Integer subId = sub.getId();
                String dccId = sub.getdCCid();

                Document doc = new Document();
                doc.add(new Field("name", subId.toString(), Field.Store.YES, Field.Index.ANALYZED));

                // submission details
                addToDocument(doc, subId, "dCCid", sub.getdCCid().toString());
                if (dccId.startsWith("modENCODE_")) {
                    addToDocument(doc, subId, "dCCid",
                            sub.getdCCid().substring("modENCODE_".length()));
                }
                addToDocument(doc, subId, "title", sub.getTitle());
                addToDocument(doc, subId, "description", sub.getDescription());

                if (sub.getExperimentType() != null) {
                    Field f = new Field("experiment_type", sub.getExperimentType(), Field.Store.NO,
                            Field.Index.NOT_ANALYZED);
                    fieldNames.add("experiment_type");
                    doc.add(f);
                }
                addToDocument(doc, subId, "organism", sub.getOrganism().getName());
                String genus = sub.getOrganism().getGenus();
                if (genus != null && "Drosophila".equals(genus)) {
                    addToDocument(doc, subId, "genus", "fly");
                } else if (genus != null && "Caenorhabditis".equals(genus)) {
                    addToDocument(doc, subId, "genus", "worm");
                }

                // experiment details
                addToDocument(doc, subId, "pi", exp.getPi());
                addToDocument(doc, subId, "experiment_name", exp.getName());
                addToDocument(doc, subId, "description", exp.getDescription());
                addToDocument(doc, subId, "project_name", exp.getProjectName());
                for (String lab : exp.getLabs()) {
                    addToDocument(doc, subId, "lab", lab);
                }

                // add submission properties
                for (SubmissionProperty prop : sub.getProperties()) {
                    Map<String, String> attributes = getAttributeMapForObject(os.getModel(), prop);

                    for (String att : attributes.keySet()) {
                        addToDocument(doc, subId, att, attributes.get(att));
                    }

                    // for antibody or strain, also add the target gene
                    Gene gene = null;
                    String geneType = null;
                    if (prop instanceof Antibody) {
                        gene = ((Antibody) prop).getTarget();
                        geneType = "antibody";
                    } else if (prop instanceof Strain) {
                        gene = ((Strain) prop).getTarget();
                        geneType = "strain";
                    }

                    if (gene != null) {
                        LOG.debug("Found gene => " + gene.getPrimaryIdentifier() + " for "
                                + prop.getClass().getName());

                        addToDocument(doc, subId, geneType + "_gene_primary_identifier", gene
                                .getPrimaryIdentifier());
                        addToDocument(doc, subId, geneType + "_gene_secondary_identifier", gene
                                .getSecondaryIdentifier());
                        addToDocument(doc, subId, geneType + "_gene_symbol", gene.getSymbol());
                        addToDocument(doc, subId, geneType + "_gene_name", gene.getName());
                    }
                }

                // add feature types
                Map<String, Long> features = MetadataCache.getSubmissionFeatureCounts(os, dccId);
                if (features != null) {
                    for (String type : features.keySet()) {
                        addToDocument(doc, subId, "features", type);
                    }
                }

                // add database repository types
                for (String db : exp.getReposited().keySet()) {
                    addToDocument(doc, subId, "databases", db);
                }

                docs.add(doc);
            }
        }

        time = System.currentTimeMillis() - time;
        LOG.info("Created " + docs.size() + " documents (" + fieldNames.size()
                + " unique fields and " + fieldBoosts.size() + " boosts) in " + time
                + " milliseconds");

        return docs;
    }

    private static HashMap<String, String> getAttributeMapForObject(Model model, Object obj) {
        HashMap<String, String> values = new HashMap<String, String>();
        for (Class<?> cls : DynamicUtil.decomposeClass(obj.getClass())) {
            ClassDescriptor cld = model.getClassDescriptorByName(cls.getName());
            for (AttributeDescriptor att : cld.getAllAttributeDescriptors()) {
                try {
                    Object value = TypeUtil.getFieldValue(obj, att.getName());
                    // ignore null values and wikiLinks
                    if (value != null
                            && !(value instanceof String
                                    && ((String) value).startsWith("http://"))) {
                        values.put((cld.getUnqualifiedName() + "_" + att.getName()).toLowerCase(),
                                String.valueOf(value));
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Error introspecting a SubmissionProperty: " + obj, e);
                }
            }
        }

        return values;
    }

    private static void addToDocument(Document doc, Integer objectId,
            String fieldName, String value) {
        if (!StringUtils.isBlank(fieldName) && !StringUtils.isBlank(value)) {
            LOG.debug("ADDED FIELD TO #" + objectId + ": " + fieldName + " = " + value);

            Field f = new Field(fieldName, value.toLowerCase(), Field.Store.NO,
                    Field.Index.ANALYZED);
            doc.add(f);
            fieldNames.add(fieldName);
        }
    }

    private static void indexMetadata(Set<Document> docs) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing metadata...");

        int indexed = 0;

        ram = new RAMDirectory();
        IndexWriter writer;
        try {
            writer = new IndexWriter(ram, new WhitespaceAnalyzer(), true,
                    IndexWriter.MaxFieldLength.UNLIMITED);
            for (Document doc : docs) {
                try {
                    writer.addDocument(doc);
                    indexed++;
                } catch (IOException e) {
                    LOG.error("Failed to submit doc #" + doc.getFieldable("name") + " to the index",
                            e);
                }
            }

            try {
                writer.close();
            } catch (IOException e) {
                LOG.error("IOException while closing IndexWriter", e);
            }
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }

        time = System.currentTimeMillis() - time;
        LOG.info("Indexed " + indexed + " out of " + docs.size() + " submissions in " + time
                + " milliseconds");
    }

}
