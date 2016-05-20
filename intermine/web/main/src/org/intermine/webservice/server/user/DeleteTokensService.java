package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.model.userprofile.PermanentToken;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.ServiceException;

/**
 * Service which ensures that the authenticating user has no tokens.
 * When finished executing, the profile will have no permanent tokens.
 * @author Alex Kalderimis
 */
public class DeleteTokensService extends ReadWriteJSONService
{

    /**
     * @param im The InterMine state object.
     */
    public DeleteTokensService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        if (profile.getUserId() == null) {
            return; // Temporary user. Nothing to delete.
        }
        ProfileManager pm = im.getProfileManager();
        UserProfile up = (UserProfile) pm.getProfileObjectStoreWriter()
                                         .getObjectById(profile.getUserId());
        if (up == null) {
            throw new ServiceException("Could not load user profile");
        }
        for (PermanentToken t: up.getPermanentTokens()) {
            pm.removePermanentToken(t);
        }
    }

}
