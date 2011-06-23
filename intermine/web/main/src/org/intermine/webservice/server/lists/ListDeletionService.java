package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;

public class ListDeletionService extends AuthenticatedListService
{

    /**
     * Usage information to help users who provide incorrect input.
     */
    public static final String USAGE =
          "List Deletion Service\n"
        + "=====================\n"
        + "Delete a list\n"
        + "Parameters:\n"
        + "name: the name of the list to delete\n"
        + "NOTE: All requests to this service must authenticate to a valid user account\n";

    public ListDeletionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Profile profile = SessionMethods.getProfile(request.getSession());
        ListInput input = getInput(request);
        addOutputInfo(LIST_NAME_KEY, input.getListName());
        ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
    }
}
