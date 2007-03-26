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

public class NotConstraintTest extends TestCase
{
    private AbstractValue v1, v2, v3, v4, a;
    private AbstractConstraint c1, c2, c3, c4, c5, c6, c7, c8;
    private AbstractConstraint nc1, nc2, nc3, nc4, nc5, nc6, nc7, nc8;

    public NotConstraintTest(String arg1) {
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
        c7 = new Constraint(a, Constraint.LIKE, v3);
        c8 = new Constraint(a, Constraint.LIKE, v4);
        nc1 = new NotConstraint(c1);
        nc2 = new NotConstraint(c2);
        nc3 = new NotConstraint(c3);
        nc4 = new NotConstraint(c4);
        nc5 = new NotConstraint(c5);
        nc6 = new NotConstraint(c6);
        nc7 = new NotConstraint(c7);
        nc8 = new NotConstraint(c8);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("table1.a != 1", nc1.getSQLString());
        assertEquals("table1.a != 2", nc2.getSQLString());
        assertEquals("table1.a >= 1", nc3.getSQLString());
        assertEquals("table1.a >= 2", nc4.getSQLString());
        assertEquals("1 >= table1.a", nc5.getSQLString());
        assertEquals("'Flibble' != 'Flobble'", nc6.getSQLString());
        assertEquals("table1.a NOT LIKE 'Flibble'", nc7.getSQLString());
    }

    public void testCompareNC() throws Exception {
        assertEquals(AbstractConstraint.OPPOSITE, nc1.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc1.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc1.compare(c3));
        assertEquals(AbstractConstraint.OR, nc1.compare(c4));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc1.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc1.compare(c7));

