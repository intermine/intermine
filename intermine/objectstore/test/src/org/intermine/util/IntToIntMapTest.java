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

public class IntToIntMapTest extends TestCase
{
    public IntToIntMapTest(String arg1) {
        super(arg1);
    }

    public void test() throws Exception {
        IntToIntMap map = new IntToIntMap();
        map.put(4, 6);
        map.put(30000, 7643);

        assertEquals(6, map.get(4));
        assertEquals(7643, map.get(30000));
        assertEquals(-1, map.get(0));
        assertEquals(2, map.size());
        assertEquals("{4 -> 6, 30000 -> 7643}", map.toString());

        map.put(new Integer(4), null);
        map.put(30000, -1);

        assertEquals(-1, map.get(4));
        assertEquals(-1, map.get(30000));
        assertEquals(-1, map.get(0));
        assertEquals(0, map.size());
        assertEquals("{}", map.toString());
    }
}
