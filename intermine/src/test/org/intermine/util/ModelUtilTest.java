package org.flymine.util;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class ModelUtilTest extends TestCase
{
    public ModelUtilTest(String arg) {
        super(arg);
    }

    // key of CEO is "name age address"
    public void testHasValidKeyValidObject() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO employee = new CEO();
        employee.setName("EmployeeB1");
        employee.setAge(40);
        employee.setAddress(address);

        assertTrue(ModelUtil.hasValidKey(employee));
    }

    public void testHasValidKeyInvalidObjectNoReference() throws Exception {
        CEO ceo = new CEO();
        ceo.setName("EmployeeB1");
        ceo.setAge(40);

        assertFalse(ModelUtil.hasValidKey(ceo));
    }

    public void testHasValidKeyInvalidObjectNoAttribute() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO ceo = new CEO();
        ceo.setAge(40);
        ceo.setAddress(address);

        assertFalse(ModelUtil.hasValidKey(ceo));
    }

    public void testHasValidKeyNullObject() throws Exception {
        try {
            ModelUtil.hasValidKey(null);
            fail ("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
