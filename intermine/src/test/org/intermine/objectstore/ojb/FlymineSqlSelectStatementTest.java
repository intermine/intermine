package org.flymine.objectstore.ojb;

import junit.framework.*;
import java.util.*;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryExpression;

public class FlymineSqlSelectStatementTest extends TestCase
{
    public FlymineSqlSelectStatementTest(String arg1) {
        super(arg1);
    }

    public void testQueryValue() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue("Hello");
        QueryValue v3 = new QueryValue(new Date(1046275720000l));
        QueryValue v4 = new QueryValue(Boolean.TRUE);
        Query q1 = new Query();
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals(s1.queryEvaluableToString(v1), "5");
        assertEquals(s1.queryEvaluableToString(v2), "'Hello'");
        assertEquals(s1.queryEvaluableToString(v3), "'2003-02-26 16:08:40.000'");
        assertEquals(s1.queryEvaluableToString(v4), "1");
    }

    public void testQueryExpression() throws Exception {
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryValue v2 = new QueryValue(new Integer(7));
        QueryExpression e1 = new QueryExpression(v1, QueryExpression.ADD, v2);
        QueryExpression e2 = new QueryExpression(v1, QueryExpression.SUBTRACT, v2);
        QueryExpression e3 = new QueryExpression(v1, QueryExpression.MULTIPLY, v2);
        QueryExpression e4 = new QueryExpression(v1, QueryExpression.DIVIDE, v2);
        Query q1 = new Query();
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals(s1.queryEvaluableToString(e1), "(5 + 7)");
        assertEquals(s1.queryEvaluableToString(e2), "(5 - 7)");
        assertEquals(s1.queryEvaluableToString(e3), "(5 * 7)");
        assertEquals(s1.queryEvaluableToString(e4), "(5 / 7)");
    }

    public void testQuerySubstringExpression() throws Exception {
        QueryValue v1 = new QueryValue("Hello");
        QueryValue v2 = new QueryValue(new Integer(3));
        QueryValue v3 = new QueryValue(new Integer(5));
        QueryExpression e1 = new QueryExpression(v1, v2, v3);
        Query q1 = new Query();
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q1, null);
        assertEquals(s1.queryEvaluableToString(e1), "Substr('Hello', 3, 5)");
    }
        
}
