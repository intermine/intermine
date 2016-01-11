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
import static org.apache.commons.lang.StringUtils.split;

import java.util.Collections;
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
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

/**
 * A service for adding tags to a list.
 * @author Alex Kalderimis
 *
 */
public class ListTagAddingService extends ListTagService
{

    private static final String TAGS = "tags";

    /**
     * Constructor.
     * @param im The InterMine API settings.
     */
    public ListTagAddingService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String listName = getRequiredParameter("name");
        Set<String> tags = getTags();

        BagManager bagManager = im.getBagManager();
        Profile profile = getPermission().getProfile();
        Map<String, InterMineBag> lists = bagManager.getBags(profile);
        InterMineBag list = lists.get(listName);
        if (list == null) {
            throw new ResourceNotFoundException(
                    "You do not have access to a list called " + listName);
        }

        modifyList(tags, list);

        List<Tag> allTags = bagManager.getTagsForBag(list, getPermission().getProfile());
        @SuppressWarnings("unchecked")
        List<String> tagNames = (List<String>) collect(allTags, invokerTransformer("getTagName"));
        output.addResultItem(tagNames);
    }

    /**
     * Modify the list by adding the tags.
     * @param tags The tags to add.
     * @param list The list to add them for.
     */
    protected void modifyList(Set<String> tags, InterMineBag list) {
        Profile profile = getPermission().getProfile();
        try {
            bagManager.addTagsToBag(tags, list, profile);
        } catch (TagManager.TagNameException e) {
            throw new BadRequestException(e.getMessage());
        } catch (TagManager.TagNamePermissionException e) {
            throw new ServiceForbiddenException(e.getMessage());
        }
    }

    private Set<String> getTags() {
        String[] tagValues = request.getParameterValues(TAGS);
        if (tagValues == null) {
            throw new BadRequestException("No tags supplied");
        }
        Set<String> tags = new HashSet<String>();
        for (String tagValue: tagValues) {
            String[] subvalues = split(tagValue, ';');
            Collections.addAll(tags, subvalues);
        }
        return tags;
    }

}
