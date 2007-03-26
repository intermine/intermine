package org.intermine.web.account;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.InterMineException;
import org.intermine.web.Constants;
import org.intermine.web.InterMineAction;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.SavedQuery;
import org.intermine.web.SessionMethods;
import org.intermine.web.WebUtil;
import org.intermine.web.bag.InterMineBag;

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
     * @param servletContext
     *            The servlet context
     * @param request
     *            The HttpServletRequest
     * @param response
     *            The HttpServletResponse
     * @param session
     *            The session
     * @param pm
     *            The profile manager
     * @param username
     *            The username
     * @param password
     *            The password
     */
    public void doLogin(ServletContext servletContext, HttpServletRequest request,
            HttpServletResponse response, HttpSession session, ProfileManager pm, String username,
            String password) {
        // Merge current history into loaded profile
        Profile currentProfile = (Profile) session.getAttribute(Constants.PROFILE);
        String superuser = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        Map mergeQueries = Collections.EMPTY_MAP;
        Map mergeBags = Collections.EMPTY_MAP;
        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            mergeQueries = new HashMap(currentProfile.getHistory());
            mergeBags = new HashMap(currentProfile.getSavedBags());
        }

        Profile profile;
        if (pm.hasProfile(username)) {
            profile = pm.getProfile(username, password);
        } else {
            profile = new Profile(pm, username, null, password, new HashMap(), new HashMap(),
                    new HashMap());
            pm.saveProfile(profile);
        }
        session.setAttribute(Constants.PROFILE, profile);

        if (profile.getUsername().equals(superuser)) {
            session.setAttribute(Constants.IS_SUPERUSER, Boolean.TRUE);
        }

        // Merge in anonymous query history
        for (Iterator iter = mergeQueries.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            SavedQuery query = (SavedQuery) entry.getValue();
            String name = makeUniqueQueryName((String) entry.getKey(), profile.getHistory()
                    .keySet());
            profile.saveHistory(query);
        }
        // Merge anonymous bags
        for (Iterator iter = mergeBags.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            InterMineBag bag = (InterMineBag) entry.getValue();
            // Make sure the userId gets set to be the profile one
            bag.setUserId(profile.getUserId());
            String name = makeUniqueQueryName((String) entry.getKey(), profile.getSavedBags()
                    .keySet());
            try {
                int maxNotLoggedSize = WebUtil.getIntSessionProperty(session,
                                              "max.bag.size.notloggedin",
                                              Constants.MAX_NOT_LOGGED_BAG_SIZE);
                profile.saveBag(name, bag, maxNotLoggedSize);
            } catch (InterMineException iex) {
                throw new RuntimeException(iex.getMessage());
            }
        }

        SessionMethods.setLoggedInCookie(session, response);
    }

    /**
     * @param name
     *            name
     * @param names
     *            names
     * @return
     */
    private String makeUniqueQueryName(String name, Set names) {
        String original = name;
        int i = 1;
        while (names.contains(name)) {
            name = original + "_" + i;
        }
        return name;
    }

}
