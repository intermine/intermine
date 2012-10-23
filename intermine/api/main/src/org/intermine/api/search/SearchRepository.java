package org.intermine.api.search;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.tag.AspectTagUtil;
import org.intermine.model.userprofile.Tag;

/**
 * The base class for search repositories. Classes that extend this base must provide
 * implementations that define the responses to change events that affect the web searchables this
 * repository is possibly interested in.
 *
 * @author Alex Kalderimis
 *
 */
public abstract class SearchRepository implements WebSearchWatcher
{

    protected static final Set<SearchRepository> GLOBALS =
            new HashSet<SearchRepository>();

    private static final Logger LOG = Logger.getLogger(SearchRepository.class);

    protected Set<WebSearchable> searchItems = new HashSet<WebSearchable>();
    protected Map<String, Directory> indexes = new HashMap<String, Directory>();

    protected final Profile profile;
    protected final TagManager tagManager;

    /**
     * Constructor. Create a new search repository for the given profile.
     * @param profile The gateway to the user data we want to search.
     */
    public SearchRepository(Profile profile) {
        this.profile = profile;
        this.tagManager = new TagManagerFactory(profile.getProfileManager()).getTagManager();
        populateSearchItems();
        startListening();
    }

    /**
     * Begin listening for events on the items of interest.
     */
    protected void startListening() {
        for (WebSearchable ws: searchItems) {
            ws.addObserver(this);
        }
    }

    /** Stop listening for events **/
    protected void stopListening() {
        for (WebSearchable ws: searchItems) {
            ws.removeObserver(this);
        }
    }

    /**
     * Get the search repositories registered as global repositories.
     * @return An unmodifiable set of global search repositories.
     */
    public static Set<SearchRepository> getGlobalSearchRepositories() {
        return Collections.unmodifiableSet(GLOBALS);
    }

    /**
     * Get the search repository registered as global repositories for the user specified in input.
     * @return the global search repositories.
     */
    public static SearchRepository getGlobalSearchRepository(Profile profile) {
        for (SearchRepository sr: GLOBALS) {
            if (profile.equals(sr.getProfile())) {
                return sr;
            }
        }
        return null;
    }

    static void clearGlobalRepositories() {
        GLOBALS.clear();
    }

    public void addGlobalRepository() {
        GLOBALS.add(this);
    }

    /**
     * Returns an unmodifiable view of the the search items observed by this
     * repository.
     * @return An unmodifiable Set.
     */
    public Set<WebSearchable> getSearchItems() {
        return Collections.unmodifiableSet(searchItems);
    }

    /**
     * Get a map from name -> object for all the websearchables of the given type.
     * @param type A valid TagType.
     * @return A new unmodifiable map containing the requested information.
     */
    public Map<String, WebSearchable> getWebSearchableMap(String type) {
        if (type == null) {
            throw new IllegalArgumentException("'type' may not be null");
        }
        Map<String, WebSearchable> retval = new HashMap<String, WebSearchable>();
        for (WebSearchable webSearchable : searchItems) {
            if (!type.equals(webSearchable.getTagType())) {
                continue;
            }
            retval.put(webSearchable.getName(), webSearchable);
        }
        return Collections.unmodifiableMap(retval);
    }

    /**
     * Collect the search items we are interested in initially. The profile associated with this
     * repository is a good place to start looking, but each implementation may decide it is
     * interested in a different subset of the available objects.
     */
    protected abstract void populateSearchItems();

    @Override
    public void receiveEvent(ChangeEvent e) {
        LOG.info("Received " + e);
        if (e instanceof PropertyChangeEvent) {
            handlePropertyChange((PropertyChangeEvent) e);
        } else if (e instanceof DeletionEvent) {
            handleDeletion((DeletionEvent) e);
        } else if (e instanceof CreationEvent) {
            handleCreation((CreationEvent) e);
        } else if (e instanceof TaggingEvent) {
            TaggingEvent te = (TaggingEvent) e;
            switch (te.getAction()) {
                case ADDED:
                    handleTagAddition(te);
                    break;
                case REMOVED:
                    handleTagRemoval(te);
                    break;
                default:
                    throw new IllegalStateException(
                            "Someone added a TagChange and didn't update this section of the code");
            }
        } else if (e instanceof MassTaggingEvent) {
            handleMassTagging();
        }
        // Propagate all events to the global repository for this user.
        if (!GLOBALS.contains(this)) {
            // No infinite recursion please...
            for (SearchRepository sr: GLOBALS) {
                if (profile.equals(sr.getProfile())) {
                    sr.receiveEvent(e);
                }
            }
        }
    }

