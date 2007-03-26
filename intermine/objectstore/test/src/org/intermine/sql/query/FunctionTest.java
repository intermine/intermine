package org.intermine.sql.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class FunctionTest extends TestCase
{
    private Function f1, f2, f3, f4, f5, f6, f7, f8, f9;
    
    public FunctionTest(String arg1) {
        super(arg1);
    }

    public void setUp()
    {
        Constant c1 = new Constant("2");
        Constant c2 = new Constant("3");
        Constant c3 = new Constant("f");
        Constant c4 = new Constant("g");

        f1 = new Function(Function.COUNT);
        f2 = new Function(Function.MAX);
        f2.add(c2);
        f3 = new Function(Function.PLUS);
        f3.add(c1);
        f3.add(c2);
        f4 = new Function(Function.PLUS);
        f4.add(c2);
        f4.add(c1);
        f5 = new Function(Function.MINUS);
        f5.add(c1);
        f5.add(c2);
        f6 = new Function(Function.MINUS);
        f6.add(c2);
        f6.add(c1);
        f7 = new Function(Function.MAX);
        f7.add(c2);
        f8 = new Function(Function.PLUS);
        f8.add(c1);
        f8.add(c1);
        f9 = new Function(Function.PLUS);
        f9.add(c1);
        f9.add(c1);
        f9.add(c1);
    }

    public void testArrayList() throws Exception {
        java.util.ArrayList l = new java.util.ArrayList();
        l.add("Hello");

        java.util.Iterator iter = l.iterator();
        while (iter.hasNext()) {
            assertTrue(iter.next() != null);
        }
    }
    
    public void testGetSQLString() throws Exception {
        assertEquals("COUNT(*)", f1.getSQLString());
        assertEquals("MAX(3)", f2.getSQLString());
        assertEquals("(2 + 3)", f3.getSQLString());
        assertEquals("(2 - 3)", f5.getSQLString());
    }

    public void testEquals() throws Exception {
        assertEquals(f1, f1);
        assertTrue("Expected f1 to not equal f2", !f1.equals(f2));
        assertTrue("Expected f1 to not equal f3", !f1.equals(f3));
        assertTrue("Expected f2 to not equal f3", !f2.equals(f3));
        assertEquals(f3, f4);
        assertTrue("Expected f3 to not equal f5", !f3.equals(f5));
        assertTrue("Expected f5 to not equal f6", !f5.equals(f6));
        assertEquals(f2, f7);
        assertTrue("Expected f8 to not equal f9", !f8.equals(f9));
    }

    public void testHashCode() throws Exception {
        assertEquals(f1.hashCode(), f1.hashCode());
        assertTrue("Expected f1 hashcode not to equal f2 hashcode", f1.hashCode() != f2.hashCode());
        assertTrue("Expected f1 hashcode not to equal f3 hashcode", f1.hashCode() != f3.hashCode());
        assertTrue("Expected f2 hashcode not to equal f3 hashcode", f2.hashCode() != f3.hashCode());
        assertEquals(f3.hashCode(), f4.hashCode());
        assertTrue("Expected f3 hashcode not to equal f5 hashcode", f3.hashCode() != f5.hashCode());
        assertTrue("Expected f5 hashcode not to equal f6 hashcode", f5.hashCode() != f6.hashCode());
        assertEquals(f2.hashCode(), f7.hashCode());
        assertTrue("Expected f8 hashcode not to equal f9 hashcode", f8.hashCode() != f9.hashCode());
    }

    public void testTooManyOperands() throws Exception {
        Constant c = new Constant("4");
        try {
            f1.add(c);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            f2.add(c);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        f3.add(c);
        f4.add(c);
        try {
            f5.add(c);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            f6.add(c);
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testTooFewOperands() throws Exception {
        Function f;

        f = new Function(Function.MAX);
        try {
            f.getSQLString();
            fail("Expected: IllegalStateException");
        } catch (IllegalStateException e) {
        }

        f = new Function(Function.PLUS);
        try {
            f.getSQLString();
            fail("Expected: IllegalStateException");
        } catch (IllegalStateException e) {
        }
        f.add(new Constant("c"));
        try {
            f.getSQLString();
            fail("Expected: IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
}
