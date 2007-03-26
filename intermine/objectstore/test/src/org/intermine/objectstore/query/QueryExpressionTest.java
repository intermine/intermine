package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import org.intermine.model.testmodel.*;

public class QueryExpressionTest extends TestCase
{
    private QueryExpression expression;

    public QueryExpressionTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
    }

    public void testAddString() {
        QueryValue arg1 = new QueryValue(new Integer(3));
        QueryValue arg2 = new QueryValue("string");
        try {
            expression = new QueryExpression(arg1, QueryExpression.ADD, arg2);
            fail("An ClassCastException should have been thrown");
        } catch (ClassCastException e) {
        }
    }
    
    public void testInvalidOp() {
        QueryValue arg1 = new QueryValue(new Integer(3));
        QueryValue arg2 = new QueryValue(new Integer(4));
        try {
            expression = new QueryExpression(arg1, QueryExpression.SUBSTRING, arg2);
            fail("A ClassCastException should have been thrown");
        } catch (ClassCastException e) {
        }
    }
    
    public void testNumberSubstring() {
        try {
            QueryField field = new QueryField(new QueryClass(Company.class), "vatNumber");
            QueryValue v1 = new QueryValue(new Integer(1));
            QueryValue v2 = new QueryValue(new Integer(4));
            new QueryExpression(field, v1, v2);
            fail("A ClassCastException should have been thrown");
        } catch (ClassCastException e) {
        }
    }

    public void testSubstringIndex() {
        try {
            QueryField field = new QueryField(new QueryClass(Company.class), "name");
            QueryValue v1 = new QueryValue(new Integer(0));
            QueryValue v2 = new QueryValue(new Integer(4));
            new QueryExpression(field, v1, v2);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidOp() {
        QueryValue arg1 = new QueryValue(new Integer(3));
        QueryValue arg2 = new QueryValue(new Integer(4));
        expression = new QueryExpression(arg1, QueryExpression.ADD, arg2);
        assertTrue(Number.class.isAssignableFrom(expression.getType()));
    }
}
