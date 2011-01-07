package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;

/**
 * @author Xavier Watkins
 *
 */
public abstract class LoginHandler extends InterMineAction
{
    /**
     * Abstract class containing the methods for login in and copying current
     * history, bags,... into profile
     *
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param session The session
     * @param pm The profile manager
     * @param username The username
     * @param password The password
     * @return the map containing the renamed bags the user created before they were logged in
     */
    public Map<String, String> doLogin(HttpServletRequest request, HttpServletResponse response,
            HttpSession session, ProfileManager pm, String username,
            String password) {
        // Merge current history into loaded profile
        Profile currentProfile = SessionMethods.getProfile(request.getSession());
        Map<String, SavedQuery> mergeQueries = Collections.emptyMap();
        Map<String, InterMineBag> mergeBags = Collections.emptyMap();
        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            mergeQueries = currentProfile.getHistory();
            mergeBags = currentProfile.getSavedBags();
        }

        Profile profile = setUpProfile(session, pm, username, password);

        // Merge in anonymous query history
        for (SavedQuery savedQuery : mergeQueries.values()) {
            profile.saveHistory(savedQuery);
        }
        // Merge anonymous bags
        Map<String, String> renamedBags = new HashMap<String, String>();
        for (Map.Entry<String, InterMineBag> entry : mergeBags.entrySet()) {
            InterMineBag bag = entry.getValue();
            // Make sure the userId gets set to be the profile one
            try {
                bag.setProfileId(profile.getUserId());
                String name = NameUtil.validateName(profile.getSavedBags().keySet(),
                        entry.getKey());
                if (!entry.getKey().equals(name)) {
                    renamedBags.put(entry.getKey(), name);
                }
                bag.setName(name);
                profile.saveBag(name, bag);
            } catch (ObjectStoreException iex) {
                throw new RuntimeException(iex.getMessage());
            }
        }
        return renamedBags;
    }

    /**
     * Retrieves profile (creates or gets from ProfileManager) and saves it to session.
     *
     * @param session http session
     * @param pm profile manager
     * @param username user name
     * @param password password
     * @return profile
     */
    public static Profile setUpProfile(HttpSession session, ProfileManager pm,
            String username, String password) {
        Profile profile;
        if (pm.hasProfile(username)) {
            profile = pm.getProfile(username, password);
        } else {
            profile = new Profile(pm, username, null, password, new HashMap(), new HashMap(),
                    new HashMap());
            pm.saveProfile(profile);
        }
        SessionMethods.setProfile(session, profile);

        if (profile.getUsername().equals(pm.getSuperuser())) {
            session.setAttribute(Constants.IS_SUPERUSER, Boolean.TRUE);
        }
        return profile;
    }
}
