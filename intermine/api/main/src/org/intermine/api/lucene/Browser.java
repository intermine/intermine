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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.intermine.api.InterMineAPI;
import org.intermine.api.data.Objects;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;

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
 * Α thing that can find result within an index.
 * @author Alex Kalderimis
 *
 */
public final class Browser
{

    private static final Logger LOG = Logger.getLogger(Browser.class);

    /**
     * maximum number of hits returned
     */
    public static final int MAX_HITS = 500;
    /**
     * maximum number of items to be displayed on a page
     */
    public static final int PER_PAGE = 100;

    private LuceneIndexContainer index;
    private IndexReader reader;
    private Configuration config;
    private BoboIndexReader boboIndexReader;

    /**
     * Create a new search browser.
     * @param index The index we will search
     * @param config The search configuration
     * @throws CorruptIndexException If the index is corrupt.
     * @throws IOException If we have trouble interacting with the outside world.
     */
    Browser(LuceneIndexContainer index, Configuration config)
            throws CorruptIndexException, IOException {
        this.index = index;
        this.config = config;
        reader = IndexReader.open(index.getDirectory(), true);
        HashSet<FacetHandler<?>> facetHandlers = new HashSet<FacetHandler<?>>();
        facetHandlers.add(new SimpleFacetHandler("Category"));
        for (KeywordSearchFacetData facet : config.getFacets()) {
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
    }

    /**
     * perform a keyword search over all document metadata fields with lucene
     * @param searchString
     *            string to search for
     * @return map of document IDs with their respective scores
     * @deprecated Use runBrowseSearch instead.
     */
    @Deprecated
    public Map<Integer, Float> runLuceneSearch(String searchString) {
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
    public Vector<KeywordSearchFacet> parseFacets(BrowseResult result,
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
    public Vector<KeywordSearchHit> getSearchHits(BrowseHit[] browseHits,
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
    public ResultsWithFacets runBrowseWithFacets(
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
            searchResultsFacets = parseFacets(results, config.getFacets(), facetValues);
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
    public BrowseResult runBrowseSearch(String searchString, int offset,
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
    public BrowseResult runBrowseSearch(String searchString, int offset,
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

            LOG.debug("Rewritten query: " + query);

            // initialize request
            BrowseRequest browseRequest = new BrowseRequest();
            if (config.isDebugOutput()) {
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
            for (KeywordSearchFacetData facet : config.getFacets()) {
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

            if (config.isDebugOutput()) {
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

    static final String[] LUCENE_SPECIAL_CHARS = {
        "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "~", "?", ":", "\\"
    };

    private static String parseQueryString(String qs) {
        String queryString = qs;
        // keep strings separated by spaces together
        queryString = queryString.replaceAll("\\b(\\s+)\\+(\\s+)\\b", "$1AND$2");
        // Replace single with double quotes.
        queryString = queryString.replaceAll("(^|\\s+)'(\\b[^']+ [^']+\\b)'(\\s+|$)", "$1\"$2\"$3");
        // escape special characters, see http://lucene.apache.org/java/2_9_0/queryparsersyntax.html
        for (String s : LUCENE_SPECIAL_CHARS) {
            if (queryString.contains(s)) {
                queryString = queryString.replace(s, "*");
            }
        }
        return toLowerCase(queryString);
    }

    private static String toLowerCase(String s) {
        StringBuilder sb = new StringBuilder();
        String[] bits = s.split(" ");
        for (String b : bits) {
            // booleans have to stay UPPER
            if ("OR".equalsIgnoreCase(b)
                    || "AND".equalsIgnoreCase(b)
                    || "NOT".equalsIgnoreCase(b)) {
                sb.append(b.toUpperCase());
            } else {
                sb.append(b.toLowerCase());
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Cleanly dispose of this object.
     */
    public void close() {
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
    }

    /** @return the facets for this browser **/
    public Collection<KeywordSearchFacetData> getFacets() {
        return config.getFacets();
    }

}
