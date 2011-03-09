package org.intermine.api.search;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.template.TemplateQuery;
import org.intermine.model.userprofile.Tag;

/**
 * Respository object for WebSearchable objects.
 *
 * @author Thomas Riley
 * @author Kim Rutherford
 */
public class SearchRepository
{
    private static final Logger LOG = Logger.getLogger(SearchRepository.class);
    // this is not being refreshed
    private Map<String, Map<String, ? extends WebSearchable>> webSearchablesMap =
        new HashMap<String, Map<String, ? extends WebSearchable>>();
    private Map<String, Directory> directoryMap = new HashMap<String, Directory>();
    private final String scope;
    private Profile profile;


    /**
     * Construct a new instance of SearchRepository.
     * @param profile the Profile to use for getting aspect tags
     * @param scope USER or GLOBAL from SearchRepository
     */
    public SearchRepository(Profile profile, String scope) {
        if (!scope.equals(Scope.GLOBAL) && !scope.equals(Scope.USER)) {
            throw new IllegalArgumentException("Unrecognised scope for SearchRepository: "
                    + scope + ".  Expected " + Scope.GLOBAL
                    + " or " + Scope.USER);
        }
        this.scope = scope;
        this.profile = profile;

        populateWebSearchables(TagTypes.TEMPLATE);
        populateWebSearchables(TagTypes.BAG);
    }


    /**
     * Initialise and index web searchables of the given type from the profile.  If scope is
     * GLOBAL will restrict to those tagged public and NOT hidden.
     *
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     */
    private void populateWebSearchables(String type) {
        Map<String, ? extends WebSearchable> wsMap = null;
        if (type.equals(TagTypes.TEMPLATE)) {
            wsMap = profile.getSavedTemplates();
        } else if (type.equals(TagTypes.BAG)) {
            wsMap = profile.getSavedBags();
        }

        // if this is the global search repository only objects tagged as public
        if (scope.equals(Scope.GLOBAL)) {
            final TagManager tagManager =
                new TagManagerFactory(profile.getProfileManager()).getTagManager();

            wsMap = new SearchFilterEngine().filterByTags(wsMap,
                    new ArrayList<String>(Collections.singleton(TagNames.IM_PUBLIC)), type,
                    profile.getUsername(), tagManager, false);
        }
        webSearchablesMap.put(type, wsMap);
        reindex(type);
    }

    /**
     * The web searchables of given type have changed, but we don't know specifically which so
     * re-initialise whole type.
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     */
    public void globalChange(String type) {
        if (scope.equals(Scope.GLOBAL)) {
            // don't know which are public so re-populate
            populateWebSearchables(type);
        } else {
            reindex(type);
        }

    }


    /**
     * Called to tell the repository that a global webSearchable has been added to
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable added
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     */
    public void webSearchableAdded(WebSearchable webSearchable, String type) {
        if (scope.equals(Scope.USER)) {
            reindex(type);
        }
        if (scope.equals(Scope.GLOBAL) && isWebSearchableGlobal(webSearchable, type)) {
            populateWebSearchables(type);
        }
    }

    /**
     * Called to tell the repository that a global webSearchable has been removed from
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable removed
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     */
    public void webSearchableRemoved(WebSearchable webSearchable, String type) {
        if (scope.equals(Scope.USER)) {
            reindex(type);
        }
        if (scope.equals(Scope.GLOBAL) && isWebSearchableGlobal(webSearchable, type)) {
            populateWebSearchables(type);
        }
    }

    /**
     * Called to tell the repository that a global webSearchable has been updated in
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable updated
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     */
    public void webSearchableUpdated(WebSearchable webSearchable, String type) {
        if (scope.equals(Scope.USER)) {
            reindex(type);
        }
        if (scope.equals(Scope.GLOBAL) && isWebSearchableGlobal(webSearchable, type)) {
            populateWebSearchables(type);
        }
    }

    /**
     * Return the type of this webSearchable from the possibilities in the TagTypes interface.
     */
    private String getWebSearchableType(WebSearchable webSearchable) {
        if (webSearchable instanceof TemplateQuery) {
            return TagTypes.TEMPLATE;
        }
        if (webSearchable instanceof InterMineBag) {
            return TagTypes.BAG;
        }
        throw new IllegalArgumentException("unknown argument: " + webSearchable);
    }


    /**
     * A tag has been added or removed.  Reindex the web searchables, if global and the tag
     * was im:public then update the main list.
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     * @param tagName the tag that has changed
     */
    public void webSearchableTagChange(String type, String tagName) {
        // TODO will reindex the global search repository even if the tagged object isn't public

        if (scope.equals(Scope.GLOBAL) && tagName.equals(TagNames.IM_PUBLIC)) {
            populateWebSearchables(type);
        } else {
            reindex(type);
        }
    }

    /**
     * Called when the description of a WebSearchable changes.
     * @param webSearchable the item that has changed
     */
    public void descriptionChanged(WebSearchable webSearchable) {
        // TODO will reindex global repository even if this object isn't public
        reindex(getWebSearchableType(webSearchable));
    }

