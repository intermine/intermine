package org.flymine.objectstore;

import junit.framework.TestCase;

import java.lang.reflect.Field;

import org.flymine.objectstore.ObjectStoreQueriesTestCase;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.model.testmodel.*;

import org.flymine.objectstore.query.*;

public class ObjectStoreWriterTestCase extends TestCase
{
    public ObjectStoreWriterTestCase(String arg) {
        super(arg);
    }

    protected ObjectStore os;
    protected ObjectStoreWriter writer;

    protected Address address1;
    protected Company company1, company2;
    protected Department department1;
    protected CEO employee1;
    protected Contractor contractor1, contractor2, contractor3, contractor4;

    protected Address addressTemplate;
    protected Company companyTemplate;
    protected Department departmentTemplate;
    protected CEO employeeTemplate;
    protected Contractor contractorTemplate, contractor3Template;

    public void setUp() throws Exception {
        super.setUp();

        address1 = new Address();
        address1.setAddress("Employee Street, BVille");

        company1 = new Company();
        company1.setName("Company 1");
        company1.setAddress(address1);

        company2 = new Company();
        company2.setName("Company 2");
        company2.setAddress(address1);

        department1 = new Department();
        department1.setName("Dept1");
        department1.setCompany(company1);

        employee1 = new CEO();
        employee1.setName("EmployeeB1");
        employee1.setFullTime(true);
        employee1.setAddress(address1);
        employee1.setAge(40);
        employee1.setTitle("Mr.");
        employee1.setSalary(45000);
        employee1.setDepartment(department1);

        contractor1 = new Contractor();
        contractor1.setName("Contractor 1");
        contractor1.setBusinessAddress(address1);
        contractor1.setPersonalAddress(address1);

        contractor2 = new Contractor();
        contractor2.setName("Contractor 2");
        contractor2.setBusinessAddress(address1);
        contractor2.setPersonalAddress(address1);

        contractor3 = new Contractor();
        contractor3.setName("Contractor 3");
        contractor3.setBusinessAddress(address1);
        contractor3.setPersonalAddress(address1);

        contractor4 = new Contractor();
        contractor4.setName("Contractor 4");
        contractor4.setBusinessAddress(address1);
        contractor4.setPersonalAddress(address1);

        addressTemplate = new Address();
        addressTemplate.setAddress("Employee Street, BVille");

        companyTemplate = new Company();
        companyTemplate.setName("Company 1");
        companyTemplate.setAddress(addressTemplate);

        departmentTemplate = new Department();
        departmentTemplate.setName("Dept1");
        departmentTemplate.setCompany(companyTemplate);

        employeeTemplate = new CEO();
        employeeTemplate.setName("EmployeeB1");
        employeeTemplate.setAddress(addressTemplate);
        employeeTemplate.setAge(40);

        contractorTemplate = new Contractor();
        contractorTemplate.setName("Contractor 1");
        contractorTemplate.setBusinessAddress(addressTemplate);
        contractorTemplate.setPersonalAddress(addressTemplate);

        contractor3Template = new Contractor();
        contractor3Template.setName("Contractor 3");
        contractor3Template.setBusinessAddress(addressTemplate);
        contractor3Template.setPersonalAddress(addressTemplate);

    }


    /**
     * Storing an object without a primary key should not be allowed
     */
    public void testStoreObjectWithInvalidKey() throws Exception {
        Address address = new Address();
        try {
            writer.store(address);
            fail("Expected: ObjectStoreException");
        } catch (ObjectStoreException e) {
        }
    }


    /**
     * Storing an object without an ID field should insert it into the database
     */
    public void testStoreObjectWithAttributeKeyNotAlreadyExists() throws Exception {

        try {
            writer.store(address1);

            // It should now have its ID field set
            assertTrue(address1.getId() != null);

            // Can we get it out again?
            Object returned = writer.getObjectByExample(addressTemplate);

            assertEquals(address1, returned);
        } finally {
            writer.delete(address1);
        }
    }


