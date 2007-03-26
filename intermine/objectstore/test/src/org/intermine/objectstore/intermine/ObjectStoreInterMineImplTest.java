package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Types;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreQueriesTestCase;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.sql.query.Constraint;

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
        QueryClass qc = new QueryClass(Address.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Query q2 = QueryCloner.cloneQuery(q);
        SingletonResults r = new SingletonResults(q, os, os.getSequence());
        r.setBatchSize(2);
        InterMineObject o = (InterMineObject) r.get(5);
        SqlGenerator.registerOffset(q2, 6, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId(), new HashMap());
        SingletonResults r2 = new SingletonResults(q2, os, os.getSequence());
        r2.setBatchSize(2);

        Query q3 = QueryCloner.cloneQuery(q);
        SqlGenerator.registerOffset(q3, 5, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId(), new HashMap());
        SingletonResults r3 = new SingletonResults(q3, os, os.getSequence());
        r3.setBatchSize(2);

        assertEquals(r, r2);
        assertTrue(!r.equals(r3));
    }

    public void testLargeOffset2() throws Exception {
        Employee nullEmployee = new Employee();
        nullEmployee.setAge(26);
        nullEmployee.setName(null);
        try {
            storeDataWriter.store(nullEmployee);
            Query q = new Query();
            QueryClass qc = new QueryClass(Employee.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            q.addToOrderBy(new QueryField(qc, "name"));
            Query q2 = QueryCloner.cloneQuery(q);
            SingletonResults r = new SingletonResults(q, os, os.getSequence());
            r.setBatchSize(2);
            Employee o = (Employee) r.get(2);
            SqlGenerator.registerOffset(q2, 3, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getName(), new HashMap());
            SingletonResults r2 = new SingletonResults(q2, os, os.getSequence());
            r2.setBatchSize(2);

            Query q3 = QueryCloner.cloneQuery(q);
            SqlGenerator.registerOffset(q3, 2, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getName(), new HashMap());
            SingletonResults r3 = new SingletonResults(q3, os, os.getSequence());
            r3.setBatchSize(2);

            assertEquals(r, r2);
            assertTrue(!r.equals(r3));
        } finally {
            storeDataWriter.delete(nullEmployee);
        }
    }

    /*public void testLargeOffset3() throws Exception {
        // This is to test the indexing of large offset queries, so it needs to do a performance test.
        Set toDelete = new HashSet();
        try {
            long start = System.currentTimeMillis();
            storeDataWriter.beginTransaction();
            for (int i = 0; i < 10105; i++) {
                Employee e = new Employee();
                String name = "Fred_";
                if (i < 10000) {
                    name += "0";
                }
                if (i < 1000) {
                    name += "0";
                }
                if (i < 100) {
                    name += "0";
                }
                if (i < 10) {
                    name += "0";
                }
                e.setName(name + i);
                e.setAge(i + 1000000);
                storeDataWriter.store(e);
                toDelete.add(e);
            }
            for (int i = 10105; i < 10205; i++) {
                Employee e = new Employee();
                e.setAge(i + 1000000);
                storeDataWriter.store(e);
                toDelete.add(e);
            }
            storeDataWriter.commitTransaction();
            Connection c = null;
            try {
                c = ((ObjectStoreWriterInterMineImpl) storeDataWriter).getConnection();
                c.createStatement().execute("ANALYSE");
            } finally {
                if (c != null) {
                    ((ObjectStoreWriterInterMineImpl) storeDataWriter).releaseConnection(c);
                }
            }
            long now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to insert data");
            start = now;
            Query q = new Query();
            QueryClass qc = new QueryClass(Employee.class);
            QueryField f = new QueryField(qc, "name");
            q.addFrom(qc);
            q.addToSelect(f);
            q.setDistinct(false);
            SingletonResults r = new SingletonResults(q, os, os.getSequence());
            r.setBatchSize(10);
            assertEquals("Fred_00000", r.get(6));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find first row");
            long timeA = now - start;
            start = now;
            assertEquals("Fred_10015", r.get(10021));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find row 10015");
            long timeB = now - start;
            start = now;
            assertEquals("Fred_10035", r.get(10041));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find row 10035");
            long timeC = now - start;
            start = now;
            assertNull(r.get(10141));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find row 10135");
            long timeD = now - start;

            q = QueryCloner.cloneQuery(q);
            ((ObjectStoreInterMineImpl) os).precompute(q);
            r = new SingletonResults(q, os, os.getSequence());
            r.setBatchSize(10);
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to precompute results");
            start = now;
            assertEquals("Fred_00000", r.get(6));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find first precomputed row");
            long timePA = now - start;
            start = now;
            assertEquals("Fred_10015", r.get(10021));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find precomputed row 10015");
            long timePB = now - start;
            start = now;
            assertEquals("Fred_10035", r.get(10041));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find precomputed row 10035");
            long timePC = now - start;
            start = now;
            assertNull(r.get(10141));
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to find precomputed row 10135");
            long timePD = now - start;
            assertTrue("Row 6 found in " + timeA + "ms", timeA < 30);
            assertTrue("Row 10015 found in " + timeB + "ms", timeB > 30);
            assertTrue("Row 10035 found in " + timeC + "ms", timeC < 22);
            assertTrue("Row 10135 found in " + timeD + "ms", timeD < 15);
            assertTrue("Precomputed row 6 found in " + timePA + "ms", timePA < 30);
            assertTrue("Precomputed row 10015 found in " + timePB + "ms", timePB > 30);
            //TODO: This should pass - it's Postgres being thick.
            //assertTrue("Precomputed row 10035 found in " + timePC + "ms", timePC < 15);
            assertTrue("Precomputed row 10135 found in " + timePD + "ms", timePD < 15);
        } finally {
            if (storeDataWriter.isInTransaction()) {
                storeDataWriter.abortTransaction();
            }
            long start = System.currentTimeMillis();
            storeDataWriter.beginTransaction();
            Iterator iter = toDelete.iterator();
            while (iter.hasNext()) {
                Employee e = (Employee) iter.next();
                storeDataWriter.delete(e);
            }
            storeDataWriter.commitTransaction();
            long now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to remove data");
            start = now;
            Connection c = null;
            try {
                c = ((ObjectStoreWriterInterMineImpl) storeDataWriter).getConnection();
                c.createStatement().execute("VACUUM FULL ANALYSE");
            } finally {
                if (c != null) {
                    ((ObjectStoreWriterInterMineImpl) storeDataWriter).releaseConnection(c);
                }
            }
            now = System.currentTimeMillis();
            System.out.println("Took " + (now - start) + "ms to VACUUM FULL ANALYSE");
        }
    }*/

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
        String tableName = ((ObjectStoreInterMineImpl) os).precompute(q, indexes, "test");
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
        expectedIndexMap.put("index" + tableName + "_field_orderby_field", "CREATE INDEX index" + tableName + "_field_orderby_field ON " + tableName + " USING btree (orderby_field)");
        expectedIndexMap.put("index" + tableName + "_field_a1_id__lower_a3____lower_a4__", "CREATE INDEX index" + tableName + "_field_a1_id__lower_a3____lower_a4__ ON " + tableName + " USING btree (a1_id, lower(a3_), lower(a4_))");
        expectedIndexMap.put("index" + tableName + "_field_lower_a3__", "CREATE INDEX index" + tableName + "_field_lower_a3__ ON " + tableName + " USING btree (lower(a3_))");
        expectedIndexMap.put("index" + tableName + "_field_lower_a3___nulls", "CREATE INDEX index" + tableName + "_field_lower_a3___nulls ON " + tableName + " USING btree (((lower(a3_) IS NULL)))");
        expectedIndexMap.put("index" + tableName + "_field_lower_a4__", "CREATE INDEX index" + tableName + "_field_lower_a4__ ON " + tableName + " USING btree (lower(a4_))");
        expectedIndexMap.put("index" + tableName + "_field_lower_a4___nulls", "CREATE INDEX index" + tableName + "_field_lower_a4___nulls ON " + tableName + " USING btree (((lower(a4_) IS NULL)))");
        assertEquals(expectedIndexMap, indexMap);
    }

    public void testGoFaster() throws Exception {
        Query q = new IqlQuery("SELECT Company, Department FROM Company, Department WHERE Department.company CONTAINS Company", "org.intermine.model.testmodel").toQuery();
        try {
            ((ObjectStoreInterMineImpl) os).goFaster(q);
            Results r = os.execute(q);
            r.get(0);
            assertEquals(3, r.size());
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseGoFaster(q);
        }
    }

    public void testPrecomputeWithNullsInOrder() throws Exception {
        Types t1 = new Types();
        t1.setIntObjType(null);
        t1.setLongObjType(new Long(234212354));
        t1.setName("fred");
        storeDataWriter.store(t1);
        Types t2 = new Types();
        t2.setIntObjType(new Integer(278652));
        t2.setLongObjType(null);
        t2.setName("fred");
        storeDataWriter.store(t2);

        Query q = new Query();
        QueryClass qc = new QueryClass(Types.class);
        QueryField into = new QueryField(qc, "intObjType");
        QueryField longo = new QueryField(qc, "longObjType");
        q.addFrom(qc);
        q.addToSelect(into);
        q.addToSelect(longo);
        q.addToSelect(qc);
        q.setDistinct(false);
        ((ObjectStoreInterMineImpl) os).precompute(q, "test");

        Results r = os.execute(q);
        r.setBatchSize(1);
        SqlGenerator.registerOffset(q, 1, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, new Integer(100000), new HashMap());

        ResultsRow row = (ResultsRow) r.get(1);
        InterMineObject o = (InterMineObject) row.get(2);
        assertEquals("Expected " + t2.toString() + " but got " + o.toString(), t2.getId(), o.getId());
        row = (ResultsRow) r.get(2);
        o = (InterMineObject) row.get(2);
        assertEquals("Expected " + t1.toString() + " but got " + o.toString(), t1.getId(), o.getId());

        q.setConstraint(new SimpleConstraint(into, ConstraintOp.GREATER_THAN, new QueryValue(new Integer(100000))));
        q = QueryCloner.cloneQuery(q);
        r = os.execute(q);
        r.setBatchSize(10);

        row = (ResultsRow) r.get(0);
        o = (InterMineObject) row.get(2);
        assertEquals("Expected " + t2.toString() + " but got " + o.toString(), t2.getId(), o.getId());
        assertEquals(1, r.size());

        storeDataWriter.delete(t1);
        storeDataWriter.delete(t2);
    }

    public void testPrecomputeWithNegatives() throws Exception {
        Types t1 = new Types();
        t1.setLongObjType(new Long(-765187651234L));
        t1.setIntObjType(new Integer(278652));
        t1.setName("Fred");
        storeDataWriter.store(t1);

        Query q = new Query();
        QueryClass qc = new QueryClass(Types.class);
        QueryField into = new QueryField(qc, "intObjType");
        QueryField longo = new QueryField(qc, "longObjType");
        q.addFrom(qc);
        q.addToSelect(into);
        q.addToSelect(longo);
        q.addToSelect(qc);
        q.setDistinct(false);
        ((ObjectStoreInterMineImpl) os).precompute(q, "test");

        Results r = os.execute(q);
        r.setBatchSize(1);
        SqlGenerator.registerOffset(q, 1, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, new Integer(278651), new HashMap());

        ResultsRow row = (ResultsRow) r.get(1);
        InterMineObject o = (InterMineObject) row.get(2);
        assertEquals("Expected " + t1.toString() + " but got " + o.toString(), t1.getId(), o.getId());
        try {
            r.get(2);
            fail("Expected size to be 2");
        } catch (Exception e) {
        }
        assertEquals(2, r.size());

        storeDataWriter.delete(t1);
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
        UndeclaredThrowableException failure = null;

        // this test sometimes fails even when all is OK, so run it multiple times and exit if it
        // passes
        for (int i = 0 ;i < 20 ; i++) {
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
                // test passed so stop immediately
                return;
            } catch (UndeclaredThrowableException t) {
                // test failed but might pass next time - try again
                failure = t;
            } finally {
                if (s != null) {
                    ((ObjectStoreInterMineImpl) os).deregisterStatement(s);
                }
                ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
                ((ObjectStoreInterMineImpl) os).releaseConnection(c);
            }
        }

        throw failure;
    }

