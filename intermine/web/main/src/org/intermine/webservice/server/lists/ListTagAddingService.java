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
import java.util.List;
import java.util.Map;

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

    /**
     * Constructor.
     * @param im The InterMine API settings.
     */
    public ListTagAddingService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String listName = request.getParameter("name");
        String[] tags = split(request.getParameter("tags"), ';');
        if (tags == null) {
            throw new BadRequestException("No tags supplied");
        }
        BagManager bagManager = im.getBagManager();
        Profile profile = getPermission().getProfile();
        Map<String, InterMineBag> lists = bagManager.getUserAndGlobalBags(profile);
        InterMineBag list = lists.get(listName);
        if (list == null) {
            throw new ResourceNotFoundException(
                    "You do not have access to a list called " + listName);
        }

        try {
            bagManager.addTagsToBag(Arrays.asList(tags), list, profile);
        } catch (TagManager.TagNameException e) {
            throw new BadRequestException(e.getMessage());
        } catch (TagManager.TagNamePermissionException e) {
            throw new ServiceForbiddenException(e.getMessage());
        }


        List<Tag> allTags = bagManager.getTagsForBag(list, getPermission().getProfile());
        List<String> tagNames = (List<String>) collect(allTags, invokerTransformer("getTagName"));
        output.addResultItem(tagNames);
    }

}
