package org.intermine.web;

import org.intermine.web.logic.LogicExpression;

import junit.framework.TestCase;

public class LogicExpressionTest extends TestCase
{
    public void testParseSimple() {
        new LogicExpression("A and B");
    }
    
    public void testParseSimple2() {
        new LogicExpression("A and B and C");
    }
    
    public void testParseSimple3() {
        new LogicExpression("A and B or C");
    }
    
    public void testSimpleBrackets() {
        new LogicExpression("(A and B)");
    }
    
    public void testDoubleBrackets() {
        new LogicExpression("((A and B ))");
    }
    
    public void testNestedBrackets() {
        roundtrip("A or B and C or D");
        roundtrip("A or B and (C or D)");
        roundtrip("(A or B) and (C or D)");
    }
    
    private void roundtrip(String expr) {
        assertEquals(expr, new LogicExpression(expr).toString());
    }
}
