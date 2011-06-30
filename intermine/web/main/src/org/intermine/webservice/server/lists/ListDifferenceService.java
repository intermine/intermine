package org.intermine.webservice.server.lists;

import java.util.Collection;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagOperations;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

public class ListDifferenceService extends CommutativeOperationService {

    public ListDifferenceService(InterMineAPI im) {
        super(im);
    }

    /**
     * Usage string to school users with, if they provoke a BadRequestException.
     */
    public static final String USAGE =
          "\nList Difference Service\n"
          + "=======================\n"
          + "Create a new list from the symmetric difference of a set of lists\n"
          + "Parameters:\n"
          + "lists: a list of list names - separated by semi-cola (';')\n"
          + "name: the name of the new list resulting from the difference\n"
          + "NOTE: All requests to this service must authenticate to a valid user account.\n";


    @Override
    protected int doOperation(ListInput input, String type, Profile profile,
        Set<String> temporaryBagNamesAccumulator) throws Exception {
        String tempName = input.getTemporaryListName();
        Collection<InterMineBag> diffBags = ListServiceUtils.castBagsToCommonType(
                input.getLists(), type, temporaryBagNamesAccumulator, profile, im.getClassKeys());

        int sizeOfDifference = BagOperations.subtract(diffBags, tempName, profile, im.getClassKeys());
        return sizeOfDifference;
    }

}
