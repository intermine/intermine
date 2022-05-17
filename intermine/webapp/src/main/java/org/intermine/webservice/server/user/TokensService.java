package org.intermine.webservice.server.user;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.userprofile.PermanentToken;
import org.intermine.api.userprofile.UserProfile;
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
            String type = getOptionalParameter("type");
            if (type == null || "perm".equals(type)) {
                for (PermanentToken t: up.getPermanentTokens()) {
                    tokens.add(PermaTokens.format(t));
                }
            } else if ("api".equals(type)) {
                if (up.getApiKey() == null) {
                    // generate key if it's not there
                    im.getProfileManager().generateApiKey(profile);
                }
                String apiKey = up.getApiKey();
                if (apiKey != null) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("token", apiKey);
                    tokens.add(map);
                }
            }
        }
        addResultItem(tokens, false);
    }

    @Override
    protected String getResultsKey() {
        return "tokens";
    }
}
