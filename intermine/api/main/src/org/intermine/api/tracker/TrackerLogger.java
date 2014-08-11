package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.intermine.api.tracker.track.Track;

/**
 * Runnable object providing insertion into the database. TrackerLogger is created
 * for a specific connection and table.
 * @author dbutano
 *
 */
public class TrackerLogger implements Runnable
{
    private static final Logger LOG = Logger.getLogger(TrackerLogger.class);
    private Connection connection;
    private BlockingQueue<Track> trackQueue;

    /**
     * Construct a TrackerLogger for a specific connection and table
     * @param connection the connection to the database
     * @param trackQueue track queue
     */
    public TrackerLogger(Connection connection, BlockingQueue<Track> trackQueue) {
        this.connection = connection;
        this.trackQueue = trackQueue;
        if (connection == null || trackQueue == null) {
            throw new IllegalArgumentException("neither connection or track queue may be null");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        for (;;) {
            if (Thread.interrupted()) {
                return;
            }
            try {
                Track track = trackQueue.poll(1, TimeUnit.SECONDS);
                if (track != null) {
                    track.store(connection);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
