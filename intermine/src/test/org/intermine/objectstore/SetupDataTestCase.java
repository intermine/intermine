package org.flymine.objectstore;

import junit.extensions.TestSetup;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.lang.reflect.Method;

import org.flymine.objectstore.ojb.ObjectStoreWriterOjbImpl;
import org.flymine.objectstore.ojb.ObjectStoreOjbImpl;
import org.flymine.sql.DatabaseFactory;

public abstract class SetupDataTestCase extends ObjectStoreQueriesTestCase
{
    protected static final org.apache.log4j.Logger LOG
        = org.apache.log4j.Logger.getLogger(SetupDataTestCase.class);

    protected static Class subClass;

    public SetupDataTestCase(String arg) {
        super(arg);
    }

    public static Test buildSuite(Class cls) {
        subClass = cls;
        TestSetup setup = new TestSetup(new TestSuite(cls)) {
                protected void setUp() throws Exception {
                    oneTimeSetUp();
                }
                protected void tearDown() throws Exception {
                    oneTimeTearDown();
                }
            };
        return setup;
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreAbstractImpl osLocal = (ObjectStoreAbstractImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        db = DatabaseFactory.getDatabase("db.unittest");
        // this needs to be changed to use AbstractImpl when one is written...
        writer = new ObjectStoreWriterOjbImpl((ObjectStoreOjbImpl) osLocal);
        setUpData();
        storeData();
        Method setUpQueries = subClass.getMethod("setUpQueries", new Class[] {});
        setUpQueries.invoke(null, new Object[] {});
        Method setUpResults = subClass.getMethod("setUpResults", new Class[] {});
        setUpResults.invoke(null, new Object[] {});
    }

    public static void oneTimeTearDown() throws Exception {
        removeDataFromStore();
    }

    public void setUp() throws Exception{
    }

    public void tearDown() throws Exception {
    }


}
