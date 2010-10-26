package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.Shutdownable;

/**
 * Abstract interface for creating trackers objects.
 * @author dbutano
 *
 */
public abstract class TrackerAbstract implements Tracker, Shutdownable
{
    private static final Logger LOG = Logger.getLogger(TrackerAbstract.class);
    protected Connection connection = null;
    protected String trackTableName;
    protected String[] trackTableColumns;
    protected Batch trackerBatch = null;

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
                trackerBatch = new Batch(new BatchWriterPostgresCopyImpl());
            } else {
                LOG.warn("trackTableName is null or empty");
            }
        } catch (SQLException e) {
            trackerBatch = null;
            throw e;
        }
    }

    /**
     * Save into the table the track object representing the user activity
     * @param track the object saved into the database
     */
    public synchronized void storeTrack(Track track) {
        if (trackTableName != null) {
            if (track.validate()) {
                try {
                    trackerBatch.addRow(connection, trackTableName, null, trackTableColumns,
                                       getFormattedTrack(track));
                    //TODO I shuldn't need to call flush method...
                    flushTrackTable();
                } catch (SQLException e) {
                    LOG.error("Failed to write to track table: " + e);
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
     * Format a track into an array of Objects to be saved in the database
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
     * Flush the track table and release the database connection
     */
    public void close() {
        flushTrackTable();
        try {
            trackerBatch.close(connection);
        } catch (SQLException e) {
            LOG.error("Could not close tha Batch" + trackerBatch, e);
        }
        releaseConnection(connection);
    }

    /**
     * Allows the track table to be flushed, guaranteeing that all templates accesses
     * are committed to the database.
     */
    public synchronized void flushTrackTable() {
        if (trackTableName  != null) {
            try {
                trackerBatch.flush(connection);
            } catch (SQLException e) {
                LOG.warn("Failed to flush log entries to log table: " + e);
            }
        }
    }

    /**
     * Called by the ShutdownHook on shutdown.
     */
    public synchronized void shutdown() {
        LOG.info("Shutting down Tracker " + getClass().getName());
        try {
            close();
        } catch (RuntimeException e) {
            LOG.warn("Exception caught while shutting down Tracker: " + e);
        }
    }

    /**
     * Override finalise method to flush the track table and release the connection
     * @throws Throwable
     */
    @Override
    protected synchronized void finalize() throws Throwable {
        super.finalize();
        LOG.error("Garbage collecting Tracker " + getClass().getName());
        try {
            close();
        } catch (RuntimeException e) {
            LOG.error("Exception while garbage-collecting Tracker: " + e);
        }
    }
}
