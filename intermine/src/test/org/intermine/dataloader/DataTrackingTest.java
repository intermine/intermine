package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.datatracking.Source;
import org.intermine.model.datatracking.Field;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.sql.DatabaseFactory;

import org.intermine.model.testmodel.Department;

import junit.framework.TestCase;

public class DataTrackingTest extends TestCase {
    protected DataTracker dt;
    protected Source source1, source2;
    
    public void setUp() throws Exception {
        dt = new DataTracker(DatabaseFactory.getDatabase("db.unittest"), 30, 10);
        source1 = dt.stringToSource("Source1");
        source2 = dt.stringToSource("Source2");
    }

    public void tearDown() throws Exception {
        dt.close();
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
