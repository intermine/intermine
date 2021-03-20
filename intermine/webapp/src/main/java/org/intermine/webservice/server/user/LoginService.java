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
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
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
            issues = mergeProfiles(currentProfile, profile);
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

    /**
     * Merge two profiles together. This is mainly of use when a new user login
     *  and we need to save their current anonymous session into their new profile.
     * @param fromProfile The profile to take information from.
     * @param toProfile The profile to merge into.
     * @return A map of bags, from old name to new name.
     */
    public static ProfileMergeIssues mergeProfiles(Profile fromProfile, Profile toProfile) {
        Map<String, SavedQuery> mergeQueries = Collections.emptyMap();
        Map<String, InterMineBag> mergeBags = Collections.emptyMap();
        ProfileMergeIssues issues = new ProfileMergeIssues();
        if (!fromProfile.getPreferences().isEmpty()) {
            toProfile.getPreferences().putAll(fromProfile.getPreferences());
        }
        if (fromProfile != null) {
            mergeQueries = fromProfile.getHistory();
            mergeBags = fromProfile.getSavedBags();
        }

        // Merge in anonymous query history
        for (SavedQuery savedQuery : mergeQueries.values()) {
            toProfile.saveHistory(savedQuery);
        }

        // Merge anonymous bags
        for (Map.Entry<String, InterMineBag> entry : mergeBags.entrySet()) {
            InterMineBag bag = entry.getValue();
            // Make sure the userId gets set to be the profile one
            try {
                bag.setProfileId(toProfile.getUserId());
                String name = NameUtil.validateName(toProfile.getSavedBags().keySet(),
                        entry.getKey());
                if (!entry.getKey().equals(name)) {
                    issues.addRenamedBag(entry.getKey(), name);
                }
                bag.setName(name);
                toProfile.saveBag(name, bag);
            } catch (ObjectStoreException iex) {
                throw new RuntimeException(iex.getMessage());
            }
        }
        return issues;
    }
}
