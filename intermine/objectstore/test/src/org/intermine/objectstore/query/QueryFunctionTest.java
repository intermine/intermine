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

public class QueryFunctionTest extends TestCase
{
    public QueryFunctionTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
    }
        
    public void testInvalidCount() throws Exception {
        try {
            QueryField field = new QueryField(new QueryClass(Company.class), "name");
            new QueryFunction(field, QueryFunction.COUNT);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testValidCount() {
        QueryFunction function = new QueryFunction();
        assertTrue(Number.class.isAssignableFrom(function.getType()));
    }

    public void testInvalidNonCount() throws Exception {
        QueryField field = new QueryField(new QueryClass(Company.class), "name");
        try {
            new QueryFunction(field, QueryFunction.SUM);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testValidNonCount() throws Exception {
        QueryField field = new QueryField(new QueryClass(Company.class), "vatNumber");
        try {
            new QueryFunction(field, QueryFunction.SUM);
        } catch (IllegalArgumentException e) {
            fail("An IllegalArgumentException should not have been thrown");
        }
    }
}
