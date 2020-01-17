package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.*;
import org.intermine.objectstore.*;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Common test codes for various permutations of the objectstore.
 *
 * We test this way instead of putting these in the ancestor class of an infrastucture for clarity of
 * test code.
 */
public class ObjectStoreInterMineImplTestCase extends ObjectStoreAbstractImplTestCase {
    protected static ObjectStoreInterMineImpl os;

    public static void oneTimeSetUp(
            String osName, String osWriterName, String modelName, String itemsXmlFilename) throws Exception {
        os = (ObjectStoreInterMineImpl)ObjectStoreFactory.getObjectStore(osName);
        ObjectStoreAbstractImplTestCase.oneTimeSetUp(os, osWriterName, modelName, itemsXmlFilename);
    }

    @Test
    public void testLargeOffset() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Address.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Query q2 = QueryCloner.cloneQuery(q);
        SingletonResults r = os.executeSingleton(q, 2, true, true, true);
        InterMineObject o = (InterMineObject) r.get(5);
        SqlGenerator.registerOffset(q2, 6, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId(), new HashMap());
        SingletonResults r2 = os.executeSingleton(q2, 2, true, true, true);

        Query q3 = QueryCloner.cloneQuery(q);
        SqlGenerator.registerOffset(q3, 5, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId(), new HashMap());
        SingletonResults r3 = new SingletonResults(q3, os, ObjectStore.SEQUENCE_IGNORE);
        r3.setBatchSize(2);