/*
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
            String errorString = e.getMessage().replaceFirst("statement", "query");
            assertEquals("ERROR: canceling query due to user request", errorString);
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
            if (!"Request id flibble8 is cancelled".equals(t.getMessage())) {
                t = t.getCause();
                String errorString =  t.getMessage().replaceFirst("statement", "query");
                assertEquals("ERROR: canceling query due to user request",errorString);
            }
        } finally {
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
        }
    }
    */

    public void testCreateTempBagTables() throws Exception {
        Query q = ObjectStoreQueriesTestCase.bagConstraint();

        Map bagTableNames = ((ObjectStoreInterMineImpl) os).bagConstraintTables;
        bagTableNames.clear();

        Connection con = null;

        try {
            con = ((ObjectStoreInterMineImpl) os).getConnection();
            con.setAutoCommit(false);

            int minBagSize = ((ObjectStoreInterMineImpl) os).minBagTableSize;
            ((ObjectStoreInterMineImpl) os).minBagTableSize = 1;
            ((ObjectStoreInterMineImpl) os).createTempBagTables(con, q);
            ((ObjectStoreInterMineImpl) os).minBagTableSize = minBagSize;

            assertEquals("Entries: " + bagTableNames, 1, bagTableNames.size());

            String tableName = (String) bagTableNames.values().iterator().next();


            Set expected = new HashSet();

            Iterator bagIter = ((BagConstraint) q.getConstraint()).getBag().iterator();

            while (bagIter.hasNext()) {
                Object thisObject = bagIter.next();

                if (thisObject instanceof String) {
                    expected.add(thisObject);
                }
            }

            Statement s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT value FROM " + tableName);
            r.next();

            Set resultStrings = new HashSet();

            resultStrings.add(r.getString(1));
            r.next();
            resultStrings.add(r.getString(1));
            r.next();
            resultStrings.add(r.getString(1));

            try {
                r.next();
            } catch (SQLException e) {
                // expected
            }

            assertEquals(expected, resultStrings);
        } finally {
            if (con != null) {
                con.commit();
                con.setAutoCommit(true);
                ((ObjectStoreInterMineImpl) os).releaseConnection(con);
            }
        }
    }

    public void testGetUniqueInteger() throws Exception {
        ObjectStoreInterMineImpl osii = (ObjectStoreInterMineImpl) os;
        Connection con = osii.getConnection();

        con.setAutoCommit(false);
        int integer1 = osii.getUniqueInteger(con);
        int integer2 = osii.getUniqueInteger(con);

        assertTrue(integer2 > integer1);

        con.setAutoCommit(true);
        int integer3 = osii.getUniqueInteger(con);
        int integer4 = osii.getUniqueInteger(con);

        assertTrue(integer3 > integer2);
        assertTrue(integer4 > integer3);
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
    
    public void testIsPrecomputed() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        QueryField qf = new QueryField(qc,"age");
        SimpleConstraint sc = new SimpleConstraint(qf,ConstraintOp.GREATER_THAN,new QueryValue(new Integer(20)));
        q.addToSelect(qc);
        q.addFrom(qc);
        q.setConstraint(sc);
        assertFalse(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
        ((ObjectStoreInterMineImpl)os).precompute(q, "template");
        assertTrue(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
        ObjectStoreBag osb = storeDataWriter.createObjectStoreBag();
        storeDataWriter.addToBag(osb, new Integer(5));
        assertTrue(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
        storeDataWriter.store((Employee) data.get("EmployeeA1"));
        assertFalse(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
    }

    public void testObjectStoreBag() throws Exception {
        ObjectStoreBag osb = storeDataWriter.createObjectStoreBag();
        ArrayList coll = new ArrayList();
        coll.add(new Integer(3));
        coll.add(((Employee) data.get("EmployeeA1")).getId());
        coll.add(((Employee) data.get("EmployeeA2")).getId());
        coll.add(new Integer(20));
        coll.add(new Integer(23));
        coll.add(new Integer(30));
        storeDataWriter.beginTransaction();
        storeDataWriter.addAllToBag(osb, coll);
        Query q = new Query();
        q.addToSelect(osb);
        SingletonResults r = new SingletonResults(q, os, os.getSequence());
        assertEquals(Collections.EMPTY_LIST, r);
        q = new Query();
        q.addToSelect(osb);
        r = new SingletonResults(q, os, os.getSequence());
        storeDataWriter.commitTransaction();
        try {
            assertEquals(Collections.EMPTY_LIST, r);
            fail("Expected: ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
        }
        q = new Query();
        q.addToSelect(osb);
        r = new SingletonResults(q, os, os.getSequence());
        assertEquals(coll, r);
        q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, osb));
        r = new SingletonResults(q, os, os.getSequence());
        assertEquals(Arrays.asList(new Object[] {data.get("EmployeeA1"), data.get("EmployeeA2")}), r);
        ObjectStoreBag osb2 = storeDataWriter.createObjectStoreBag();
        storeDataWriter.addToBag(osb2, ((Employee) data.get("EmployeeA1")).getId());
        storeDataWriter.addToBagFromQuery(osb2, q);
        q = new Query();
        q.addToSelect(osb2);
        r = new SingletonResults(q, os, os.getSequence());
        assertEquals(Arrays.asList(new Object[] {((Employee) data.get("EmployeeA1")).getId(), ((Employee) data.get("EmployeeA2")).getId()}), r);
    }

    public void testClosedConnectionBug() throws Exception {
        Query pq = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        pq.addFrom(qc);
        pq.addToSelect(qc);
        ((ObjectStoreInterMineImpl) os).precompute(pq, "Whatever");
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Manager.class);
        QueryClass qc2 = new QueryClass(Manager.class);
        QueryClass qc3 = new QueryClass(Manager.class);
        QueryClass qc4 = new QueryClass(Manager.class);
        QueryClass qc5 = new QueryClass(Manager.class);
        QueryClass qc6 = new QueryClass(Manager.class);
        QueryClass qc7 = new QueryClass(Manager.class);
        QueryClass qc8 = new QueryClass(Manager.class);
        QueryClass qc9 = new QueryClass(Manager.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addFrom(qc3);
        q.addFrom(qc4);
        q.addFrom(qc5);
        q.addFrom(qc6);
        q.addFrom(qc7);
        q.addFrom(qc8);
        q.addFrom(qc9);
        q.addToSelect(qc1);
        q.addToSelect(qc2);
        q.addToSelect(qc3);
        q.addToSelect(qc4);
        q.addToSelect(qc5);
        q.addToSelect(qc6);
        q.addToSelect(qc7);
        q.addToSelect(qc8);
        q.addToSelect(qc9);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        q.setConstraint(cs);
        cs.addConstraint(new SimpleConstraint(new QueryField(qc1, "id"), ConstraintOp.EQUALS, new QueryValue(new Integer(1))));
        cs.addConstraint(new SimpleConstraint(new QueryField(qc1, "id"), ConstraintOp.EQUALS, new QueryValue(new Integer(2))));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc2));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc3));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc4));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc5));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc6));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc7));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc8));
        cs.addConstraint(new ClassConstraint(qc1, ConstraintOp.EQUALS, qc9));
        assertEquals(Collections.EMPTY_LIST, os.execute(q, 0, 1000, true, true, os.getSequence()));
    }
}
