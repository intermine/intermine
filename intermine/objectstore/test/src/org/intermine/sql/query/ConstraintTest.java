package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class ConstraintTest extends TestCase
{
    private AbstractValue v1, v2, v3, v4, a, b, c;
    private Constraint c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12;

    public ConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        v1 = new Constant("1");
        v2 = new Constant("2");
        v3 = new Constant("'Flibble'");
        v4 = new Constant("'Flobble'");
        a = new Field("a", new Table("table1"));
        b = new Field("b", new Table("table1"));
        c = new Field("c", new Table("table1"));
        c1 = new Constraint(a, Constraint.EQ, v1);
        c2 = new Constraint(a, Constraint.EQ, v2);
        c3 = new Constraint(a, Constraint.LT, v1);
        c4 = new Constraint(a, Constraint.LT, v2);
        c5 = new Constraint(v1, Constraint.LT, a);
        c6 = new Constraint(v3, Constraint.EQ, v4);
        c7 = new Constraint(a, Constraint.LIKE, v3);
        c8 = new Constraint(a, Constraint.LIKE, v4);
        c9 = new Constraint(a, Constraint.EQ, b);
        c10 = new Constraint(a, Constraint.LT, b);
        c11 = new Constraint(b, Constraint.LT, a);
        c12 = new Constraint(a, Constraint.EQ, c);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("table1.a = 1", c1.getSQLString());
        assertEquals("table1.a = 2", c2.getSQLString());
        assertEquals("table1.a < 1", c3.getSQLString());
        assertEquals("table1.a < 2", c4.getSQLString());
        assertEquals("1 < table1.a", c5.getSQLString());
        assertEquals("'Flibble' = 'Flobble'", c6.getSQLString());
        assertEquals("table1.a LIKE 'Flibble'", c7.getSQLString());
        assertEquals("table1.a LIKE 'Flobble'", c8.getSQLString());
        assertEquals("table1.a = table1.b", c9.getSQLString());
        assertEquals("table1.a < table1.b", c10.getSQLString());
        assertEquals("table1.b < table1.a", c11.getSQLString());
        assertEquals("table1.a = table1.c", c12.getSQLString());
    }

    public void testCompare() throws Exception {
        assertEquals(AbstractConstraint.EQUAL, c1.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(c3));
        assertEquals(AbstractConstraint.IMPLIES, c1.compare(c4));
        assertEquals(AbstractConstraint.EXCLUDES, c1.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c1.compare(c7));
        assertEquals(AbstractConstraint.INDEPENDENT, c1.compare(c9));
        assertEquals(AbstractConstraint.INDEPENDENT, c1.compare(c10));
        assertEquals(AbstractConstraint.INDEPENDENT, c1.compare(c11));

        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(c1));
        assertEquals(AbstractConstraint.EQUAL, c2.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(c3));
        assertEquals(AbstractConstraint.EXCLUDES, c2.compare(c4));
        assertEquals(AbstractConstraint.IMPLIES, c2.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c2.compare(c7));

        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(c2));
        assertEquals(AbstractConstraint.EQUAL, c3.compare(c3));
        assertEquals(AbstractConstraint.IMPLIES, c3.compare(c4));
        assertEquals(AbstractConstraint.EXCLUDES, c3.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c3.compare(c7));

        assertEquals(AbstractConstraint.IMPLIED_BY, c4.compare(c1));
        assertEquals(AbstractConstraint.EXCLUDES, c4.compare(c2));
        assertEquals(AbstractConstraint.IMPLIED_BY, c4.compare(c3));
        assertEquals(AbstractConstraint.EQUAL, c4.compare(c4));
        assertEquals(AbstractConstraint.OR, c4.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c4.compare(c7));

        assertEquals(AbstractConstraint.EXCLUDES, c5.compare(c1));
        assertEquals(AbstractConstraint.IMPLIED_BY, c5.compare(c2));
        assertEquals(AbstractConstraint.EXCLUDES, c5.compare(c3));
        assertEquals(AbstractConstraint.OR, c5.compare(c4));
        assertEquals(AbstractConstraint.EQUAL, c5.compare(c5));
        assertEquals(AbstractConstraint.INDEPENDENT, c5.compare(c7));


        assertEquals(AbstractConstraint.INDEPENDENT, c6.compare(c1));

        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c1));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c2));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c3));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c4));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c5));
        assertEquals(AbstractConstraint.EQUAL, c7.compare(c7));
        assertEquals(AbstractConstraint.INDEPENDENT, c7.compare(c8));

        assertEquals(AbstractConstraint.EQUAL, c9.compare(c9));
        assertEquals(AbstractConstraint.EXCLUDES, c9.compare(c10));
        assertEquals(AbstractConstraint.EXCLUDES, c9.compare(c11));
        assertEquals(AbstractConstraint.INDEPENDENT, c9.compare(c12));

        assertEquals(AbstractConstraint.EXCLUDES, c10.compare(c9));
        assertEquals(AbstractConstraint.EQUAL, c10.compare(c10));
        assertEquals(AbstractConstraint.EXCLUDES, c10.compare(c11));
        assertEquals(AbstractConstraint.INDEPENDENT, c10.compare(c12));

        assertEquals(AbstractConstraint.EXCLUDES, c11.compare(c9));
        assertEquals(AbstractConstraint.EXCLUDES, c11.compare(c10));
        assertEquals(AbstractConstraint.EQUAL, c11.compare(c11));
        assertEquals(AbstractConstraint.INDEPENDENT, c11.compare(c12));

        assertEquals(AbstractConstraint.INDEPENDENT, c12.compare(c9));
        assertEquals(AbstractConstraint.INDEPENDENT, c12.compare(c10));
        assertEquals(AbstractConstraint.INDEPENDENT, c12.compare(c11));
        assertEquals(AbstractConstraint.EQUAL, c12.compare(c12));

    }
}
