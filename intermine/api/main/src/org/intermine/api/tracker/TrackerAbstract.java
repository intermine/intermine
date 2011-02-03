package org.intermine.api.tracker;

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
import org.apache.log4j.Logger;
import org.intermine.sql.DatabaseUtil;

/**
 * Abstract interface for creating trackers objects.
 * @author dbutano
 *
 */
public abstract class TrackerAbstract implements Tracker
{
    private static final Logger LOG = Logger.getLogger(TrackerAbstract.class);
    protected Connection connection = null;
    protected String trackTableName;
    protected String[] trackTableColumns;
    protected TrackerLogger trackerLogger = null;

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
     */
    public void createTrackerTable() throws Exception {
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
                Object[] values = getFormattedTrack(track);
                synchronized (values) {
                    trackerLogger = new TrackerLogger(connection, trackTableName,
                                                      trackTableColumns, values);
                    new Thread(trackerLogger).start();
                }
            } else {
                LOG.error("Failed to write to track table: input non valid");
            }
        }
    }

    /**
     * Generate the sql statement to create the table used by the tracker
     * @return String sql statement
     */
    public abstract String getStatementCreatingTable();

    /**
     * Format a track into an array of String to be saved in the database
     * @param track the track to format
     * @return Object[] an array of Objects
     */
    public abstract Object[] getFormattedTrack(Track track);

    /**
     * Release the database connection
     * @param conn the connection to release
     */
    public void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    Exception e = new Exception();
                    e.fillInStackTrace();
                    LOG.warn("releaseConnection called while in transaction - rolling back."
                              + System.getProperty("line.separator"), e);
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
                conn.close();
            } catch (SQLException e) {
                LOG.error("Could not release SQL connection " + conn, e);
            }
        }
    }

    /**
     * Override finalise method to flush the track table and release the connection
     * @throws Throwable
     */
    @Override
    protected synchronized void finalize() throws Throwable {
        super.finalize();
        LOG.warn("Garbage collecting Tracker " + getClass().getName());
        try {
            releaseConnection(connection);
        } catch (RuntimeException e) {
            LOG.error("Exception while garbage-collecting Tracker: " + e);
        }
    }
}
