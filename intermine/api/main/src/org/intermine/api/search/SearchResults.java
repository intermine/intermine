package org.intermine.api.search;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.split;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.template.TemplateQuery;

/**
 * Representations of Lucene search results over the web-searchable data of the user-profile and
 * the mechanisms for generating a set of such results.
 *
 * The LuceneSearchResults data structure encapsulates the relevant data returned from a search
 * over the user-profile's data. It should be constructed with a call to runLuceneSearch.
 *
 * @author Alex Kalderimis
 * @author Thomas Riley
 * @author Kim Rutherford
 */
public final class SearchResults implements Iterable<SearchResult>
{
    private static final Logger LOG = Logger.getLogger(SearchResults.class);

    /** The iterator for this iterable **/
    private static final class SearchResultIt implements Iterator<SearchResult>
    {
        private final SearchResults parent;
        private final Iterator<WebSearchable> subiter;

        SearchResultIt(SearchResults parent) {
            this.parent = parent;
            subiter = parent.items.values().iterator();
        }

        @Override
        public boolean hasNext() {
            return subiter.hasNext();
        }


        @Override
        public SearchResult next() {
            WebSearchable n = subiter.next();
            return new SearchResult(n, parent.hits.get(n), parent.descs.get(n), parent.tags.get(n));
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented");
        }

    }

    ///// INSTANCE API /////

    private final Map<WebSearchable, Float> hits = new HashMap<WebSearchable, Float>();
    private final Map<String, WebSearchable> items = new HashMap<String, WebSearchable>();
    private final Map<WebSearchable, String> descs = new HashMap<WebSearchable, String>();
    private final Map<WebSearchable, Set<String>> tags = new HashMap<WebSearchable, Set<String>>();

    // Constructor only available to the static methods below.
    private SearchResults(
            Map<WebSearchable, Float> hitMap,
            Map<String, WebSearchable> items,
            Map<WebSearchable, String> descriptions,
            Map<WebSearchable, Set<String>> itemsTags) {
        this.hits.putAll(hitMap);
        this.items.putAll(items);
        this.descs.putAll(descriptions);
        this.tags.putAll(itemsTags);
    }

    /**
     *
     * @return size
     */
    public int size() {
        return items.size();
    }

    @Override
    public Iterator<SearchResult> iterator() {
        return new SearchResultIt(this);
    }

    ///// STATIC SEARCH API //////

    /**
     * Filter out invalid templates from a map.
     * @param map The map of websearchables. MUST be modifiable.
     */
    public static void filterOutInvalidTemplates(Map<String, ? extends WebSearchable> map) {
        List<String> removeKeys = new ArrayList<String>();
        for (String key : map.keySet()) {
            if (isInvalidTemplate(map.get(key))) {
                removeKeys.add(key);
            }
        }
        for (String key : removeKeys) {
            map.remove(key);
        }
    }

    /**
     * Check if a websearchable is an invalid template.
     * @param webSearchable The item to check.
     * @return True if this a template and it is not valid.
     */
    public static boolean isInvalidTemplate(WebSearchable webSearchable) {
        if (webSearchable instanceof TemplateQuery) {
            TemplateQuery template = (TemplateQuery) webSearchable;
            return !template.isValid();
        }
        return false;
    }

    /**
     * The formatter for presenting descriptions of web searchable objects.
     *
     * The term will be returned in a <code>&lt;span&gt;</code> element, with the css class
     * <code>im-highlighted-search-term</code> applied to it.
     */
    private static final Formatter FORMATTER = new Formatter() {
        @Override
        public String highlightTerm(String term, TokenGroup group) {
            if (group.getTotalScore() > 0) {
                return "<span class=\"im-highlighted-search-term\">" + term + "</span>";
            }
            return term;
        }
    };

