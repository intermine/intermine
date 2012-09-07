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

import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;

/**
 * A repository for searching the searchable items of a single user.
 * @author Alex Kalderimis.
 *
 */
public class UserRepository extends SearchRepository
{
    /**
     * Constructor.
     * @param profile The profile of the user whose data we are indexing.
     */
    public UserRepository(Profile profile) {
        super(profile);
    }

    private static final Logger LOG = Logger.getLogger(UserRepository.class);

    @Override
    protected void handleCreation(CreationEvent e) {
        WebSearchable ws = e.getOrigin();
        searchItems.add(ws);
        dropIndex(ws);
        ws.addObserver(this);
    }

    @Override
    protected void handleDeletion(DeletionEvent e) {
        WebSearchable ws = e.getOrigin();
        searchItems.remove(ws);
        dropIndex(ws);
        ws.removeObserver(this);
    }

    @Override
    protected void handlePropertyChange(PropertyChangeEvent e) {
        dropIndex(e.getOrigin());
    }

    @Override
    protected void handleTagAddition(TaggingEvent e) {
        dropIndex(e.getOrigin());
    }

    @Override
    protected void handleTagRemoval(TaggingEvent e) {
        dropIndex(e.getOrigin());
    }

    @Override
    protected void populateSearchItems() {
        searchItems.addAll(getProfile().getSavedBags().values());
        searchItems.addAll(getProfile().getSharedBags().values());
        searchItems.addAll(getProfile().getSavedTemplates().values());
        LOG.info("Populated repository with " + searchItems.size() + " items");
    }

    @Override
    protected void handleMassTagging() {
        // Nothing much we can do. May have been gloal indices, may not.
        indexes.clear();
    }

}
