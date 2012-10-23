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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;
import org.intermine.api.tracker.track.ListTrack;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.ListBuildMode;
import org.intermine.api.tracker.util.ListTrackerEvent;
import org.intermine.api.tracker.util.TrackerUtil;

public class ListTracker extends TrackerAbstract
{
    private static final Logger LOG = Logger.getLogger(ListTracker.class);
    private static ListTracker listTracker = null;

    /**
     * Build a list tracker
     * @param conn connection to the database
     */
    protected ListTracker(Connection conn, Queue<Track> trackQueue) {
        super(trackQueue, TrackerUtil.LIST_TRACKER_TABLE);
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }
    /**
     * Return an instance of the ListTracker
     * @param con connection to the database
     * @return ListTracker the list tracker
     */
    public static ListTracker getInstance(Connection con, Queue<Track> trackQueue) {
        if (listTracker == null) {
            listTracker = new ListTracker(con, trackQueue);
            try {
                listTracker.createTrackerTable(con);
            } catch (Exception e) {
                LOG.error("Error creating the table associated to the ListTracker" + e);
            }
        } else {
            listTracker.setTrackQueue(trackQueue);
        }
        return listTracker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName + "(type text, count int, buildmode text,"
               + "event text, username text, sessionidentifier text, timestamp timestamp)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return TrackerUtil.LIST_TRACKER;
    }

    protected void trackList(String type, int count, ListBuildMode buildMode,
                             ListTrackerEvent event, Profile profile, String sessionIdentifier) {
        String userName = (profile.getUsername() != null)
                          ? profile.getUsername()
                          : "";
        ListTrack listTrack = new ListTrack(type, count, buildMode, event,
                                           userName, sessionIdentifier,
                                           new Timestamp(System.currentTimeMillis()));
        if (listTracker  != null) {
            listTracker.storeTrack(listTrack);
        } else {
            LOG.warn("List not tracked. Check if the ListTracker has been configured");
        }
    }

    /**
     * Return the operations done for each list (execution or creation)
     * @param con the connection
     * @return the list of ListTrack
     */
    protected List<ListTrack> getListOperations(Connection con) {
        ResultSet rs = null;
        Statement stm = null;
        List<ListTrack> listOperations = new ArrayList<ListTrack>();
        try {
            stm = con.createStatement();
            String sql = "SELECT type, count, buildmode, event FROM listtrack";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                listOperations.add(new ListTrack(rs.getString(1), rs.getInt(2),
                                                 getBuildMode(rs.getString(3)),
                                                 getListEvent(rs.getString(4))));
            }
            return listOperations;
        } catch (SQLException sqle) {
            LOG.error("Error in getKeywordSearches method: ", sqle);
        } finally {
            releaseResources(rs, stm);
        }
        return null;
    }

    private ListBuildMode getBuildMode(String buildMode) {
        if ("QUERY".equals(buildMode)) {
            return ListBuildMode.QUERY;
        }
        if ("IDENTIFIERS".equals(buildMode)) {
            return ListBuildMode.IDENTIFIERS;
        }
        if ("OPERATION".equals(buildMode)) {
            return ListBuildMode.OPERATION;
        }
        return null;
    }

    private ListTrackerEvent getListEvent(String listEvent) {
        if ("CREATION".equals(listEvent)) {
            return ListTrackerEvent.CREATION;
        }
        if ("EXECUTION".equals(listEvent)) {
            return ListTrackerEvent.EXECUTION;
        }
        return null;
    }
}
