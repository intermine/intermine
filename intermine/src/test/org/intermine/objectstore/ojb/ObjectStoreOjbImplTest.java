package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;

import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.*;

import org.flymine.model.testmodel.*;

public class ObjectStoreOjbImplTest extends QueryTestCase
{
    private ObjectStore os;

    public ObjectStoreOjbImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        setUpData();
        super.setUp();
        os = ObjectStoreOjbImpl.getInstance(db);
    }

    public void tearDown() throws Exception {
        tearDownData();
    }
    
    public void setUpResults() throws Exception {
    }

    public void executeTest(String type) throws Exception {
        assertEquals(results.get(type), os.execute((Query)queries.get(type), 0, 1));
    }
}
