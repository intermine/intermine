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
import java.util.Queue;

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
    private Queue<Track> trackQueue;

    /**
     * Construct a TrackerLogger for a specific connection and table
     * @param connection the connection to the database
     */
    public TrackerLogger(Connection connection, Queue<Track> trackQueue) {
        this.connection = connection;
        this.trackQueue = trackQueue;
    }

    public void run() {
        for (;;) {
            Track track = trackQueue.poll();
            if (track != null) {
                track.store(connection);
            } else {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }
}
