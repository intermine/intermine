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

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.tag.TagTypes;
import org.intermine.api.util.AnonProfile;
import org.intermine.model.userprofile.Tag;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * A service for getting the tags of a list.
 * @author Alex Kalderimis
 *
 */
public class ListTagService extends AbstractListService
{

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public ListTagService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Map<String, Object> getHeaderAttributes() {
        Map<String, Object> attributes = super.getHeaderAttributes();
        attributes.put(JSONFormatter.KEY_INTRO, "\"tags\":[");
        attributes.put(JSONFormatter.KEY_OUTRO, "]");
        attributes.put(JSONFormatter.KEY_QUOTE, true);
        return attributes;
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        String listName = getOptionalParameter("name", null);


        Set<String> tags = new HashSet<String>();
        // if not logged in, return empty. See #1222
        if (!AnonProfile.USERNAME.equals(profile.getUsername())) {
            if (listName == null) {
                tags = getAllTags(profile);
            } else {
                tags = getTagsForSingleList(listName, profile);
            }
        }

        output.addResultItem(new ArrayList<String>(tags));
    }

    private Set<String> getTagsForSingleList(String name, Profile profile) {
        BagManager bagManager = im.getBagManager();

        Map<String, InterMineBag> lists = bagManager.getBags(profile);
        InterMineBag list = lists.get(name);
        if (list == null) {
            throw new ResourceNotFoundException(
                    "You do not have access to a list called " + name);
        }
        List<Tag> tags = bagManager.getTagsForBag(list, profile);
        @SuppressWarnings("unchecked")
        Collection<String> tagNames = collect(tags, invokerTransformer("getTagName"));
        return new HashSet<String>(tagNames);
    }

    private Set<String> getAllTags(Profile profile) {
        return im.getTagManager().getUserTagNames(TagTypes.BAG, profile);
    }

}
