package org.flymine.sql.query;

import junit.framework.*;

public class QueryTest extends TestCase
{
    public QueryTest(String arg1) {
        super(arg1);
    }

    public void testGetSQLString() throws Exception {
        Query q = new Query();
        Table t = new Table("mytable");
        Constant c = new Constant("1");
        q.addFrom(t);
        q.addSelect(c);
        q.addWhere(new Constraint(new Constant("a"), Constraint.EQ, new Constant("1")));
        assertEquals("SELECT 1 FROM mytable WHERE a = 1", q.getSQLString());
    }
}
