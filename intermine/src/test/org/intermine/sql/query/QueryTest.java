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

    public void testAntlr() throws Exception {
        Query q = new Query("select flibble.flobble from flibble");
        //throw (new Exception(q.getSQLString()));
        //assertEquals("SELECT FROM flibble".length(), q.getSQLString().trim().length());
        assertTrue("Expected \"" + q.getSQLString() + "\" to equal \"SELECT flibble.flobble FROM flibble\"", q.getSQLString().equals("SELECT flibble.flobble FROM flibble"));
        q = new Query("select flibble.flobble from flibble, wotsit");
        assertEquals("SELECT flibble.flobble FROM wotsit, flibble", q.getSQLString());
        q = new Query("select flibble.flobble from (select flobble.flib from flobble) as flibble");
        assertEquals("SELECT flibble.flobble FROM (SELECT flobble.flib FROM flobble) AS flibble", q.getSQLString());
    }

    //public static String toHexDump(String in) {
    //    for (int i = 0; i<
}
