package org.flymine.sql.query;

import junit.framework.*;

public class FieldTest extends TestCase
{
    private Field f1, f2, f3, f4, f5, f6, f7, f8;
    private Table t1, t2;

    public FieldTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        t1 = new Table("table1", "alias1");
        t2 = new Table("table1");

        f1 = new Field("field1", t1, "first1");
        f2 = new Field("field1", t1, "first2");
        f3 = new Field("field1", t1);
        f4 = new Field("field1", t1, "first1");
        f5 = new Field("field2", t1, "second");
        f6 = new Field("field1", t1);
        f7 = new Field("field1", t2, "first1");
        f8 = new Field("field1", t2);
    }

    public void testFieldWithAlias() throws Exception {
        assertEquals("alias1.field1 AS first1", f1.getSQLString());
        assertEquals("table1.field1 AS first1", f7.getSQLString());
    }

    public void testFieldWithoutAlias() throws Exception {
        assertEquals("alias1.field1", f3.getSQLString());
        assertEquals("table1.field1", f8.getSQLString());
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

    public void testFieldWithNullAlias() throws Exception {
        Field f = new Field("hello", t1, null);
        assertEquals("alias1.hello", f.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(f1, f1);
        assertTrue("Expected f1 not to equal f2", !f1.equals(f2));
        assertTrue("Expected f1 not to equal f3", !f1.equals(f3));
        assertEquals(f1, f4);
        assertTrue("Expected f1 not to equal f5", !f1.equals(f5));
        assertTrue("Expected f1 not to equal null", !f1.equals(null));
        assertEquals(f3, f6);
        assertTrue("Expected f1 not to equal f7", !f1.equals(f7));
        assertTrue("Expected f7 not to equal f8", !f7.equals(f8));
    }

    public void testEqualsIgnoreAlias() throws Exception {
        assertTrue("Expected f1 to equal f1", f1.equalsIgnoreAlias(f1));
        assertTrue("Expected f1 to equal f2", f1.equalsIgnoreAlias(f2));
        assertTrue("Expected f1 to equal f3", f1.equalsIgnoreAlias(f3));
        assertTrue("Expected f1 to equal f4", f1.equalsIgnoreAlias(f4));
        assertTrue("Expected f1 not to equal f5", !f1.equalsIgnoreAlias(f5));
        assertTrue("Expected f1 not to equal null", !f1.equalsIgnoreAlias(null));
        assertTrue("Expected f3 to equal f6", f3.equalsIgnoreAlias(f6));
        assertTrue("Expected f1 not to equal f7", !f1.equalsIgnoreAlias(f7));
        assertTrue("Expected f7 not to equal f8", !f1.equalsIgnoreAlias(f8));
    }

    public void testHashCode() throws Exception {
        assertTrue("Expected f1.hashCode() not to equal f2.hashCode()",
                   !(f1.hashCode() == f2.hashCode()));
        assertTrue("Expected f1.hashCode() not to equal f3.hashCode()",
                   !(f1.hashCode() == f3.hashCode()));
        assertEquals(f1.hashCode(), f4.hashCode());
        assertTrue("Expected f1.hashCode() not to equal f5.hashCode()",
                   !(f1.hashCode() == f5.hashCode()));
        assertEquals(f3.hashCode(), f6.hashCode());
        assertTrue("Expected f1.hashCode() not to equal f7.hashCode()",
                   !(f1.hashCode() == f7.hashCode()));
        assertTrue("Expected f7.hashCode() not to equal f8.hashCode()",
                   !(f7.hashCode() == f8.hashCode()));
    }
}
