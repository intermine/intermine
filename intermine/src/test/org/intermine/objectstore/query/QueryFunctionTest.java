package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class QueryFunctionTest extends TestCase
{
    private QueryFunction function;

    public QueryFunctionTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
    }
        
    public void testInvalidCount() throws Exception {
        try {
            QueryField field = new QueryField(new QueryClass(Company.class), "name");
            new QueryFunction(field, QueryFunction.COUNT);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testValidCount() {
        QueryFunction function = new QueryFunction();
        assertTrue(Number.class.isAssignableFrom(function.getType()));
    }

    public void testInvalidNonCount() throws Exception {
        QueryField field = new QueryField(new QueryClass(Company.class), "name");
        try {
            new QueryFunction(field, QueryFunction.SUM);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidNonCount() throws Exception {
        QueryField field = new QueryField(new QueryClass(Company.class), "vatNumber");
        try {
            new QueryFunction(field, QueryFunction.SUM);
        } catch (IllegalArgumentException e) {
            fail("An IllegalArgumentException should not have been thrown");
        }
    }
}
