package org.flymine.objectstore.query;

import junit.framework.TestCase;

public class QueryClassTest extends TestCase
{
    private final Class CLASS = String.class;
    private QueryClass qc;

    public QueryClassTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(CLASS);
    }

    public void testGetType() {
        assertEquals(CLASS, qc.getType());
    }
}
