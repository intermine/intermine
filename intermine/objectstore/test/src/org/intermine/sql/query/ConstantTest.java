package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;
import java.math.BigDecimal;

public class ConstantTest extends TestCase
{
    private Constant c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13;
    
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
        c7 = new Constant("5::REAL");
        c8 = new Constant("1089290109834.28728747598768541");
        c9 = new Constant("1089290109834.28728747598768542");
        c10 = new Constant("5.1");
        c11 = new Constant("5.1::REAL");
        c12 = new Constant(new BigDecimal((new Float(5.1)).doubleValue()).toString());
        c13 = new Constant("1.3432E-11");
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
        assertEquals(c4, c7);
        assertTrue("Expected c4 not to equal c8", !c4.equals(c8));
        assertTrue("Expected c4 not to equal c9", !c4.equals(c9));
        assertTrue("Expected c8 not to equal c9", !c8.equals(c9));
        assertTrue("Expected c4 not to equal c10", !c4.equals(c10));
        assertTrue("Expected c10 not to equal c11", !c10.equals(c11));
        assertTrue("Expected c10 not to equal c12", !c10.equals(c12));
        assertEquals(c11, c12);
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
        assertEquals(AbstractValue.INCOMPARABLE, c4.compare(c6, null, null));
        assertEquals(AbstractValue.INCOMPARABLE, c6.compare(c4, null, null));
        assertEquals(AbstractValue.INCOMPARABLE, c3.compare(c6, null, null));
        assertEquals(AbstractValue.INCOMPARABLE, c6.compare(c3, null, null));
        assertEquals(AbstractValue.NOT_EQUAL, c3.compare(c5, null, null));
        assertEquals(AbstractValue.NOT_EQUAL, c5.compare(c3, null, null));
        assertEquals(AbstractValue.LESS, c4.compare(c5, null, null));
        assertEquals(AbstractValue.GREATER, c5.compare(c4, null, null));
        assertEquals(AbstractValue.LESS, c1.compare(c3, null, null));
        assertEquals(AbstractValue.GREATER, c3.compare(c1, null, null));
        assertEquals(AbstractValue.INCOMPARABLE, c1.compare(null, null, null));
        assertEquals(AbstractValue.GREATER, c5.compare(c7, null, null));
        assertEquals(AbstractValue.LESS, c7.compare(c8, null, null));
        assertEquals(AbstractValue.LESS, c8.compare(c9, null, null));
        assertEquals(AbstractValue.LESS, c7.compare(c10, null, null));
        assertEquals(AbstractValue.GREATER, c10.compare(c11, null, null));
        assertEquals(AbstractValue.GREATER, c10.compare(c12, null, null));
        assertEquals(AbstractValue.GREATER, c10.compare(c13, null, null));
    }

}
