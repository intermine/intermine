package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class QueryFieldTest extends TestCase
{
    private QueryClass qc;

    public QueryFieldTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(Department.class);
    }

    public void testMissingField() {
        try {
            new QueryField(qc, "genes");
            fail("A NoSuchFieldException should have been thrown");
        } catch (NoSuchFieldException e) {
        }
    }
    
    public void testEmptyField() {
        try {
            new QueryField(qc, "");
            fail("A NoSuchFieldException should have been thrown");
        } catch (NoSuchFieldException e) {
        }
    }
    
    public void testNullField() throws NoSuchFieldException {
        try {
            new QueryField(qc, (String)null);
            fail("A NullPointerException should have been thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidField() throws NoSuchFieldException {
        try {
            new QueryField(qc, "employees");
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidField() {
        try {
            new QueryField(qc, "name");
        } catch (Exception e) {
            fail("An exception should not be thrown for a valid field");
        }
    }
}
