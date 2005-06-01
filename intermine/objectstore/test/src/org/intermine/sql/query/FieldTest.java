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

public class FieldTest extends TestCase
{
    private Field f1, f2, f3, f4, f5;
    private Table t1, t2, t3;

    public FieldTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        t1 = new Table("table1", "alias1");
        t2 = new Table("table1");
        t3 = new Table("table2", "alias1");

        f1 = new Field("field1", t1);
        f2 = new Field("field1", t1);
        f3 = new Field("field2", t1);
        f4 = new Field("field1", t2);
        f5 = new Field("field1", t3);
    }

    public void testGetSQLString() throws Exception {
        assertEquals("alias1.field1", f2.getSQLString());
        assertEquals("table1.field1", f4.getSQLString());
    }

    public void testFieldWithNullName() throws Exception {
        try {
            Field t = new Field(null, t1);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testFieldWithNullTable() throws Exception {
        try {
            Field t = new Field("field1", null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testEquals() throws Exception {
        assertEquals(f1, f1);
        assertEquals(f1, f2);
        assertTrue("Expected f1 not to equal f3", !f1.equals(f3));
        assertTrue("Expected f1 not to equal f4", !f1.equals(f4));
        assertTrue("Expected f1 not to equal f5", !f1.equals(f5));
        assertTrue("Expected f1 not to equal null", !f1.equals(null));
    }

    public void testEqualsTableOnlyAlias() throws Exception {
        assertTrue("Expected f1 to equal f1", f1.equalsTableOnlyAlias(f1));
        assertTrue("Expected f1 to equal f2", f1.equalsTableOnlyAlias(f2));
        assertTrue("Expected f1 not to equal f3", !f1.equalsTableOnlyAlias(f3));
        assertTrue("Expected f1 not to equal f4", !f1.equalsTableOnlyAlias(f4));
        assertTrue("Expected f1 to equal f5", f1.equalsTableOnlyAlias(f5));
        assertTrue("Expected f1 not to equal null", !f1.equalsTableOnlyAlias(null));
        assertTrue("Expected f4 not to equal f5", !f4.equalsTableOnlyAlias(f5));
    }

    public void testHashCode() throws Exception {
        assertEquals(f1.hashCode(), f1.hashCode());
        assertEquals(f1.hashCode(), f2.hashCode());
        assertTrue("Expected f1.hashCode() not to equal f3.hashCode()",
                   !(f1.hashCode() == f3.hashCode()));
        assertTrue("Expected f1.hashCode() not to equal f4.hashCode()",
                   !(f1.hashCode() == f4.hashCode()));
        assertTrue("Expected f1.hashCode() not to equal f5.hashCode()",
                   !(f1.hashCode() == f5.hashCode()));
    }
}
