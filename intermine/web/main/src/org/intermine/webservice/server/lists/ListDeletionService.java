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

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;

/**
 * A service for deleting lists from the user-profile database.
 * @author Alex Kalderimis
 *
 */
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

    /**
     * Constructor.
     * @param im The InterMine application object.
     */
    public ListDeletionService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        ListInput input = getInput(request);
        addOutputInfo(LIST_NAME_KEY, input.getListName());
        ListServiceUtils.ensureBagIsDeleted(profile, input.getListName());
    }
}
