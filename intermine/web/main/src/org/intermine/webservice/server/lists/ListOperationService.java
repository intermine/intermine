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

import java.util.Arrays;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.operations.BagOperation;
import org.intermine.api.bag.operations.IncompatibleTypes;
import org.intermine.api.bag.operations.NoContent;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.webservice.server.exceptions.BadRequestException;

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

    @Override
    public String getNewListType(ListInput input) {
        return null; // Not needed, as it can be calculated.
    }

    @Override
    protected ListInput getInput() {
        return new CommutativeOperationInput(request, bagManager, getPermission().getProfile());
    }

    /**
     * Get the BagOperation that will create the list.
     * @param input The parameters provided by the user.
     * @return A BagOperation.
     */
    protected abstract BagOperation getOperation(ListInput input);

    @Override
    protected void makeList(ListInput input, String type, Profile profile, Set<String> rubbishbin)
        throws Exception {

        int size = 0;
        InterMineBag newBag;
        BagOperation operation = getOperation(input);
        operation.setClassKeys(im.getClassKeys());

        try { // Make sure we can clean up if ANYTHING goes wrong.
            rubbishbin.add(operation.getNewBagName());
        } catch (IncompatibleTypes e) {
            throw new BadRequestException("Incompatible types", e);
        }

        try {
            newBag = operation.operate();
            size = newBag.getSize();
        } catch (NoContent e) {
            // This service guarantees a bag, even an empty one.
            size = 0;
            newBag = profile.createBag(
                operation.getNewBagName(), // If this throws, it should have done so by now.
                operation.getNewBagType(), // If this throws, it should have done so by now.
                input.getDescription(),
                im.getClassKeys());
        }

        if (input.getDescription() != null) {
            newBag.setDescription(input.getDescription());
        }
        if (!input.getTags().isEmpty()) {
            bagManager.addTagsToBag(input.getTags(), newBag, profile);
        }

        output.addResultItem(Arrays.asList("" + size));

        if (input.doReplace()) {
            ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
        }

        profile.renameBag(newBag.getName(), input.getListName());
    }

}
