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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.model.userprofile.PermanentToken;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.webservice.server.core.ReadWriteJSONService;

/** Service that lets a user inspect their currently active tokens
 * @author Alex Kalderimis
 **/
public class TokensService extends ReadWriteJSONService
{

    /** @param im The InterMine state object **/
    public TokensService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute() throws Exception {
        Profile profile = getPermission().getProfile();
        List<Map<String, Object>> tokens = new ArrayList<Map<String, Object>>();

        if (profile.getUserId() != null) { // ie. is really in the DB.
            UserProfile up = (UserProfile) im.getProfileManager()
                                             .getProfileObjectStoreWriter()
                                             .getObjectById(profile.getUserId());

            for (PermanentToken t: up.getPermanentTokens()) {
                tokens.add(PermaTokens.format(t));
            }
        }
        addResultItem(tokens, false);
    }

    @Override
    protected String getResultsKey() {
        return "tokens";
    }
}
