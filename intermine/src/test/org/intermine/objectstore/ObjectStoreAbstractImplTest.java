package org.flymine.objectstore;

import junit.framework.TestCase;

import java.util.List;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.sql.query.ExplainResult;

public class ObjectStoreAbstractImplTest extends TestCase
{
    public ObjectStoreAbstractImplTest(String arg) {
        super(arg);
    }

    private ObjectStoreTestImpl os;

    public void setUp() throws Exception {
        os = new ObjectStoreTestImpl();
        os.maxOffset = 20;
        os.maxLimit = 10;
    }


    public void testCheckStartLimit() throws Exception {

        try {
            os.checkStartLimit(30,5);
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }
        try {
            os.checkStartLimit(10,15);
            fail("Expected ObjectStoreLimitReachedException");
        } catch (ObjectStoreLimitReachedException e) {
        }

    }

    /**
     * Implementation of the abstract class ObjectStoreAbstractImpl for test purposes
     */
    protected class ObjectStoreTestImpl extends ObjectStoreAbstractImpl {
        protected ObjectStoreTestImpl() {
        }

        public Results execute(Query q) throws ObjectStoreException {
            return null;
        }

        public List execute(Query q, int start, int limit) throws ObjectStoreException {
            return null;
        }

        public ExplainResult estimate(Query q) throws ObjectStoreException {
            return null;
        }
        public ExplainResult estimate(Query q, int start, int limit) throws ObjectStoreException {
            return null;
        }

        public int count(Query q) {
            return 0;
        }
    }


}
