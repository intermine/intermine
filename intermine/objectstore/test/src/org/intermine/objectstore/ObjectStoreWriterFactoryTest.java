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

public class ObjectStoreWriterFactoryTest extends TestCase
{
    public ObjectStoreWriterFactoryTest(String arg1) {
        super(arg1);
    }

    public void testValid() throws Exception {
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        assertNotNull(osw);
        osw.close();
    }

    public void testNull() throws Exception {
        try {
            ObjectStoreWriterFactory.getObjectStoreWriter(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }
    
    public void testEmpty() throws Exception {
        try {
            ObjectStoreWriterFactory.getObjectStoreWriter("");
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testInvalid() throws Exception {
        try {
            ObjectStoreWriterFactory.getObjectStoreWriter("db.unittest");
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }
}
