package org.flymine.objectstore.query;

import junit.framework.*;

import org.flymine.model.testmodel.*;

public class QueryTestCaseTest extends QueryTestCase
{
    public QueryTestCaseTest(String arg1) {
        super(arg1);
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
        Constraint c1 = new SimpleConstraint(qf1, SimpleConstraint.EQUALS, new QueryValue("Department1"));
        q1.setConstraint(c1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        QueryField qf2 = new QueryField(qc2, "name");
        Constraint c2 = new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue("Department1"));
        q2.setConstraint(c2);

        assertEquals(q1, q2);

        c2 = new SimpleConstraint(qf2, SimpleConstraint.EQUALS, new QueryValue("Department2"));
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
        Constraint c1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, new QueryClass(Company.class));
        q1.setConstraint(c1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        QueryReference qr2 = new QueryObjectReference(qc2, "company");
        Constraint c2 = new ContainsConstraint(qr2, ContainsConstraint.CONTAINS, new QueryClass(Company.class));
        q2.setConstraint(c2);

        assertEquals(q1, q2);
        QueryReference qr3 = new QueryObjectReference(qc2, "manager");
        c2 = new ContainsConstraint(qr3, ContainsConstraint.CONTAINS, new QueryClass(Manager.class));
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
        Query q1 = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        q1.addToSelect(qc1);
        q1.addFrom(qc1);

        Constraint c1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, new Department());
        q1.setConstraint(c1);

        Query q2 = new Query();
        QueryClass qc2 = new QueryClass(Department.class);
        q2.addToSelect(qc2);
        q2.addFrom(qc2);

        Constraint c2 = new ClassConstraint(qc2, ClassConstraint.EQUALS, new Department());
        q2.setConstraint(c2);

        assertEquals(q1, q2);
        Department d1 = new Department();
        d1.setName("Department1");
        c2 = new ClassConstraint(qc2, ClassConstraint.EQUALS, d1);
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


}
