package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import org.flymine.objectstore.*;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;

public class ObjectStoreOjbImplTest extends TestCase
{
    public ObjectStoreOjbImplTest(String arg) {
        super(arg);
    }

    private Database db;

    public void setUp() throws Exception {
        db = DatabaseFactory.getDatabase("db.unittest");
    }



    public void testNullConstructor() throws Exception {
        try {
            new ObjectStoreOjbImpl(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetInstance() throws Exception {
        ObjectStore os1 = ObjectStoreOjbImpl.getInstance(db);
        ObjectStore os2 = ObjectStoreOjbImpl.getInstance(db);
        assertSame(os1, os2);
    }


}
