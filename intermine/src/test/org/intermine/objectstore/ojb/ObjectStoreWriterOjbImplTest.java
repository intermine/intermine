package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreQueriesTestCase;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.sql.DatabaseFactory;
import org.flymine.model.testmodel.*;

public class ObjectStoreWriterOjbImplTest extends ObjectStoreQueriesTestCase
{
    public ObjectStoreWriterOjbImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        writer = new ObjectStoreWriterOjbImpl((ObjectStoreOjbImpl) ObjectStoreFactory.getObjectStore("os.unittest"));

        db = DatabaseFactory.getDatabase("db.unittest");

        storeData();
        // clear the cache to ensure that objects are materialised later (in case broker reused)
        ((ObjectStoreWriterOjbImpl) writer).pb.clearCache();
    }

    public void tearDown() throws Exception {
        removeDataFromStore();
    }

    // Not doing the Query tests here
    public void executeTest(String type) throws Exception {

    }

    /**
     * Test that we can get back one of the data objects by example
     */
    public void testGetByExampleExistingObject() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO employee = new CEO();
        employee.setName("EmployeeB1");
        employee.setFullTime(true);
        employee.setAddress(address);
        employee.setAge(40);
        employee.setTitle("Mr.");
        employee.setSalary(45000);

        Object returned = writer.getObjectByExample(employee);

        assertTrue(returned instanceof CEO);
        assertEquals(data.get("EmployeeB1"), returned);

    }

    public void testGetByExampleNonExistentObject() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        Employee employee = new Employee();
        employee.setName("EmployeeNotThere");
        employee.setFullTime(true);
        employee.setAddress(address);
        employee.setAge(40);

        Object returned = writer.getObjectByExample(employee);

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