    /**
     * Get the profile that this repository belongs to.
     * @return A profile.
     */
    protected Profile getProfile() {
        return profile;
    }

    /**
     * Drop the index that contains information about the websearchable object passed in.
     * @param ws The item whose cached information we wish to dispose of.
     */
    protected void dropIndex(WebSearchable ws) {
        indexes.remove(ws.getTagType());
    }

    /**
     * Retrieve a search index for a given type of web searchable objects. If there is already
     * an index around, that will be returned. If not, one will be created and returned.
     * @param type The type of the web searchables to index. Should be a valid TagType.
     * @return a RAMDirectory containing the index.
     */
    public Directory getSearchIndex(String type) {
        if (!(indexes.containsKey(type) && indexes.get(type) != null)) {

            indexes.put(type, index(type, searchItems, profile));
        }
        return indexes.get(type);
    }

    /**
     * Index some WebSearchables and return the RAMDirectory containing the index.
     *
     * @param type The type of the websearchables to index. Should be a valid TagType.
     * @param items The collection of websearchables watched by this repository.
     * @param profile The profile of the owner of the websearchables.
     * @return a RAMDirectory containing the index
     */
    private static Directory index(String type, Collection<WebSearchable> items, Profile profile) {
        if (type == null) {
            throw new IllegalArgumentException("'type' may not be null");
        }
        long time = System.currentTimeMillis();
        LOG.info("Indexing webSearchable queries");

        TagManager tagManager = new TagManagerFactory(profile.getProfileManager()).getTagManager();
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

        for (WebSearchable webSearchable : items) {
            if (!type.equals(webSearchable.getTagType())) {
                continue;
            }
            Document doc = new Document();
            // Store names and tags for retrieval, but all searched information goes into the
            // content buffer.
            doc.add(new Field("name", webSearchable.getName(), Field.Store.YES,
                              Field.Index.ANALYZED));
            StringBuffer contentBuffer = new StringBuffer(webSearchable.getTitle() + " : "
                                               + webSearchable.getDescription());
            List<Tag> tags = tagManager.getTags(null, webSearchable.getName(), type,
                    profile.getUsername());
            StringBuilder tagSB = new StringBuilder();

            for (Tag tag: tags) {
                String tagName = tag.getTagName();
                tagSB.append(" " + tagName);
                if (AspectTagUtil.isAspectTag(tagName)) {
                    contentBuffer.append(' ').append(AspectTagUtil.getAspect(tagName));
                } else {
                    contentBuffer.append(' ').append(tagName);
                }
            }
            doc.add(new Field("tags", tagSB.toString(), Field.Store.YES, Field.Index.ANALYZED));

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
        LOG.info("Indexed " + indexed + " webSearchables in " + time + " milliseconds");

        return ram;
    }

    /**
     * Respond in some way to the change in the properties of a web searchable object.
     * @param e The notification of the change event.
     */
    protected abstract void handlePropertyChange(PropertyChangeEvent e);

    /**
     * Respond in some way to the creation of a web searchable object.
     * @param e The notification of the change event.
     */
    protected abstract void handleCreation(CreationEvent e);

    /**
     * Respond in some way to the deletion of a web searchable object.
     * @param e The notification of the change event.
     */
    protected abstract void handleDeletion(DeletionEvent e);

    /**
     * Respond in some way to the addition of a tag to a web searchable object.
     * @param e The notification of the change event.
     */
    protected abstract void handleTagAddition(TaggingEvent e);

    /**
     * Respond in some way to the removal of a tag from a web searchable object.
     * @param e The notification of the change event.
     */
    protected abstract void handleTagRemoval(TaggingEvent e);

    /**
     * Respond to the fact that multiple tags have been added and that we do not know what they are.
     */
    protected abstract void handleMassTagging();

}
