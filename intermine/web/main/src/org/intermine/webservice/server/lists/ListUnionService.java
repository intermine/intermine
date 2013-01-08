package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2013 FlyMine
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
import org.intermine.api.bag.BagOperations;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

/**
 * A class for exposing creating unions of lists as a resource.
 * @author Alex Kalderimis.
 */
public class ListUnionService extends CommutativeOperationService
{

    /**
     * A message to give to users when something goes wrong.
     */
    public static final String USAGE =
        "\nList Union Service\n"
        + "===================\n"
        + "Combine lists into a new one\n"
        + "Parameters:\n"
        + "lists: a list of list names - separated by semi-cola (';')\n"
        + "       this parameter may be repeated."
        + "name: the name of the new list resulting from the union\n"
        + "description: an optional description of the new list\n";

    /**
     * Constructor.
     * @param im The InterMine magic sauce.
     */
    public ListUnionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected int doOperation(ListInput input, String type, Profile profile,
        Set<String> temporaryBagNamesAccumulator) throws Exception {
        Collection<InterMineBag> unionBags = ListServiceUtils.castBagsToCommonType(
               input.getLists(), type, temporaryBagNamesAccumulator, profile, im.getClassKeys());
        int sizeOfUnion = BagOperations.union(unionBags, input.getTemporaryListName(), profile,
                                        im.getClassKeys());
        return sizeOfUnion;
    }
}
