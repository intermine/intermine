package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;

/**
 * Respository object for TemplateQueries.
 *
 * @author Thomas Riley
 */
public class SearchRepository
{
    private static final Logger LOG = Logger.getLogger(SearchRepository.class);
    /** "Miscellaneous" */
    public static final String MISC = "aspect:Miscellaneous";

    private ServletContext servletContext;
    private static final List<String> PUBLIC_TAG = Arrays.asList(new String[] {TagNames.IM_PUBLIC});

    /**
     * Construct a new instance of SearchRepository.
     * @param context the servlet context
     */
    public SearchRepository(ServletContext context) {
        // index global webSearchables
        servletContext = context;

        servletContext.setAttribute(Constants.GLOBAL_TEMPLATE_QUERIES, new AbstractMap() {
            public Set entrySet() {
                Profile superProfile = SessionMethods.getSuperUserProfile(servletContext);
                ProfileManager pm =
                    (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
                return pm.filterByTags(superProfile.getSavedTemplates(), PUBLIC_TAG,
                                       TagTypes.TEMPLATE, superProfile.getUsername()).entrySet();
            }
        });

        reindexGlobalTemplates(servletContext);
    }

    /**
     * Get the singleton TemplateRespository.
     *
     * @param context the servlet context
     * @return the singleton SearchRepository object
     */
    public static final SearchRepository getTemplateRepository(ServletContext context) {
        return (SearchRepository) context.getAttribute(Constants.TEMPLATE_REPOSITORY);
    }

    /**
     * Called to tell the repository that a global webSearchable has been added to
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable added
     */
    public void globalTemplateAdded(WebSearchable webSearchable) {
        reindexGlobalTemplates(servletContext);
    }

    /**
     * Called to tell the repository that a global webSearchable has been removed from
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable removed
     */
    public void globalTemplateRemoved(WebSearchable webSearchable) {
        reindexGlobalTemplates(servletContext);
    }

    /**
     * Called to tell the repository that a global webSearchable has been updated in
     * the superuser user profile.
     *
     * @param webSearchable the WebSearchable updated
     */
    public void globalTemplateUpdated(WebSearchable webSearchable) {
        reindexGlobalTemplates(servletContext);
    }

    /**
     * Called to tell the repository that the set of global webSearchables in the superuser
     * profile has changed.
     */
    public void globalTemplatesChanged() {
        reindexGlobalTemplates(servletContext);
    }

    /**
     * Call to update the index when a Tag is added.
     * @param tag the Tag
     */
    public void webSearchableTagged(Tag tag) {
        reindexGlobalTemplates(servletContext);
    }

    /**
     * Call to update the index when a Tag is removed.
     * @param tag the Tag
     */
    public void webSearchableUnTagged(Tag tag) {
        reindexGlobalTemplates(servletContext);
    }
    /**
     * Create the lucene search index of all global webSearchable queries.
     *
     * @param servletContext the servlet context
     */
    private static void reindexGlobalTemplates(ServletContext servletContext) {
        Map webSearchables = (Map) servletContext.getAttribute(Constants.GLOBAL_TEMPLATE_QUERIES);
        RAMDirectory ram = indexWebSearchables(webSearchables, "global");
        servletContext.setAttribute(Constants.GLOBAL_TEMPLATE_INDEX_DIR, ram);
    }

    /**
     * Index some TemplateQueries and return the RAMDirectory containing the index.
     *
     * @param webSearchableMap from name to WebSearchable
     * @param type webSearchable type (see TemplateHelper)
     * @return a RAMDirectory containing the index
     */
    public static RAMDirectory indexWebSearchables(Map webSearchableMap, String type) {
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
            doc.add(new Field("type", type, Field.Store.YES, Field.Index.NO));

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

}
