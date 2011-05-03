package org.intermine.webservice.server.lists;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;

public class ListRenameService extends ListUploadService {

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
        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated.\n" + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        String oldName = request.getParameter("oldname");
        String newName = request.getParameter("newname");

        setHeaderAttributes(Arrays.asList(oldName, newName));

        if (!profile.getSavedBags().containsKey(oldName)) {
            throw new BadRequestException(oldName + " is not a list you have access to");
        }
        profile.renameBag(oldName, newName);
        InterMineBag list = profile.getSavedBags().get(newName);

        setListName(newName);
        setListSize(list.size());

    }
}
