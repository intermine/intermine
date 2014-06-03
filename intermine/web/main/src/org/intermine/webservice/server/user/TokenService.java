package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2014 FlyMine
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
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

public class TokenService extends ReadWriteJSONService {

    public TokenService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        final ProfileManager pm = im.getProfileManager();
        Profile profile = getPermission().getProfile();
        String tokenType = getOptionalParameter("type", "day").toLowerCase();
        String message = getOptionalParameter("message");
        String token = null;
        if ("day".equals(tokenType)) {
            token = pm.generate24hrKey(profile);
        } else if ("once".equals(tokenType)) {
            token = pm.generateSingleUseKey(profile);
        } else if ("api".equals(tokenType)) {
            token = pm.generateApiKey(profile);
        } else if ("perm".equals(tokenType)) {
            if (profile.getUserId() == null) throw new BadRequestException("Temporary users cannot have permanent tokens");
            token = pm.generateReadOnlyAccessToken(profile, message);
        } else {
            throw new BadRequestException("Unknown token type: " + tokenType);
        }
        addResultValue(token, false);
    }

    @Override
    protected String getResultsKey() {
        return "token";
    }
}
