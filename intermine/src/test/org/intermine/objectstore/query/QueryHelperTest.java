package org.intermine.objectstore.query;

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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.text.SimpleDateFormat;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Company;
import org.intermine.objectstore.query.fql.FqlQuery;

import org.apache.log4j.Logger;

public class QueryHelperTest extends TestCase
{
    protected static final Logger LOG = Logger.getLogger(QueryHelperTest.class);

    Model model;

    public QueryHelperTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testAddQueryClass() throws Exception {
        Query q = new Query();

        QueryHelper.addQueryClass(q,new QueryClass(Employee.class));

        assertEquals(Employee.class, ((QueryClass) q.getFrom().iterator().next()).getType());
    }

    public void testAddToQuery() throws Exception {
        Query q = new Query();

        QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                  ConstraintOp.EQUALS, new QueryValue("Dennis"));

        assertEquals(Employee.class, ((QueryClass) q.getFrom().iterator().next()).getType());

        SimpleConstraint sc = (SimpleConstraint) q.getConstraint();
        assertEquals("name", ((QueryField) sc.getArg1()).getFieldName());
        assertEquals(String.class, ((QueryField) sc.getArg1()).getType());
        assertEquals(ConstraintOp.EQUALS, sc.getOp());
        assertEquals("Dennis", ((QueryValue) sc.getArg2()).getValue());
        assertEquals(String.class, ((QueryValue) sc.getArg2()).getType());
    }

    public void testAddSubqueryConstraint() throws Exception {
        Query q = new Query();
        Query subQuery = new FqlQuery("SELECT a1 from Employee as a1",
                                      "org.intermine.model.testmodel").toQuery();

        QueryClass qc = new QueryClass(Employee.class);

        QueryHelper.addConstraint(q, "id", qc,
                                  ConstraintOp.IN, subQuery);

        LOG.info("testAddSubqueryConstraint(): " + q.getConstraint());

        assertEquals(Employee.class, ((QueryClass) q.getFrom().iterator().next()).getType());

        SubqueryConstraint sc = (SubqueryConstraint) q.getConstraint();
        assertEquals(qc, (QueryClass) sc.getQueryClass());
        assertEquals(ConstraintOp.IN, sc.getOp());
    }

    // Add a QueryClass and its constraints to a query then alter and add
    // the same again - i.e. editing existing QueryClass
    public void testAddToQueryExists() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);

        QueryHelper.addConstraint(q, "name", qc,
                                  ConstraintOp.EQUALS, new QueryValue("Dennis"));
        assertTrue(q.getConstraint() instanceof SimpleConstraint);
        QueryHelper.addConstraint(q, "fullTime", qc,
                                  ConstraintOp.EQUALS, new QueryValue(Boolean.TRUE));

        LOG.info("testAddToQueryExists(): " + q.getConstraint());
        LOG.info("testAddToQueryExists(): " +
                 ((ConstraintSet) q.getConstraint()).getConstraints());

        assertEquals(1, q.getFrom().size());
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());

        QueryHelper.addConstraint(q, "name", qc,
                                  ConstraintOp.EQUALS, new QueryValue("Gerald"));
        QueryHelper.addConstraint(q, "fullTime", qc,
                                  ConstraintOp.NOT_EQUALS, new QueryValue(Boolean.TRUE));
        QueryHelper.addConstraint(q, "age", qc,
                                  ConstraintOp.EQUALS, new QueryValue(new Integer(43)));

        assertEquals(1, q.getFrom().size());

        Set constraints = ((ConstraintSet) q.getConstraint()).getConstraints();
        assertFalse(constraints.iterator().next() instanceof ConstraintSet);
        assertEquals(5, constraints.size());
    }

    public void testAddToQueryNullParameters() throws Exception {
        Query q = new Query();

        try {
            QueryHelper.addConstraint(null, "name", new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, new QueryValue("Dennis"));
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, null, new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, new QueryValue("Dennis"));
            fail("Expected NullPointerException, fieldName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", null,
                                      ConstraintOp.EQUALS, new QueryValue("Dennis"));
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                      null, new QueryValue("Dennis"));
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, (QueryValue) null);
            fail("Expected NullPointerException, qv parameter null");
        } catch (NullPointerException e) {
        }


        try {
            QueryHelper.addConstraint(null, "name", new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, new Query());
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, null, new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, new Query());
            fail("Expected NullPointerException, fieldName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", null,
                                      ConstraintOp.EQUALS, new Query());
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                      null, new Query());
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, (Query) null);
            fail("Expected NullPointerException, qv parameter null");
        } catch (NullPointerException e) {
        }


        try {
            QueryHelper.addConstraint(null, "name", new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, new HashSet());
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, null, new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, new HashSet());
            fail("Expected NullPointerException, fieldName parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", null,
                                      ConstraintOp.EQUALS, new HashSet());
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                      null, new HashSet());
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(q, "name", new QueryClass(Employee.class),
                                      ConstraintOp.EQUALS, (Collection) null);
            fail("Expected NullPointerException, qv parameter null");
        } catch (NullPointerException e) {
        }
    }


    public void testRemoveFromQuery() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        q.addToSelect(qc2);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        assertEquals(2, q.getSelect().size());
        assertEquals(2, q.getFrom().size());
        QueryHelper.removeFromQuery(q, qc1);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        assertEquals(1, q.getSelect().size());
        assertEquals(1, q.getFrom().size());
        QueryHelper.removeFromQuery(q, qc2);
        assertEquals(0, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        assertEquals(0, q.getSelect().size());
        assertEquals(0, q.getFrom().size());
    }


    public void testRemoveFromQueryNotExists() throws Exception {
                Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        q.addToSelect(qc2);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        QueryClass qc3 = new QueryClass(Employee.class);

        try {
            QueryHelper.removeFromQuery(q, qc2);
        } catch (Exception e) {
            fail("Expected no Exception to be thrown but was: " + e.getClass());
        }

    }

    public void testRemoveFromQueryNullArguments() throws Exception {
        try {
            QueryHelper.removeFromQuery(null, new QueryClass(Employee.class));
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.removeFromQuery(new Query(), null);
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }
    }

    public void testAddConstraintNull() throws Exception {
       try {
           QueryHelper.addConstraint(null, new QueryClass(Employee.class),
                                     new ConstraintSet(ConstraintOp.AND));
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }

        try {
            QueryHelper.addConstraint(new Query(), new QueryClass(Employee.class), null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testAddConstraintEmpty() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"),
                                                   ConstraintOp.EQUALS, new QueryValue("Bob"));

        q.setConstraint(sc);
        QueryHelper.addConstraint(q, qc,
                                  new ConstraintSet(ConstraintOp.AND));

        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }

    public void testAddConstraintToNull() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        Constraint sc = new SimpleConstraint(new QueryField(qc, "name"),
                                             ConstraintOp.EQUALS, new QueryValue("Bob"));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(sc);

        QueryHelper.addConstraint(q, qc, cs);

        assertEquals(cs, q.getConstraint());
    }

    public void testAddConstraintToConstraint() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 =
            new SimpleConstraint(new QueryField(qc, "name"),
                                 ConstraintOp.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 =
            new SimpleConstraint(new QueryField(qc, "age"),
                                 ConstraintOp.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(sc1);
        QueryHelper.addConstraint(q, qc, cs2);

        assertTrue(q.getConstraint() instanceof ConstraintSet);

        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, ((ConstraintSet) q.getConstraint()));
    }


    public void testAddConstraintToConstraintSet() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 =
            new SimpleConstraint(new QueryField(qc, "name"),
                                 ConstraintOp.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 =
            new SimpleConstraint(new QueryField(qc, "age"),
                                 ConstraintOp.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.AND);
        cs1.addConstraint(sc1);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(cs1);
        QueryHelper.addConstraint(q, qc, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }


    public void testRemoveConstraintsAssociated() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("company1"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        SimpleConstraint sc3 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryField(qc2, "name"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        c.addConstraint(sc3);
        q.setConstraint(c);

        // cross-reference (sc3) should not get removed
        assertEquals(3, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc1, false);
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc2, false);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }

    public void testRemoveConstraintsRelated() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        q.addFrom(qc1);
        q.addFrom(qc2);

        SimpleConstraint sc1 = new SimpleConstraint(new QueryField(qc1, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryField(qc2, "name"));
        SimpleConstraint sc2 = new SimpleConstraint(new QueryField(qc2, "name"),
                                                    ConstraintOp.EQUALS,
                                                    new QueryValue("department1"));
        ConstraintSet c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);

        // remove qc1, leaves sc2
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc1, true);
        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());

        // removing qc2 gets rid of all constraints
        c = new ConstraintSet(ConstraintOp.AND);
        c.addConstraint(sc1);
        c.addConstraint(sc2);
        q.setConstraint(c);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        assertEquals(2, ((ConstraintSet) q.getConstraint()).getConstraints().size());
        QueryHelper.removeConstraints(q, qc2, true);
        assertEquals(0, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }

    public void testRemoveConstraintsSimple() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Department.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"),
                                                   ConstraintOp.EQUALS,
                                                   new QueryValue("Dave"));
        q.setConstraint(sc);
        
        QueryHelper.removeFromQuery(q, qc);
        assertEquals(0, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }
}
