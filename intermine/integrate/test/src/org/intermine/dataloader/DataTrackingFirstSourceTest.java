package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;

import junit.framework.TestCase;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

public class DataTrackingFirstSourceTest extends TestCase {
    protected DataTrackerFirstSource dt;
    protected Source source1, source2;

    public void setUp() throws Exception {
        dt = new DataTrackerFirstSource(DatabaseFactory.getDatabase("db.unittest"), 30, 10);
        source1 = dt.stringToSource("Source1");
    }

    public void tearDown() throws Exception {
        dt.close();
        try {
            Database db = DatabaseFactory.getDatabase("db.unittest");
            Connection c = db.getConnection();
            c.setAutoCommit(true);
            c.createStatement().execute("DROP TABLE tracker");
            c.close();
        } catch (Exception e) {
        }
    }


    public void testGetSource() throws Exception {
        try {
            dt.getSource(new Integer(13), "name").getName();
            fail("expected IllegalStateException - no skelSource set");
        } catch (IllegalStateException e) {
        }

        // correct
        Source skelSource1 = dt.stringToSource("skel_" + source1.getName());
        dt.setSkelSource(skelSource1);
        assertEquals(skelSource1, dt.getSource(new Integer(13), "name"));

        // source must be a skeleton
        try {
            dt.setSkelSource(source1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
