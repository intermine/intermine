package org.intermine.webservice.server.lists;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.split;
import static org.intermine.api.bag.BagOperations.intersect;
import static org.intermine.api.bag.BagOperations.subtract;
import static org.intermine.api.bag.BagOperations.union;
import static org.intermine.webservice.server.lists.ListServiceUtils.castBagsToCommonType;
import static org.intermine.webservice.server.lists.ListServiceUtils.ensureBagIsDeleted;
import static org.intermine.webservice.server.lists.ListServiceUtils.findCommonSuperTypeOf;
import static org.intermine.webservice.server.lists.ListServiceUtils.getBagsAndClasses;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;

/**
 * A service for subtracting one group of lists from another group of lists to produce a new
 * list.
 * @author Alexis Kalderimis
 *
 */
public class ListSubtractionService extends ListUnionService
{

    private static final String LEFT = "_temp_left";
    private static final String RIGHT = "_temp_right";
    private static final String SYMETRIC_DIFF = "_temp_symdiff";
    private static final String INTERSECT = "_temp_intersect";

    /**
     * Constructor
     * @param im A reference to the main settings bundle.
     */
    public ListSubtractionService(InterMineAPI im) {
        super(im);
    }

    /**
     * Usage information to be displayed for bad requests.
     */
    public static final String USAGE =
        "\nList Subtraction Service\n"
        + "===================\n"
        + "Subtract lists from a main one\n"
        + "Parameters:\n"
        + "list: The main list to subtract the others from\n"
        + "lists: a list of list names - separated by semi-cola (';')\n"
        + "name: the name of the new list resulting from the subtraction\n\n"
        + "Content: text/plain - list of ids\n";

    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated." + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        String references = request.getParameter("references");
        String subtract = request.getParameter("subtract");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String[] tags = split(request.getParameter("tags"), ';');
        boolean replace = Boolean.parseBoolean("replaceExisting");

        if (isEmpty(references) || isEmpty(subtract) || isEmpty(name)) {
            throw new BadRequestException("Name or list or lists is blank." + USAGE);
        }

        Map<String, Object> attributes = getHeaderAttributes();
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("listName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);

        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        Set<InterMineBag> referenceBags = new HashSet<InterMineBag>();
        Set<InterMineBag> bagsToSubtract = new HashSet<InterMineBag>();

        String[] referenceNames = split(references, LIST_SEPARATOR);
        String[] subtractNames = split(subtract, LIST_SEPARATOR);

        getBagsAndClasses(classes, referenceBags, profile, bagManager, model, referenceNames);
        getBagsAndClasses(classes, bagsToSubtract, profile, bagManager, model, subtractNames);

        String type = findCommonSuperTypeOf(classes);

        String tempName = name + TEMP_SUFFIX;
        String leftName = name + LEFT;
        String rightName = name + RIGHT;
        String symDiffName = name + SYMETRIC_DIFF;
        String intersectName = name + INTERSECT;

        Set<String> bagsToDelete = new HashSet<String>(
                asList(tempName, leftName, rightName, symDiffName, intersectName));
        try {
            final Collection<InterMineBag> leftBags
                = castBagsToCommonType(referenceBags, type, bagsToDelete, profile);
            final Collection<InterMineBag> rightBags
                = castBagsToCommonType(bagsToSubtract, type, bagsToDelete, profile);
            final int leftSize = union(leftBags, leftName, profile);
            final int rightSize = union(rightBags, rightName, profile);

            int finalBagSize = 0;

            if (leftSize + rightSize > 0) {
                final InterMineBag leftList = profile.getSavedBags().get(leftName);
                final InterMineBag rightList = profile.getSavedBags().get(rightName);
                final int sizeOfSymDiff
                    = subtract(asList(leftList, rightList), symDiffName, profile);

                if (sizeOfSymDiff != 0) {
                    final InterMineBag diffBag = profile.getSavedBags().get(symDiffName);

                    finalBagSize
                        = intersect(asList(diffBag, leftList), intersectName, profile);
                }
            }

            InterMineBag newList;
            if (finalBagSize == 0) {
                newList = profile.createBag(tempName, type, description);
            } else {
                newList = profile.getSavedBags().get(intersectName);
                if (description != null) {
                    newList.setDescription(description);
                }
            }
            output.addResultItem(asList("" + newList.size()));

            if (replace) {
                ensureBagIsDeleted(profile, name);
            }
            if (tags != null) {
	            im.getBagManager().addTagsToBag(asList(tags), newList, profile);
            }
            profile.renameBag(newList.getName(), name);
        } finally {
            for (String delendum: bagsToDelete) {
                ensureBagIsDeleted(profile, delendum);
            }
        }
    }

}
