package org.flymine.sql.query;

import junit.framework.*;

public class QueryTest extends TestCase
{
    private Query q1, q2, q3;

    public QueryTest(String arg1) {
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

        q2 = new Query();
        t = new Table("mytable");
        c = new Constant("1");
        f = new Field("a", t);
        sv = new SelectValue(f, null);
        q2.addFrom(t);
        q2.addSelect(sv);
        q2.addWhere(new Constraint(f, Constraint.EQ, c));

        q3 = new Query();
        t = new Table("anotherTable");
        c = new Constant("2");
        f = new Field("b", t);
        sv = new SelectValue(f, null);
        q3.addFrom(t);
        q3.addSelect(sv);
        q3.addWhere(new Constraint(f, Constraint.LT, c));
    }
    public void testGetSQLString() throws Exception {
        Query q = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        SelectValue sv = new SelectValue(f, null);
        q.addFrom(t);
        q.addSelect(sv);
        q.addWhere(new Constraint(f, Constraint.EQ, c));
        assertEquals("SELECT mytable.a FROM mytable WHERE mytable.a = 1", q.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(q1, q1);
        assertEquals(q1, q2);
        assertTrue("Expected q1 to not equal q3", !q1.equals(q3));
    }

    public void testHashCode() throws Exception {
        assertEquals(q1.hashCode(), q1.hashCode());
        assertEquals(q1.hashCode(), q2.hashCode());
        assertTrue("Expected q1 hashcode not to equal q3 hashcode", q1.hashCode() != q3.hashCode());
    }

    public void testConstructNullString() throws Exception {
        try {
            Query q1 = new Query((String) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testConstructEmptyString() throws Exception {
        try {
            Query q1 = new Query("");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testConstructIllegalString() throws Exception {
        try {
            Query q1 = new Query("A load of rubbish");
            fail("Expected: IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    public void testSelectNoAlias() throws Exception {
        Query q1 = new Query("select table1.field1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectTableAlias() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 as t1");
        Query q1_alt = new Query("select t1.field1 from table1 t1");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
        assertEquals(q2, q1_alt);
    }

    public void testSelectFieldAlias() throws Exception {
        Query q1 = new Query("select table1.field1 as alias1 from table1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, "alias1");
        q2.addSelect(sv1);
        q2.addFrom(t1);
        assertEquals(q2, q1);
    }

    public void testSelectTwoTables() throws Exception {
        Query q1 = new Query("select table1.field1 from table1, table2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Table t2 = new Table("table2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectTwoSameTables() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 t1, table1 t2");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table1", "t2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectTwoDifferentTables() throws Exception {
        Query q1 = new Query("select t1.field1 from table1 t1, table2 t2");
        Query q2 = new Query();
        Table t1 = new Table("table1", "t1");
        Table t2 = new Table("table2", "t2");
        Field f1 = new Field("field1", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addFrom(t2);
        assertEquals(q2, q1);
    }

    public void testSelectFromSubQuery() throws Exception {
        Query q1 = new Query("select t1.field1 from (select table2.field2 from table2) as t1");
        Query q2 = new Query();
        SubQuery sq1 = new SubQuery(new Query("select table2.field2 from table2"), "t1");
        Field f1 = new Field("field1", sq1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(sq1);
        //throw new Exception(q1.getSQLString() + "     " + q2.getSQLString());
        assertEquals(q2, q1);
    }

    public void testOneEqualWhere() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 = 1");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Constant c = new Constant("1");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c));
        assertEquals(q2, q1);
    }

    public void testTwoEqualWhere() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 = 1 and table1.field2 = 2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        Constant c1 = new Constant("1");
        Constant c2 = new Constant("2");
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.EQ, c1));
        q2.addWhere(new Constraint(f2, Constraint.EQ, c2));
        assertEquals(q2, q1);
    }

    public void testFieldLessThanField() throws Exception {
        Query q1 = new Query("select table1.field1 from table1 where table1.field1 < table1.field2");
        Query q2 = new Query();
        Table t1 = new Table("table1");
        Field f1 = new Field("field1", t1);
        Field f2 = new Field("field2", t1);
        SelectValue sv1 = new SelectValue(f1, null);
        q2.addSelect(sv1);
        q2.addFrom(t1);
        q2.addWhere(new Constraint(f1, Constraint.LT, f2));
        assertEquals(q2, q1);
    }

}
