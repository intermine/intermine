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
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.webservice.server.core.ReadWriteJSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * A service that issues new tokens to a user.
 * @author Alex Kalderimis
 *
 */
public class TokenService extends ReadWriteJSONService
{

    /** @param im The InterMine state object **/
    public TokenService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        final ProfileManager pm = im.getProfileManager();
        Profile profile = getPermission().getProfile();
        String tokenType = getOptionalParameter("type", "day").toLowerCase();
        String message = getOptionalParameter("message");
        String token = getToken(pm, profile, tokenType, message);
        addResultValue(token, false);
    }

    private String getToken(
            final ProfileManager pm,
            final Profile profile,
            final String tokenType,
            final String message)
        throws ObjectStoreException {
        if ("day".equals(tokenType)) {
            return pm.generate24hrKey(profile);
        } else if ("once".equals(tokenType)) {
            return pm.generateSingleUseKey(profile);
        } else if ("api".equals(tokenType)) {
            return pm.generateApiKey(profile);
        } else if ("perm".equals(tokenType)) {
            if (profile.getUserId() == null) {
                throw new BadRequestException("Temporary users cannot have permanent tokens");
            }
            return pm.generateReadOnlyAccessToken(profile, message);
        } else {
            throw new BadRequestException("Unknown token type: " + tokenType);
        }
    }

    @Override
    protected String getResultsKey() {
        return "token";
    }
}
