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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.TagManagerFactory;
import org.intermine.api.profile.Taggable;
import org.intermine.model.userprofile.Tag;

/**
 * Class for filtering web-searchables based on their tags.
 * @author Alex Kalderimis
 *
 */
public final class TagFilter
{

    private final List<String> tags = new ArrayList<String>();
    private final Profile profile;
    private final TagManager tagManager;
    private final String type;

    /**
     * Constructor
     * @param tags The tags we require taggable item to have.
     * @param profile The profile all the tags are to belong to.
     * @param tagType The type of item we are expecting. The purpose of this is to initalise the
     *                results cache correctly. Passing in the wrong type, or even null will be fine,
     *                just less efficient.
     */
    public TagFilter(Collection<String> tags, Profile profile, String tagType) {
        this.tags.addAll(tags);
        this.profile = profile;
        this.tagManager = new TagManagerFactory(profile.getProfileManager()).getTagManager();
        this.type = tagType;

        initCache();
    }

    private void initCache() {
     // prime the cache
        for (String tagName: this.tags) {
            tagManager.getTags(tagName, null, type, profile.getUsername());
        }
    }

    /**
     * Check to see if this taggable item has all the tags it is meant to.
     * @param t A taggable item.
     * @return A truth value.
     */
    public boolean hasAllTags(Taggable t) {
        String uname = profile.getUsername();
        for (String tagName: this.tags) {
            List<Tag> found = tagManager.getTags(tagName, t.getName(), t.getTagType(), uname);
            if (found.size() == 0) {
                return false;
            }
        }
        return true;
    }

}