        Assert.assertTrue(r == r2);
        Assert.assertTrue(r != r3);
        Assert.assertTrue(r2 != r3);
        Assert.assertEquals(r, r2);
        Assert.assertTrue(!r.equals(r3));
    }

    @Test
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
            SingletonResults r = os.executeSingleton(q, 2, true, true, true);
            Employee o = (Employee) r.get(2);
            SqlGenerator.registerOffset(q2, 3, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getName(), new HashMap());
            SingletonResults r2 = os.executeSingleton(q2, 2, true, true, true);

            Query q3 = QueryCloner.cloneQuery(q);
            SqlGenerator.registerOffset(q3, 2, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getName(), new HashMap());
            SingletonResults r3 = new SingletonResults(q3, os, ObjectStore.SEQUENCE_IGNORE);
            r3.setBatchSize(2);

            Assert.assertTrue(r == r2);
            Assert.assertTrue(r != r3);
            Assert.assertTrue(r2 != r3);
            Assert.assertEquals(r, r2);
            Assert.assertTrue(!r.equals(r3));
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
            SingletonResults r = os.executeSingleton(q);
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
            r = os.executeSingleton(q);
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

    @Test
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

        ObjectStoreInterMineImpl objectStoreInterMineImpl = ((ObjectStoreInterMineImpl) os);
        List<String> precomputes = objectStoreInterMineImpl.precompute(q, indexes, "test");
        String tableName = String.valueOf(precomputes.get(0));

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
        expectedIndexMap.put(tableName + "_orderby_field", "CREATE INDEX " + tableName + "_orderby_field ON " + tableName + " USING btree (orderby_field)");
        expectedIndexMap.put(tableName + "_a1_id__lower_a3____lower_a4__", "CREATE INDEX " + tableName + "_a1_id__lower_a3____lower_a4__ ON " + tableName + " USING btree (a1_id, lower(a3_), lower(a4_))");
        expectedIndexMap.put(tableName + "_lower_a3__", "CREATE INDEX " + tableName + "_lower_a3__ ON " + tableName + " USING btree (lower(a3_))");
        expectedIndexMap.put(tableName + "_lower_a3___text_pattern_ops", "CREATE INDEX " + tableName + "_lower_a3___text_pattern_ops ON " + tableName + " USING btree (lower(a3_) text_pattern_ops)");
        expectedIndexMap.put(tableName + "_lower_a4__", "CREATE INDEX " + tableName + "_lower_a4__ ON " + tableName + " USING btree (lower(a4_))");
        expectedIndexMap.put(tableName + "_lower_a4___text_pattern_ops", "CREATE INDEX " + tableName + "_lower_a4___text_pattern_ops ON " + tableName + " USING btree (lower(a4_) text_pattern_ops)");
        Assert.assertEquals(expectedIndexMap, indexMap);
    }

    @Test
    public void testGoFaster() throws Exception {
        Query q = new IqlQuery("SELECT Company, Department FROM Company, Department WHERE Department.company CONTAINS Company", "org.intermine.model.testmodel").toQuery();
        try {
            ((ObjectStoreInterMineImpl) os).goFaster(q);
            Results r = os.execute(q);
            r.get(0);
            Assert.assertEquals(3, r.size());
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseGoFaster(q);
        }
    }

    @Test
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

        Results r = os.execute(q, 1, true, true, true);
        SqlGenerator.registerOffset(q, 1, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, new Integer(100000), new HashMap());

        ResultsRow row = (ResultsRow) r.get(1);
        InterMineObject o = (InterMineObject) row.get(2);
        Assert.assertEquals("Expected " + t2.toString() + " but got " + o.toString(), t2.getId(), o.getId());
        row = (ResultsRow) r.get(2);
        o = (InterMineObject) row.get(2);
        Assert.assertEquals("Expected " + t1.toString() + " but got " + o.toString(), t1.getId(), o.getId());

        q.setConstraint(new SimpleConstraint(into, ConstraintOp.GREATER_THAN, new QueryValue(new Integer(100000))));
        q = QueryCloner.cloneQuery(q);
        r = os.execute(q, 10, true, true, true);

        row = (ResultsRow) r.get(0);
        o = (InterMineObject) row.get(2);
        Assert.assertEquals("Expected " + t2.toString() + " but got " + o.toString(), t2.getId(), o.getId());
        Assert.assertEquals(1, r.size());

        storeDataWriter.delete(t1);
        storeDataWriter.delete(t2);
    }

    @Test
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

        Results r = os.execute(q, 1, true, true, true);
        SqlGenerator.registerOffset(q, 1, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, new Integer(278651), new HashMap());

        ResultsRow row = (ResultsRow) r.get(1);
        InterMineObject o = (InterMineObject) row.get(2);
        Assert.assertEquals("Expected " + t1.toString() + " but got " + o.toString(), t1.getId(), o.getId());
        try {
            r.get(2);
            Assert.fail("Expected size to be 2");
        } catch (Exception e) {
        }
        Assert.assertEquals(2, r.size());

        storeDataWriter.delete(t1);
    }

    @Test
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

    @Test
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
            try {
                s.executeQuery("SELECT 1");
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        } finally {
            ((ObjectStoreInterMineImpl) os).releaseConnection(c);
        }
    }

    @Test
    public void testCancelMethods3() throws Exception {
        Object id = "flibble3";
        try {
            ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
            Assert.fail("Should have thrown exception");
        } catch (ObjectStoreException e) {
            Assert.assertEquals("This Thread is not registered with ID flibble3", e.getMessage());
        }
    }

    /*
    // this test sometimes fails due to a race condition
    public void testCancelMethods4() throws Exception {
        Object id = "flibble4";
        UndeclaredThrowableException failure = null;

        // this test sometimes fails even when all is OK, so run it multiple times and exit if it
        // passes
        for (int i = 0 ;i < 20 ; i++) {
            Connection c = ((ObjectStoreInterMineImpl) os).getConnection();
            try {
                Statement s = c.createStatement();
                ((ObjectStoreInterMineImpl) os).registerRequestId(id);
                ((ObjectStoreInterMineImpl) os).cancelRequest(id);
                ((ObjectStoreInterMineImpl) os).registerStatement(s);
                fail("Should have thrown exception");
            } catch (ObjectStoreException e) {
                assertEquals("Request id flibble4 is cancelled", e.getMessage());
                // test passed so stop immediately
                return;
            } catch (UndeclaredThrowableException t) {
                // test failed but might pass next time - try again
                failure = t;
            } finally {
                ((ObjectStoreInterMineImpl) os).deregisterRequestId(id);
                ((ObjectStoreInterMineImpl) os).releaseConnection(c);
            }
        }

        throw failure;
    }
    */

    /*
    // this test sometimes fails due to a race condition
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
*/

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

    @Test
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

            Assert.assertEquals("Entries: " + bagTableNames, 1, bagTableNames.size());

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

            Assert.assertEquals(expected, resultStrings);
        } finally {
            if (con != null) {
                con.commit();
                con.setAutoCommit(true);
                ((ObjectStoreInterMineImpl) os).releaseConnection(con);
            }
        }
    }

    @Test
    public void testGetUniqueInteger() throws Exception {
        ObjectStoreInterMineImpl osii = (ObjectStoreInterMineImpl) os;
        Connection con = osii.getConnection();

        con.setAutoCommit(false);
        int integer1 = osii.getUniqueInteger(con);
        int integer2 = osii.getUniqueInteger(con);

        Assert.assertTrue(integer2 > integer1);

        con.setAutoCommit(true);
        int integer3 = osii.getUniqueInteger(con);
        int integer4 = osii.getUniqueInteger(con);

        Assert.assertTrue(integer3 > integer2);
        Assert.assertTrue(integer4 > integer3);
    }

    /*
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
    */

    @Test
    public void testIsPrecomputed() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        QueryField qf = new QueryField(qc,"age");
        SimpleConstraint sc = new SimpleConstraint(qf,ConstraintOp.GREATER_THAN,new QueryValue(new Integer(20)));
        q.addToSelect(qc);
        q.addFrom(qc);
        q.setConstraint(sc);
        Assert.assertFalse(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
        ((ObjectStoreInterMineImpl)os).precompute(q, "template");
        Assert.assertTrue(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
        ObjectStoreBag osb = storeDataWriter.createObjectStoreBag();
        storeDataWriter.addToBag(osb, new Integer(5));
        Assert.assertTrue(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
        storeDataWriter.store(data.get("EmployeeA1"));
        Assert.assertFalse(((ObjectStoreInterMineImpl)os).isPrecomputed(q,"template"));
    }

    @Test
    public void testObjectStoreBag() throws Exception {
        System.out.println("Starting testObjectStoreBag");
        ObjectStoreBag osb = storeDataWriter.createObjectStoreBag();
        ArrayList<Integer> coll = new ArrayList<Integer>();
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
        SingletonResults r = os.executeSingleton(q);
        Assert.assertEquals(Collections.EMPTY_LIST, r);
        q = new Query();
        q.addToSelect(osb);
        r = new SingletonResults(q, os, os.getSequence(os.getComponentsForQuery(q)));
        storeDataWriter.commitTransaction();
        try {
            r.get(0);
            Assert.fail("Expected: ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
        }
        q = new Query();
        q.addToSelect(osb);
        r = os.executeSingleton(q);
        Assert.assertEquals(coll, r);
        q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, osb));
        r = os.executeSingleton(q);
        Assert.assertEquals(Arrays.asList(new Object[] {data.get("EmployeeA1"), data.get("EmployeeA2")}), r);
        ObjectStoreBag osb2 = storeDataWriter.createObjectStoreBag();
        storeDataWriter.addToBag(osb2, ((Employee) data.get("EmployeeA1")).getId());
        storeDataWriter.addToBagFromQuery(osb2, q);
        q = new Query();
        q.addToSelect(osb2);
        r = os.executeSingleton(q);
        Assert.assertEquals(Arrays.asList(new Object[] {((Employee) data.get("EmployeeA1")).getId(), ((Employee) data.get("EmployeeA2")).getId()}), r);
    }

    @Test
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
        Assert.assertEquals(Collections.EMPTY_LIST, os.execute(q, 0, 1000, true, true, ObjectStore.SEQUENCE_IGNORE));
    }

    @Test
    public void testFailFast() throws Exception {
        Query q1 = new Query();
        ObjectStoreBag osb1 = new ObjectStoreBag(100);
        q1.addToSelect(osb1);
        Query q2 = new Query();
        ObjectStoreBag osb2 = new ObjectStoreBag(200);
        q2.addToSelect(osb2);
        Query q3 = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q3.addFrom(qc1);
        q3.addToSelect(qc1);

        Results r1 = os.execute(q1);
        Results r2 = os.execute(q2);
        Results r3 = os.execute(q3);
        storeDataWriter.addToBag(osb1, new Integer(1));
        try {
            r1.iterator().hasNext();
            Assert.fail("Expected: ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
        }
        r2.iterator().hasNext();
        r3.iterator().hasNext();

        r1 = new Results(q1, os, os.getSequence(os.getComponentsForQuery(q1)));
        r2 = new Results(q2, os, os.getSequence(os.getComponentsForQuery(q2)));
        r3 = new Results(q3, os, os.getSequence(os.getComponentsForQuery(q3)));
        storeDataWriter.addToBag(osb2, new Integer(2));
        r1.iterator().hasNext();
        try {
            r2.iterator().hasNext();
            Assert.fail("Expected: ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
        }
        r3.iterator().hasNext();

        r1 = new Results(q1, os, os.getSequence(os.getComponentsForQuery(q1)));
        r2 = new Results(q2, os, os.getSequence(os.getComponentsForQuery(q2)));
        r3 = new Results(q3, os, os.getSequence(os.getComponentsForQuery(q3)));
        storeDataWriter.store((Employee) data.get("EmployeeA1"));
        r1.iterator().hasNext();
        r2.iterator().hasNext();
        try {
            r3.iterator().hasNext();
            Assert.fail("Expected: ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
        }
    }

    /**
     * Test that running the same query multiple times hits the results cache if other
     * parameters are set appropriately
     */
    @Test
    public void testBatchesCache() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Results r1 = os.execute(q, 1000, true, true, true);
        Results r2 = os.execute(q, 1000, true, true, true);
        // same query executed with same parameters should give a cache hit
        Assert.assertTrue(r1 == r2);
        // prefetch is off for r3 so no results cache hit but batches (the actual results) should
        // be the same whatever
        Results r3 = os.execute(q, 1000, true, false, false);
        Assert.assertTrue(r1 != r3);
        Assert.assertTrue(r1.getResultsBatches() == r3.getResultsBatches());
        Assert.assertTrue(!r1.isSingleBatch());
        Assert.assertTrue(!r3.isSingleBatch());
        r1.get(0);
        Assert.assertTrue(r1.isSingleBatch());
        Assert.assertTrue(r3.isSingleBatch());
        Results r4 = os.execute(q, 500, true, true, true);
        Assert.assertTrue(r1.getResultsBatches() != r4.getResultsBatches());
        Assert.assertTrue(r4.isSingleBatch());
        Assert.assertEquals(new ArrayList<Object>(r1), new ArrayList<Object>(r4));
        SingletonResults r5 = os.executeSingleton(q, 400, true, true, true);
        Assert.assertTrue(r1.getResultsBatches() != r5.getResultsBatches());
        Assert.assertTrue(r5.isSingleBatch());
    }

    @Test
    public void testBatchesCacheSmallToLarge() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Company.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Results r1 = os.execute(q, 1, false, false, false);
        r1.get(0);
        Results r2 = os.execute(q, 100, false, false, false);
        r2.get(0);
        Assert.assertNotNull(r2.get(1));
    }

    @Test
    public void testBatchesFavourFilled() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(CEO.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Results r1 = os.execute(q, 100, false, false, false);
        Results r2 = os.execute(q, 101, false, false, false);
        Assert.assertTrue(!r1.isSingleBatch());
        Assert.assertTrue(!r2.isSingleBatch());
        r1.get(0);
        Assert.assertTrue(r1.isSingleBatch());
        Assert.assertTrue(!r2.isSingleBatch());
        Results r3 = os.execute(q, 102, false, false, false);
        Assert.assertTrue(r3.isSingleBatch());
    }
}
