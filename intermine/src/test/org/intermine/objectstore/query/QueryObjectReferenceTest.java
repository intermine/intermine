package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class QueryObjectReferenceTest extends TestCase
{
    private QueryClass qc;

    public QueryObjectReferenceTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(Department.class);
    }

    public void testMissingField() {
        try {
            new QueryObjectReference(qc, "secretarys");
            fail("A NoSuchFieldException should have been thrown");
        } catch (NoSuchFieldException e) {
        }
    }
    
    public void testEmptyField() {
        try {
            new QueryObjectReference(qc, "");
            fail("A NoSuchFieldException should have been thrown");
        } catch (NoSuchFieldException e) {
        }
    }
    
    public void testNullField() throws Exception {
        try {
            new QueryObjectReference(qc, (String)null);
            fail("A NoSuchFieldException should have been thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidField() {
        try {
            new QueryObjectReference(qc, "employees");
            fail("An IllegalArgumentException should have been thrown");
        } catch (Exception e) {
        }
    }

    public void testInvalidField2() {
        try {
            new QueryObjectReference(qc, "name");
            fail("An IllegalArgumentException should have been thrown");
        } catch (Exception e) {
        }
    }

    public void testValidField() {
        try {
            new QueryObjectReference(qc, "company");
        } catch (Exception e) {
            fail("An exception should not be thrown for a valid field: " + e);
        }
    }
}
