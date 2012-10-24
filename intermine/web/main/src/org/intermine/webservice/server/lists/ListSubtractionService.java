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

import static java.util.Arrays.asList;
import static org.intermine.api.bag.BagOperations.intersect;
import static org.intermine.api.bag.BagOperations.subtract;
import static org.intermine.api.bag.BagOperations.union;
import static org.intermine.webservice.server.lists.ListServiceUtils.castBagsToCommonType;
import static org.intermine.webservice.server.lists.ListServiceUtils.findCommonSuperTypeOf;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;

/**
 * A service for subtracting one group of lists from another group of lists to produce a new
 * list.
 * @author Alexis Kalderimis
 *
 */
public class ListSubtractionService extends ListOperationService
{

    private static final String LEFT = "_temp_left";
    private static final String RIGHT = "_temp_right";
    private static final String SYMETRIC_DIFF = "_temp_symdiff";

    /**
     * Constructor
     * @param im A reference to the main settings bundle.
     */
    public ListSubtractionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected ListInput getInput(HttpServletRequest request) {
        return new AsymmetricOperationInput(request, bagManager, getPermission().getProfile());
    }

    @Override
    protected String getNewListType(ListInput input) {
        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        classes.addAll(getClassesForBags(input.getLists()));
        classes.addAll(getClassesForBags(input.getReferenceLists()));
        String type = findCommonSuperTypeOf(classes);
        return type;
    }

    @Override
    protected void initialiseDelendumAccumulator(Set<String> accumulator, ListInput input) {
        for (String suffix: Arrays.asList(LEFT, RIGHT, SYMETRIC_DIFF)) {
            accumulator.add(input.getListName() + suffix);
        }
    }

    /**
     * Usage information to be displayed for bad requests.
     */
    public static final String USAGE =
        "\nList Subtraction Service\n"
        + "===================\n"
        + "Subtract one set of list from another\n"
        + "Parameters:\n"
        + "references: The main list to subtract the others from\n"
        + "subtract: a list of list names - separated by semi-cola (';')\n"
        + "name: the name of the new list resulting from the subtraction\n"
        + "description: an optional description for the new list\n"
        + "NOTE: All requests to this service must authenticate to a valid user account\n";

    @Override
    protected int doOperation(ListInput input, String type, Profile profile,
        Set<String> temporaryBagNamesAccumulator) throws Exception {

        final Collection<InterMineBag> leftBags
            = castBagsToCommonType(input.getReferenceLists(), type, temporaryBagNamesAccumulator, profile,
                                  im.getClassKeys());
        final Collection<InterMineBag> rightBags
            = castBagsToCommonType(input.getLists(), type, temporaryBagNamesAccumulator, profile,
                                  im.getClassKeys());
        final int leftSize = union(leftBags, input.getListName() + LEFT, profile, im.getClassKeys());
        final int rightSize = union(rightBags, input.getListName() + RIGHT, profile, im.getClassKeys());

        int finalBagSize = 0;

        if (leftSize + rightSize > 0) {
            final InterMineBag leftList = profile.getSavedBags().get(input.getListName() + LEFT);
            final InterMineBag rightList = profile.getSavedBags().get(input.getListName() + RIGHT);
            final int sizeOfSymDiff
                = subtract(asList(leftList, rightList), input.getListName() + SYMETRIC_DIFF, profile,
                           im.getClassKeys());

            if (sizeOfSymDiff != 0) {
                final InterMineBag diffBag = profile.getSavedBags().get(input.getListName() + SYMETRIC_DIFF);

                finalBagSize
                    = intersect(asList(diffBag, leftList), input.getTemporaryListName(), profile,
                                       im.getClassKeys());
            }
        }
        return finalBagSize;
    }

}
