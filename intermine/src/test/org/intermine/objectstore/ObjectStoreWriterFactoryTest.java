package org.flymine.objectstore;

import junit.framework.*;

public class ObjectStoreWriterFactoryTest extends TestCase
{
    public ObjectStoreWriterFactoryTest(String arg1) {
        super(arg1);
    }

    public void testValid() throws Exception {
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        assertNotNull(osw);
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