        assertEquals(AbstractConstraint.IMPLIED_BY, nc2.compare(c1));
        assertEquals(AbstractConstraint.OPPOSITE, nc2.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc2.compare(c3));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc2.compare(c4));
        assertEquals(AbstractConstraint.OR, nc2.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc2.compare(c7));

        assertEquals(AbstractConstraint.IMPLIED_BY, nc3.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc3.compare(c2));
        assertEquals(AbstractConstraint.OPPOSITE, nc3.compare(c3));
        assertEquals(AbstractConstraint.OR, nc3.compare(c4));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc3.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc3.compare(c7));

        assertEquals(AbstractConstraint.EXCLUDES, nc4.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc4.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, nc4.compare(c3));
        assertEquals(AbstractConstraint.OPPOSITE, nc4.compare(c4));
        assertEquals(AbstractConstraint.IMPLIES, nc4.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc4.compare(c7));

        assertEquals(AbstractConstraint.IMPLIED_BY, nc5.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, nc5.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc5.compare(c3));
        assertEquals(AbstractConstraint.IMPLIES, nc5.compare(c4));
        assertEquals(AbstractConstraint.OPPOSITE, nc5.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc5.compare(c7));

        assertEquals(AbstractConstraint.INDEPENDENT, nc6.compare(c1));

        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(c1));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(c2));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(c3));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(c4));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(c5));
        assertEquals(AbstractConstraint.OPPOSITE, nc7.compare(c7));
    }

    public void testCompareCN() throws Exception {
        assertEquals(AbstractConstraint.OPPOSITE, c1.compare(nc1));
        assertEquals(AbstractConstraint.IMPLIES, c1.compare(nc2));
        assertEquals(AbstractConstraint.IMPLIES, c1.compare(nc3));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(nc4));
        assertEquals(AbstractConstraint.IMPLIES, c1.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, c1.compare(nc7));

        assertEquals(AbstractConstraint.IMPLIES, c2.compare(nc1));
        assertEquals(AbstractConstraint.OPPOSITE, c2.compare(nc2));
        assertEquals(AbstractConstraint.IMPLIES, c2.compare(nc3));
        assertEquals(AbstractConstraint.IMPLIES, c2.compare(nc4));
        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, c2.compare(nc7));

        assertEquals(AbstractConstraint.IMPLIES, c3.compare(nc1));
        assertEquals(AbstractConstraint.IMPLIES, c3.compare(nc2));
        assertEquals(AbstractConstraint.OPPOSITE, c3.compare(nc3));
        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(nc4));
        assertEquals(AbstractConstraint.IMPLIES, c3.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, c3.compare(nc7));

        assertEquals(AbstractConstraint.OR, c4.compare(nc1));
        assertEquals(AbstractConstraint.IMPLIES, c4.compare(nc2));
        assertEquals(AbstractConstraint.OR, c4.compare(nc3));
        assertEquals(AbstractConstraint.OPPOSITE, c4.compare(nc4));
        assertEquals(AbstractConstraint.IMPLIED_BY, c4.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, c4.compare(nc7));

        assertEquals(AbstractConstraint.IMPLIES, c5.compare(nc1));
        assertEquals(AbstractConstraint.OR, c5.compare(nc2));
        assertEquals(AbstractConstraint.IMPLIES, c5.compare(nc3));
        assertEquals(AbstractConstraint.IMPLIED_BY, c5.compare(nc4));
        assertEquals(AbstractConstraint.OPPOSITE, c5.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, c5.compare(nc7));

        assertEquals(AbstractConstraint.INDEPENDENT, c6.compare(nc1));

        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(nc1));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(nc2));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(nc3));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(nc4));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(nc5));
        assertEquals(AbstractConstraint.OPPOSITE, c7.compare(nc7));
    }

    public void testCompareNN() throws Exception {
        assertEquals(AbstractConstraint.EQUAL, nc1.compare(nc1));
        assertEquals(AbstractConstraint.OR, nc1.compare(nc2));
        assertEquals(AbstractConstraint.OR, nc1.compare(nc3));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc1.compare(nc4));
        assertEquals(AbstractConstraint.OR, nc1.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc1.compare(nc7));

        assertEquals(AbstractConstraint.OR, nc2.compare(nc1));
        assertEquals(AbstractConstraint.EQUAL, nc2.compare(nc2));
        assertEquals(AbstractConstraint.OR, nc2.compare(nc3));
        assertEquals(AbstractConstraint.OR, nc2.compare(nc4));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc2.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc2.compare(nc7));

        assertEquals(AbstractConstraint.OR, nc3.compare(nc1));
        assertEquals(AbstractConstraint.OR, nc3.compare(nc2));
        assertEquals(AbstractConstraint.EQUAL, nc3.compare(nc3));
        assertEquals(AbstractConstraint.IMPLIED_BY, nc3.compare(nc4));
        assertEquals(AbstractConstraint.OR, nc3.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc3.compare(nc7));

        assertEquals(AbstractConstraint.IMPLIES, nc4.compare(nc1));
        assertEquals(AbstractConstraint.OR, nc4.compare(nc2));
        assertEquals(AbstractConstraint.IMPLIES, nc4.compare(nc3));
        assertEquals(AbstractConstraint.EQUAL, nc4.compare(nc4));
        assertEquals(AbstractConstraint.EXCLUDES, nc4.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc4.compare(nc7));

        assertEquals(AbstractConstraint.OR, nc5.compare(nc1));
        assertEquals(AbstractConstraint.IMPLIES, nc5.compare(nc2));
        assertEquals(AbstractConstraint.OR, nc5.compare(nc3));
        assertEquals(AbstractConstraint.EXCLUDES, nc5.compare(nc4));
        assertEquals(AbstractConstraint.EQUAL, nc5.compare(nc5));
        assertEquals(AbstractConstraint.INDEPENDENT, nc5.compare(nc7));

        assertEquals(AbstractConstraint.INDEPENDENT, nc6.compare(nc1));

        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(nc1));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(nc2));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(nc3));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(nc4));
        assertEquals(AbstractConstraint.INDEPENDENT, nc7.compare(nc5));
        assertEquals(AbstractConstraint.EQUAL, nc7.compare(nc7));
    }
}
