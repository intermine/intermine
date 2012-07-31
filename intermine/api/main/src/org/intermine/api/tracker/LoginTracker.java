package org.intermine.api.tracker;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.intermine.api.tracker.track.LoginTrack;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;

/**
 * Class for tracking when the users log into their accounts.
 * @author dbutano
 */
public class LoginTracker extends TrackerAbstract
{
    private static final Logger LOG = Logger.getLogger(LoginTracker.class);
    private static LoginTracker loginTracker = null;

    /**
    * Build a login tracker
    * @param conn connection to the database
    * @param trackQueue the queue where the tracks are temporary stored
    */
    protected LoginTracker(Connection conn, Queue<Track> trackQueue) {
        super(trackQueue, TrackerUtil.LOGIN_TRACKER_TABLE);
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }

    /**
     * Return an instance of the LoginTracker
     * @param con connection to the database
     * @param trackQueue the queue where the tracks are temporary stored
     * @return LoginTracker the login tracker
     */
    public static LoginTracker getInstance(Connection con, Queue<Track> trackQueue) {
        if (loginTracker == null) {
            loginTracker = new LoginTracker(con, trackQueue);
            try {
                loginTracker.createTrackerTable(con);
            } catch (Exception e) {
                LOG.error("Error creating the table associated to the ListTracker" + e);
            }
        } else {
            loginTracker.setTrackQueue(trackQueue);
        }
        return loginTracker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return TrackerUtil.LOGIN_TRACKER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName + "(username text, timestamp timestamp)";
    }

    /**
     * Track when the user logs in his account
     * @param username the user name
     */
    protected void trackLogin(String username) {
        LoginTrack loginTrack = new LoginTrack(username, new Timestamp(System.currentTimeMillis()));
        if (loginTracker  != null) {
            loginTracker.storeTrack(loginTrack);
        } else {
            LOG.warn("Logine not tracked. Check if the LoginTracker has been configured");
        }
    }

    /**
     * Return the number of access for each user
     * @param con the connection
     * @return map with key the user name and access number
     */
    protected Map<String, Integer> getUserLogin(Connection con) {
        ResultSet rs = null;
        Statement stm = null;
        Map<String, Integer> userLogin = new HashMap<String, Integer>();
        try {
            stm = con.createStatement();
            String sql = "SELECT username, COUNT(username) as accessnumbers "
                        + "FROM logintrack "
                        + "GROUP BY username";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                userLogin.put(rs.getString(1), rs.getInt(2));
            }
            return userLogin;
        } catch (SQLException sqle) {
            LOG.error("Error in getUserLogin method: ", sqle);
        } finally {
            releaseResources(rs, stm);
        }
        return null;
    }
}
