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

import java.util.Collection;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import static org.intermine.api.bag.BagOperations.subtract;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

/**
 * A service for performing a symmetric difference operation on a collection
 * of lists.
 * @author Alex Kalderimis
 *
 */
public class ListDifferenceService extends CommutativeOperationService
{

    /**
     * Constructor
     * @param im The InterMine application object.
     */
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

        int sizeOfDifference = subtract(diffBags, tempName, profile, im.getClassKeys());
        return sizeOfDifference;
    }

}
