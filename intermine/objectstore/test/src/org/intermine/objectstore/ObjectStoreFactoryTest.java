package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class ObjectStoreFactoryTest extends TestCase
{
    public ObjectStoreFactoryTest(String arg1) {
        super(arg1);
    }

    public void testValid() throws Exception {
        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        assertNotNull(os);
    }

    public void testConfigure() throws Exception {
        ObjectStore os1 = ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStore os2 = ObjectStoreFactory.getObjectStore("os.unittest");
        // These should be exactly the same object
        assertTrue(os1 == os2);
    }

    public void testNull() throws Exception {
        try {
            ObjectStoreFactory.getObjectStore(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testEmpty() throws Exception {
        try {
            ObjectStoreFactory.getObjectStore("");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testInvalid() throws Exception {
        try {
            ObjectStoreFactory.getObjectStore("db.unittest");
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}
