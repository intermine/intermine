package org.flymine.objectstore.query;

import junit.framework.TestCase;

import org.flymine.model.testmodel.*;

public class SubqueryConstraintTest extends TestCase {

    private SubqueryConstraint constraint;
    private QueryValue qe1;
    private QueryClass qc1;
    private Query subquery;

    public SubqueryConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        qc1 = new QueryClass(Department.class);
        qe1 = new QueryValue("test");
        subquery = new Query();
    }

    public void testNullQueryConstructor() throws Exception {
        try {
            constraint = new SubqueryConstraint(null, SubqueryConstraint.CONTAINS, qc1);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            constraint = new SubqueryConstraint(null, SubqueryConstraint.CONTAINS, qe1);
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
            QueryClass qc2 = null;
            constraint = new SubqueryConstraint(subquery, SubqueryConstraint.DOES_NOT_CONTAIN, qc2);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }

    }

    public void testInvalidType() {
        try {
            constraint = new SubqueryConstraint(subquery, 234, qc1);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            constraint = new SubqueryConstraint(subquery, 234, qe1);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }

    }

    public void testSelectListClass() throws Exception {
        Query q1 = new Query();
        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
            fail("Expected: IllegalArgumentException - no items in subquery select");
        } catch ( IllegalArgumentException e) {
        }

        q1.addToSelect(qe1);
        q1.addToSelect(qc1);

        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
            fail("Expected: IllegalArgumentException - no items in subquery select");
        } catch ( IllegalArgumentException e) {
        }
    }

    public void testSelectListEvaluable() throws Exception {
        Query q1 = new Query();
        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qe1);
            fail("Expected: IllegalArgumentException - no items in subquery select");
        } catch ( IllegalArgumentException e) {
        }

        q1.addToSelect(qe1);
        q1.addToSelect(qc1);

        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qe1);
            fail("Expected: IllegalArgumentException - no items in subquery select");
        } catch ( IllegalArgumentException e) {
        }
    }

    public void testQueryEvaluableTypes() throws Exception {
        // select a string in subquery, try to compare with a number
        QueryValue qeNum = new QueryValue(new Float(2.1));
        QueryValue qeStr = new QueryValue("test");
        Query q1 = new Query();
        q1.addToSelect(qeStr);

        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qeNum);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSubqueryNotAClass() throws Exception {
        // select a QueryEvaluable from subquery, try to compare with a QueryClass
        Query q1 = new Query();
        q1.addToSelect(qe1);

        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSubqueryNotAnEvaluable() throws Exception {
        // select a QueryEvaluable from subquery, try to compare with a QueryClass
        Query q1 = new Query();
        q1.addToSelect(qc1);

        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qe1);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testIncompatibleClassTypes() throws Exception {
        // select a QueryClass, compare to QueryClass of a different java type
        QueryClass department = new QueryClass(Department.class);
        QueryClass manager = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addToSelect(manager);

        try {
            constraint = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, department);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testEquals() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(qc1);
        Query q2 = new Query();
        q2.addToSelect(qe1);

        SubqueryConstraint sc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
        SubqueryConstraint sc2 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
        SubqueryConstraint sc3 = new SubqueryConstraint(q2, SubqueryConstraint.CONTAINS, qe1);
        SubqueryConstraint sc4 = new SubqueryConstraint(q2, SubqueryConstraint.DOES_NOT_CONTAIN, qe1);

        assertEquals(sc1, sc1);
        assertEquals(sc1, sc2);
        assertTrue("Expected sc1 to not equal sc3:", !sc1.equals(sc3));
        assertTrue("Expected sc3 to not equal sc4:", !sc3.equals(sc4));
    }


   public void testHashCode() throws Exception {
        Query q1 = new Query();
        q1.addToSelect(qc1);
        Query q2 = new Query();
        q2.addToSelect(qe1);

        SubqueryConstraint sc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
        SubqueryConstraint sc2 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, qc1);
        SubqueryConstraint sc3 = new SubqueryConstraint(q2, SubqueryConstraint.CONTAINS, qe1);
        SubqueryConstraint sc4 = new SubqueryConstraint(q2, SubqueryConstraint.DOES_NOT_CONTAIN, qe1);

        assertEquals(sc1.hashCode(), sc1.hashCode());
        assertEquals(sc1.hashCode(), sc2.hashCode());
        assertTrue("Expected sc1.hashCode() to not equal sc3.hashCode():", sc1.hashCode() != sc3.hashCode());
        assertTrue("Expected sc3.hashCode() to not equal sc4.hashCode():", sc3.hashCode() != sc4.hashCode());
    }
}

