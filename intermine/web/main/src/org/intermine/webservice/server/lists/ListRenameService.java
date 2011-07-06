package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;

public class ListRenameService extends AuthenticatedListService
{

    /**
     * Usage information to help users who provide incorrect input.
     */
    public static final String USAGE =
          "List Renaming Service\n"
        + "=====================\n"
        + "Rename a list\n"
        + "Parameters:\n"
        + "oldname: the old name of the list\n"
        + "newname: the new name of the list\n"
        + "NOTE: All requests to this service must authenticate to a valid user account\n";

    public ListRenameService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Profile profile = SessionMethods.getProfile(request.getSession());

        ListRenameInput input = new ListRenameInput(request, bagManager);

        output.setHeaderAttributes(getHeaderAttributes());

        profile.renameBag(input.getOldName(), input.getNewName());
        InterMineBag list = profile.getSavedBags().get(input.getNewName());

        addOutputInfo(LIST_NAME_KEY, list.getName());
        addOutputInfo(LIST_SIZE_KEY, "" + list.size());

    }
}
