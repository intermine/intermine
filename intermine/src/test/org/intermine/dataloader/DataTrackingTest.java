package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.model.datatracking.Source;
import org.flymine.model.datatracking.Field;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreWriterFactory;

import org.flymine.model.testmodel.Department;

import junit.framework.TestCase;

public class DataTrackingTest extends TestCase {
    protected ObjectStoreWriter osw;
    protected Source source1, source2;
    
    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.datatrackingtest");
        source1 = new Source();
        source1.setName("Source1");
        osw.store(source1);
        source2 = new Source();
        source2.setName("Source2");
        osw.store(source2);
    }

    public void tearDown() throws Exception {
        osw.delete(source1);
        osw.delete(source2);
        osw.close();
    }

    public void testSetSourceNullIds() throws Exception {
        try {
            DataTracking.setSource(new Department(), "", source1, osw);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        
        Department dept = new Department();
        dept.setId(new Integer(42));
        
        try {
            DataTracking.setSource(dept, "", new Source(), osw);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetSource() throws Exception {
        Field field = new Field();
        field.setSource(source1);
        field.setName("name");
        field.setObjectId(new Integer(13));
        osw.store(field);
        Department dept = new Department();
        dept.setId(new Integer(13));
        try {
            assertEquals(source1.getName(), DataTracking.getSource(dept, "name", osw).getName());
        } finally {
            osw.delete(field);
        }
    }

    public void testSetSource() throws Exception {
        Department dept = new Department();
        dept.setId(new Integer(42));
        DataTracking.clearObj(dept, osw);
        DataTracking.setSource(dept, "name", source1, osw);
        assertEquals(source1.getName(), DataTracking.getSource(dept, "name", osw).getName());
        DataTracking.setSource(dept, "name", source2, osw);
        assertEquals(source2.getName(), DataTracking.getSource(dept, "name", osw).getName());
    }
}
