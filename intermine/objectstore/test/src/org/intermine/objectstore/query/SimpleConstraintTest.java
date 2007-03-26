package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.Date;

public class SimpleConstraintTest extends TestCase {
    private SimpleConstraint constraint;
    private QueryEvaluable qeStr1;
    private QueryEvaluable qeStr2;
    private QueryEvaluable qeNum1;
    private QueryEvaluable qeNum2;
    private QueryEvaluable qeBool1;
    private QueryEvaluable qeDate1;

    public SimpleConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qeStr1 = new QueryValue("String1");
        qeStr2 = new QueryValue("String2");
        qeNum1 = new QueryValue(new Integer(124));
        qeNum2 = new QueryValue(new Double(3.22));
        qeBool1 = new QueryValue(new Boolean(true));
        qeDate1 = new QueryValue(new Date());
    }

    public void testNullDualConstructor() throws Exception {
        try {
            constraint = new SimpleConstraint(null, ConstraintOp.EQUALS, qeNum2);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
        try {
            constraint = new SimpleConstraint(qeNum1,null, qeNum2);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            constraint = new SimpleConstraint(qeNum1, ConstraintOp.EQUALS, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testNullSingleConstructor() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            constraint = new SimpleConstraint(null, ConstraintOp.IS_NULL);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testSingleWrongType() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, ConstraintOp.GREATER_THAN);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualWrongType() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, ConstraintOp.IS_NULL, qeStr1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeStr1, ConstraintOp.IS_NOT_NULL, qeStr1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualTypeString() throws Exception {
        constraint = new SimpleConstraint(qeStr1, ConstraintOp.GREATER_THAN, qeStr1);
        constraint = new SimpleConstraint(qeStr1, ConstraintOp.GREATER_THAN_EQUALS, qeStr1);
        constraint = new SimpleConstraint(qeStr1, ConstraintOp.LESS_THAN, qeStr1);
        constraint = new SimpleConstraint(qeStr1, ConstraintOp.LESS_THAN_EQUALS, qeStr1);
    }

    public void testDualWrongTypeNumber() throws Exception {
        try {
            constraint = new SimpleConstraint(qeNum1, ConstraintOp.MATCHES, qeNum1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeNum1, ConstraintOp.DOES_NOT_MATCH, qeNum1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualWrongTypeBoolean() throws Exception {
        try {
            constraint = new SimpleConstraint(qeBool1, ConstraintOp.MATCHES, qeBool1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, ConstraintOp.DOES_NOT_MATCH, qeBool1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, ConstraintOp.GREATER_THAN, qeBool1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, ConstraintOp.GREATER_THAN_EQUALS, qeBool1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, ConstraintOp.LESS_THAN, qeBool1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, ConstraintOp.LESS_THAN_EQUALS, qeBool1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualWrongTypeDate() throws Exception {
        try {
            constraint = new SimpleConstraint(qeDate1, ConstraintOp.MATCHES, qeDate1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeDate1, ConstraintOp.DOES_NOT_MATCH, qeDate1);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    // create negated SimpleConstraints and check getOp gives correct answer
    public void testGetRealType() throws Exception {
        SimpleConstraint c1 = new SimpleConstraint(qeNum1, ConstraintOp.EQUALS, qeNum1);
        c1.negate();
        assertEquals(c1.getOp(), ConstraintOp.NOT_EQUALS);
        SimpleConstraint c2 = new SimpleConstraint(qeNum1, ConstraintOp.NOT_EQUALS, qeNum1);
        c2.negate();
        assertEquals(c2.getOp(), ConstraintOp.EQUALS);
        SimpleConstraint c3 = new SimpleConstraint(qeNum1, ConstraintOp.LESS_THAN, qeNum1);
        c3.negate();
        assertEquals(c3.getOp(), ConstraintOp.GREATER_THAN_EQUALS);
        SimpleConstraint c4 = new SimpleConstraint(qeNum1, ConstraintOp.GREATER_THAN_EQUALS, qeNum1);
        c4.negate();
        assertEquals(c4.getOp(), ConstraintOp.LESS_THAN);
        SimpleConstraint c5 = new SimpleConstraint(qeNum1, ConstraintOp.GREATER_THAN, qeNum1);
        c5.negate();
        assertEquals(c5.getOp(), ConstraintOp.LESS_THAN_EQUALS);
        SimpleConstraint c6 = new SimpleConstraint(qeNum1, ConstraintOp.LESS_THAN_EQUALS, qeNum1);
        c6.negate();
        assertEquals(c6.getOp(), ConstraintOp.GREATER_THAN);
        SimpleConstraint c7 = new SimpleConstraint(qeStr1, ConstraintOp.MATCHES, qeStr1);
        c7.negate();
        assertEquals(c7.getOp(), ConstraintOp.DOES_NOT_MATCH);
        SimpleConstraint c8 = new SimpleConstraint(qeStr1, ConstraintOp.DOES_NOT_MATCH, qeStr1);
        c8.negate();
        assertEquals(c8.getOp(), ConstraintOp.MATCHES);
        SimpleConstraint c9 = new SimpleConstraint(qeNum1, ConstraintOp.IS_NULL);
        c9.negate();
        assertEquals(c9.getOp(), ConstraintOp.IS_NOT_NULL);
        SimpleConstraint c10 = new SimpleConstraint(qeNum1, ConstraintOp.IS_NOT_NULL);
        c10.negate();
        assertEquals(c10.getOp(), ConstraintOp.IS_NULL);
        SimpleConstraint c11 = new SimpleConstraint(qeBool1, ConstraintOp.EQUALS, qeBool1);
        c11.negate();
        assertEquals(c1.getOp(), ConstraintOp.NOT_EQUALS);
        SimpleConstraint c12 = new SimpleConstraint(qeBool1, ConstraintOp.NOT_EQUALS, qeBool1);
        c12.negate();
        assertEquals(c2.getOp(), ConstraintOp.EQUALS);
    }

    public void testEquals() throws Exception {
        SimpleConstraint c1 = new SimpleConstraint(qeNum1, ConstraintOp.EQUALS, qeNum1);
        SimpleConstraint c2 = new SimpleConstraint(qeNum1, ConstraintOp.EQUALS, qeNum1);
        SimpleConstraint c3 = new SimpleConstraint(qeStr1, ConstraintOp.EQUALS, qeStr2);
        SimpleConstraint c4 = new SimpleConstraint(qeStr1, ConstraintOp.EQUALS, qeStr2);
        c4.negate();

        assertEquals(c1, c1);
        assertEquals(c1, c2);
        assertTrue("Expected c1 to not equal c3:", !c1.equals(c3));
        assertTrue("Expected c3 to not equal c4:", !c3.equals(c4));
    }

    public void testHashCode() throws Exception {
        SimpleConstraint c1 = new SimpleConstraint(qeNum1, ConstraintOp.EQUALS, qeNum1);
        SimpleConstraint c2 = new SimpleConstraint(qeNum1, ConstraintOp.EQUALS, qeNum1);
        SimpleConstraint c3 = new SimpleConstraint(qeStr1, ConstraintOp.EQUALS, qeStr2);
        SimpleConstraint c4 = new SimpleConstraint(qeStr1, ConstraintOp.EQUALS, qeStr2);
        c4.negate();

        assertEquals(c1.hashCode(), c1.hashCode());
        assertEquals(c1.hashCode(), c2.hashCode());
        assertTrue("Expected c1 hashCode() to not equal c3.hashCode():", c1.hashCode() != c3.hashCode());
        assertTrue("Expected c3.hashCode() to not equal c4.hashCode():", c3.hashCode() != c4.hashCode());
    }
}
