package org.flymine.sql.query;

import junit.framework.*;

public class ConstantTest extends TestCase
{
    private Constant c1, c2, c3, c4, c5, c6;
    
    public ConstantTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        c1 = new Constant("'value1'");
        c2 = new Constant("'value1'");
        c3 = new Constant("'value22'");
        c4 = new Constant("5");
        c5 = new Constant("66");
        c6 = new Constant("flibble");
    }
        
    public void testGetSQLString() throws Exception {
        Constant c = new Constant("'A constant'");
        assertEquals("'A constant'", c.getSQLString());
    }

    public void testConstantWithNullValue() throws Exception {
        try {
            Constant c = new Constant(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testEquals() throws Exception {
        assertEquals(c1, c1);
        assertEquals(c1, c2);
        assertEquals(c2, c1);
        assertTrue("Expected c1 not to equal c3", !c1.equals(c3));
        assertTrue("Expected c3 not to equal c1", !c3.equals(c1));
        assertTrue("Expected c1 not to equal c4", !c1.equals(c4));
        assertTrue("Expected c4 not to equal c1", !c4.equals(c1));
        assertTrue("Expected c1 not to equal c5", !c1.equals(c5));
        assertTrue("Expected c5 not to equal c1", !c5.equals(c1));
        assertTrue("Expected c1 not to equal null", !c1.equals(null));
    }

    public void testHashCode() throws Exception {
        assertEquals(c1.hashCode(), c1.hashCode());
        assertEquals(c1.hashCode(), c2.hashCode());
        assertTrue("Expected c1.hashCode() not to equal c3.hashCode()",
                   !(c1.hashCode() == c3.hashCode()));
        assertTrue("Expected c1.hashCode() not to equal c4.hashCode()",
                   !(c1.hashCode() == c4.hashCode()));
        assertTrue("Expected c1.hashCode() not to equal c5.hashCode()",
                   !(c1.hashCode() == c5.hashCode()));
    }

    public void testCompare() throws Exception {
        assertEquals(AbstractValue.INCOMPARABLE, c4.compare(c6));
        assertEquals(AbstractValue.INCOMPARABLE, c6.compare(c4));
        assertEquals(AbstractValue.INCOMPARABLE, c3.compare(c6));
        assertEquals(AbstractValue.INCOMPARABLE, c6.compare(c3));
        assertEquals(AbstractValue.NOT_EQUAL, c3.compare(c5));
        assertEquals(AbstractValue.NOT_EQUAL, c5.compare(c3));
        assertEquals(AbstractValue.LESS, c4.compare(c5));
        assertEquals(AbstractValue.GREATER, c5.compare(c4));
        assertEquals(AbstractValue.LESS, c1.compare(c3));
        assertEquals(AbstractValue.GREATER, c3.compare(c1));
        assertEquals(AbstractValue.INCOMPARABLE, c1.compare(null));
    }

}
