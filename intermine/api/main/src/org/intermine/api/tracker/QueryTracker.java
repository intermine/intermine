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
import java.sql.Timestamp;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.intermine.api.profile.Profile;
import org.intermine.api.tracker.track.QueryTrack;
import org.intermine.api.tracker.track.Track;
import org.intermine.api.tracker.util.TrackerUtil;

public class QueryTracker extends TrackerAbstract
{
    private static final Logger LOG = Logger.getLogger(QueryTracker.class);
    private static QueryTracker queryTracker = null;

    /**
    * Build a query tracker
    * @param conn connection to the database
    * @param trackQueue the queue where the tracks are temporary stored
    */
    protected QueryTracker(Connection conn, Queue<Track> trackQueue) {
        super(trackQueue, TrackerUtil.QUERY_TRACKER_TABLE);
        LOG.info("Creating new " + getClass().getName() + " tracker");
    }

    /**
     * Return an instance of the QueryTracker
     * @param con connection to the database
     * @param trackQueue the queue where the tracks are temporary stored
     * @return QueryTracker the query tracker
     */
    public static QueryTracker getInstance(Connection con, Queue<Track> trackQueue) {
        if (queryTracker == null) {
            queryTracker = new QueryTracker(con, trackQueue);
            try {
                queryTracker.createTrackerTable(con);
            } catch (Exception e) {
                LOG.error("Error creating the table associated to the QueryTracker" + e);
            }
        }
        return queryTracker;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return TrackerUtil.QUERY_TRACKER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatementCreatingTable() {
        return "CREATE TABLE " + trackTableName + "(type text, username text, sessionidentifier text, "
               + "timestamp timestamp)";
    }

    protected void trackQuery(String type, Profile profile, String sessionIdentifier) {
        String userName = (profile.getUsername() != null)
                          ? profile.getUsername()
                          : "";
        QueryTrack queryTrack = new QueryTrack(type, userName, sessionIdentifier,
                                              new Timestamp(System.currentTimeMillis()));
        if (queryTracker  != null) {
            queryTracker.storeTrack(queryTrack);
        } else {
            LOG.warn("Query execution not tracked. Check if the QueryTracker has been configured");
        }
    }
}
