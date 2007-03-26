package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

public class CombinedIteratorTest extends TestCase
{
    public CombinedIteratorTest(String arg) {
        super(arg);
    }

    public void test() throws Exception {
        List a = new ArrayList();
        List b = new ArrayList();
        List c = new ArrayList();

        Iterator i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));

        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        a.add("a1");
        i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));
        assertTrue(i.hasNext());
        assertEquals("a1", i.next());
        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
        
        a.add("a2");
        i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));
        assertTrue(i.hasNext());
        assertEquals("a1", i.next());
        assertTrue(i.hasNext());
        assertEquals("a2", i.next());
        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        b.add("b1");
        i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));
        assertTrue(i.hasNext());
        assertEquals("a1", i.next());
        assertTrue(i.hasNext());
        assertEquals("a2", i.next());
        assertTrue(i.hasNext());
        assertEquals("b1", i.next());
        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        c.add("c1");
        i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));
        assertTrue(i.hasNext());
        assertEquals("a1", i.next());
        assertTrue(i.hasNext());
        assertEquals("a2", i.next());
        assertTrue(i.hasNext());
        assertEquals("b1", i.next());
        assertTrue(i.hasNext());
        assertEquals("c1", i.next());
        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        a.clear();
        i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));
        assertTrue(i.hasNext());
        assertEquals("b1", i.next());
        assertTrue(i.hasNext());
        assertEquals("c1", i.next());
        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        b.clear();
        i = new CombinedIterator(Arrays.asList(new Iterator[] {a.iterator(), b.iterator(),
            c.iterator()}));
        assertTrue(i.hasNext());
        assertEquals("c1", i.next());
        assertFalse(i.hasNext());
        try {
            i.next();
            fail("Expected: NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }
}

