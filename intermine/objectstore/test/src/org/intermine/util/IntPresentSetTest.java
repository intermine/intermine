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

import junit.framework.*;

public class IntPresentSetTest extends TestCase
{
    public IntPresentSetTest(String arg1) {
        super(arg1);
    }

    public void test() throws Exception {
        IntPresentSet set = new IntPresentSet();

        set.set(42, true);
        set.set(28673452, true);

        assertEquals(2, set.size());
        assertTrue(set.contains(42));
        assertTrue(set.contains(28673452));
        assertFalse(set.contains(63));
        assertEquals("[42, 28673452]", set.toString());

        set.set(42, true);

        assertEquals(2, set.size());
        assertTrue(set.contains(42));
        assertTrue(set.contains(28673452));
        assertFalse(set.contains(63));
        assertEquals("[42, 28673452]", set.toString());

        set.set(42, false);

        assertEquals(1, set.size());
        assertFalse(set.contains(42));
        assertTrue(set.contains(28673452));
        assertFalse(set.contains(63));
        assertEquals("[28673452]", set.toString());
    }
}

