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
        }
        catch (NullPointerException e) {
        }

    }


    public void testNullSingleConstrctor() throws Exception {
        try {
            constraint = new SimpleConstraint(null, SimpleConstraint.IS_NULL);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
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



}