    /**
     * Return true if the web searchable is tagged as public
     * @param webSearchable the template or bag object
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     * @return true if the web searchable is tagged as public
     */
    private boolean isWebSearchableGlobal(WebSearchable webSearchable, String type) {
        final TagManager tagManager =
            new TagManagerFactory(profile.getProfileManager()).getTagManager();
        Set<String> tagNames = tagManager.getObjectTagNames(webSearchable.getName(),
                type, profile.getUsername());
        for (String tagName : tagNames) {
            if (tagName.equals(TagNames.IM_PUBLIC)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create the lucene search index of all global webSearchable queries.
     *
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     */
    private void reindex(String type) {
        directoryMap.put(type, null);
    }

    /**
     * Get the lucene Directory for the given type
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     * @return the Directory
     */
    public Directory getDirectory(String type) {
        if (directoryMap.containsKey(type) && directoryMap.get(type) != null) {
            return directoryMap.get(type);
        }
        Map<String, ? extends WebSearchable> webSearchables = webSearchablesMap.get(type);
        RAMDirectory ram = indexWebSearchables(webSearchables, type);
        directoryMap.put(type, ram);
        return ram;
    }

    /**
     * Index some WebSearchables and return the RAMDirectory containing the index.
     *
     * @param webSearchableMap from name to WebSearchable
     * @param scope webSearchable type (see TemplateHelper)
     * @return a RAMDirectory containing the index
     */
    private RAMDirectory indexWebSearchables(Map<String, ? extends WebSearchable> webSearchableMap,
            String type) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing webSearchable queries");

        RAMDirectory ram = new RAMDirectory();
        IndexWriter writer;
        try {
            SnowballAnalyzer snowballAnalyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                    StopAnalyzer.ENGLISH_STOP_WORDS_SET);
            writer = new IndexWriter(ram, snowballAnalyzer, true,
                    IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }

        // step global webSearchables, indexing a Document for each webSearchable
        int indexed = 0;

        TagManager tagManager = new TagManagerFactory(profile.getProfileManager()).getTagManager();

        for (WebSearchable webSearchable : webSearchableMap.values()) {
            Document doc = new Document();
            doc.add(new Field("name", webSearchable.getName(), Field.Store.YES,
                              Field.Index.ANALYZED));
            StringBuffer contentBuffer = new StringBuffer(webSearchable.getTitle() + " : "
                                               + webSearchable.getDescription());
            List<Tag> tags = tagManager.getTags(null, webSearchable.getName(), type,
                    profile.getUsername());
            for (Tag tag: tags) {
                String tagName = tag.getTagName();
                if (AspectTagUtil.isAspectTag(tagName)) {
                    contentBuffer.append(' ').append(AspectTagUtil.getAspect(tagName));
                }
            }

            // normalise the text
            String content = contentBuffer.toString().replaceAll("[^a-zA-Z0-9]", " ");
            doc.add(new Field("content", content, Field.Store.NO,
                              Field.Index.ANALYZED));
            //doc.add(new Field("scope", scope, Field.Store.YES, Field.Index.NO));

            try {
                writer.addDocument(doc);
                indexed++;
            } catch (IOException e) {
                LOG.error("Failed to add webSearchable " + webSearchable.getName()
                        + " to the index", e);
                throw new RuntimeException("Failed to write to index", e);
            }
        }

        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
            throw new RuntimeException("Failed to close IndexWriter", e);
        }

        time = System.currentTimeMillis() - time;
        LOG.info("Indexed " + indexed + " out of " + webSearchableMap.size() + " webSearchables in "
                + time + " milliseconds");

        return ram;
    }


    /**
     * Return a Map from name to WebSearchable for the given type.
     * @param type the type of webSearchable TagTypes.TEMPLATE or TagTypes.BAG
     * @return the WebSearchable Map
     */
    public Map<String, ? extends WebSearchable> getWebSearchableMap(String type) {
        return webSearchablesMap.get(type);
    }


    /**
     * Add a Map from name to WebSearchable for the given type.  The Map can be retrieved later
     * with getWebSearchableMap().
     * @param type a tag type from TagTypes
     */
    public void addWebSearchables(String type) {
        populateWebSearchables(type);
    }

    /**
     * Filter out invalid templates from map.
     * @param map map
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

    private static boolean isInvalidTemplate(WebSearchable webSearchable) {
        if (webSearchable instanceof TemplateQuery) {
            TemplateQuery template = (TemplateQuery) webSearchable;
            return !template.isValid();
        }
        return false;
    }


    private static Formatter formatter = new Formatter() {
        public String highlightTerm(String term, TokenGroup group) {
            if (group.getTotalScore() > 0) {
                return "<span style=\"background: #ffff77\">" + term + "</span>";
            }
            return term;
        }
    };

    /**
     * Search a Lucene index and return maps of the results.
     * @param origQueryString the query string from the user
     * @param scope "global", "user" or "both" - which scope to search
     * @param type the type of object to search ("bag", "template" ...)
     * @param profile the user profile
     * @param globalSearchRepository the global SearchRepository
     * @param hitMap set to be a Map from WebSearchable name to score in the search
     * @param highlightedTitleMap set to be a Map from WebSearchable name to the title of the
     *    WebSearchable, marked up in HTML to highlight the matching parts
     * @param descrMap
     * @param highlightedDescMap highlightedTitleMap set to be a Map from WebSearchable name to
     *    the description of the WebSearchable, marked up in HTML to highlight the matching parts
     * @return the number of milliseconds the search took
     * @throws ParseException if the origQueryString has a syntax error for a lucene string
     * @throws IOException if there is a problem creating the Lucene IndexSearcher
     */
    public static long runLeuceneSearch(String origQueryString, String scope, String type,
                                        Profile profile,
                                        SearchRepository globalSearchRepository,
                                        Map<WebSearchable, Float> hitMap,
                                        //Map<WebSearchable, String> scopeMap,
                                        Map<WebSearchable, String> highlightedTitleMap,
                                        Map<WebSearchable, String> highlightedDescMap)
        throws ParseException, IOException {
        // special case for word ending in "log" eg. "ortholog" - add "orthologue" to the search
        String queryString = origQueryString.replaceAll("(\\w+log\\b)", "$1ue $1");
        queryString = queryString.replaceAll("[^a-zA-Z0-9]", " ").trim();
        queryString = queryString.replaceAll("(\\w+)$", "$1 $1*");
        SearchRepository.LOG.info("Searching " + scope + " for "
                + " was:" + origQueryString + " now:" + queryString + "  - type: " + type);
        long time = System.currentTimeMillis();
        Map<String, ? extends WebSearchable> globalWebSearchables =
            globalSearchRepository.getWebSearchableMap(type);
        Directory globalDirectory = globalSearchRepository.getDirectory(type);
        SearchRepository userSearchRepository = profile.getSearchRepository();
        Map<String, ? extends WebSearchable> userWebSearchables =
            userSearchRepository.getWebSearchableMap(type);
        Directory userDirectory = userSearchRepository.getDirectory(type);
        IndexSearcher userIndexSearcher = new IndexSearcher(userDirectory);
        IndexSearcher globalIndexSearcher = new IndexSearcher(globalDirectory);
        Searchable[] searchables;
        if (scope.equals(Scope.USER)) {
            searchables = new Searchable[]{userIndexSearcher};
        } else if (scope.equals(Scope.GLOBAL)) {
            searchables = new Searchable[]{globalIndexSearcher};
        } else {
            searchables = new Searchable[]{userIndexSearcher, globalIndexSearcher};
        }
        MultiSearcher searcher = new MultiSearcher(searchables);

        Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_30, "English",
                StopAnalyzer.ENGLISH_STOP_WORDS_SET);

        org.apache.lucene.search.Query query;
        QueryParser queryParser = new QueryParser(Version.LUCENE_30, "content", analyzer);
        query = queryParser.parse(queryString);

        // required to expand search terms
        query = query.rewrite(IndexReader.open(globalDirectory));
        TopDocs topDocs = searcher.search(query, 1000); //FIXME: hardcoded limit

        time = System.currentTimeMillis() - time;
        SearchRepository.LOG.info("Found " + topDocs.totalHits + " document(s) that matched query '"
                + queryString + "' in " + time + " milliseconds:");

        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(SearchRepository.formatter, scorer);

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
            // different versions of Lucene return not-/normalized results, see:
            //  http://stackoverflow.com/questions/4642160/
            //  cap the top hits
            if (luceneScore > 1) {
                hitMap.put(webSearchable, new Float(1));
            } else {
                hitMap.put(webSearchable, luceneScore);
            }
            //scopeMap.put(webSearchable, docScope);

            try {
                if (highlightedTitleMap != null) {
                    String highlightString = webSearchable.getTitle();
                    TokenStream tokenStream =
                        analyzer.tokenStream("", new StringReader(highlightString));
                    highlighter.setTextFragmenter(new NullFragmenter());
                    highlightedTitleMap.put(webSearchable,
                                       highlighter.getBestFragment(tokenStream, highlightString));
                }

                if (highlightedDescMap != null) {
                    String highlightString = webSearchable.getDescription();
                    if (highlightString == null) {
                        highlightString = "";
                    }
                    TokenStream tokenStream =
                        analyzer.tokenStream("", new StringReader(highlightString));
                    highlighter.setTextFragmenter(new NullFragmenter());
                    highlightedDescMap.put(webSearchable, highlighter.getBestFragment(tokenStream,
                            highlightString));
                }
            } catch (InvalidTokenOffsetsException e) {
                LOG.warn("Highlighter exception", e);
            }
        }

        return time;
    }
}
