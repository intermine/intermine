package org.flymine.objectstore.query;

import junit.framework.TestCase;


public class SimpleConstraintTest extends TestCase {


    private SimpleConstraint constraint;
    private QueryEvaluable qeStr1;
    private QueryEvaluable qeStr2;
    private QueryEvaluable qeNum1;
    private QueryEvaluable qeNum2;

    public SimpleConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qeStr1 = new QueryValue("String1");
        qeStr2 = new QueryValue("String2");
        qeNum1 = new QueryValue(new Integer(124));
        qeNum2 = new QueryValue(new Double(3.22));
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
        } finally {
        }

    }

}
