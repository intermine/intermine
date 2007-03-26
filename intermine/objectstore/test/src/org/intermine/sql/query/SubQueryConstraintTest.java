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

public class SubQueryConstraintTest extends TestCase
{
    private AbstractValue v1, v2;
    private Query q1, q2;
    private SubQueryConstraint c1, c2, c3, c4;

    public SubQueryConstraintTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        q1 = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q1.addFrom(t);
        q1.addSelect(sv);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));

        q2 = new Query();
        t = new Table("mytable2");
        c = new Constant("2");
        f = new Field("b", t);
        sv = new SelectValue(f, null);
        q2.addFrom(t);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f, Constraint.LT, c));

        v1 = new Constant("5");
        v2 = new Constant("7");
        
        c1 = new SubQueryConstraint(v1, q1);
        c2 = new SubQueryConstraint(v1, q2);
        c3 = new SubQueryConstraint(v2, q1);
        c4 = new SubQueryConstraint(v1, q1);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("5 IN (SELECT mytable.a FROM mytable WHERE mytable.a = 1)", c1.getSQLString());
        assertEquals("5 IN (SELECT mytable2.b FROM mytable2 WHERE mytable2.b < 2)",
                c2.getSQLString());
        assertEquals("7 IN (SELECT mytable.a FROM mytable WHERE mytable.a = 1)", c3.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(c1, c1);
        assertTrue("Expected c1 to not equal c2", !c1.equals(c2));
        assertTrue("Expected c1 to not equal c3", !c1.equals(c3));
        assertEquals(c1, c4);
    }

    public void testHashCode() throws Exception {
        assertEquals(c1.hashCode(), c1.hashCode());
        assertTrue("Expected c1.hashCode() to not equal c2.hashCode()",
                !(c1.hashCode() == c2.hashCode()));
        assertTrue("Expected c1.hashCode() to not equal c3.hashCode()",
                !(c1.hashCode() == c3.hashCode()));
        assertEquals(c1.hashCode(), c4.hashCode());
    }

    public void testTooManyColumns() throws Exception {
        q1.addSelect(new SelectValue(new Constant("8"), "r"));
        try {
            c1.getSQLString();
            fail("Expected: IllegalStateException");
        } catch (IllegalStateException e) {
        }
        try {
            SubQueryConstraint c = new SubQueryConstraint(v1, q1);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}

