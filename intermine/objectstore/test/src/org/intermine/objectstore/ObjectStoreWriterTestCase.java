package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ConcurrentModificationException;

import org.intermine.metadata.ConstraintOp;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.BigDepartment;
import org.intermine.model.testmodel.Cleaner;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Contractor;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.ClobAccess;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.util.DynamicUtil;

public class ObjectStoreWriterTestCase extends ObjectStoreAbstractImplTestCase
{
    protected static ObjectStoreWriter writer;
    protected static ObjectStore realOs;

    public ObjectStoreWriterTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        os = writer;
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
        realOs = writer.getObjectStore();
    }

    public void setUp() throws Exception {
        super.setUp();
        if (writer.isInTransaction()) {
            writer.abortTransaction();
        }
    }
    /**
     * Storing an object without an ID field should insert it into the database
     */
    /* TODO: Move to dataloader
    public void testStoreObjectWithAttributeKeyNotAlreadyExists() throws Exception {

        try {
            writer.store(address1);

            // It should now have its ID field set
            assertTrue(address1.getId() != null);

            // Can we get it out again?
            Object returned = os.getObjectByExample(address1Template);

            assertEquals(address1, returned);
        } finally {
            writer.delete(address1);
        }
    }
    */

    /**
     * Changing a non-primary key attribute on an object should update it in the database
     */
    /* TODO: Move to dataloader
    public void testStoreObjectWithAttributeKeyAlreadyExists() throws Exception {

        try {
            // Store it in there to begin with
            writer.store(address1);
            writer.store(employee1);

            // It should now have its ID field set
            assertTrue(address1.getId() != null);
            assertTrue(employee1.getId() != null);

            employee1.setFullTime(false);
            writer.store(employee1);

            // Can we get it out again?
            Object returned = os.getObjectByExample(employee1Template);
            assertFalse(((Employee) returned).getFullTime());

        } finally {
            writer.delete(address1);
            writer.delete(employee1);
        }
    }
    */

    /**
     * Storing an object with a valid primary key should store it and set its ID field
     */
    /* TODO: Move to dataloader
    public void testStoreObjectWithAttributeReferenceKey() throws Exception {

        try {
            writer.store(address1);
            writer.store(employee1);

            // It should now have its ID field set
            assertTrue(employee1.getId() != null);

            // Can we get it out again?
            Object returned = os.getObjectByExample(employee1Template);

            assertEquals(employee1, returned);
            assertEquals(address1, ((Employee) returned).getAddress());
        } finally {
            writer.delete(address1);
            writer.delete(employee1);
        }
    }
    */

    /**
     * Test referenced object including updating and setting to null
     */
    /* TODO: Move to dataloader
    public void testStoreObjectWithReferencedObject() throws Exception {

        Department department2 = null;
        Department department3 = null;

        try {
            writer.store(address1);
            writer.store(company1);
            writer.store(department1);
            writer.store(employee1);

            // Check that the department is referenced
            Object returned = os.getObjectByExample(employee1Template);

            assertEquals(employee1, returned);
            assertEquals(department1, ((Employee) returned).getDepartment());

            department2 = new Department();
            department2.setName("Department 2");
            department2.setCompany(company1);
            writer.store(department2);

            // Override the department
            employee1.setDepartment(department2);
            writer.store(employee1);

            // Check that the new department is referenced
            returned = os.getObjectByExample(employee1Template);

            assertEquals(employee1, returned);
            assertEquals(department2, ((Employee) returned).getDepartment());

            // Set department to null
            employee1.setDepartment(null);
            writer.store(employee1);

            // Check that the department is not referenced
            returned = os.getObjectByExample(employee1Template);

            assertEquals(employee1, returned);
            assertEquals(null, ((Employee) returned).getDepartment());


            // Store a reference to department that is not yet in the database
            department3 = new Department();
            department3.setName("Department 3 (not in db)");
            department3.setCompany(company1);
            employee1.setDepartment(department3);
            writer.store(employee1);

            // Now store department3
            writer.store(department3);

            // Check that the department3 is referenced, even though it was not
            // previously in database
            returned = os.getObjectByExample(employee1Template);

            assertEquals(employee1, returned);
            assertEquals(department3, ((Employee) returned).getDepartment());


        } finally {
            writer.delete(employee1);
            writer.delete(department1);
            writer.delete(department2);
            writer.delete(department3);
            writer.delete(company1);
            writer.delete(address1);
        }
    }
    */

    /**
     * Test collections including updating and setting to null
     */
    /* TODO: Move to dataloader
    public void testStoreObjectWith1NCollection() throws Exception {

        Department department2 = null;

        try {
            department2 = new Department();
            department2.setName("Department 2");
            department2.setCompany(company1);


            // Add one department to collection
            company1.getDepartments().add(department2);
            writer.store(address1);
            writer.store(company1);
            writer.store(department2);

            Company returnedCompany = (Company) os.getObjectByExample(company1Template);

            assertNotNull(returnedCompany);
            assertEquals(1, returnedCompany.getDepartments().size());
            assertTrue(returnedCompany.getDepartments().contains(department2));

            // Add another
            company1.getDepartments().add(department1);
            writer.store(company1);
            writer.store(department1);

            returnedCompany = (Company) os.getObjectByExample(company1Template);
            assertEquals(2, returnedCompany.getDepartments().size());
            assertTrue(returnedCompany.getDepartments().contains(department1));
            assertTrue(returnedCompany.getDepartments().contains(department2));

            // Remove one
            company1.getDepartments().remove(department1);
            department1.setCompany(company2);
            writer.store(company1);
            writer.store(department1);

            returnedCompany = (Company) os.getObjectByExample(company1Template);
            assertEquals(1, returnedCompany.getDepartments().size());
            assertTrue(returnedCompany.getDepartments().contains(department2));

            // Set to empty collection in original object - department links should disappear

            company1.getDepartments().clear();
            department2.setCompany(company2);
            writer.store(department2);
            writer.store(company1);

            returnedCompany = (Company) os.getObjectByExample(company1Template);
            assertEquals(0, returnedCompany.getDepartments().size());

        } finally {
            writer.delete(department1);
            writer.delete(department2);
            writer.delete(company1);
            writer.delete(address1);
        }
    }
    */

    /**
     * Test collections including updating and setting to null
     */
    /* TODO: Move to dataloader
    public void testStoreObjectWithMNCollection() throws Exception {

        try {

            // Add contractors to companies
            company1.getContractors().add(contractor1);
            company2.getContractors().add(contractor1);
            company1.getContractors().add(contractor2);
            company2.getContractors().add(contractor2);

            writer.store(address1);
            writer.store(contractor1);
            writer.store(contractor2);
            writer.store(contractor3);
            writer.store(company1);
            writer.store(company2);

            os.flushObjectByExample();

            // Check we have collections filled on both sides

            Company returnedCompany = (Company) os.getObjectByExample(company1Template);

            assertNotNull(returnedCompany);
            assertEquals(2, returnedCompany.getContractors().size());
            assertTrue(returnedCompany.getContractors().contains(contractor1));
            assertTrue(returnedCompany.getContractors().contains(contractor2));

            Contractor returnedContractor = (Contractor) os.getObjectByExample(contractor1Template);

            assertNotNull(returnedContractor);
            assertEquals(2, returnedContractor.getCompanys().size());
            assertTrue(returnedContractor.getCompanys().contains(company1));
            assertTrue(returnedContractor.getCompanys().contains(company2));

            // Add a contractor to company1's collection
            company1.getContractors().add(contractor3);
            contractor3.getCompanys().add(company1);
            writer.store(company1);
            os.flushObjectByExample();
            returnedCompany = (Company) os.getObjectByExample(company1Template);

            assertNotNull(returnedCompany);
            assertEquals(3, returnedCompany.getContractors().size());
            assertTrue(returnedCompany.getContractors().contains(contractor1));
            assertTrue(returnedCompany.getContractors().contains(contractor2));
            assertTrue(returnedCompany.getContractors().contains(contractor3));

            returnedContractor = (Contractor) os.getObjectByExample(contractor3Template);

            assertNotNull(returnedContractor);
            assertEquals(1, returnedContractor.getCompanys().size());
            assertTrue(returnedContractor.getCompanys().contains(company1));

            // Delete a contractor from company1's collection
            company1.getContractors().remove(contractor2);
            writer.store(company1);
            os.flushObjectByExample();
            returnedCompany = (Company) os.getObjectByExample(company1Template);

            assertNotNull(returnedCompany);
            assertEquals(2, returnedCompany.getContractors().size());
            assertTrue(returnedCompany.getContractors().contains(contractor1));
            assertTrue(returnedCompany.getContractors().contains(contractor3));


        } finally {
            writer.delete(contractor1);
            writer.delete(contractor2);
            writer.delete(contractor3);
            writer.delete(company1);
            writer.delete(company2);
            writer.delete(address1);
        }
    }
    */

    /**
     * Test that transactions do actually commit and that isInTransaction() works.
     */
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
            assertTrue(writer.isInTransaction());

            writer.store(address1);
            writer.store(address2);

            // Should be nothing in OS until we commit
            Results res = realOs.execute(q);
            assertEquals(0, res.size());

            // However, they should be in the WRITER.
            // TODO: These lines now fail, because we do not allow querying on writers with uncommitted data. The writer should relax this restriction.
            res = writer.execute(q);
            assertEquals(2, res.size());

            writer.commitTransaction();
            assertFalse(writer.isInTransaction());
            res = realOs.execute(q);
            assertEquals(2, res.size());
            assertEquals(address1, (Address) ((ResultsRow) res.get(0)).get(0));
            assertEquals(address2, (Address) ((ResultsRow) res.get(1)).get(0));

        } finally {
            writer.delete(address1);
            writer.delete(address2);
        }
    }

    /**
     * Test that transactions can be aborted
     */
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
        assertEquals(res.toString(), 0, res.size());

        res = realOs.execute(q);
        assertEquals(res.toString(), 0, res.size());

        writer.beginTransaction();
        assertTrue(writer.isInTransaction());

        writer.store(address1);
        writer.store(address2);

        // TODO: These lines now fail, because we do not allow querying on writers with uncommitted data. The writer should relax this restriction.
        res = writer.execute(q);
        assertEquals(2, res.size());

        res = realOs.execute(q);
        assertEquals(res.toString(), 0, res.size());

        writer.abortTransaction();
        assertFalse(writer.isInTransaction());

        // Should be nothing there unless we commit

        res = writer.execute(q);
        assertEquals(res.toString(), 0, res.size());

        res = realOs.execute(q);
        assertEquals(res.toString(), 0, res.size());
    }

    public void testTransactionsAndCaches() throws Exception {
        Address address1 = new Address();
        address1.setAddress("Address 1");
        Address address2 = new Address();
        address2.setAddress("Address 2");

        writer.flushObjectById();
        realOs.flushObjectById();

        try {
            writer.store(address1);
            Integer id = address1.getId();
            address2.setId(id);

            assertNull(realOs.pilferObjectById(id));
            assertNull(writer.pilferObjectById(id));

            assertNotNull("Looked for id " + id, realOs.getObjectById(id, Address.class));
            assertNull(writer.pilferObjectById(id));
            assertNotNull(realOs.pilferObjectById(id));
            realOs.flushObjectById();

            assertNotNull(writer.getObjectById(id, Address.class));
            assertNotNull(writer.pilferObjectById(id));
            assertNull(realOs.pilferObjectById(id));
            assertNotNull(realOs.getObjectById(id, Address.class));

            writer.store(address2);
            assertNotNull(writer.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            assertNotNull(realOs.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) realOs.getObjectById(id, Address.class)).getAddress());

            writer.delete(address2);
            assertNull(writer.getObjectById(id, Address.class));
            assertNull(realOs.getObjectById(id, Address.class));

            writer.store(address1);
            writer.beginTransaction();
            writer.store(address2);
            assertNotNull(writer.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            assertNotNull(realOs.getObjectById(id, Address.class));
            assertEquals("Address 1", ((Address) realOs.getObjectById(id, Address.class)).getAddress());

            writer.commitTransaction();
            assertNotNull(writer.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            assertNotNull(realOs.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) realOs.getObjectById(id, Address.class)).getAddress());

            writer.beginTransaction();
            writer.delete(address1);
            assertNull(writer.getObjectById(id, Address.class));
            assertNotNull(realOs.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) realOs.getObjectById(id, Address.class)).getAddress());

            writer.abortTransaction();
            assertNotNull(writer.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) writer.getObjectById(id, Address.class)).getAddress());
            assertNotNull(realOs.getObjectById(id, Address.class));
            assertEquals("Address 2", ((Address) realOs.getObjectById(id, Address.class)).getAddress());
        } finally {
            writer.delete(address1);
        }
    }

    public void testWriteBatchingAndGetObject() throws Exception {
        Address address1 = new Address();
        address1.setAddress("Address 1");

        writer.flushObjectById();
        realOs.flushObjectById();

        try {
            writer.beginTransaction();
            writer.store(address1);
            assertNotNull(writer.getObjectById(address1.getId(), Address.class));
        } finally {
            if (writer.isInTransaction()) {
                writer.abortTransaction();
            }
        }
    }


    public void testWriteInterMineObject() throws Exception {
        InterMineObject o = (InterMineObject) DynamicUtil.createObject(InterMineObject.class);
        try {
            writer.store(o);
        } finally {
            writer.delete(o);
        }
    }

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

    public void testAddToCollection() throws Exception {
        Company c1 = (Company) DynamicUtil.createObject(Company.class);
        Contractor c2 = new Contractor();
        c1.setName("Michael");
        c2.setName("Albert");

        try {
            writer.store(c1);
            writer.store(c2);

            Company c3 = (Company) writer.getObjectById(c1.getId(), Company.class);
            assertEquals(0, c3.getContractors().size());

            writer.addToCollection(c1.getId(), Company.class, "contractors", c2.getId());

            c3 = (Company) writer.getObjectById(c1.getId(), Company.class);
            assertEquals(1, c3.getContractors().size());
            assertTrue(c3.getContractors().iterator().next() instanceof Contractor);
            assertEquals(c2.getId(), ((Contractor) c3.getContractors().iterator().next()).getId());
        } finally {
            writer.delete(c1);
            writer.delete(c2);
        }
    }

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

    public void testClob() throws Exception {
        Clob clob = writer.createClob();
        writer.replaceClob(clob, "Monkey");
        ClobAccess ca = new ClobAccess(writer, clob);
        assertEquals("Monkey", ca.toString());
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longString.append("Lots of monkeys. ");
        }
        writer.replaceClob(clob, longString.toString());
        assertEquals("Monkey", ca.toString());
        ca = new ClobAccess(writer, clob);
        assertEquals(170000, ca.length());
        assertEquals(longString.toString(), ca.toString());
        assertEquals('L', ca.charAt(1700));
        assertEquals('L', ca.charAt(16983));
        ClobAccess sub = ca.subSequence(85000, 85016);
        assertEquals("Lots of monkeys.", sub.toString());
        assertEquals(16, sub.length());
    }
}