    /**
     * Perform transformations on the query string to make it behave intuitively (for some
     * definition of intuition...).
     * @param origQueryString The original form of the query string.
     * @return A munged and transformed string, that will do what people actually want in Lucene.
     */
    private static String prepareQueryString(String origQueryString) {
        // special case for word ending in "log" eg. "ortholog" - add "orthologue" to the search
        String queryString = origQueryString.replaceAll("(\\w+log\\b)", "$1ue $1");
        queryString = queryString.replaceAll("[^a-zA-Z0-9]", " ").trim();
        queryString = queryString.replaceAll("(\\w+)$", "$1 $1*");
        return queryString;
    }

    /**
     * Get an object capable of searching over multiple directories.
     * @param target What type and scope we are looking for.
     * @param userDirectory The user's index.
     * @param globalDirectories All the globally available indices.
     * @return A searcher object.
     * @throws CorruptIndexException If one of the indices cannot be used.
     * @throws IOException If there is a problem reading the indices.
     */
    private static MultiSearcher prepareSearcher(SearchTarget target,
            Directory userDirectory, List<Directory> globalDirectories)
        throws CorruptIndexException, IOException {
        IndexSearcher userIndexSearcher = new IndexSearcher(userDirectory);
        IndexSearcher[] globalIndexSearchers = new IndexSearcher[globalDirectories.size()];
        for (int i = 0; i < globalDirectories.size(); i++) {
            globalIndexSearchers[i] = new IndexSearcher(globalDirectories.get(i));
        }
        Searchable[] searchables;
        if (target.isUserOnly()) {
            searchables = new Searchable[]{userIndexSearcher};
        } else if (target.isGlobalOnly()) {
            searchables = globalIndexSearchers;
        } else { // ALL
            searchables = (Searchable[]) ArrayUtils.add(globalIndexSearchers, userIndexSearcher);
        }
        MultiSearcher searcher = new MultiSearcher(searchables);

        return searcher;
    }

    /**
     * Actually filter the web searchable items we have to get a reduced list of matches.
     * @param origQueryString A query to filter the items against. Assumes the query
     *                        string is neither null not empty.
     * @param target Information about the scope and type of items to receive.
     * @param profileRepo The repository of the user who wants to find something.
     * @return A set of search results.
     * @throws ParseException If the query string cannot be parsed.
     * @throws IOException If there is an issue opening the indices.
     */
    private static SearchResults doFilteredSearch(
            String origQueryString, SearchTarget target, SearchRepository profileRepo)
        throws ParseException, IOException {

        Map<WebSearchable, String> highlightedDescMap = new HashMap<WebSearchable, String>();

        String queryString = prepareQueryString(origQueryString);

        LOG.info("Searching " + target + " for "
                + " was:" + origQueryString + " now:" + queryString);
        long time = System.currentTimeMillis();

        org.apache.lucene.search.Query query;

        Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                StopAnalyzer.ENGLISH_STOP_WORDS_SET);

        // The default search field is the content buffer.
        QueryParser queryParser = new QueryParser(Version.LUCENE_30, "content", analyzer);
        query = queryParser.parse(queryString);

        // Get directories.
        String type = target.getType();
        Map<String, WebSearchable> globalWebSearchables = new HashMap<String, WebSearchable>();
        Set<SearchRepository> globals = SearchRepository.getGlobalSearchRepositories();
        List<Directory> globalDirs = new ArrayList<Directory>();
        for (SearchRepository sr: globals) {
            globalWebSearchables.putAll(sr.getWebSearchableMap(type));
            globalDirs.add(sr.getSearchIndex(type));
        }
        Map<String, WebSearchable> userWebSearchables = profileRepo.getWebSearchableMap(type);
        Directory userDirectory = profileRepo.getSearchIndex(type);

        MultiSearcher searcher = prepareSearcher(target, userDirectory, globalDirs);

        // required to expand search terms
        query = searcher.rewrite(query);
        TopDocs topDocs = searcher.search(query, 1000); //FIXME: hardcoded limit

        time = System.currentTimeMillis() - time;
        LOG.info("Found " + topDocs.totalHits + " document(s) that matched query '"
                + queryString + "' in " + time + " milliseconds:");

        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(FORMATTER, scorer);

        Map<WebSearchable, Float> hitMap = new HashMap<WebSearchable, Float>();
        Map<WebSearchable, Set<String>> tags = new HashMap<WebSearchable, Set<String>>();

