package org.flymine.sql.query;

import junit.framework.*;

public class ConstraintSetTest extends TestCase
{
    private AbstractValue v1, v2, v3, v4, a;
    private AbstractConstraint c1, c2, c3, c4, c5, c6;
    private AbstractConstraint nc1, nc2, nc3, nc4, nc5, nc6;
    private ConstraintSet cs1, cs2, cs3;

    public ConstraintSetTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        v1 = new Constant("1");
        v2 = new Constant("2");
        v3 = new Constant("'Flibble'");
        v4 = new Constant("'Flobble'");
        a = new Constant("a");
        c1 = new Constraint(a, Constraint.EQ, v1);
        c2 = new Constraint(a, Constraint.EQ, v2);
        c3 = new Constraint(a, Constraint.LT, v1);
        c4 = new Constraint(a, Constraint.LT, v2);
        c5 = new Constraint(v1, Constraint.LT, a);
        c6 = new Constraint(v3, Constraint.EQ, v4);
        nc1 = new NotConstraint(c1);
        nc2 = new NotConstraint(c2);
        nc3 = new NotConstraint(c3);
        nc4 = new NotConstraint(c4);
        nc5 = new NotConstraint(c5);
        nc6 = new NotConstraint(c6);
        cs1 = new ConstraintSet();
        cs1.add(c1);
        cs2 = new ConstraintSet();
        cs2.add(c2);
        cs2.add(c3);
        cs2.add(nc4);
        cs3 = new ConstraintSet();
        cs3.add(nc1);
        cs3.add(nc5);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("a = 1", cs1.getSQLString());
        assertTrue("Expected certain string from cs2.getSQLString()", 
                "(a = 2 OR a < 1 OR a >= 2)".equals(cs2.getSQLString())
                || "(a = 2 OR a >= 2 OR a < 1)".equals(cs2.getSQLString())
                || "(a < 1 OR a = 2 OR a >= 2)".equals(cs2.getSQLString())
                || "(a < 1 OR a >= 2 OR a = 2)".equals(cs2.getSQLString())
                || "(a >= 2 OR a < 1 OR a = 2)".equals(cs2.getSQLString())
                || "(a >= 2 OR a = 2 OR a < 1)".equals(cs2.getSQLString()));
        assertTrue("Expected certain string from cs3.getSQLString()", 
                "(a != 1 OR 1 >= a)".equals(cs3.getSQLString())
                || "1 >= a OR a != 1)".equals(cs3.getSQLString()));
    }
}
