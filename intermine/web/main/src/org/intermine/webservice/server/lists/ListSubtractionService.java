package org.intermine.webservice.server.lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagOperations;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;

public class ListSubtractionService extends ListUnionService {

    public static final String LEFT = "_temp_left";
    public static final String RIGHT = "_temp_right";
    public static final String SYMETRIC_DIFF = "_temp_symdiff";
    public static final String INTERSECT = "_temp_intersect";

    public ListSubtractionService(InterMineAPI im) {
        super(im);
    }

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
        boolean replace = Boolean.parseBoolean("replaceExisting");

        if (StringUtils.isEmpty(references) || StringUtils.isEmpty(subtract) || StringUtils.isEmpty(name)) {
            throw new BadRequestException("Name or list or lists is blank." + USAGE);
        }

        Map<String, Object> attributes = getHeaderAttributes();
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("newListName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);

        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        Set<InterMineBag> referenceBags = new HashSet<InterMineBag>();
        Set<InterMineBag> bagsToSubtract = new HashSet<InterMineBag>();

        String[] referenceNames = StringUtils.split(references, LIST_SEPARATOR);
        String[] subtractNames = StringUtils.split(subtract, LIST_SEPARATOR);

        ListServiceUtils.getBagsAndClasses(classes, referenceBags,
                profile, bagManager, model, referenceNames);
        ListServiceUtils.getBagsAndClasses(classes, bagsToSubtract,
                profile, bagManager, model, subtractNames);

        String type = ListServiceUtils.findCommonSuperTypeOf(classes);

        String tempName = name + TEMP_SUFFIX;
        String leftName = name + LEFT;
        String rightName = name + RIGHT;
        String symDiffName = name + SYMETRIC_DIFF;
        String intersectName = name + INTERSECT;

        Set<String> bagsToDelete = new HashSet<String>(
                Arrays.asList(tempName, leftName, rightName, symDiffName, intersectName));
        try {
            final Collection<InterMineBag> leftBags = ListServiceUtils.castBagsToCommonType(referenceBags, type, bagsToDelete, profile);
            final Collection<InterMineBag> rightBags = ListServiceUtils.castBagsToCommonType(bagsToSubtract, type, bagsToDelete, profile);
            final int leftSize = BagOperations.union(leftBags, leftName, profile);
            final int rightSize = BagOperations.union(rightBags, rightName, profile);

            int finalBagSize = 0;

            if (leftSize + rightSize > 0) {
                final InterMineBag leftList = profile.getSavedBags().get(leftName);
                final InterMineBag rightList = profile.getSavedBags().get(rightName);
                final int sizeOfSymDiff = BagOperations.subtract(Arrays.asList(leftList, rightList), symDiffName, profile);

                if (sizeOfSymDiff != 0) {
                    final InterMineBag diffBag = profile.getSavedBags().get(symDiffName);

                    finalBagSize = BagOperations.intersect(Arrays.asList(diffBag, leftList), intersectName, profile);
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
            output.addResultItem(Arrays.asList("" + newList.size()));

            if (replace) {
                ListServiceUtils.ensureBagIsDeleted(profile, name);
            }
            profile.renameBag(newList.getName(), name);
        } finally {
            for (String delendum: bagsToDelete) {
                ListServiceUtils.ensureBagIsDeleted(profile, delendum);
            }
        }
    }

}
