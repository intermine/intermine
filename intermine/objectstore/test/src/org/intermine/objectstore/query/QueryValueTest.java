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

public class QueryValueTest extends TestCase
{
    public QueryValueTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
    }

    public void testGetType() throws Exception {
        QueryValue value = new QueryValue("string");
        assertEquals(String.class, value.getType());
    }

    public void testInvalidType() throws Exception {
        try {
            QueryValue value = new QueryValue(new Object());
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testDifferentNumbersEqual() throws Exception {
        assertEquals(new QueryValue(new Integer(5)), new QueryValue(new Long(5)));
        assertEquals(new QueryValue(new Integer(5)), new QueryValue(new Double(5.0)));
    }

    public void testDifferentNumbersNotEqual() throws Exception {
        assertTrue("Expected 5 to not equal 5.00001", !(new QueryValue(new Integer(5))).equals(new QueryValue(new Double(5.00001))));
    }
}
