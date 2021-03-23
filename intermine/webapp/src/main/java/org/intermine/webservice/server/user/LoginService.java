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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.web.logic.profile.LoginHandler;
import org.intermine.web.logic.profile.ProfileMergeIssues;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.UnauthorizedException;
import org.json.JSONObject;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Login service which authenticates an existing user, given username and password
 * and attaches the anonymously created lists to it.
 * It requires token authentication
 * @author Daniela Butano
 *
 */
public class LoginService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(LoginService.class);

    /**
     * Constructor
     * @param im A reference to the InterMine API settings bundle
     */
    public LoginService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getResultsKey() {
        return "output";
    }

    @Override
    protected void execute() throws Exception {
        String username = getRequiredParameter("username");
        String password = getRequiredParameter("password");

        Profile currentProfile = getPermission().getProfile();
        Profile profile = null;
        try {
            profile = getUser(username, password);
        } catch (ProfileManager.AuthenticationException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }

        Map<String, Object> output = new HashMap<String, Object>();
        JSONUserFormatter formatter = new JSONUserFormatter(profile);
        output.put("user", new JSONObject(formatter.format()));
        output.put("token", im.getProfileManager().generate24hrKey(profile));

        //merge anonymous profile with the logged profile
        ProfileMergeIssues issues = null;
        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            // The current profile was for an anonymous guest.
            issues = LoginHandler.mergeProfiles(currentProfile, profile);
            output.put("renamedLists", new JSONObject(issues.getRenamedBags()));
        } else {
            output.put("renamedLists", new JSONObject(Collections.emptyMap()));
        }

        addResultItem(output, false);
    }

    private Profile getUser(String username, String password) {
        ProfileManager pm = im.getProfileManager();
        if (pm.hasProfile(username)) {
            if (!pm.validPassword(username, password)) {
                throw new ProfileManager.AuthenticationException("Invalid password supplied");
            } else {
                Profile p = pm.getProfile(username, im.getClassKeys());
                return p;
            }
        } else {
            throw new ProfileManager.AuthenticationException("Unknown username: " + username);
        }
    }
}
