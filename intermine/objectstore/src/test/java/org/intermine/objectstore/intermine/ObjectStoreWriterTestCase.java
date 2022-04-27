package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.util.*;

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.*;
import org.intermine.objectstore.*;
import org.intermine.objectstore.query.*;
import org.intermine.util.DynamicUtil;
import org.junit.*;

import static org.junit.Assert.fail;

public class ObjectStoreWriterTestCase
{
    protected static ObjectStoreWriter writer;
    protected static ObjectStore os;
    protected static Map data;

    private boolean finished = false;
    private Throwable failureException = null;

    public static void oneTimeSetUp(ObjectStoreWriter writer) throws Exception {
        ObjectStoreWriterTestCase.writer = writer;
        ObjectStoreTestUtils.deleteAllObjectsInStore(writer);

        os = writer.getObjectStore();
        data = ObjectStoreTestUtils.getTestData("testmodel", "testmodel_data.xml");
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        writer.close();
    }

    @After
    public void tearDown() throws Exception {
        ObjectStoreTestUtils.deleteAllObjectsInStore(writer);
    }

    /**
     * Test that transactions do actually commit and that isInTransaction() works.
     */
    @Test
    public void testCommitTransactions() throws Exception {
        Address address1 = new Address();
        address1.setAddress("Address 1");
        Address address2 = new Address();
        address2.setAddress("Address 2");

        Query q = new Query();
        QueryClass qcAddress = new QueryClass(Address.class);
        QueryField qf = new QueryField(qcAddress, "address");
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        cs1.addConstraint(new SimpleConstraint(qf, ConstraintOp.MATCHES, new QueryValue("Address%")));
        q.addToSelect(qcAddress);
        q.addFrom(qcAddress);
        q.addToOrderBy(qf);
        q.setConstraint(cs1);

        try {
            writer.beginTransaction();
            Assert.assertTrue(writer.isInTransaction());

            writer.store(address1);
            writer.store(address2);

            ObjectStore os = writer.getObjectStore();

            // Should be nothing in OS until we commit
            Results res = os.execute(q);
            Assert.assertEquals(0, res.size());

            // However, they should be in the WRITER.
            // TODO: These lines now fail, because we do not allow querying on writers with uncommitted data. The writer should relax this restriction.
            res = writer.execute(q);
            Assert.assertEquals(2, res.size());

            writer.commitTransaction();
            Assert.assertFalse(writer.isInTransaction());
            res = os.execute(q);
            Assert.assertEquals(2, res.size());
            Assert.assertEquals(address1, ((ResultsRow) res.get(0)).get(0));
            Assert.assertEquals(address2, ((ResultsRow) res.get(1)).get(0));

        } finally {
            writer.delete(address1);
            writer.delete(address2);
        }
    }

    /**
     * Test that transactions can be aborted
     */
    @Test
    public void testAbortTransactions() throws Exception {
        Address address1 = new Address();
        address1.setAddress("Address 3");
        Address address2 = new Address();
        address2.setAddress("Address 4");

        Query q = new Query();
        QueryClass qcAddress = new QueryClass(Address.class);
        QueryField qf = new QueryField(qcAddress, "address");
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        cs1.addConstraint(new SimpleConstraint(qf, ConstraintOp.MATCHES, new QueryValue("Address%")));
        q.addToSelect(qcAddress);
        q.addFrom(qcAddress);
        q.addToOrderBy(qf);
        q.setConstraint(cs1);

        Results res = writer.execute(q);
        Assert.assertEquals(res.toString(), 0, res.size());

        res = os.execute(q);
        Assert.assertEquals(res.toString(), 0, res.size());

        writer.beginTransaction();
        Assert.assertTrue(writer.isInTransaction());

        writer.store(address1);
        writer.store(address2);

        // TODO: These lines now fail, because we do not allow querying on writers with uncommitted data. The writer should relax this restriction.
        res = writer.execute(q);
        Assert.assertEquals(2, res.size());

        res = os.execute(q);
        Assert.assertEquals(res.toString(), 0, res.size());

        writer.abortTransaction();
        Assert.assertFalse(writer.isInTransaction());

        // Should be nothing there unless we commit

        res = writer.execute(q);
        Assert.assertEquals(res.toString(), 0, res.size());

        res = os.execute(q);
        Assert.assertEquals(res.toString(), 0, res.size());
    }

