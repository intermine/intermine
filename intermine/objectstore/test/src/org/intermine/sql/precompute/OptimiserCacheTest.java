package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class OptimiserCacheTest extends TestCase
{
    public OptimiserCacheTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        OptimiserCache cache = new OptimiserCache();
        cache.addCacheLine("original1", "optimised1_1", 1000);
        cache.addCacheLine("original1", "optimised1_2", 16000);
        assertEquals("optimised1_1", cache.lookup("original1", 1000));
        assertEquals("optimised1_1", cache.lookup("original1", 3999));
        assertEquals("optimised1_2", cache.lookup("original1", 4001));
        assertNull(cache.lookup("original1", 10));
        assertNull(cache.lookup("somethingelse", 1000));
    }
}
