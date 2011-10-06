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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.DatabaseUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;

/**
 * @author Xavier Watkins
 *
 */
public abstract class LoginHandler extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(LoginHandler.class);
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
    public Map<String, String> doLogin(HttpServletRequest request, String username, String password) {
        Map<String, String> renamedBags = doStaticLogin(request, username, password);
        HttpSession session = request.getSession();
        ProfileManager pm = SessionMethods.getInterMineAPI(session).getProfileManager();
        Profile profile = pm.getProfile(username);
        InterMineAPI im = SessionMethods.getInterMineAPI(session);
        if (im.getBagManager().isAnyBagNotCurrent(profile)) {
            recordError(new ActionMessage("login.upgradeListStarted"), request);
        } else if (im.getBagManager().isAnyBagToUpgrade(profile)) {
            recordError(new ActionMessage("login.upgradeListManually"), request);
        }
        return renamedBags;
    }

    public static Map<String, String> mergeProfiles(Profile fromProfile, Profile toProfile) {
        Map<String, SavedQuery> mergeQueries = Collections.emptyMap();
        Map<String, InterMineBag> mergeBags = Collections.emptyMap();
        if (fromProfile != null) {
            mergeQueries = fromProfile.getHistory();
            mergeBags = fromProfile.getSavedBags();
        }

        // Merge in anonymous query history
        for (SavedQuery savedQuery : mergeQueries.values()) {
            toProfile.saveHistory(savedQuery);
        }
        // Merge anonymous bags
        Map<String, String> renamedBags = new HashMap<String, String>();
        for (Map.Entry<String, InterMineBag> entry : mergeBags.entrySet()) {
            InterMineBag bag = entry.getValue();
            // Make sure the userId gets set to be the profile one
            try {
                bag.setProfileId(toProfile.getUserId());
                String name = NameUtil.validateName(toProfile.getSavedBags().keySet(),
                        entry.getKey());
                if (!entry.getKey().equals(name)) {
                    renamedBags.put(entry.getKey(), name);
                }
                bag.setName(name);
                toProfile.saveBag(name, bag);
            } catch (ObjectStoreException iex) {
                throw new RuntimeException(iex.getMessage());
            }
        }
        return renamedBags;
    }

    public static Map<String, String> doStaticLogin(HttpServletRequest request, String username, String password) {
        // Merge current history into loaded profile

        Profile currentProfile = SessionMethods.getProfile(request.getSession());
        HttpSession session = request.getSession();
        ProfileManager pm = SessionMethods.getInterMineAPI(session).getProfileManager();
        Profile profile = setUpProfile(session, username, password);
        Map<String, String> renamedBags = new HashMap<String, String>();

        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            // The current profile was for an anonymous guest.
            renamedBags = mergeProfiles(currentProfile, profile);
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
    public static Profile setUpProfile(HttpSession session, String username, String password) {
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile;
        ProfileManager pm = SessionMethods.getInterMineAPI(session).getProfileManager();
        if (pm.hasProfile(username)) {
            profile = pm.getProfile(username, password, im.getClassKeys());
        } else {
            throw new LoginException("There is no profile for " + username);
        }
        return setUpProfile(session, profile);
    }

    public static Profile setUpProfile(HttpSession session, Profile profile) {
        SessionMethods.setProfile(session, profile);
        ProfileManager pm = SessionMethods.getInterMineAPI(session).getProfileManager();
        if (profile.getUsername().equals(pm.getSuperuser())) {
            session.setAttribute(Constants.IS_SUPERUSER, Boolean.TRUE);
        }
        SessionMethods.setNotCurrentSavedBagsStatus(session, profile);
        InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Connection con = null;
        try {
            con = ((ObjectStoreWriterInterMineImpl) pm.getProfileObjectStoreWriter())
                                                             .getDatabase().getConnection();
            if (im.getBagManager().isAnyBagNotCurrent(profile)
                && !DatabaseUtil.isBagValuesEmpty(con)) {
                new Thread(new UpgradeBagList(profile, im.getBagQueryRunner(), session)).start();
            }
        } catch (SQLException sqle) {
            LOG.error("Problems retriving the connection", sqle);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException sqle) {
            }
        }
        return profile;
    }

    private static class LoginException extends RuntimeException {

    	/**
		 * Default serial id.
		 */
		private static final long serialVersionUID = 1L;

		private LoginException() {
    		super();
    	}

    	private LoginException(String message) {
    		super(message);
    	}
    }
}
