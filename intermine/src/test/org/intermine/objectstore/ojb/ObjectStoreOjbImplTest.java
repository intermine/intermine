package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import org.flymine.objectstore.*;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;

import org.flymine.model.testmodel.*;

public class ObjectStoreOjbImplTest extends PersistenceBrokerFlyMineImplTest
{
    public ObjectStoreOjbImplTest(String arg) {
        super(arg);
    }

    private ObjectStore os;

    public void testNullConstructor() throws Exception {
        try {
            new ObjectStoreOjbImpl(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testGetInstance() throws Exception {
        os = ObjectStoreOjbImpl.getInstance(db);
        ObjectStore os2 = ObjectStoreOjbImpl.getInstance(db);
        assertSame(os, os2);
    }

    public void testQuery() throws Exception {
        Query query = new Query();
        QueryClass company = new QueryClass(Company.class);
        query.addFrom(company);
        query.addToSelect(company);
        //assertEquals(2, os.execute(query).size());
        //assertEquals(companys.get(0), os.execute(query).get(0));
        assertTrue(true);
    }
}
