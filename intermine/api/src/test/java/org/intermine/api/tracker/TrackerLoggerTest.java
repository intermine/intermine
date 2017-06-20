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
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.TestCase;

import org.intermine.api.tracker.track.LoginTrack;
import org.intermine.api.tracker.track.Track;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;

public class TrackerLoggerTest extends TestCase {
    ObjectStoreWriter uosw;
    Connection con;
    BlockingQueue<Track> trackQueue;
    TrackerLogger trackerLogger = null;
    private int count = 100;

    protected void setUp() throws Exception {
        super.setUp();
        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        if (uosw instanceof ObjectStoreWriterInterMineImpl) {
            con = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
        }
        trackQueue = new ArrayBlockingQueue<Track>(count);
        //create the table if doesn't exist
        LoginTracker.getInstance(con, trackQueue);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        removeTracks();
        ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(con);
        con.close();
        uosw.close();
    }

    public void testRun() throws SQLException, InterruptedException {
        for (int index = 0; index < count; index++) {
            trackQueue.add(new LoginTrack("user" + index,
                          new Timestamp(System.currentTimeMillis())));
        }
        trackerLogger = new TrackerLogger(con, trackQueue);
        new Thread(trackerLogger).start();
        synchronized (trackQueue) {
            while (!trackQueue.isEmpty()) {
                Thread.sleep(100);
            }
        }
        String sql = "SELECT COUNT(*) FROM logintrack";
        Statement stm = con.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        rs.next();
        assertEquals(count, rs.getInt(1));
        rs.close();
        stm.close();
    }

    private void removeTracks() throws SQLException {
        String sql = "DELETE FROM logintrack";
        Statement stm = con.createStatement();
        stm.executeUpdate(sql);
        stm.close();
    }
}
