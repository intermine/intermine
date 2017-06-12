package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.api.profile.Profile;
import org.intermine.api.tracker.track.KeySearchTrack;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;

/**
 *
 * @author Daniela
 *
 */
public class KeySearchTracker extends AbstractTracker
{
    private static final Logger LOG = Logger.getLogger(KeySearchTracker.class);
    private static KeySearchTracker searchTracker = null;

    /**
        * Build a search tracker
        * @param conn connection to the database
        * @param trackQueue the queue where the tracks are temporary stored
        */
    protected KeySearchTracker(Connection conn, Queue<Track> trackQueue) {
        super(trackQueue, TrackerUtil.SEARCH_TRACKER_TABLE);
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }

    /**
     * Return an instance of the KeySearchTracker
     * @param con connection to the database
     * @param trackQueue the queue where the tracks are temporary stored
     * @return KeySearchTracker the searchy tracker
     */
    public static KeySearchTracker getInstance(Connection con, Queue<Track> trackQueue) {
        if (searchTracker == null) {
            searchTracker = new KeySearchTracker(con, trackQueue);
            try {
                searchTracker.createTrackerTable(con);
            } catch (Exception e) {
                LOG.error("Error creating the table associated to the ListTracker" + e);
            }
        } else {
            searchTracker.setTrackQueue(trackQueue);
        }
        return searchTracker;
    }

    @Override
    public String getName() {
        return TrackerUtil.SEARCH_TRACKER;
    }

    @Override
    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName + " ("
                + "keyword text,"
                + "username text,"
                + "sessionidentifier text,"
                + "timestamp timestamp)";
    }

    /**
     * Record the search.
     * @param keyword keyword
     * @param profile user
     * @param sessionIdentifier session
     */
    protected void trackSearch(String keyword, Profile profile, String sessionIdentifier) {
        String userName = (profile.getUsername() != null)
                          ? profile.getUsername()
                          : "";
        KeySearchTrack searchTrack = new KeySearchTrack(keyword, userName, sessionIdentifier,
                                                       new Timestamp(System.currentTimeMillis()));
        if (searchTracker  != null) {
            searchTracker.storeTrack(searchTrack);
        } else {
            LOG.warn("Keyword search not tracked. Check if the KeySearchTrack has been configured");
        }
    }

    /**
     * Return the number of search for each keyword
     * @param con the connection
     * @return map with key the keyword and the number of searches for that keyword
     */
    protected Map<String, Integer> getKeywordSearches(Connection con) {
        ResultSet rs = null;
        Statement stm = null;
        Map<String, Integer> keywordSearches = new HashMap<String, Integer>();
        try {
            stm = con.createStatement();
            String sql = "SELECT keyword, COUNT(keyword) as searchnumbers "
                        + "FROM searchtrack "
                        + "GROUP BY keyword";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                keywordSearches.put(rs.getString(1), rs.getInt(2));
            }
            return keywordSearches;
        } catch (SQLException sqle) {
            LOG.error("Error in getKeywordSearches method: ", sqle);
        } finally {
            releaseResources(rs, stm);
        }
        return null;
    }

}
