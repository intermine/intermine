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

import java.util.Arrays;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

/**
 * A base class for services that perform operations on lists.
 * @author Alex Kalderimis
 *
 */
public abstract class ListOperationService extends ListMakerService
{

    /**
     * Constructor.
     * @param api The InterMine application object.
     */
    public ListOperationService(InterMineAPI api) {
        super(api);
    }

    /**
     * Perform the operation on the lists.
     * @param input The parameters passed with the request.
     * @param type The class of the objects in the new list.
     * @param profile The profile any new lists will be saved to.
     * @param temporaryBagsAccumulator A rubbish bin for temporary bags.
     *  Anything placed here will be deleted.
     * @return The size of the newly created list.
     * @throws Exception if something goes wrong.
     */
    protected abstract int doOperation(ListInput input, String type, Profile profile,
            Set<String> temporaryBagsAccumulator) throws Exception;


    @Override
    protected void makeList(ListInput input, String type, Profile profile,
        Set<String> rubbishbin) throws Exception {
        int size = doOperation(input, type, profile, rubbishbin);
        InterMineBag newList;
        if (size == 0) {
            output.addResultItem(Arrays.asList("0"));
            newList = profile.createBag(
                input.getTemporaryListName(), type, input.getDescription(), im.getClassKeys());
        } else {
            newList = profile.getSavedBags().get(input.getTemporaryListName());
            if (input.getDescription() != null) {
                newList.setDescription(input.getDescription());
            }
            output.addResultItem(Arrays.asList("" + newList.size()));
        }
        if (input.doReplace()) {
            ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
        }
        if (!input.getTags().isEmpty()) {
            bagManager.addTagsToBag(input.getTags(),
                    profile.getSavedBags().get(input.getTemporaryListName()),
                    profile);
        }
        profile.renameBag(input.getTemporaryListName(), input.getListName());

    }

}
