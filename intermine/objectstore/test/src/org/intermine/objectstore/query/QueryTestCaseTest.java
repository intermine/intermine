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

import junit.framework.*;
import org.intermine.testing.OneTimeTestCase;
import org.intermine.model.testmodel.*;

public class QueryTestCaseTest extends QueryTestCase
{
    public QueryTestCaseTest(String arg1) {
        super(arg1);
    }

    public static Test suite() {
        return OneTimeTestCase.buildSuite(QueryTestCaseTest.class);
    }
    
    private boolean failed;

    public void testEmptyQueries() throws Exception {
        assertEquals(new Query(), new Query());
    }

    public void testSimpleQueriesNoConstraints() throws Exception {
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        assertEquals(q1, q2);

        QueryClass qc3 = new QueryClass(Department.class);
        q2.addFrom(qc3);

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("q1 and q2 should not be equal");
            }
        }
    }

    public void testQueriesSimpleConstraint() throws Exception {
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        QueryField qf1 = new QueryField(qc1, "name");
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Department1"));
        q1.setConstraint(c1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        QueryField qf2 = new QueryField(qc2, "name");
        Constraint c2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("Department1"));
        q2.setConstraint(c2);

        assertEquals(q1, q2);

        c2 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("Department2"));
        q2.setConstraint(c2);

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("q1 and q2 should not be equal");
            }
        }
    }

    public void testQueriesContainsConstraint() throws Exception {
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        QueryReference qr1 = new QueryObjectReference(qc1, "company");
        Constraint c1 = new ContainsConstraint(qr1, ConstraintOp.CONTAINS, new QueryClass(Company.class));
        q1.setConstraint(c1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        QueryReference qr2 = new QueryObjectReference(qc2, "company");
        Constraint c2 = new ContainsConstraint(qr2, ConstraintOp.CONTAINS, new QueryClass(Company.class));
        q2.setConstraint(c2);

        assertEquals(q1, q2);
        QueryReference qr3 = new QueryObjectReference(qc2, "manager");
        c2 = new ContainsConstraint(qr3, ConstraintOp.CONTAINS, new QueryClass(Manager.class));
        q2.setConstraint(c2);

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("q1 and q2 should not be equal");
            }
        }
    }

    public void testQueriesClassConstraint() throws Exception {
        Department dept = new Department();
        dept.setId(new Integer(14));

        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        Constraint c1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, dept);
        q1.setConstraint(c1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        Constraint c2 = new ClassConstraint(qc2, ConstraintOp.EQUALS, dept);
        q2.setConstraint(c2);

        assertEquals(q1, q2);
        Department d1 = new Department();
        d1.setId(new Integer(25));
        c2 = new ClassConstraint(qc2, ConstraintOp.EQUALS, d1);
        q2.setConstraint(c2);

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("q1 and q2 should not be equal");
            }
        }
    }

    public void testQueriesConstraintSet() throws Exception {
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);

        QueryField qf1 = new QueryField(qc1, "name");
        Constraint c1 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Department1"));
        Constraint c2 = new SimpleConstraint(qf1, ConstraintOp.EQUALS, new QueryValue("Department2"));
        cs1.addConstraint(c1);
        cs1.addConstraint(c2);
        q1.setConstraint(cs1);



        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.OR);

        QueryField qf2 = new QueryField(qc2, "name");
        Constraint c3 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("Department1"));
        Constraint c4 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("Department2"));
        cs2.addConstraint(c3);
        cs2.addConstraint(c4);
        q2.setConstraint(cs2);

        assertEquals(q1, q2);

        Constraint c5 = new SimpleConstraint(qf2, ConstraintOp.EQUALS, new QueryValue("Department4"));
        cs2.addConstraint(c5);
        q2.setConstraint(cs2);

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("q1 and q2 should not be equal");
            }
        }
    }

    public void testQ1IsNull() throws Exception {
        failed = false;
        try {
            assertEquals(null, new Query());
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("Failure should have happened");
            }
        }
    }

    public void testQ2IsNull() throws Exception {
        failed = false;
        try {
            assertEquals(new Query(), null);
            failed = true;
        } catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("Failure should have happened");
            }
        }
    }

    public void testBothNull() throws Exception {
        Query q1 = null;
        Query q2 = null;
        assertEquals(q1, q2);
    }

    public void testBothSubqueriesSame() throws Exception {
        Query q1 = new Query();
        Query q2 = new Query();
        Query q1Sub = new Query();
        Query q2Sub = new Query();
        q1.addFrom(q1Sub);
        q2.addFrom(q2Sub);

        assertEquals(q1, q2);
    }

    public void testBothSubqueriesDifferent() throws Exception {
        Query q1 = new Query();
        Query q2 = new Query();
        Query q1Sub = new Query();
        Query q2Sub = new Query();
        q2Sub.addFrom(new QueryClass(Department.class));
        q1.addFrom(q1Sub);
        q2.addFrom(q2Sub);

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        } finally {
            if (failed) {
                fail("Failure should have happened");
            }
        }
    }

    public void testOneSubquery() throws Exception {
        Query q1 = new Query();
        Query q2 = new Query();
        Query q1Sub = new Query();
        q1.addFrom(q1Sub);
        q2.addFrom(new QueryClass(Department.class));

        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
        } finally {
            if (failed) {
                fail("Failure should have happened");
            }
        }
    }

    public void testDifferentSubquery() throws Exception {
        Query q1 = new Query();
        Query q2 = new Query();
        Query q1Sub1 = new Query();
        Query q1Sub2 = new Query();
        Query q2Sub1 = new Query();
        Query q2Sub2 = new Query();
        QueryClass c1 = new QueryClass(Department.class);
        QueryClass c2 = new QueryClass(Company.class);
        QueryClass c3 = new QueryClass(Department.class);
        QueryClass c4 = new QueryClass(Company.class);
        q1Sub1.addFrom(c1);
        q1Sub1.addToSelect(c1);
        q1Sub2.addFrom(c2);
        q1Sub2.addToSelect(c2);
        q2Sub1.addFrom(c3);
        q2Sub1.addToSelect(c3);
        q2Sub2.addFrom(c4);
        q2Sub2.addToSelect(c4);
        q1.addFrom(q1Sub1);
        q1.addFrom(q1Sub2);
        q2.addFrom(q2Sub1);
        q2.addFrom(q2Sub2);

        QueryField f1 = new QueryField(q1Sub1, c1, "name");
        QueryField f2 = new QueryField(q2Sub2, c4, "name");
        q1.addToSelect(f1);
        q2.addToSelect(f2);
        
        failed = false;
        try {
            assertEquals(q1, q2);
            failed = true;
        } catch (AssertionFailedError e) {
            assertEquals("asserting equal: expected <" + q1.toString() + "> but was <" + q2.toString() + ">: SELECT lists are not equal: query nodes are not the same: field members of different subquery aliases expected:<a1_> but was:<a2_>", e.getMessage());
        } finally {
            if (failed) {
                fail("Failure should have happened");
            }
        }
    }

}
