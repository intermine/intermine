package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

public class MetadataManagerTest extends TestCase
{
    private Database db;

    @Override
    public void setUp() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
    }

    public void testValue() throws Exception {
        MetadataManager.store(db, "test_key", "Hello");
        assertEquals("Hello", MetadataManager.retrieve(db, "test_key"));
    }

    public void testBinaryValue() throws Exception {
        byte[] expected = "Hello".getBytes();
        MetadataManager.storeBinary(db, "test_key_bin", expected);
        InputStream is = MetadataManager.retrieveBLOBInputStream(db, "test_key_bin");
        byte[] got = new byte[expected.length + 1];
        assertEquals(expected.length, is.read(got));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], got[i]);
        }
    }

    public void testLargeValue() throws Exception {
        byte[] expected = "Hello".getBytes();
        OutputStream os = MetadataManager.storeLargeBinary(db, "test_key_large");
        os.write(expected);
        os.close();
        InputStream is = MetadataManager.readLargeBinary(db, "test_key_large");
        byte[] got = new byte[expected.length + 1];
        assertEquals(expected.length, is.read(got));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], got[i]);
        }
    }

    public void testDeleteLargeBinary() throws Exception {
        byte[] expected = "Hello".getBytes();
        String key = "test_key_large";

        OutputStream os = MetadataManager.storeLargeBinary(db, key);
        os.write(expected);
        os.close();
        assertNotNull(MetadataManager.retrieve(db, key));
        MetadataManager.deleteLargeBinary(db, key);
        assertNull(MetadataManager.retrieve(db, key));

    }
}
