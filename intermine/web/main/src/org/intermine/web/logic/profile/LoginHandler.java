package org.intermine.web.logic.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.ProfileManager.ApiPermission;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.util.NameUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.DatabaseUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.InterMineAction;

/**
 * @author Xavier Watkins
 *
 *
 * Abstract class containing the methods for login in and copying current
 * history, bags,... into profile.
 *
 */
public abstract class LoginHandler extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(LoginHandler.class);

    /**
     * Log-in a user.
     * @param request The HttpServletRequest
     * @param username The username
     * @param password The password
     * @return the map containing the renamed bags the user created before they were logged in
     */
    public Map<String, String> doLogin(HttpServletRequest request,
            String username, String password) {
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

    /**
     * Merge two profiles together. This is mainly of use when a new user registers and we need
     * to save their current anonymous session into their new profile.
     * @param fromProfile The profile to take information from.
     * @param toProfile The profile to merge into.
     * @return A map of bags, from old name to new name.
     */
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

    /**
     * Main log-in logic.
     * @param request The current request.
     * @param username The current user's login name.
     * @param password The current user's password.
     * @return A map of renamed-bags from old to new name.
     */
    public static Map<String, String> doStaticLogin(HttpServletRequest request,
            String username, String password) {
        // Merge current history into loaded profile

        Profile currentProfile = SessionMethods.getProfile(request.getSession());
        HttpSession session = request.getSession();
        Profile profile = setUpProfile(session, username, password);
        Map<String, String> renamedBags = new HashMap<String, String>();

        if (currentProfile != null && StringUtils.isEmpty(currentProfile.getUsername())) {
            // The current profile was for an anonymous guest.
            renamedBags = mergeProfiles(currentProfile, profile);
        }

        return renamedBags;
    }

    /**
     * Initialises a profile for the current user based on their user name and password.
     *
     * @param session HTTP session
     * @param username user name
     * @param password password
     * @return profile, fully ready to use.
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

    /**
     * Does whatever needs to be done to a permissions object to get it ready
     * for a life cyle in a web service request. At the moment, this just means
     * determining if this is the super user, and running the bag upgrade
     * thread.
     *
     * @param api
     *            The InterMine API object.
     * @param permission
     *            The permission that needs setting up.
     */
    public static void setUpPermission(InterMineAPI api,
            ApiPermission permission) {
        final ProfileManager pm = api.getProfileManager();
        final Profile profile = permission.getProfile();
        final String userName = profile.getUsername();
        if (userName != null && userName.equals(pm.getSuperuser())) {
            permission.addRole("SUPERUSER");
        }
        Runnable upgrade = new SessionlessBagUpgrade(profile,
                api.getBagQueryRunner());
        runBagUpgrade(upgrade, api, profile);

    }

    private static void runBagUpgrade(Runnable procedure, InterMineAPI api,
            Profile profile) {
        Connection con = null;
        try {
            con = ((ObjectStoreWriterInterMineImpl) api.getProfileManager()
                    .getProfileObjectStoreWriter()).getDatabase()
                    .getConnection();
            if (api.getBagManager().isAnyBagNotCurrent(profile)
                    && !DatabaseUtil.isBagValuesEmpty(con)) {
                Thread upgrade = new Thread(procedure);
                upgrade.setDaemon(true);
                upgrade.start();
            }
        } catch (SQLException sqle) {
            LOG.error("Problems retrieving the connection", sqle);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException sqle) {
            }
        }
    }
    /**
     * Sets up a profile ready for a session in InterMine.
     *
     * @param session http session
     * @param profile the user's profile (possibly anonymous and temporary)
     * @return profile The profile all cleaned up and good to go.
     */
    public static Profile setUpProfile(HttpSession session, Profile profile) {
        SessionMethods.setProfile(session, profile);
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        if (profile.isSuperuser()) {
            session.setAttribute(Constants.IS_SUPERUSER, Boolean.TRUE);
        }
        SessionMethods.setNotCurrentSavedBagsStatus(session, profile);
        Runnable upgrade = new UpgradeBagList(profile, im.getBagQueryRunner(),
                session);
        runBagUpgrade(upgrade, im, profile);
        return profile;
    }

    private static final class LoginException extends RuntimeException
    {

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
