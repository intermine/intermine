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

public class DataTrackingTest extends TestCase {
    protected DataTracker dt;
    protected Source source1, source2;
    
    public void setUp() throws Exception {
        dt = new DataTracker(DatabaseFactory.getDatabase("db.unittest"), 30, 10);
        source1 = dt.stringToSource("Source1");
        source2 = dt.stringToSource("Source2");
    }

    public void tearDown() throws Exception {
        try {
            dt.close();
            Database db = DatabaseFactory.getDatabase("db.unittest");
            Connection c = db.getConnection();
            c.setAutoCommit(true);
            c.createStatement().execute("DROP TABLE tracker");
            c.close();
        } catch (Exception e) {
        }
    }

    public void testSetSourceNullIds() throws Exception {
        try {
            dt.setSource(null, "name", source1);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
        
        try {
            dt.setSource(new Integer(46), "name", new Source());
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            dt.setSource(new Integer(46), null, source1);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetSource() throws Exception {
        dt.setSource(new Integer(13), "name", source1);
        assertEquals(source1.getName(), dt.getSource(new Integer(13), "name").getName());

        for (int i = 100; i < 200; i++) {
            dt.setSource(new Integer(i), "name", source2);
        }

        assertEquals(source1.getName(), dt.getSource(new Integer(13), "name").getName());
    }
}
