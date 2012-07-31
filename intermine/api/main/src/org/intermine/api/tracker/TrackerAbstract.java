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
import java.util.Queue;

import org.apache.log4j.Logger;
import org.intermine.api.tracker.track.Track;
import org.intermine.sql.DatabaseUtil;

/**
 * Abstract interface for creating trackers objects.
 * @author dbutano
 *
 */
public abstract class TrackerAbstract implements Tracker
{
    private static final Logger LOG = Logger.getLogger(TrackerAbstract.class);
    protected Queue<Track> trackQueue = null;
    protected String trackTableName;
    protected TrackerLogger trackerLogger = null;

    /**
     * Construct a Tracker setting the tracks queue and the table name
     * @param trackQueue the queue where the tracks are temporary stored
     * @param trackTableName the table where store the tracks
     */
    protected TrackerAbstract(Queue<Track> trackQueue, String trackTableName) {
        this.trackQueue = trackQueue;
        this.trackTableName = trackTableName;
    }

    /**
     * Return the table's name associated to the tracker
     * @return String table's name
     */
    public String getTrackTableName() {
        return trackTableName;
    }

    /**
     * Create the table where the tracker saves data
     * @throws Exception when a database error access is verified
     * @param connection the userprofile connection
     */
    public void createTrackerTable(Connection connection) throws Exception {
        try {
            if (trackTableName != null && !"".equals(trackTableName)) {
                if (!DatabaseUtil.tableExists(connection, trackTableName)) {
                    connection.createStatement().execute(getStatementCreatingTable());
                }
            } else {
                LOG.warn("trackTableName is null or empty");
            }
        } catch (SQLException e) {
            trackerLogger = null;
            throw e;
        }
    }

    /**
     * Save into the table the track object representing the user activity
     * @param track the object saved into the database
     */
    public void storeTrack(Track track) {
        if (trackTableName != null) {
            if (track.validate()) {
                try {
                    trackQueue.add(track);
                } catch (IllegalStateException ise) {
                    LOG.error("Problem adding the track to the queue", ise);
                }
            } else {
                LOG.error("Failed to write to track table: input non valid");
            }
        } else {
            LOG.error("The trackTableName is null, set it");
        }
    }

    /**
     * Return the tracker's name
     * @return String tracker's name
     */
    public abstract String getName();

    /**
     * Set the queue of tracks
     * @param trackQueue the queue to set
     */
    public void setTrackQueue(Queue<Track> trackQueue) {
        this.trackQueue = trackQueue;
    }

    /**
     * Generate the sql statement to create the table used by the tracker
     * @return String sql statement
     */
    public abstract String getStatementCreatingTable();

    /**
     * Close the result set and statement objects specified in input
     * @param rs the ResultSet object to close
     * @param stm the STatement object to close
     */
    protected void releaseResources(ResultSet rs, Statement stm) {
        if (rs != null) {
            try {
                rs.close();
                if (stm != null) {
                    stm.close();
                }
            } catch (SQLException e) {
                LOG.error("Problem closing  resources.", e);
            }
        }
    }
}
