package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.iql.IqlQuery;

public class ObjectStoreInterMineImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreInterMineImplTest.class);
    }

    public void testLargeOffset() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Query q2 = QueryCloner.cloneQuery(q);
        SingletonResults r = new SingletonResults(q, os, os.getSequence());
        r.setBatchSize(2);
        InterMineObject o = (InterMineObject) r.get(5);
        SqlGenerator.registerOffset(q2, 6, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId());
        SingletonResults r2 = new SingletonResults(q2, os, os.getSequence());
        r2.setBatchSize(2);

        Query q3 = QueryCloner.cloneQuery(q);
        SqlGenerator.registerOffset(q3, 5, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId());
        SingletonResults r3 = new SingletonResults(q3, os, os.getSequence());
        r3.setBatchSize(2);

        assertEquals(r, r2);
        assertTrue(!r.equals(r3));
    }

    public void testPrecompute() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc1);
        q.addToSelect(qc2);
        QueryField f1 = new QueryField(qc1, "name");
        QueryField f2 = new QueryField(qc2, "name");
        q.addToSelect(f1);
        q.addToSelect(f2);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "employees"), ConstraintOp.CONTAINS, qc2));
        q.setDistinct(false);
        Set indexes = new LinkedHashSet();
        indexes.add(qc1);
        indexes.add(f1);
        indexes.add(f2);
        String tableName = ((ObjectStoreInterMineImpl) os).precompute(q, indexes);
        Connection con = null;
        Map indexMap = new HashMap();
        try {
            con = ((ObjectStoreInterMineImpl) os).getConnection();
            Statement s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT * FROM pg_indexes WHERE tablename = '" + tableName + "'");
            while (r.next()) {
                indexMap.put(r.getString("indexname"), r.getString("indexdef"));
            }
        } finally {
            if (con != null) {
                ((ObjectStoreInterMineImpl) os).releaseConnection(con);
            }
        }
        Map expectedIndexMap = new HashMap();
        expectedIndexMap.put("index" + tableName + "_field_a1_id__a3___a4_", "CREATE INDEX index" + tableName + "_field_a1_id__a3___a4_ ON " + tableName + " USING btree (a1_id, a3_, a4_)");
        expectedIndexMap.put("index" + tableName + "_field_a3_", "CREATE INDEX index" + tableName + "_field_a3_ ON " + tableName + " USING btree (a3_)");
        expectedIndexMap.put("index" + tableName + "_field_a4_", "CREATE INDEX index" + tableName + "_field_a4_ ON " + tableName + " USING btree (a4_)");
        assertEquals(expectedIndexMap, indexMap);
    }

    public void testCancelMethods1() throws Exception {
        Object id = "flibble1";
        Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
        try {
            Statement s = c.createStatement();
            ((ObjectStoreInterMineImpl) os).registerRequestId(id);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            ((ObjectStoreInterMineImpl) os).deregisterStatement(s);
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }

    public void testCancelMethods2() throws Exception {
        Object id = "flibble2";
        Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
        try {
            Statement s = c.createStatement();
            ((ObjectStoreInterMineImpl) os).registerRequestId(id);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            ((ObjectStoreInterMineImpl) os).cancelRequest(id);
            ((ObjectStoreInterMineImpl) os).deregisterStatement(s);
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }

    public void testCancelMethods3() throws Exception {
        Object id = "flibble3";
        try {
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
            fail("Should have thrown exception");
        } catch (ObjectStoreException e) {
            assertEquals("This Thread is not registered with ID flibble3", e.getMessage());
        }
    }

    public void testCancelMethods4() throws Exception {
        Object id = "flibble4";
        Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
        try {
            Statement s = c.createStatement();
            ((ObjectStoreInterMineImpl) os).registerRequestId(id);
            ((ObjectStoreInterMineImpl) os).cancelRequest(id);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            fail("Should have thrown exception");
        } catch (ObjectStoreException e) {
            assertEquals("Request id flibble4 is cancelled", e.getMessage());
        } finally {
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }

    public void testCancelMethods5() throws Exception {
        Object id = "flibble5";
        Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
        Statement s = null;
        try {
            s = c.createStatement();
            ((ObjectStoreInterMineImpl) os).registerRequestId(id);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            fail("Should have thrown exception");
        } catch (ObjectStoreException e) {
            assertEquals("Request id flibble5 is currently being serviced in another thread. Don't share request IDs over multiple threads!", e.getMessage());
        } finally {
            if (s != null) {
                ((ObjectStoreInterMineImpl) os).deregisterStatement(s);
            }
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }

    public void testCancelMethods6() throws Exception {
        Object id = "flibble6";
        Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
        Statement s = null;
        long start = 0;
        try {
            s = c.createStatement();
            s.execute("CREATE TABLE test (col1 int, col2 int)");
            s.execute("INSERT INTO test VALUES (1, 1)");
            s.execute("INSERT INTO test VALUES (1, 2)");
            s.execute("INSERT INTO test VALUES (2, 1)");
            s.execute("INSERT INTO test VALUES (2, 2)");
            ((ObjectStoreInterMineImpl) os).registerRequestId(id);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            Thread delayedCancel = new Thread(new DelayedCancel(id));
            delayedCancel.start();
            start = System.currentTimeMillis();
            s.executeQuery("SELECT * FROM test AS a, test AS b, test AS c, test AS d, test AS e, test AS f, test AS g, test AS h, test AS i, test AS j, test AS k, test AS l, test AS m WHERE a.col2 = b.col1 AND b.col2 = c.col1 AND c.col2 = d.col1 AND d.col2 = e.col1 AND e.col2 = f.col1 AND f.col2 = g.col1 AND g.col2 = h.col1 AND h.col2 = i.col1 AND i.col2 = j.col1 AND j.col2 = k.col1 AND k.col2 = l.col1 AND l.col2 = m.col1");
            System.out.println("testCancelMethods6: time for query = " + (System.currentTimeMillis() - start) + " ms");
            fail("Request should have been cancelled");
        } catch (SQLException e) {
            if (start != 0) {
                System.out.println("testCancelMethods6: time for query = " + (System.currentTimeMillis() - start) + " ms");
            }
            assertEquals("ERROR: canceling query due to user request", e.getMessage());
        } finally {
            if (s != null) {
                try {
                    ((ObjectStoreInterMineImpl) os).deregisterStatement(s);
                } catch (ObjectStoreException e) {
                    e.printStackTrace(System.out);
                }
            }
            try {
                ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
            } catch (ObjectStoreException e) {
                e.printStackTrace(System.out);
            }
            try {
                c.createStatement().execute("DROP TABLE test");
            } catch (SQLException e) {
            }
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }

    /*This test does not work. This is due to a failing of JDBC. The Statement.cancel() method is
     * not fully Thread-safe, in that if one performs a cancel() request just before a Statement is
     * used, that operation will not be cancelled. There is a race condition between the
     * ObjectStore.execute() method registering the Statement and the Statement becoming
     * cancellable.
    public void testCancelMethods7() throws Exception {
        Object id = "flibble7";
        Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
        Statement s = null;
        long start = 0;
        try {
            s = c.createStatement();
            s.execute("CREATE TABLE test (col1 int, col2 int)");
            s.execute("INSERT INTO test VALUES (1, 1)");
            s.execute("INSERT INTO test VALUES (1, 2)");
            s.execute("INSERT INTO test VALUES (2, 1)");
            s.execute("INSERT INTO test VALUES (2, 2)");
            ((ObjectStoreInterMineImpl) os).registerRequestId(id);
            ((ObjectStoreInterMineImpl) os).registerStatement(s);
            start = System.currentTimeMillis();
            s.cancel();
            s.executeQuery("SELECT * FROM test AS a, test AS b, test AS c, test AS d, test AS e, test AS f, test AS g, test AS h, test AS i, test AS j, test AS k, test AS l, test AS m WHERE a.col2 = b.col1 AND b.col2 = c.col1 AND c.col2 = d.col1 AND d.col2 = e.col1 AND e.col2 = f.col1 AND f.col2 = g.col1 AND g.col2 = h.col1 AND h.col2 = i.col1 AND i.col2 = j.col1 AND j.col2 = k.col1 AND k.col2 = l.col1 AND l.col2 = m.col1");
            System.out.println("testCancelMethods6: time for query = " + (System.currentTimeMillis() - start) + " ms");
            fail("Request should have been cancelled");
        } catch (SQLException e) {
            if (start != 0) {
                System.out.println("testCancelMethods6: time for query = " + (System.currentTimeMillis() - start) + " ms");
            }
            assertEquals("ERROR: canceling query due to user request", e.getMessage());
        } finally {
            if (s != null) {
                try {
                    ((ObjectStoreInterMineImpl) os).deregisterStatement(s);
                } catch (ObjectStoreException e) {
                    e.printStackTrace(System.out);
                }
            }
            try {
                ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
            } catch (ObjectStoreException e) {
                e.printStackTrace(System.out);
            }
            try {
                c.createStatement().execute("DROP TABLE test");
            } catch (SQLException e) {
            }
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }
    */

    public void testCancel() throws Exception {
        Object id = "flibble8";
        Query q = new IqlQuery("SELECT a, b, c, d, e FROM Employee AS a, Employee AS b, Employee AS c, Employee AS d, Employee AS e", "org.intermine.model.testmodel").toQuery();
        ((ObjectStoreInterMineImpl) os).registerRequestId(id);
        try {
            Thread delayedCancel = new Thread(new DelayedCancel(id));
            delayedCancel.start();
            Results r = os.execute(q);
            r.setBatchSize(10000);
            r.setNoOptimise();
            r.setNoExplain();
            r.get(0);
            fail("Operation should have been cancelled");
        } catch (RuntimeException e) {
            assertEquals("ObjectStore error has occured (in get)", e.getMessage());
            Throwable t = e.getCause();
            t = t.getCause();
            assertEquals("ERROR: canceling query due to user request", t.getMessage());
        } finally {
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
        }
    }

    private static class DelayedCancel implements Runnable
    {
        private Object id;

        public DelayedCancel(Object id) {
            this.id = id;
        }

        public void run() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            try {
                ((ObjectStoreInterMineImpl) os).cancelRequest(id);
            } catch (ObjectStoreException e) {
                e.printStackTrace(System.out);
            }
        }
    }
}
