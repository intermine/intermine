package org.flymine.sql.query;

import junit.framework.*;

public class ConstantTest extends TestCase
{
    private Constant c1, c2, c3, c4, c5, c6, c7, c8, c9;
    
    public ConstantTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        c1 = new Constant("'value1'", "'alias1'");
        c2 = new Constant("'value1'", "'alias2'");
        c3 = new Constant("'value1'");
        c4 = new Constant("'value1'", "'alias1'");
        c5 = new Constant("'value2'", "'alias3'");
        c6 = new Constant("'value1'");
        c7 = new Constant("5");
        c8 = new Constant("6");
        c9 = new Constant("flibble");
    }
        
    public void testConstantWithAlias() throws Exception {
        Constant c = new Constant("'A constant'", "myalias");
        assertEquals("'A constant' AS myalias", c.getSQLString());
    }

    public void testConstantWithoutAlias() throws Exception {
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

    public void testConstantWithNullAlias() throws Exception {
        Constant c = new Constant("hello", null);
        assertEquals("hello", c.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(c1, c1);
        assertTrue("Expected c1 not to equal c2", !c1.equals(c2));
        assertTrue("Expected c1 not to equal c3", !c1.equals(c3));
        assertEquals(c1, c4);
        assertTrue("Expected c1 not to equal c5", !c1.equals(c5));
        assertTrue("Expected c1 not to equal null", !c1.equals(null));
        assertEquals(c3, c6);
    }

    public void testEqualsIgnoreAlias() throws Exception {
        assertTrue("Expected c1 to equal c1", c1.equalsIgnoreAlias(c1));
        assertTrue("Expected c1 to equal c2", c1.equalsIgnoreAlias(c2));
        assertTrue("Expected c1 to equal c3", c1.equalsIgnoreAlias(c3));
        assertTrue("Expected c1 to equal c4", c1.equalsIgnoreAlias(c4));
        assertTrue("Expected c1 not to equal c5", !c1.equalsIgnoreAlias(c5));
        assertTrue("Expected c1 not to equal null", !c1.equalsIgnoreAlias(null));
        assertTrue("Expected c3 to equal c6", c3.equalsIgnoreAlias(c6));
    }

    public void testHashCode() throws Exception {
        assertTrue("Expected c1.hashCode() not to equal c2.hashCode()",
                   !(c1.hashCode() == c2.hashCode()));
        assertTrue("Expected c1.hashCode() not to equal c3.hashCode()",
                   !(c1.hashCode() == c3.hashCode()));
        assertEquals(c1.hashCode(), c4.hashCode());
        assertTrue("Expected c1.hashCode() not to equal c5.hashCode()",
                   !(c1.hashCode() == c5.hashCode()));
        assertEquals(c3.hashCode(), c6.hashCode());
    }

    public void testCompare() throws Exception {
        assertEquals(AbstractValue.INCOMPARABLE, c5.compare(c7));
        assertEquals(AbstractValue.INCOMPARABLE, c8.compare(c3));
        assertEquals(AbstractValue.INCOMPARABLE, c9.compare(c1));
        assertEquals(AbstractValue.INCOMPARABLE, c9.compare(c7));
        assertEquals(AbstractValue.LESS, c6.compare(c5));
        assertEquals(AbstractValue.GREATER, c5.compare(c6));
        assertEquals(AbstractValue.LESS, c7.compare(c8));
        assertEquals(AbstractValue.GREATER, c8.compare(c7));
    }
}
