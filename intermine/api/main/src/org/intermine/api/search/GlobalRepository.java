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

import java.util.Arrays;
import java.util.List;

import org.intermine.api.profile.Profile;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tag.TagTypes;

/**
 * A representation of an object that knows how to index global objects, and also how to respond
 * to changes to those objects.
 *
 * @author Alex Kalderimis
 *
 */
public class GlobalRepository extends UserRepository
{

    private static final List<String> PUBLIC_FILTER_TAGS = Arrays.asList(TagNames.IM_PUBLIC);

    /**
     * Build a new global search repository and register it with the list of global repositories
     * available.
     * @param profile The profile of the user whose items we are to index.
     */
    public GlobalRepository(Profile profile) {
        super(profile);
        GLOBALS.add(this);
    }

    @Override
    protected void handleCreation(CreationEvent e) {
        // Ignore => cannot be global on creation.
    }

    @Override
    protected void handleTagAddition(TaggingEvent e) {
        WebSearchable ws = e.getOrigin();
        if (TagNames.IM_PUBLIC.equals(e.getTagName())) {
            searchItems.add(ws);
            dropIndex(ws);
            ws.addObserver(this);
        } else {
            if (searchItems.contains(ws)) {
                dropIndex(ws);
            }
        }
    }

    @Override
    protected void handleTagRemoval(TaggingEvent e) {
        WebSearchable ws = e.getOrigin();
        if (TagNames.IM_PUBLIC.equals(e.getTagName())) {
            searchItems.remove(ws);
            dropIndex(ws);
            ws.removeObserver(this);
        } else {
            if (searchItems.contains(ws)) {
                dropIndex(ws);
            }
        }
    }

    @Override
    protected void populateSearchItems() {
        SearchFilterEngine sfe = new SearchFilterEngine();
        Profile p = getProfile();
        // Only index items tagged with "im:public".
        searchItems.addAll(
                sfe.filterByTags(p.getSavedBags(), PUBLIC_FILTER_TAGS, TagTypes.BAG,
                        p.getUsername(), tagManager, false).values());
        searchItems.addAll(
                sfe.filterByTags(p.getSavedTemplates(), PUBLIC_FILTER_TAGS, TagTypes.TEMPLATE,
                        p.getUsername(), tagManager, false).values());
    }

    @Override
    protected void handleMassTagging() {
        stopListening();
        indexes.clear();
        // And additionally, repopulate the set of items.
        searchItems.clear();
        populateSearchItems();
        startListening();
    }
}
