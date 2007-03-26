package org.intermine.sql.query;

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

public class ConstraintSetTest extends TestCase
{
    private AbstractValue v1, v2, v3, v4, a;
    private AbstractConstraint c1, c2, c3, c4, c5, c6;
    private AbstractConstraint nc1, nc2, nc3, nc4, nc5, nc6;
    private ConstraintSet cs1, cs2, cs3, cs4;

    public ConstraintSetTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        v1 = new Constant("1");
        v2 = new Constant("2");
        v3 = new Constant("'Flibble'");
        v4 = new Constant("'Flobble'");
        a = new Field("a", new Table("table1"));
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
        cs3.add(c2);
        cs3.add(nc5);
        cs4 = new ConstraintSet();
        cs4.add(c5);
        cs4.add(c2);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("table1.a = 1", cs1.getSQLString());
        assertTrue("Expected certain string from cs2.getSQLString()",
                "(table1.a = 2 OR table1.a < 1 OR table1.a >= 2)".equals(cs2.getSQLString())
                || "(table1.a = 2 OR table1.a >= 2 OR table1.a < 1)".equals(cs2.getSQLString())
                || "(table1.a < 1 OR table1.a = 2 OR table1.a >= 2)".equals(cs2.getSQLString())
                || "(table1.a < 1 OR table1.a >= 2 OR table1.a = 2)".equals(cs2.getSQLString())
                || "(table1.a >= 2 OR table1.a < 1 OR table1.a = 2)".equals(cs2.getSQLString())
                || "(table1.a >= 2 OR table1.a = 2 OR table1.a < 1)".equals(cs2.getSQLString()));
        assertTrue("Expected certain string from cs3.getSQLString()",
                "(table1.a = 2 OR 1 >= table1.a)".equals(cs3.getSQLString())
                || "(1 >= table1.a OR table1.a = 2)".equals(cs3.getSQLString()));
    }

    public void testCompareCS() throws Exception {
        assertEquals(AbstractConstraint.EQUAL, c1.compare(cs1));
        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(cs1));
        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(cs1));
        assertEquals(AbstractConstraint.IMPLIED_BY, c4.compare(cs1));
        assertEquals(AbstractConstraint.EXCLUDES, c5.compare(cs1));
        assertEquals(AbstractConstraint.INDEPENDENT, c6.compare(cs1));

        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(cs2));
        assertEquals(AbstractConstraint.IMPLIES, c2.compare(cs2));
        assertEquals(AbstractConstraint.IMPLIES, c3.compare(cs2));
        assertEquals(AbstractConstraint.OR, c4.compare(cs2));
        assertEquals(AbstractConstraint.INDEPENDENT, c5.compare(cs2));
        assertEquals(AbstractConstraint.INDEPENDENT, c6.compare(cs2));

        assertEquals(AbstractConstraint.IMPLIES, c1.compare(cs3));
        assertEquals(AbstractConstraint.IMPLIES, c2.compare(cs3));
        assertEquals(AbstractConstraint.IMPLIES, c3.compare(cs3));
        assertEquals(AbstractConstraint.INDEPENDENT, c4.compare(cs3));
        assertEquals(AbstractConstraint.OR, c5.compare(cs3));
        assertEquals(AbstractConstraint.INDEPENDENT, c6.compare(cs3));

        assertEquals(AbstractConstraint.OPPOSITE, nc1.compare(cs1));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc2.compare(cs1));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc3.compare(cs1));
        assertEquals(AbstractConstraint.EXCLUDES, nc4.compare(cs1));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc5.compare(cs1));
        assertEquals(AbstractConstraint.INDEPENDENT, nc6.compare(cs1));

        assertEquals(AbstractConstraint.IMPLIED_BY, nc1.compare(cs2));
        assertEquals(AbstractConstraint.OR, nc2.compare(cs2));
        assertEquals(AbstractConstraint.OR, nc3.compare(cs2));
        assertEquals(AbstractConstraint.IMPLIES, nc4.compare(cs2));
        assertEquals(AbstractConstraint.INDEPENDENT, nc5.compare(cs2));
        assertEquals(AbstractConstraint.INDEPENDENT, nc6.compare(cs2));

        assertEquals(AbstractConstraint.OR, nc1.compare(cs3));
        assertEquals(AbstractConstraint.OR, nc2.compare(cs3));
        assertEquals(AbstractConstraint.OR, nc3.compare(cs3));
        assertEquals(AbstractConstraint.INDEPENDENT, nc4.compare(cs3));
        assertEquals(AbstractConstraint.IMPLIES, nc5.compare(cs3));
        assertEquals(AbstractConstraint.INDEPENDENT, nc6.compare(cs3));

    }

    public void testCompareSC() throws Exception {

        assertEquals(AbstractConstraint.EQUAL, cs1.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, cs1.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, cs1.compare(c3));
        assertEquals(AbstractConstraint.IMPLIES, cs1.compare(c4));
        assertEquals(AbstractConstraint.EXCLUDES, cs1.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, cs1.compare(c6));

        assertEquals(AbstractConstraint.EXCLUDES, cs2.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, cs2.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, cs2.compare(c3));
        assertEquals(AbstractConstraint.OR, cs2.compare(c4));
        assertEquals(AbstractConstraint.INDEPENDENT, cs2.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, cs2.compare(c6));

        assertEquals(AbstractConstraint.IMPLIED_BY, cs3.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, cs3.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, cs3.compare(c3));
        assertEquals(AbstractConstraint.INDEPENDENT, cs3.compare(c4));
        assertEquals(AbstractConstraint.OR, cs3.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, cs3.compare(c6));

        assertEquals(AbstractConstraint.OPPOSITE, cs1.compare(nc1));
        assertEquals(AbstractConstraint.IMPLIES, cs1.compare(nc2));
        assertEquals(AbstractConstraint.IMPLIES, cs1.compare(nc3));
        assertEquals(AbstractConstraint.EXCLUDES, cs1.compare(nc4));
        assertEquals(AbstractConstraint.IMPLIES, cs1.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, cs1.compare(nc6));

        assertEquals(AbstractConstraint.IMPLIES, cs2.compare(nc1));
        assertEquals(AbstractConstraint.OR, cs2.compare(nc2));
        assertEquals(AbstractConstraint.OR, cs2.compare(nc3));
        assertEquals(AbstractConstraint.IMPLIED_BY, cs2.compare(nc4));
        assertEquals(AbstractConstraint.INDEPENDENT, cs2.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, cs2.compare(nc6));

        assertEquals(AbstractConstraint.OR, cs3.compare(nc1));
        assertEquals(AbstractConstraint.OR, cs3.compare(nc2));
        assertEquals(AbstractConstraint.OR, cs3.compare(nc3));
        assertEquals(AbstractConstraint.INDEPENDENT, cs3.compare(nc4));
        assertEquals(AbstractConstraint.IMPLIED_BY, cs3.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, cs3.compare(nc6));

    }

    public void testCompareSS() throws Exception {

        assertEquals(AbstractConstraint.EQUAL, cs1.compare(cs1));
        assertEquals(AbstractConstraint.EXCLUDES, cs1.compare(cs2));
        assertEquals(AbstractConstraint.IMPLIES, cs1.compare(cs3));
        assertEquals(AbstractConstraint.EXCLUDES, cs1.compare(cs4));

        assertEquals(AbstractConstraint.EXCLUDES, cs2.compare(cs1));
        assertEquals(AbstractConstraint.EQUAL, cs2.compare(cs2));
        assertEquals(AbstractConstraint.INDEPENDENT, cs2.compare(cs3));
        assertEquals(AbstractConstraint.INDEPENDENT, cs2.compare(cs4));

        assertEquals(AbstractConstraint.IMPLIED_BY, cs3.compare(cs1));
        assertEquals(AbstractConstraint.INDEPENDENT, cs3.compare(cs2));
        assertEquals(AbstractConstraint.EQUAL, cs3.compare(cs3));
        assertEquals(AbstractConstraint.OR, cs3.compare(cs4));

        assertEquals(AbstractConstraint.EXCLUDES, cs4.compare(cs1));
        assertEquals(AbstractConstraint.INDEPENDENT, cs4.compare(cs2));
        assertEquals(AbstractConstraint.OR, cs4.compare(cs3));
        assertEquals(AbstractConstraint.EQUAL, cs4.compare(cs4));

    }



}