    /**
     * Changing a non-primary key attribute on an object should update it in the database
     */
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
            Object returned = writer.getObjectByExample(employeeTemplate);
            assertFalse(((Employee) returned).getFullTime());

        } finally {
            writer.delete(address1);
            writer.delete(employee1);
        }
    }

    /**
     * Storing an object with a valid primary key should store it and set its ID field
     */
    public void testStoreObjectWithAttributeReferenceKey() throws Exception {

        try {
            writer.store(address1);
            writer.store(employee1);

            // It should now have its ID field set
            assertTrue(employee1.getId() != null);

            // Can we get it out again?
            Object returned = writer.getObjectByExample(employeeTemplate);

            assertEquals(employee1, returned);
            assertEquals(address1, ((Employee) returned).getAddress());
        } finally {
            writer.delete(address1);
            writer.delete(employee1);
        }
    }


    /**
     * Test referenced object including updating and setting to null
     */
    public void testStoreObjectWithReferencedObject() throws Exception {

        Department department2 = null;
        Department department3 = null;

        try {
            writer.store(address1);
            writer.store(company1);
            writer.store(department1);
            writer.store(employee1);

            // Check that the department is referenced
            Object returned = writer.getObjectByExample(employeeTemplate);

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
            returned = writer.getObjectByExample(employeeTemplate);

            assertEquals(employee1, returned);
            assertEquals(department2, ((Employee) returned).getDepartment());

            // Set department to null
            employee1.setDepartment(null);
            writer.store(employee1);

            // Check that the department is not referenced
            returned = writer.getObjectByExample(employeeTemplate);

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
            returned = writer.getObjectByExample(employeeTemplate);

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


    /**
     * Test collections including updating and setting to null
     */
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

            Company returnedCompany = (Company) writer.getObjectByExample(companyTemplate);

            assertNotNull(returnedCompany);
            assertEquals(1, returnedCompany.getDepartments().size());
            assertTrue(returnedCompany.getDepartments().contains(department2));

            // Add another
            company1.getDepartments().add(department1);
            writer.store(company1);
            writer.store(department1);

            returnedCompany = (Company) writer.getObjectByExample(companyTemplate);
            assertEquals(2, returnedCompany.getDepartments().size());
            assertTrue(returnedCompany.getDepartments().contains(department1));
            assertTrue(returnedCompany.getDepartments().contains(department2));

            // Remove one
            company1.getDepartments().remove(department1);
            department1.setCompany(company2);
            writer.store(company1);
            writer.store(department1);

            returnedCompany = (Company) writer.getObjectByExample(companyTemplate);
            assertEquals(1, returnedCompany.getDepartments().size());
            assertTrue(returnedCompany.getDepartments().contains(department2));

            // Set to empty collection in original object - department links should disappear

            company1.getDepartments().clear();
            department2.setCompany(company2);
            writer.store(department2);
            writer.store(company1);

            returnedCompany = (Company) writer.getObjectByExample(companyTemplate);
            assertEquals(0, returnedCompany.getDepartments().size());

        } finally {
            writer.delete(department1);
            writer.delete(department2);
            writer.delete(company1);
            writer.delete(address1);
        }

    }

    /**
     * Test collections including updating and setting to null
     */
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

            // Check we have collections filled on both sides

            Company returnedCompany = (Company) writer.getObjectByExample(companyTemplate);

            assertNotNull(returnedCompany);
            assertEquals(2, returnedCompany.getContractors().size());
            assertTrue(returnedCompany.getContractors().contains(contractor1));
            assertTrue(returnedCompany.getContractors().contains(contractor2));

            Contractor returnedContractor = (Contractor) writer.getObjectByExample(contractorTemplate);

            assertNotNull(returnedContractor);
            assertEquals(2, returnedContractor.getCompanys().size());
            assertTrue(returnedContractor.getCompanys().contains(company1));
            assertTrue(returnedContractor.getCompanys().contains(company2));

            // Add a contractor to company1's collection
            company1.getContractors().add(contractor3);
            contractor3.getCompanys().add(company1);
            writer.store(company1);
            returnedCompany = (Company) writer.getObjectByExample(companyTemplate);

            assertNotNull(returnedCompany);
            assertEquals(3, returnedCompany.getContractors().size());
            assertTrue(returnedCompany.getContractors().contains(contractor1));
            assertTrue(returnedCompany.getContractors().contains(contractor2));
            assertTrue(returnedCompany.getContractors().contains(contractor3));

            returnedContractor = (Contractor) writer.getObjectByExample(contractor3Template);

            assertNotNull(returnedContractor);
            assertEquals(1, returnedContractor.getCompanys().size());
            assertTrue(returnedContractor.getCompanys().contains(company1));

            // Delete a contractor from company1's collection
            company1.getContractors().remove(contractor2);
            writer.store(company1);
            returnedCompany = (Company) writer.getObjectByExample(companyTemplate);

            assertNotNull(returnedCompany);
            assertEquals(2, returnedCompany.getContractors().size());
            assertTrue(returnedCompany.getContractors().contains(contractor1));
            assertTrue(returnedCompany.getContractors().contains(contractor3));


        } finally {
            writer.delete(contractor1);
            writer.delete(contractor2);
            writer.delete(contractor3);
            writer.delete(contractor4);
            writer.delete(company1);
            writer.delete(company2);
            writer.delete(address1);
        }

    }

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
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.OR);
        cs1.addConstraint(new SimpleConstraint(qf, SimpleConstraint.MATCHES, new QueryValue("Address%")));
        q.addToSelect(qcAddress);
        q.addFrom(qcAddress);
        q.addToOrderBy(qf);
        q.setConstraint(cs1);

        try {
            writer.beginTransaction();
            assertTrue(writer.isInTransaction());

            writer.store(address1);
            writer.store(address2);

            // Should be nothing there until we commit
            Results res = os.execute(q);
            assertEquals(0, res.size());

            writer.commitTransaction();
            assertFalse(writer.isInTransaction());
            res = os.execute(q);
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
        address1.setAddress("Address 1");
        Address address2 = new Address();
        address2.setAddress("Address 2");

        Query q = new Query();
        QueryClass qcAddress = new QueryClass(Address.class);
        QueryField qf = new QueryField(qcAddress, "address");
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.OR);
        cs1.addConstraint(new SimpleConstraint(qf, SimpleConstraint.MATCHES, new QueryValue("Address%")));
        q.addToSelect(qcAddress);
        q.addFrom(qcAddress);
        q.addToOrderBy(qf);
        q.setConstraint(cs1);

        writer.beginTransaction();
        assertTrue(writer.isInTransaction());

        writer.store(address1);
        writer.store(address2);

        writer.abortTransaction();
        assertFalse(writer.isInTransaction());

        // Should be nothing there until we commit
        Results res = os.execute(q);
        assertEquals(0, res.size());
    }

    /**
     * Test that we can get back one of the data objects by example
     */
    public void testGetByExampleExistingObject() throws Exception {

        try {
            writer.store(address1);
            writer.store(company1);
            writer.store(employee1);

            Object returned = writer.getObjectByExample(employeeTemplate);

            assertTrue(returned instanceof CEO);
            assertEquals(employee1, returned);

            returned = writer.getObjectByExample(companyTemplate);

            assertTrue(returned instanceof Company);
            assertEquals(company1, returned);
        } finally {
            writer.delete(employee1);
            writer.delete(company1);
            writer.delete(address1);
        }

    }

    public void testGetByExampleNonExistentObject() throws Exception {
        Object returned = writer.getObjectByExample(employeeTemplate);
        assertNull(returned);
    }

    public void testGetByExampleKeysNotSet() throws Exception {
        Employee employee = new Employee();
        employee.setName("EmployeeB1");
        employee.setFullTime(true);
        employee.setAge(40);
        // Address not set

        try {
            writer.getObjectByExample(employee);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetByExampleNullObject() throws Exception {
        try {
            writer.getObjectByExample(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
