package org.flymine.sql.precompute;

import junit.framework.*;

public class LimitOffsetQueryTest extends TestCase
{
    public LimitOffsetQueryTest(String arg1) {
        super(arg1);
    }

    public void test1() {
        LimitOffsetQuery q1 = new LimitOffsetQuery("blooble blarb LIMIT 10 OFFSET 28742");
        assertEquals("blooble blarb", q1.getQuery());
        assertEquals(10, q1.getLimit());
        assertEquals(28742, q1.getOffset());
        q1 = new LimitOffsetQuery("blarp blip blob");
        assertEquals("blarp blip blob", q1.getQuery());
        assertEquals(Integer.MAX_VALUE, q1.getLimit());
        assertEquals(0, q1.getOffset());
        q1 = new LimitOffsetQuery("blarp blip blob OFFSET 342");
        assertEquals("blarp blip blob OFFSET 342", q1.getQuery());
        assertEquals(Integer.MAX_VALUE, q1.getLimit());
        assertEquals(0, q1.getOffset());
        q1 = new LimitOffsetQuery("blarp blip blob IT 4002 OFFSET 23");
        assertEquals("blarp blip blob IT 4002 OFFSET 23", q1.getQuery());
        assertEquals(Integer.MAX_VALUE, q1.getLimit());
        assertEquals(0, q1.getOffset());
    }
}
