package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;
import static org.apache.commons.lang.StringUtils.split;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.model.userprofile.Tag;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

/**
 * A service for removing tags from a list.
 * @author Alex Kalderimis
 *
 */
public class ListTagRemovalService extends ListTagService
{

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public ListTagRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String listName = request.getParameter("name");
        String[] tags = split(request.getParameter("tags"), ';');
        if (tags == null) {
            throw new BadRequestException("No tags supplied");
        }
        Set<String> tagset = new HashSet<String>(Arrays.asList(tags));

        BagManager bagManager = im.getBagManager();
        Profile profile = getPermission().getProfile();
        Map<String, InterMineBag> lists = bagManager.getBags(profile);
        InterMineBag list = lists.get(listName);
        if (list == null) {
            throw new ResourceNotFoundException("You do not have access to a list called "
                    + listName);
        }
        TagManager tm = im.getTagManager();

        for (Tag tag: bagManager.getTagsForBag(list, profile)) {
            if (tagset.contains(tag.getTagName())) {
                tm.deleteTag(tag.getTagName(), list, profile);
            }
        }
        List<Tag> allTags = bagManager.getTagsForBag(list, profile);
        List<String> tagNames = (List<String>) collect(allTags, invokerTransformer("getTagName"));
        output.addResultItem(tagNames);
    }

}
