package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class QueryCollectionTest extends TestCase
{
    private QueryClass qc;

    public QueryCollectionTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(Department.class);
    }

    public void testMissingField() {
        try {
            new QueryCollection(qc, "secretarys");
            fail("A NoSuchFieldException should have been thrown");
        } catch (NoSuchFieldException e) {
        }
    }
    
    public void testEmptyField() {
        try {
            new QueryCollection(qc, "");
            fail("A NoSuchFieldException should have been thrown");
        } catch (NoSuchFieldException e) {
        }
    }
    
    public void testNullField() throws Exception {
        try {
            new QueryCollection(qc, (String)null);
            fail("A NoSuchFieldException should have been thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidField() {
        try {
            new QueryCollection(qc, "name");
            fail("An IllegalArgumentException should have been thrown");
        } catch (Exception e) {
        }
    }

    public void testValidField() {
        try {
            new QueryCollection(qc, "employees");
        } catch (Exception e) {
            fail("An exception should not be thrown for a valid field");
        }
    }
}
