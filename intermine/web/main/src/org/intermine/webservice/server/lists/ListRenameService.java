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

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;

/**
 * A service for renaming lists.
 * @author Alex Kalderimis
 *
 */
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

    /**
     * Constructor.
     * @param im The InterMine API settings.
     */
    public ListRenameService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();

        ListRenameInput input = new ListRenameInput(request, bagManager, profile);

        output.setHeaderAttributes(getHeaderAttributes());

        profile.renameBag(input.getOldName(), input.getNewName());
        InterMineBag list = profile.getSavedBags().get(input.getNewName());

        addOutputInfo(LIST_NAME_KEY, list.getName());
        addOutputInfo(LIST_SIZE_KEY, "" + list.size());

    }
}
