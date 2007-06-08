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
import java.util.Map;

import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
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


    /**
     * Construct a new instance of SearchRepository.
     */
    public SearchRepository() {
        // empty
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
        Map<String, ? extends WebSearchable> webSearchables = webSearchablesMap.get(type);
        RAMDirectory ram = indexWebSearchables(webSearchables, "global");
        directoryMap.put(type, ram);
    }

    /**
     * Get the lucene Directory for the given type
     * @param type a tag type from TagTypes
     * @return the Directory
     */
    public Directory getDirectory(String type) {
        return directoryMap.get(type);
    }

    /**
     * Index some WebSearchables and return the RAMDirectory containing the index.
     *
     * @param webSearchableMap from name to WebSearchable
     * @param scope webSearchable type (see TemplateHelper)
     * @return a RAMDirectory containing the index
     */
    private static RAMDirectory indexWebSearchables(Map webSearchableMap, String scope) {
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

        while (iter.hasNext()) {
            WebSearchable webSearchable = (WebSearchable) iter.next();

            Document doc = new Document();
            doc.add(new Field("name", webSearchable.getName(), Field.Store.YES, 
                              Field.Index.TOKENIZED));
            doc.add(new Field("content", webSearchable.getTitle() + " : "
                              + webSearchable.getDescription(),
                              Field.Store.NO, Field.Index.TOKENIZED));
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

}
