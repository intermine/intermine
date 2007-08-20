package org.intermine.web.logic.search;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.struts.AspectController;

import java.io.IOException;
import java.io.StringReader;

import javax.servlet.ServletContext;

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
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Respository object for WebSearchable objects.
 *
 * @author Thomas Riley
 * @author Kim Rutherford
 */
public class SearchRepository
{
    private static final Logger LOG = Logger.getLogger(SearchRepository.class);
    /** "Miscellaneous" */
    public static final String MISC = "aspect:Miscellaneous";

    private Map<String, Map<String, ? extends WebSearchable>> webSearchablesMap =
        new HashMap<String, Map<String, ? extends WebSearchable>>();
    private Map<String, Directory> directoryMap = new HashMap<String, Directory>();
    private final String scope;
    private Profile profile;

    /**
     * Construct a new instance of SearchRepository.
     * @param profile the Profile to use for getting aspect tags
     * @param scope USER_TEMPLATE or GLOBAL_TEMPLATE from TemplateHelper
     */
    public SearchRepository(Profile profile, String scope) {
        this(scope);
        setProfile(profile);
    }

    /**
     * Construct a new instance of SearchRepository.
     * @param scope USER_TEMPLATE or GLOBAL_TEMPLATE from TemplateHelper
     */
    public SearchRepository(String scope) {
        this.scope = scope;
    }

    /**
     * Set the Profile to use for getting aspect tags
     * @param profile
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }
    
    /**
     * Get the SearchRepository for global (public) objects.
     *
     * @param context the servlet context
     * @return the singleton SearchRepository object
     */
    public static final SearchRepository getGlobalSearchRepository(ServletContext context) {
        return (SearchRepository) context.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
    }

    /**
     * Called to tell the repository that a global webSearchable has been added to
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable added
     */
    public void webSearchableAdded(WebSearchable webSearchable) {
        reindex(getWebSearchableType(webSearchable));
    }

    /**
     * Called to tell the repository that a global webSearchable has been removed from
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable removed
     */
    public void webSearchableRemoved(WebSearchable webSearchable) {
        reindex(getWebSearchableType(webSearchable));
    }

    /**
     * Called to tell the repository that a global webSearchable has been updated in
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable updated
     */
    public void webSearchableUpdated(WebSearchable webSearchable) {
        reindex(getWebSearchableType(webSearchable));
    }

    /**
     * Called to tell the repository that the set of global webSearchables in the superuser
     * profile has changed.
     * @param type a tag type from TagTypes
     */
    public void globalChange(String type) {
        reindex(type);
    }

    /**
     * Return the type of this webSearchable from the possibilities in the TagTypes interface.
     */
    private String getWebSearchableType(WebSearchable webSearchable) {
        if (webSearchable instanceof TemplateQuery) {
            return TagTypes.TEMPLATE;
        } else {
            if (webSearchable instanceof InterMineBag) {
                return TagTypes.BAG;
            } else {
                throw new IllegalArgumentException("unknown argument: " + webSearchable);
            }
        }
    }

    /**
     * Call to update the index when a Tag is added.
     * @param tag the Tag
     */
    public void webSearchableTagged(Tag tag) {
        reindex(tag.getType());
    }

    /**
     * Call to update the index when a Tag is removed.
     * @param tag the Tag
     */
    public void webSearchableUnTagged(Tag tag) {
        reindex(tag.getType());
    }
    
    /**
     * Called when the description of a WebSearchable changes.
     * @param webSearchable the item that has changed
     */
    public void descriptionChanged(WebSearchable webSearchable) {
        reindex(getWebSearchableType(webSearchable));
    }

    /**
     * Create the lucene search index of all global webSearchable queries.
     *
     * @param servletContext the servlet context
     */
    private void reindex(String type) {
        directoryMap.put(type, null);
    }

    /**
     * Get the lucene Directory for the given type
     * @param type a tag type from TagTypes
     * @return the Directory
     */
    public Directory getDirectory(String type) {
        if (directoryMap.containsKey(type) && directoryMap.get(type) != null) {
            return directoryMap.get(type);
        } else {
            Map<String, ? extends WebSearchable> webSearchables = webSearchablesMap.get(type);
            RAMDirectory ram = indexWebSearchables(webSearchables, type);
            directoryMap.put(type, ram);
            return ram;
        }
    }

    /**
     * Index some WebSearchables and return the RAMDirectory containing the index.
     *
     * @param webSearchableMap from name to WebSearchable
     * @param scope webSearchable type (see TemplateHelper)
     * @return a RAMDirectory containing the index
     */
    private RAMDirectory indexWebSearchables(Map webSearchableMap, String type) {
        long time = System.currentTimeMillis();
        LOG.info("Indexing webSearchable queries");
    
        RAMDirectory ram = new RAMDirectory();
        IndexWriter writer;
        try {
            SnowballAnalyzer snowballAnalyzer =
                new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);
            writer = new IndexWriter(ram, snowballAnalyzer, true);
        } catch (IOException err) {
            throw new RuntimeException("Failed to create lucene IndexWriter", err);
        }
    
        // step global webSearchables, indexing a Document for each webSearchable
        Iterator iter = webSearchableMap.values().iterator();
        int indexed = 0;
    
        ProfileManager pm = profile.getProfileManager();
        
