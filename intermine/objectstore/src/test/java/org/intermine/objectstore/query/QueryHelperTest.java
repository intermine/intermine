package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Employee;

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

        //main
        try {
            QueryHelper.addAndConstraint(null, new BagConstraint(qc, ConstraintOp.IN, new HashSet()));
            fail("Expected NullPointerException, q parameter null");
        } catch (NullPointerException e) {
        }
        try {
            QueryHelper.addAndConstraint(q, null);
            fail("Expected NullPointerException, constraint parameter null");
        } catch (NullPointerException e) {
        }
    }


    public void testAddConstraintEmpty() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc = new SimpleConstraint(new QueryField(qc, "name"),
                                                   ConstraintOp.EQUALS, new QueryValue("Bob"));

        q.setConstraint(sc);
        QueryHelper.addAndConstraint(q, new ConstraintSet(ConstraintOp.AND));

        assertEquals(1, ((ConstraintSet) q.getConstraint()).getConstraints().size());
    }

    public void testAddConstraintToNull() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        Constraint sc = new SimpleConstraint(new QueryField(qc, "name"),
                                             ConstraintOp.EQUALS, new QueryValue("Bob"));
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(sc);

        QueryHelper.addAndConstraint(q, cs);

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
        QueryHelper.addAndConstraint(q, cs2);

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
        QueryHelper.addAndConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }

    public void testAddConstraintToConstraintSetOrAnd() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        SimpleConstraint sc1 =
            new SimpleConstraint(new QueryField(qc, "name"),
                                 ConstraintOp.EQUALS, new QueryValue("Bob"));
        SimpleConstraint sc2 =
            new SimpleConstraint(new QueryField(qc, "age"),
                                 ConstraintOp.EQUALS, new QueryValue(new Integer(54)));
        ConstraintSet cs1 = new ConstraintSet(ConstraintOp.OR);
        cs1.addConstraint(sc1);
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.AND);
        cs2.addConstraint(sc2);

        q.setConstraint(cs1);
        QueryHelper.addAndConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(cs1);
        cs3.addConstraint(sc2);
        assertEquals(cs3, q.getConstraint());
    }

    public void testAddConstraintToConstraintSetAndOr() throws Exception {
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
        ConstraintSet cs2 = new ConstraintSet(ConstraintOp.OR);
        cs2.addConstraint(sc2);

        q.setConstraint(cs1);
        QueryHelper.addAndConstraint(q, cs2);

        ConstraintSet cs3 = new ConstraintSet(ConstraintOp.AND);
        cs3.addConstraint(sc1);
        cs3.addConstraint(cs2);
        assertEquals(cs3, q.getConstraint());
    }

}
