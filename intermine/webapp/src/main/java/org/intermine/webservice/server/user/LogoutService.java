package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2021 FlyMine
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
 * Logout service which invalidates the token assigned to the user logging out
 * It requires token authentication
 * @author Daniela Butano
 *
 */
public class LogoutService extends JSONService
{
    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public LogoutService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile;
        if (isAuthenticated()) {
            profile = getPermission().getProfile();
        } else {
            throw new UnauthorizedException("The request must be authenticated");
        }
        ProfileManager pm = im.getProfileManager();
        pm.removeTokensForProfile(profile);
    }
}
