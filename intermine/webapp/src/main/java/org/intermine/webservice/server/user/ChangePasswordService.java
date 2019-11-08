package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2019 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.UnauthorizedException;

/**
 * Class for changing the password of an existing user
 *
 * @author Daniela Butano
 *
 */
public class ChangePasswordService extends JSONService
{
    /**
     * Constructor
     * @param im The InterMine API object.
     */
    public ChangePasswordService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        String newPassword = getRequiredParameter("newPassword");
        ProfileManager pm = im.getProfileManager();
        Profile profile;
        if (isAuthenticated()) {
            profile = getPermission().getProfile();
        } else {
            throw new UnauthorizedException("The request must be authenticated");
        }

        pm.setPassword(profile.getUsername(), newPassword);
    }
}
