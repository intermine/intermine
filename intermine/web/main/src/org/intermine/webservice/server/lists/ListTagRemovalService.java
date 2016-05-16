package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.model.userprofile.Tag;

/**
 * A service for removing tags from a list.
 * @author Alex Kalderimis
 *
 */
public class ListTagRemovalService extends ListTagAddingService
{

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public ListTagRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void modifyList(Set<String> tags, InterMineBag list) {
        Profile profile = getPermission().getProfile();
        TagManager tm = im.getTagManager();

        for (Tag tag: bagManager.getTagsForBag(list, profile)) {
            if (tags.contains(tag.getTagName())) {
                tm.deleteTag(tag.getTagName(), list, profile);
            }
        }
    }

}
