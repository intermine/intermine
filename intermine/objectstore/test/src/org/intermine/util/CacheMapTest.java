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

import junit.framework.TestCase;

public class CacheMapTest extends TestCase
{
    public CacheMapTest(String arg1) {
        super(arg1);
    }

    public void test() throws Exception {
        CacheMap cm = new CacheMap();
        for (int i = 0; i < 300; i++) {
            Integer iI = new Integer(i);
            cm.put(iI, new byte[1048576]);
        }

        assertTrue(cm.size() < 1500);
        assertTrue("Expected first two to be missing",!(cm.containsKey(new Integer(2)) && cm.containsKey(new Integer(1))));
        assertTrue("Expected last two to be present", cm.containsKey(new Integer(298)) || cm.containsKey(new Integer(297)));
    }

    public void test2() throws Exception {
        CacheMap cm = new CacheMap();

        cm.put(new Integer(5), new Integer(40));
        cm.put(new Integer(763), new Integer(67));
        cm.put(new Integer(2), null);

        assertEquals(new Integer(40), cm.get(new Integer(5)));
        assertEquals(new Integer(67), cm.get(new Integer(763)));
        assertNull(cm.get(new Integer(2)));
        assertTrue(cm.containsKey(new Integer(5)));
        assertTrue(cm.containsKey(new Integer(763)));
        assertTrue(cm.containsKey(new Integer(2)));

        assertNull(cm.remove(new Integer(3)));
        assertEquals(new Integer(40), cm.get(new Integer(5)));
        assertEquals(new Integer(67), cm.get(new Integer(763)));
        assertNull(cm.get(new Integer(2)));
        assertTrue(cm.containsKey(new Integer(5)));
        assertTrue(cm.containsKey(new Integer(763)));
        assertTrue(cm.containsKey(new Integer(2)));

        assertEquals(new Integer(40), cm.remove(new Integer(5)));
        assertNull(cm.get(new Integer(5)));
        assertEquals(new Integer(67), cm.get(new Integer(763)));
        assertNull(cm.get(new Integer(2)));
        assertFalse(cm.containsKey(new Integer(5)));
        assertTrue(cm.containsKey(new Integer(763)));
        assertTrue(cm.containsKey(new Integer(2)));

        assertEquals(new Integer(67), cm.remove(new Integer(763)));
        assertNull(cm.get(new Integer(5)));
        assertNull(cm.get(new Integer(763)));
        assertNull(cm.get(new Integer(2)));
        assertFalse(cm.containsKey(new Integer(5)));
        assertFalse(cm.containsKey(new Integer(763)));
        assertTrue(cm.containsKey(new Integer(2)));

        assertNull(cm.remove(new Integer(2)));
        assertNull(cm.get(new Integer(5)));
        assertNull(cm.get(new Integer(763)));
        assertNull(cm.get(new Integer(2)));
        assertFalse(cm.containsKey(new Integer(5)));
        assertFalse(cm.containsKey(new Integer(763)));
        assertFalse(cm.containsKey(new Integer(2)));
    }
}
