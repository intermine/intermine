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

public class SubQueryTest extends TestCase
{
    private SubQuery t1, t2, t3, t4;
    private Query q1, q2, q3;
    
    public SubQueryTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        q1 = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q1.addFrom(t);
        q1.addSelect(sv);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));
        t1 = new SubQuery(q1, "alias1");
        t2 = new SubQuery(q1, "alias2");

        q2 = new Query();
        t = new Table("mytable");
        c = new Constant("1");
        f = new Field("a", t);
        sv = new SelectValue(f, null);
        q2.addFrom(t);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f, Constraint.EQ, c));
        t3 = new SubQuery(q2, "alias1");

        q3 = new Query();
        t = new Table("anotherTable");
        c = new Constant("2");
        f = new Field("b", t);
        sv = new SelectValue(f, null);
        q3.addFrom(t);
        q3.addSelect(sv);
        q3.addWhere(new Constraint(f, Constraint.LT, c));
        t4 = new SubQuery(q3, "alias3");
    }
 
    public void testGetSQLString() throws Exception {
        assertEquals("(SELECT mytable.a FROM mytable WHERE mytable.a = 1) AS alias1",
                t1.getSQLString());
    }

    public void testSubQueryWithNullAlias() throws Exception {
        try {
            SubQuery t = new SubQuery(q1, null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testSubQueryWithNullQuery() throws Exception {
        try {
            SubQuery t = new SubQuery(null, "myalias");
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testEquals() throws Exception {
        assertEquals(t1, t1);
        assertTrue("Expected t1 not to equal t2", !t1.equals(t2));
        assertEquals(t1, t3);
        assertTrue("Expected t1 not to equal t4", !t1.equals(t4));
        assertTrue("Expected t1 not to equal null", !t1.equals(null));
        assertTrue("Expected t2 not to equal t3", !t2.equals(t3));
    }

    public void testEqualsIgnoreAlias() throws Exception {
        assertTrue("Expected t1 to equal t1", t1.equalsIgnoreAlias(t1));
        assertTrue("Expected t1 to equal t2", t1.equalsIgnoreAlias(t2));
        assertTrue("Expected t1 to equal t3", t1.equalsIgnoreAlias(t3));
        assertTrue("Expected t1 not to equal t4", !t1.equalsIgnoreAlias(t4));
        assertTrue("Expected t1 not to equal null", !t1.equalsIgnoreAlias(null));
        assertTrue("Expected t2 to equal t3", t2.equalsIgnoreAlias(t3));
    }

    public void testHashCode() throws Exception {
        assertTrue("Expected t1.hashCode() not to equal t2.hashCode()",
                   !(t1.hashCode() == t2.hashCode()));
        assertTrue("Expected t1.hashCode() to equal t3.hashCode()",
                   (t1.hashCode() == t3.hashCode()));
        assertTrue("Expected t1.hashCode() not to equal t4.hashCode()",
                   !(t1.hashCode() == t4.hashCode()));
        assertTrue("Expected t2.hashCode() not to equal t3.hashCode()",
                   !(t2.hashCode() == t3.hashCode()));
    }
}