        for (int i = 0; i < topDocs.totalHits; i++) {
            WebSearchable webSearchable = null;
            Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
            //String docScope = doc.get("scope");
            String name = doc.get("name");

            webSearchable = userWebSearchables.get(name);
            if (webSearchable == null) {
                webSearchable = globalWebSearchables.get(name);
            }
            if (webSearchable == null) {
                throw new RuntimeException("unknown WebSearchable: " + name);
            }

            Float luceneScore = new Float(topDocs.scoreDocs[i].score);
            hitMap.put(webSearchable, luceneScore);

            tags.put(webSearchable, new HashSet<String>(asList(split(doc.get("tags")))));

            try {
                if (highlightedDescMap != null) {
                    String highlightString = webSearchable.getDescription();
                    if (highlightString == null) {
                        highlightString = "";
                    }
                    TokenStream tokenStream =
                        analyzer.tokenStream("", new StringReader(highlightString));
                    highlighter.setTextFragmenter(new NullFragmenter());
                    highlightedDescMap.put(webSearchable, highlighter.getBestFragment(
                            tokenStream, highlightString));
                }
            } catch (InvalidTokenOffsetsException e) {
                LOG.warn("Highlighter exception", e);
            }
        }

        Map<String, WebSearchable> wsMap = new HashMap<String, WebSearchable>();
        for (WebSearchable ws: hitMap.keySet()) {
            wsMap.put(ws.getName(), ws);
        }

        return new SearchResults(hitMap, wsMap, highlightedDescMap, tags);
    }

    /**
     * Search a Lucene index and return a search result. Where the query string is blank, all
     * web searchable items of the requested scope will be returned.
     *
     * @param queryString the query string from the user.
     * @param target a parameter object containing information about the scope of the search.
     * @param userSearchRepository The current user's search repository.
     *
     * @throws ParseException if the origQueryString has a syntax error for a Lucene string
     * @throws IOException if there is a problem creating the Lucene IndexSearcher
     *
     * @return A result set.
     */
    public static SearchResults runLuceneSearch(
            String queryString, SearchTarget target,
            SearchRepository userSearchRepository)
        throws ParseException, IOException {

        if (isBlank(queryString)) {
            return doUnfilteredSearch(target, userSearchRepository);
        } else {
            return doFilteredSearch(queryString, target, userSearchRepository);
        }
    }

    /**
     * Return all the available objects in the requested scope in a set of search results.
     * @param target a parameter object containing information about the scope of the search.
     * @param userRepo The current user's search repository.
     * @return A set of search results.
     */
    private static SearchResults doUnfilteredSearch(SearchTarget target, SearchRepository userRepo)
    {
        LOG.info("unfiltered search");
        Map<String, WebSearchable> wsMap = new HashMap<String, WebSearchable>();
        Map<WebSearchable, Float> hitMap = new HashMap<WebSearchable, Float>();
        Map<WebSearchable, String> descs = new HashMap<WebSearchable, String>();
        Map<WebSearchable, Set<String>> tags = new HashMap<WebSearchable, Set<String>>();

        Set<WebSearchable> items;
        if (target.isUserOnly()) {
            items = userRepo.getSearchItems();
        } else {
            items = new HashSet<WebSearchable>();
            for (SearchRepository sr : SearchRepository.getGlobalSearchRepositories()) {
                items.addAll(sr.getSearchItems());
            }
            if (target.isAll()) {
                items.addAll(userRepo.getSearchItems());
            }
        }

        TagManager tm = new TagManagerFactory(userRepo.getProfile().getProfileManager())
                                .getTagManager();

        for (WebSearchable ws: items) {
            if (target.getType().equals(ws.getTagType())) {
                wsMap.put(ws.getName(), ws);
                descs.put(ws, ws.getDescription());
            }
            tags.put(ws, tm.getObjectTagNames(ws, userRepo.getProfile()));
        }

        return new SearchResults(hitMap, wsMap, descs, tags);
    }

}
