package org.flymine.util;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;
import org.flymine.metadata.Model;

public class ModelUtilTest extends TestCase
{
    Model model;

    public ModelUtilTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    // key of CEO is "name age address"
    public void testHasValidKeyValidObject() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO employee = new CEO();
        employee.setName("EmployeeB1");
        employee.setAge(40);
        employee.setAddress(address);

        assertTrue(ModelUtil.hasValidKey(employee, model));
    }

    public void testHasValidKeyInvalidObjectNoReference() throws Exception {
        CEO ceo = new CEO();
        ceo.setName("EmployeeB1");
        ceo.setAge(40);

        assertFalse(ModelUtil.hasValidKey(ceo, model));
    }

    public void testHasValidKeyInvalidObjectNoAttribute() throws Exception {
        Address address = new Address();
        address.setAddress("Employee Street, BVille");

        CEO ceo = new CEO();
        ceo.setAge(40);
        ceo.setAddress(address);

        assertFalse(ModelUtil.hasValidKey(ceo, model));
    }

    public void testHasValidKeyNullObject() throws Exception {
        try {
            ModelUtil.hasValidKey(null, model);
            fail ("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }
}