    @Test
    public void testTransactionsAndCaches() throws Exception {
        Address address1 = new Address();
        address1.setAddress("Address 1");
        Address address2 = new Address();
        address2.setAddress("Address 2");

        writer.flushObjectById();
        os.flushObjectById();

        try {
            writer.store(address1);
            Integer id = address1.getId();
            address2.setId(id);

            Assert.assertNull(os.pilferObjectById(id));
            Assert.assertNull(writer.pilferObjectById(id));

            Assert.assertNotNull("Looked for id " + id, os.getObjectById(id, Address.class));
            Assert.assertNull(writer.pilferObjectById(id));
            Assert.assertNotNull(os.pilferObjectById(id));
            os.flushObjectById();

            Assert.assertNotNull(writer.getObjectById(id, Address.class));
            Assert.assertNotNull(writer.pilferObjectById(id));
            Assert.assertNull(os.pilferObjectById(id));
            Assert.assertNotNull(os.getObjectById(id, Address.class));

            writer.store(address2);
            Assert.assertNotNull(writer.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            Assert.assertNotNull(os.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) os.getObjectById(id, Address.class)).getAddress());

            writer.delete(address2);
            Assert.assertNull(writer.getObjectById(id, Address.class));
            Assert.assertNull(os.getObjectById(id, Address.class));

            writer.store(address1);
            writer.beginTransaction();
            writer.store(address2);
            Assert.assertNotNull(writer.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            Assert.assertNotNull(os.getObjectById(id, Address.class));
            Assert.assertEquals("Address 1", ((Address) os.getObjectById(id, Address.class)).getAddress());

            writer.commitTransaction();
            Assert.assertNotNull(writer.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            Assert.assertNotNull(os.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) os.getObjectById(id, Address.class)).getAddress());

            writer.beginTransaction();
            writer.delete(address1);
            Assert.assertNull(writer.getObjectById(id, Address.class));
            Assert.assertNotNull(os.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) os.getObjectById(id, Address.class)).getAddress());

            writer.abortTransaction();
            Assert.assertNotNull(writer.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            Assert.assertNotNull(os.getObjectById(id, Address.class));
            Assert.assertEquals("Address 2", ((Address) os.getObjectById(id, Address.class)).getAddress());
        } finally {
            writer.delete(address1);
        }
    }

    @Test
    public void testWriteBatchingAndGetObject() throws Exception {
        Address address1 = new Address();
        address1.setAddress("Address 1");

        writer.flushObjectById();
        os.flushObjectById();

        try {
            writer.beginTransaction();
            writer.store(address1);
            Assert.assertNotNull(writer.getObjectById(address1.getId(), Address.class));
        } finally {
            if (writer.isInTransaction()) {
                writer.abortTransaction();
            }
        }
    }

    @Test
    public void testWriteDynamicObject() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Company.class, Employee.class})));
        try {
            writer.store(o);
            o = writer.getObjectById(o.getId(), Employee.class);
            if (!(o instanceof Company)) {
                fail("Expected a Company back");
            }
            if (!(o instanceof Employee)) {
                fail("Expected an Employee back");
            }
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteDynamicObject2() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {ImportantPerson.class, Employee.class})));
        try {
            writer.store(o);
            o = writer.getObjectById(o.getId(), Employee.class);
            if (!(o instanceof ImportantPerson)) {
                fail("Expected an ImportantPerson back");
            }
            if (!(o instanceof Employee)) {
                fail("Expected an Employee back");
            }
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteInterMineObject() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(Collections.singleton(InterMineObject.class));
        try {
            writer.store(o);
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteCleaner() throws Exception {
        InterMineObject o = new Cleaner();
        try {
            writer.store(o);
            o = writer.getObjectById(o.getId(), Employee.class);
            if (!(o instanceof Cleaner)) {
                fail("Expected a Cleaner back");
            }
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteCloneable() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(new HashSet(Arrays.asList(new Class[] {Employee.class, Cloneable.class})));
        try {
            writer.store(o);
            o = writer.getObjectById(o.getId(), Employee.class);
            if (!(o instanceof Cloneable)) {
                fail("Expected a Cloneable back");
            }
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testWriteBigDepartment() throws Exception {
        InterMineObject o = new BigDepartment();
        try {
            writer.store(o);
            o = writer.getObjectById(o.getId(), Department.class);
            if (!(o instanceof BigDepartment)) {
                fail("Expected a BigDepartment back");
            }
        } finally {
            writer.delete(o);
        }
    }

    @Test
    public void testAddToCollection() throws Exception {
        Company c1 = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
        Contractor c2 = new Contractor();
        c1.setName("Michael");
        c2.setName("Albert");

        try {
            writer.store(c1);
            writer.store(c2);

            Company c3 = (Company) writer.getObjectById(c1.getId(), Company.class);
            Assert.assertEquals(0, c3.getContractors().size());

            writer.addToCollection(c1.getId(), Company.class, "contractors", c2.getId());

            c3 = (Company) writer.getObjectById(c1.getId(), Company.class);
            Assert.assertEquals(1, c3.getContractors().size());
            Assert.assertTrue(c3.getContractors().iterator().next() instanceof Contractor);
            Assert.assertEquals(c2.getId(), ((Contractor) c3.getContractors().iterator().next()).getId());
        } finally {
            writer.delete(c1);
            writer.delete(c2);
        }
    }

    @Test
    public void testFailFast() throws Exception {
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Employee.class);
        q1.addFrom(qc1);
        q1.addToSelect(qc1);

        Results r1 = writer.execute(q1);
        writer.store(data.get("EmployeeA1"));
        try {
            r1.iterator().hasNext();
            fail("Expected: ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
        }
    }

    @Test
    public void testClob() throws Exception {
        Clob clob = writer.createClob();
        writer.replaceClob(clob, "Monkey");
        ClobAccess ca = new ClobAccess(writer, clob);
        Assert.assertEquals("Monkey", ca.toString());
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longString.append("Lots of monkeys. ");
        }
        writer.replaceClob(clob, longString.toString());
        Assert.assertEquals("Monkey", ca.toString());
        ca = new ClobAccess(writer, clob);
        Assert.assertEquals(170000, ca.length());
        Assert.assertEquals(longString.toString(), ca.toString());
        Assert.assertEquals('L', ca.charAt(1700));
        Assert.assertEquals('L', ca.charAt(16983));
        ClobAccess sub = ca.subSequence(85000, 85016);
        Assert.assertEquals("Lots of monkeys.", sub.toString());
        Assert.assertEquals(16, sub.length());
    }

    @Test
    public void testRapidShutdown() throws Exception {
        Thread t = new Thread(new ShutdownThread());
        t.start();
        synchronized (this) {
            try {
                wait(5000);
            } catch (InterruptedException e) {
            }
            Assert.assertTrue(finished);
            if (failureException != null) {
                throw new Exception(failureException);
            }
        }
    }

    public synchronized void signalFinished(Throwable e) {
        finished = true;
        failureException = e;
        notifyAll();
    }

    @Ignore
    private class ShutdownThread implements Runnable {
        public void run() {
            try {
                ObjectStoreWriterInterMineImpl w = (ObjectStoreWriterInterMineImpl)ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
                Connection c = w.getConnection();
                try {
                    w.close();
                    fail("Expected an ObjectStoreException");
                } catch (ObjectStoreException e) {
                    Assert.assertEquals("Closed ObjectStoreWriter while it is being used. Note this writer will be automatically closed when the current operation finishes", e.getMessage());
                }
                w.releaseConnection(c);
                signalFinished(null);
            } catch (Throwable e) {
                System.out.println("Error in ShutdownThread: " + e);
                signalFinished(e);
            }
        }
    }
}
