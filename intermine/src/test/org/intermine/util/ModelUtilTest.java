package org.flymine.util;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class ModelUtilTest extends TestCase
{
    public ModelUtilTest(String arg) {
        super(arg);
    }

    public void testGetFieldTypeCollection() {
        assertEquals(ModelUtil.COLLECTION, ModelUtil.getFieldType(Company.class, "departments"));
    }

    public void testGetFieldTypeAttribute() {
        assertEquals(ModelUtil.ATTRIBUTE, ModelUtil.getFieldType(Company.class, "name"));
    }

    public void testGetFieldTypeReference() {
        assertEquals(ModelUtil.REFERENCE, ModelUtil.getFieldType(Company.class, "address"));
    }


    public void testCheckKeyValidObject() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO employee = new CEO();
        employee.setName("EmployeeB1");
        employee.setFullTime(true);
        employee.setAddress(address);
        employee.setAge(40);
        employee.setTitle("Mr.");
        employee.setSalary(45000);

        assertTrue(ModelUtil.checkKey(employee));
    }

    public void testCheckKeyInvalidObjectNoReference() throws Exception {

        CEO employee = new CEO();
        employee.setName("EmployeeB1");
        employee.setFullTime(true);
        employee.setAge(40);
        employee.setTitle("Mr.");
        employee.setSalary(45000);

        assertFalse(ModelUtil.checkKey(employee));
    }

    public void testCheckKeyInvalidObjectNoAttribute() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO employee = new CEO();
        employee.setFullTime(true);
        employee.setAddress(address);
        employee.setAge(40);
        employee.setTitle("Mr.");
        employee.setSalary(45000);

        assertFalse(ModelUtil.checkKey(employee));
                    }

    public void testCheckKeyNullObject() throws Exception {
        try {
            ModelUtil.checkKey(null);
            fail ("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }


}
