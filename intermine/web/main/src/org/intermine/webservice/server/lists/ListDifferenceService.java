package org.intermine.webservice.server.lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;
import org.intermine.webservice.server.output.JSONFormatter;

public class ListDifferenceService extends ListUnionService {

    public ListDifferenceService(InterMineAPI im) {
        super(im);
    }

    /**
     * Usage string to school users with, if they provoke a BadRequestException.
     */
    public static final String USAGE =
          "\nList Difference Service\n"
          + "=======================\n"
          + "Create a new list from the asymmetric difference of a set of lists\n"
          + "Parameters:\n"
          + "lists: a list of list names - separated by semi-cola (';')\n"
          + "name: the name of the new list resulting from the difference\n"
          + "NOTE: All requests to this service must authenticate to a valid user account.\n";

    @Override
    protected void execute(HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated." + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        String lists = request.getParameter("lists");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        boolean replace = Boolean.parseBoolean("replaceExisting");

        if (StringUtils.isEmpty(lists) || StringUtils.isEmpty(name)) {
            throw new BadRequestException("Name or lists is blank." + USAGE);
        }

        Map<String, Object> attributes = getHeaderAttributes();
        Map<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("newListName", name);
        attributes.put(JSONFormatter.KEY_KV_PAIRS, kvPairs);
        output.setHeaderAttributes(attributes);

        String[] listNames = StringUtils.split(lists, LIST_SEPARATOR);

        Set<ClassDescriptor> classes = new HashSet<ClassDescriptor>();
        Set<InterMineBag> listsToDiff = new HashSet<InterMineBag>();
        ListServiceUtils.getBagsAndClasses(classes, listsToDiff,
                profile, bagManager, model, listNames);
        String type = ListServiceUtils.findCommonSuperTypeOf(classes);
        String tempName = name + TEMP_SUFFIX;
        Set<String> bagsToDelete = new HashSet<String>(Arrays.asList(tempName));
        try {
            Collection<InterMineBag> diffBags = ListServiceUtils.castBagsToCommonType(
                    listsToDiff, type, bagsToDelete, profile);

            int sizeOfDifference = BagOperations.subtract(diffBags, tempName, profile);

            InterMineBag newList;
            if (sizeOfDifference == 0) {
                output.addResultItem(Arrays.asList("0"));
                newList = profile.createBag(tempName, type, description);
            } else {
                newList = profile.getSavedBags().get(tempName);
                if (description != null) {
                    newList.setDescription(description);
                }
                output.addResultItem(Arrays.asList("" + newList.size()));
            }

            if (replace) {
                ListServiceUtils.ensureBagIsDeleted(profile, name);
            }
            profile.renameBag(tempName, name);
        } finally {
            for (String delendum: bagsToDelete) {
                ListServiceUtils.ensureBagIsDeleted(profile, delendum);
            }
        }
    }

}
