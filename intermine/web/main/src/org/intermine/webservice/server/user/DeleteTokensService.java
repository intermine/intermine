package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2013 FlyMine
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

public class DeleteTokensService extends ReadWriteJSONService {

	public DeleteTokensService(InterMineAPI im) {
		super(im);
	}

	@Override
	protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        ProfileManager pm = im.getProfileManager();
        UserProfile up = (UserProfile) pm.getProfileObjectStoreWriter().getObjectById(profile.getUserId());
        if (up == null) {
        	throw new ServiceException("Could not load user profile");
        }
        for (PermanentToken t: up.getPermanentTokens()) {
        	pm.removePermanentToken(t);
        }
	}

}
