package org.flymine.objectstore.query;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;


public class SimpleConstraintTest extends TestCase {


    private SimpleConstraint constraint;
    private QueryEvaluable qeStr1;
    private QueryEvaluable qeStr2;
    private QueryEvaluable qeNum1;
    private QueryEvaluable qeNum2;
    private QueryEvaluable qeBool1;

    public SimpleConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qeStr1 = new QueryValue("String1");
        qeStr2 = new QueryValue("String2");
        qeNum1 = new QueryValue(new Integer(124));
        qeNum2 = new QueryValue(new Double(3.22));
        qeBool1 = new QueryValue(new Boolean(true));
    }


    public void testNullDualConstructor() throws Exception {
        try {
            constraint = new SimpleConstraint(null, SimpleConstraint.EQUALS, qeNum2);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
        try {
            constraint = new SimpleConstraint(qeNum1, SimpleConstraint.EQUALS, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testNullSingleConstrctor() throws Exception {
        try {
            constraint = new SimpleConstraint(null, SimpleConstraint.IS_NULL);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testSingleInvalidType() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, 234);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualInvalidType() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, 234, qeStr2);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSingleWrongType() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.GREATER_THAN);
            fail ("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualWrongType() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.IS_NULL, qeStr1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.IS_NOT_NULL, qeStr1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testDualWrongTypeString() throws Exception {
        try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.GREATER_THAN, qeStr1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.GREATER_THAN_EQUALS, qeStr1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
                try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.LESS_THAN, qeStr1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeStr1, SimpleConstraint.LESS_THAN_EQUALS, qeStr1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testDualWrongTypeNumber() throws Exception {
        try {
            constraint = new SimpleConstraint(qeNum1, SimpleConstraint.MATCHES, qeNum1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeNum1, SimpleConstraint.DOES_NOT_MATCH, qeNum1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDualWrongTypeBoolean() throws Exception {
        try {
            constraint = new SimpleConstraint(qeBool1, SimpleConstraint.MATCHES, qeBool1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, SimpleConstraint.DOES_NOT_MATCH, qeBool1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, SimpleConstraint.GREATER_THAN, qeBool1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, SimpleConstraint.GREATER_THAN_EQUALS, qeBool1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
                try {
            constraint = new SimpleConstraint(qeBool1, SimpleConstraint.LESS_THAN, qeBool1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SimpleConstraint(qeBool1, SimpleConstraint.LESS_THAN_EQUALS, qeBool1);
            fail ("Expected: InvalidArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    // create negated SimpleConstraints and check getRealType gives correct answer
    public void testGetRealType() throws Exception {
        try {
            SimpleConstraint c1 = new SimpleConstraint(qeNum1, SimpleConstraint.EQUALS, qeNum1, true);
            assertEquals(c1.getRealType(), SimpleConstraint.NOT_EQUALS);
            SimpleConstraint c2 = new SimpleConstraint(qeNum1, SimpleConstraint.NOT_EQUALS, qeNum1, true);
            assertEquals(c2.getRealType(), SimpleConstraint.EQUALS);
            SimpleConstraint c3 = new SimpleConstraint(qeNum1, SimpleConstraint.LESS_THAN, qeNum1, true);
            assertEquals(c3.getRealType(), SimpleConstraint.GREATER_THAN_EQUALS);
            SimpleConstraint c4 = new SimpleConstraint(qeNum1, SimpleConstraint.GREATER_THAN_EQUALS, qeNum1, true);
            assertEquals(c4.getRealType(), SimpleConstraint.LESS_THAN);
            SimpleConstraint c5 = new SimpleConstraint(qeNum1, SimpleConstraint.GREATER_THAN, qeNum1, true);
            assertEquals(c5.getRealType(), SimpleConstraint.LESS_THAN_EQUALS);
            SimpleConstraint c6 = new SimpleConstraint(qeNum1, SimpleConstraint.LESS_THAN_EQUALS, qeNum1, true);
            assertEquals(c6.getRealType(), SimpleConstraint.GREATER_THAN);
            SimpleConstraint c7 = new SimpleConstraint(qeStr1, SimpleConstraint.MATCHES, qeStr1, true);
            assertEquals(c7.getRealType(), SimpleConstraint.DOES_NOT_MATCH);
            SimpleConstraint c8 = new SimpleConstraint(qeStr1, SimpleConstraint.DOES_NOT_MATCH, qeStr1, true);
            assertEquals(c8.getRealType(), SimpleConstraint.MATCHES);
            SimpleConstraint c9 = new SimpleConstraint(qeNum1, SimpleConstraint.IS_NULL, true);
            assertEquals(c9.getRealType(), SimpleConstraint.IS_NOT_NULL);
            SimpleConstraint c10 = new SimpleConstraint(qeNum1, SimpleConstraint.IS_NOT_NULL, true);
            assertEquals(c10.getRealType(), SimpleConstraint.IS_NULL);
            SimpleConstraint c11 = new SimpleConstraint(qeBool1, SimpleConstraint.EQUALS, qeBool1, true);
            assertEquals(c1.getRealType(), SimpleConstraint.NOT_EQUALS);
            SimpleConstraint c12 = new SimpleConstraint(qeBool1, SimpleConstraint.NOT_EQUALS, qeBool1, true);
            assertEquals(c2.getRealType(), SimpleConstraint.EQUALS);

        } finally {
        }

    }

    public void testEquals() throws Exception {
        SimpleConstraint c1 = new SimpleConstraint(qeNum1, SimpleConstraint.EQUALS, qeNum1);
        SimpleConstraint c2 = new SimpleConstraint(qeNum1, SimpleConstraint.EQUALS, qeNum1);
        SimpleConstraint c3 = new SimpleConstraint(qeStr1, SimpleConstraint.EQUALS, qeStr2);
        SimpleConstraint c4 = new SimpleConstraint(qeStr1, SimpleConstraint.EQUALS, qeStr2, true);

        assertEquals(c1, c1);
        assertEquals(c1, c2);
        assertTrue("Expected c1 to not equal c3:", !c1.equals(c3));
        assertTrue("Expected c3 to not equal c4:", !c3.equals(c4));
    }

    public void testHashCode() throws Exception {
        SimpleConstraint c1 = new SimpleConstraint(qeNum1, SimpleConstraint.EQUALS, qeNum1);
        SimpleConstraint c2 = new SimpleConstraint(qeNum1, SimpleConstraint.EQUALS, qeNum1);
        SimpleConstraint c3 = new SimpleConstraint(qeStr1, SimpleConstraint.EQUALS, qeStr2);
        SimpleConstraint c4 = new SimpleConstraint(qeStr1, SimpleConstraint.EQUALS, qeStr2, true);

        assertEquals(c1.hashCode(), c1.hashCode());
        assertEquals(c1.hashCode(), c2.hashCode());
        assertTrue("Expected c1 hashCode() to not equal c3.hashCode():", c1.hashCode() != c3.hashCode());
        assertTrue("Expected c3.hashCode() to not equal c4.hashCode():", c3.hashCode() != c4.hashCode());

    }


}
