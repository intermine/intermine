package org.flymine.objectstore.query;

import junit.framework.TestCase;

public class QueryValueTest extends TestCase
{
    private QueryValue value;

    public QueryValueTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
    }

    public void testGetType() throws Exception {
        QueryValue value = new QueryValue("string");
        assertEquals(String.class, value.getType());
    }

    public void testInvalidType() throws Exception {
        try {
            QueryValue value = new QueryValue(new Object());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDifferentNumbersEqual() throws Exception {
        assertEquals(new QueryValue(new Integer(5)), new QueryValue(new Long(5)));
        assertEquals(new QueryValue(new Integer(5)), new QueryValue(new Double(5.0)));
    }

    public void testDifferentNumbersNotEqual() throws Exception {
        assertTrue("Expected 5 to not equal 5.00001", !(new QueryValue(new Integer(5))).equals(new QueryValue(new Double(5.00001))));
    }
}
