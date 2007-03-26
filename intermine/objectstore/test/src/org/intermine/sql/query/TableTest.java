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

public class TableTest extends TestCase
{
    private Table t1, t2, t3, t4, t5, t6;
    
    public TableTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        t1 = new Table("value1", "alias1");
        t2 = new Table("value1", "alias2");
        t3 = new Table("value1");
        t4 = new Table("value1", "alias1");
        t5 = new Table("value2", "alias3");
        t6 = new Table("value1");
    }
 
    public void testTableWithAlias() throws Exception {
        Table t = new Table("mytable", "myalias");
        assertEquals("mytable AS myalias", t.getSQLString());
    }

    public void testTableWithoutAlias() throws Exception {
        Table t = new Table("mytable");
        assertEquals("mytable", t.getSQLString());
    }

    public void testTableWithSameAlias() throws Exception {
        Table t = new Table("mytable", "mytable");
        assertEquals("mytable", t.getSQLString());
    }

    public void testTableWithNullName() throws Exception {
        try {
            Table t = new Table(null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testTableWithNullAlias() throws Exception {
        Table t = new Table("hello", null);
        assertEquals("hello", t.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(t1, t1);
        assertTrue("Expected t1 not to equal t2", !t1.equals(t2));
        assertTrue("Expected t1 not to equal t3", !t1.equals(t3));
        assertEquals(t1, t4);
        assertTrue("Expected t1 not to equal t5", !t1.equals(t5));
        assertTrue("Expected t1 not to equal null", !t1.equals(null));
        assertEquals(t3, t6);
    }

    public void testEqualsIgnoreAlias() throws Exception {
        assertTrue("Expected t1 to equal t1", t1.equalsIgnoreAlias(t1));
        assertTrue("Expected t1 to equal t2", t1.equalsIgnoreAlias(t2));
        assertTrue("Expected t1 to equal t3", t1.equalsIgnoreAlias(t3));
        assertTrue("Expected t1 to equal t4", t1.equalsIgnoreAlias(t4));
        assertTrue("Expected t1 not to equal t5", !t1.equalsIgnoreAlias(t5));
        assertTrue("Expected t1 not to equal null", !t1.equalsIgnoreAlias(null));
        assertTrue("Expected t3 to equal t6", t3.equalsIgnoreAlias(t6));
    }

    public void testHashCode() throws Exception {
        assertTrue("Expected t1.hashCode() not to equal t2.hashCode()",
                   !(t1.hashCode() == t2.hashCode()));
        assertTrue("Expected t1.hashCode() not to equal t3.hashCode()",
                   !(t1.hashCode() == t3.hashCode()));
        assertEquals(t1.hashCode(), t4.hashCode());
        assertTrue("Expected t1.hashCode() not to equal t5.hashCode()",
                   !(t1.hashCode() == t5.hashCode()));
        assertEquals(t3.hashCode(), t6.hashCode());
    }
}
