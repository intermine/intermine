package org.flymine.sql.precompute;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

public class OptimiserCacheTest extends TestCase
{
    public OptimiserCacheTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        OptimiserCache cache = new OptimiserCache();
        cache.addCacheLine("original1", "optimised1_1", 1000, 0, 1000000);
        cache.addCacheLine("original1", "optimised1_2", 1000, 3000, 1000000);
        cache.addCacheLine("original1", "optimised1_3", 1000, 200000, 1000000);
        cache.addCacheLine("original1", "optimised1_4", 16000, 0, 1000000);
        cache.addCacheLine("original1", "optimised1_5", 1000, 900000, 1000000);
        assertEquals("optimised1_1", cache.lookup("original1", 1000, 0));
        assertEquals("optimised1_1", cache.lookup("original1", 1000, 1490));
        assertEquals("optimised1_2", cache.lookup("original1", 1000, 1510));
        assertEquals("optimised1_1", cache.lookup("original1", 3999, 0));
        assertEquals("optimised1_4", cache.lookup("original1", 4001, 0));
        assertEquals("optimised1_3", cache.lookup("original1", 1000, 150000));
        assertEquals("optimised1_5", cache.lookup("original1", 1000, 1000000));
        assertNull(cache.lookup("original1", 1000, 600000));
        assertNull(cache.lookup("somethingelse", 1000, 0));
    }

}