        while (iter.hasNext()) {
            WebSearchable webSearchable = (WebSearchable) iter.next();
    
            Document doc = new Document();
            doc.add(new Field("name", webSearchable.getName(), Field.Store.YES, 
                              Field.Index.TOKENIZED));
            StringBuffer contentBuffer = new StringBuffer(webSearchable.getTitle() + " : "
                                               + webSearchable.getDescription());
            List<Tag> tags = pm.getTags(null, webSearchable.getName(), type, profile.getUsername());
            for (Tag tag: tags) {
                String tagName = tag.getTagName();
                if (tagName.startsWith(AspectController.ASPECT_PREFIX)) {
                    String aspect = tagName.substring(AspectController.ASPECT_PREFIX.length());
                    contentBuffer.append(' ').append(aspect);
                }
            }
            
            // normalise the text
            String content = contentBuffer.toString().replaceAll("[^a-zA-Z0-9]", " ");
            doc.add(new Field("content", content, Field.Store.NO,
                              Field.Index.TOKENIZED));
            doc.add(new Field("scope", scope, Field.Store.YES, Field.Index.NO));
    
            try {
                writer.addDocument(doc);
                indexed++;
            } catch (IOException e) {
                LOG.error("Failed to add webSearchable " + webSearchable.getName()
                        + " to the index", e);
            }
        }
    
        try {
            writer.close();
        } catch (IOException e) {
            LOG.error("IOException while closing IndexWriter", e);
        }
    
        time = System.currentTimeMillis() - time;
        LOG.info("Indexed " + indexed + " out of " + webSearchableMap.size() + " webSearchables in "
                + time + " milliseconds");
    
        return ram;
    }

    /**
     * Return a Map from name to WebSearchable for the given type.
     * @param type a tag type from TagTypes
     * @return the WebSearchable Map
     */
    public Map<String, ? extends WebSearchable> getWebSearchableMap(String type) {
        return webSearchablesMap.get(type);
    }
    
    /**
     * Return a map from type (TagTypes: "template", "bag", etc.) to Map from name to WebSearchable.
     * @return the Map
     */
    public Map<String, Map<String, ? extends WebSearchable>> getWebSearchableMaps() {
        return webSearchablesMap;
    }

    /**
     * Add a Map from name to WebSearchable for the given type.  The Map can be retrieved later
     * with getWebSearchableMap().
     * @param type a tag type from TagTypes
     * @param map the WebSearchable Map
     */
    public void addWebSearchables(String type, Map<String, ? extends WebSearchable> map) {
        webSearchablesMap.put(type, map);
        reindex(type);
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
     * @param context the ServletContext used to get the global SearchRepository
     * @param hitMap set to be a Map from WebSearchable name to score in the search
     * @param scopeMap set to be a Map from WebSearchable name to scope of the WebSearchable we
     *    match
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
                                        ServletContext context,
                                        Map<WebSearchable, Float> hitMap, 
                                        Map<WebSearchable, String> scopeMap,
                                        Map<WebSearchable, String> highlightedTitleMap,
                                        Map<WebSearchable, String> highlightedDescMap) 
        throws ParseException, IOException {
        // special case for word ending in "log" eg. "ortholog" - add "orthologue" to the search
        String queryString = origQueryString.replaceAll("(\\w+log\\b)", "$1ue $1");
        queryString = queryString.replaceAll("[^a-zA-Z0-9]", " ");
        SearchRepository.LOG.info("Searching " + scope + " for \""
                + origQueryString + "\"    - type: " + type);
        long time = System.currentTimeMillis();
        SearchRepository globalSearchRepository =
            (SearchRepository) context.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
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
        if (scope.equals("user")) {
            searchables = new Searchable[]{userIndexSearcher};
        } else if (scope.equals("global")) {
            searchables = new Searchable[]{globalIndexSearcher};
        } else {
            searchables = new Searchable[]{userIndexSearcher, globalIndexSearcher};
        }
        MultiSearcher searcher = new MultiSearcher(searchables);
    
        Analyzer analyzer = new SnowballAnalyzer("English", StopAnalyzer.ENGLISH_STOP_WORDS);
    
        org.apache.lucene.search.Query query;
        QueryParser queryParser = new QueryParser("content", analyzer);
        query = queryParser.parse(queryString);
    
        // required to expand search terms
        query = query.rewrite(IndexReader.open(globalDirectory));
        Hits hits = searcher.search(query);
    
        time = System.currentTimeMillis() - time;
        SearchRepository.LOG.info("Found " + hits.length() + " document(s) that matched query '"
                + queryString + "' in " + time + " milliseconds:");
    
        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(SearchRepository.formatter, scorer);
    
        for (int i = 0; i < hits.length(); i++) {
            WebSearchable webSearchable = null;
            Document doc = hits.doc(i);
            String docScope = doc.get("scope");
            String name = doc.get("name");
    
            webSearchable = userWebSearchables.get(name);
            if (webSearchable == null) {
                webSearchable = globalWebSearchables.get(name);
            }
            if (webSearchable == null) {
                throw new RuntimeException("unknown WebSearchable: " + name);
            }
    
            hitMap.put(webSearchable, new Float(hits.score(i)));
            scopeMap.put(webSearchable, docScope);
    
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
                highlightedDescMap.put(webSearchable,
                                       highlighter.getBestFragment(tokenStream, highlightString));
            }
        }
        return time;
    }

}
