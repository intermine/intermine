package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.Department;


public class SubqueryConstraintTest extends TestCase {

    private SubqueryConstraint constraint;
    private QueryValue qe;
    private QueryClass cls;
    private Query subquery;

    public SubqueryConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        cls = new QueryClass(Department.class);
        qe = new QueryValue("test");
        subquery = new Query();
    }

    public void testNullQueryConstructor() throws Exception {
        try {
            constraint = new SubqueryConstraint(null, SubqueryConstraint.CONTAINS, cls);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            constraint = new SubqueryConstraint(null, SubqueryConstraint.CONTAINS, qe);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testNullArgConstructor() throws Exception {
        try {
            QueryEvaluable qe2 = null;
            constraint = new SubqueryConstraint(subquery, SubqueryConstraint.DOES_NOT_CONTAIN, qe2);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            QueryClass cls2 = null;
            constraint = new SubqueryConstraint(subquery, SubqueryConstraint.DOES_NOT_CONTAIN, cls2);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

    public void testInvalidType() {
        try {
            constraint = new SubqueryConstraint(subquery, 234, cls);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SubqueryConstraint(subquery, 234, qe);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }

    }

}

