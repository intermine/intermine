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

public class SelectValueTest extends TestCase
{
    private SelectValue v1, v2, v3, v4, v5, v6, v7, v8;

    public SelectValueTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        Constant c1 = new Constant("3");
        Constant c2 = new Constant("f");
        Table t = new Table("mytable");
        Field f = new Field("myfield", t);
        v1 = new SelectValue(c1, "alias1");
        v2 = new SelectValue(c1, "alias1");
        v3 = new SelectValue(c1, "alias2");
        v4 = new SelectValue(c2, "alias1");
        v5 = new SelectValue(c2, "alias3");
        v6 = new SelectValue(f, null);
        v7 = new SelectValue(f, "myfield");
        v8 = new SelectValue(f, "alias4");
    }

    public void testGetSQLString() throws Exception {
        assertEquals("3 AS alias1", v1.getSQLString());
        assertEquals("f AS alias1", v4.getSQLString());
        assertEquals("mytable.myfield", v6.getSQLString());
        assertEquals("mytable.myfield", v7.getSQLString());
        assertEquals("mytable.myfield AS alias4", v8.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(v1, v2);
        assertTrue("Expected v1 to not equal v3", !v1.equals(v3));
        assertTrue("Expected v1 to not equal v4", !v1.equals(v4));
        assertTrue("Expected v1 to not equal v5", !v1.equals(v5));
        assertTrue("Expected v1 to not equal v6", !v1.equals(v6));
        assertTrue("Expected v1 to not equal v7", !v1.equals(v7));
        assertTrue("Expected v1 to not equal v8", !v1.equals(v8));
        assertEquals(v6, v7);
        assertTrue("Expected v6 to not equal v8", !v6.equals(v8));
    }

    public void testHashCode() throws Exception {
        assertEquals(v1.hashCode(), v1.hashCode());
        assertEquals(v1.hashCode(), v2.hashCode());
        assertTrue("Expected v1.hashCode() to not equal v3.hashCode()",
                !(v1.hashCode() == v3.hashCode()));
        assertTrue("Expected v1.hashCode() to not equal v4.hashCode()",
                !(v1.hashCode() == v4.hashCode()));
        assertTrue("Expected v1.hashCode() to not equal v5.hashCode()",
                !(v1.hashCode() == v5.hashCode()));
        assertTrue("Expected v1.hashCode() to not equal v6.hashCode()",
                !(v1.hashCode() == v6.hashCode()));
        assertTrue("Expected v1.hashCode() to not equal v7.hashCode()",
                !(v1.hashCode() == v7.hashCode()));
        assertTrue("Expected v1.hashCode() to not equal v8.hashCode()",
                !(v1.hashCode() == v8.hashCode()));
        assertEquals(v6.hashCode(), v7.hashCode());
        assertTrue("Expected v6.hashCode() to not equal v8.hashCode()",
                !(v6.hashCode() == v8.hashCode()));
    }
}
