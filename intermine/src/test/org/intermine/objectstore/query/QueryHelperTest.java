package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Employee;

import org.apache.log4j.Logger;

public class QueryHelperTest extends TestCase
{
    private static final Logger LOG = Logger.getLogger(QueryHelperTest.class);

    Model model;

    public QueryHelperTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testAddConstraintNull() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);

        //simpleconstraint
        try {
            QueryHelper.addConstraint(q, null, qc, ConstraintOp.EQUALS, new QueryValue("Dennis"));
            fail("Expected NullPointerException, fieldName parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, "name", null, ConstraintOp.EQUALS, new QueryValue("Dennis"));
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, "name", qc, null, new QueryValue("Dennis"));
            fail("Expected NullPointerException, ops parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, "name", qc, ConstraintOp.EQUALS, (QueryValue) null);
            fail("Expected NullPointerException, qv parameter null");
        } catch (NullPointerException e) {
        }

        //queryfield bagconstraint
        try {
            QueryHelper.addConstraint(q, null, qc, ConstraintOp.EQUALS, new HashSet());
            fail("Expected NullPointerException, fieldName parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, "name", null, ConstraintOp.EQUALS, new HashSet());
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, "name", qc, null, new HashSet());
            fail("Expected NullPointerException, op parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, "name", qc, ConstraintOp.EQUALS, (QueryValue) null);
            fail("Expected NullPointerException, qv parameter null");
        } catch (NullPointerException e) {
        }

        //main
        try {
            QueryHelper.addConstraint(null, qc, new BagConstraint(qc, ConstraintOp.IN, new HashSet()));
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, null, new BagConstraint(qc, ConstraintOp.IN, new HashSet()));
            fail("Expected NullPointerException, qc parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addConstraint(q, qc, null);
            fail("Expected NullPointerException, constraint parameter null");
        } catch (NullPointerException e) {
        }
    }

    public void testAddSimpleConstraint() throws Exception {
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

    // Add a QueryClass and its constraints to a query then alter and add
    // the same again - i.e. editing existing QueryClass
    public void testAddSimpleConstraintExists() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);

        QueryHelper.addConstraint(q, "name", qc,
                                  ConstraintOp.EQUALS, new QueryValue("Dennis"));
        assertTrue(q.getConstraint() instanceof SimpleConstraint);
        QueryHelper.addConstraint(q, "fullTime", qc,
                                  ConstraintOp.EQUALS, new QueryValue(Boolean.TRUE));

        LOG.debug("testAddToQueryExists(): " + q.getConstraint());
        LOG.debug("testAddToQueryExists(): " +
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

    public void testAddConstraintEmpty() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"),
                                                   ConstraintOp.EQUALS, new QueryValue("Bob"));

        q.setConstraint(sc);
        QueryHelper.addConstraint(q, qc, new ConstraintSet(ConstraintOp.AND));

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

    public void testAddQueryClass() throws Exception {
        Query q = new Query();

        QueryHelper.addQueryClass(q,new QueryClass(Employee.class));

        assertEquals(Employee.class, ((QueryClass) q.getFrom().iterator().next()).getType());
    }
}
