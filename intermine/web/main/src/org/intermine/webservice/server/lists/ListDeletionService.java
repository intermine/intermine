package org.intermine.webservice.server.lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.exceptions.BadRequestException;

public class ListDeletionService extends ListUploadService {

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
        if (!this.isAuthenticated()) {
            throw new BadRequestException("Not authenticated.\n" + USAGE);
        }
        HttpSession session = request.getSession();
        Profile profile = SessionMethods.getProfile(session);

        String name = request.getParameter("name");

        if (StringUtils.isEmpty(name)) {
            throw new BadRequestException("Name is blank." + USAGE);
        }

        output.setHeaderAttributes(getHeaderAttributes());

        if (!profile.getSavedBags().containsKey(name)) {
            throw new BadRequestException(name + " is not a list you have access to");
        }
        ListServiceUtils.ensureBagIsDeleted(profile, name);
    }
}
