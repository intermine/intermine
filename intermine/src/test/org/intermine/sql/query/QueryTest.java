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
        q1.addFrom(t);
        q1.addSelect(f);
        q1.addWhere(new Constraint(f, Constraint.EQ, c));

        q2 = new Query();
        t = new Table("mytable");
        c = new Constant("1");
        f = new Field("a", t);
        q2.addFrom(t);
        q2.addSelect(f);
        q2.addWhere(new Constraint(f, Constraint.EQ, c));

        q3 = new Query();
        t = new Table("anotherTable");
        c = new Constant("2");
        f = new Field("b", t);
        q3.addFrom(t);
        q3.addSelect(f);
        q3.addWhere(new Constraint(f, Constraint.LT, c));
    }
    public void testGetSQLString() throws Exception {
        Query q = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        Field f = new Field("a", t);
        q.addFrom(t);
        q.addSelect(f);
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
}
