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

    public void testGetType() {
        QueryValue value = new QueryValue("string");
        assertEquals(String.class, value.getType());
    }

    public void testInvalidType() {
        try {
            QueryValue value = new QueryValue(new Object());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}
