package org.intermine.sql.precompute;

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
import java.util.Date;
import java.util.HashSet;

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

    public void test2() throws Exception {
        OptimiserCache cache = new OptimiserCache();
        cache.untilNextExpiration = 10000000;
        for (int i = 0; i < 1200; i++) {
            cache.addCacheLine("line" + i, "optimised" + i, 1000, 0, 1000);
        }

        HashSet soonSet = new HashSet();
        OptimiserCacheLine soonLine = new OptimiserCacheLine("hello", 1000, 0, 1000, soonSet,
                "soon");
        soonLine.expires = new Date((new Date()).getTime() + 100);
        HashSet longSet = new HashSet();
        OptimiserCacheLine longLine = new OptimiserCacheLine("hello", 1000, 0, 1000, longSet,
                "long");
        longLine.expires = new Date((new Date()).getTime() + 2000000);

        cache.cacheLines.put("soon", soonSet);
        soonSet.add(soonLine);
        cache.evictionQueue.put(new OptimiserCache.DateAndSequence(soonLine.getExpiry(), 0),
                soonLine);
        cache.cacheLines.put("long", longSet);
        longSet.add(longLine);
        cache.evictionQueue.put(new OptimiserCache.DateAndSequence(longLine.getExpiry(), 0),
                longLine);

        assertEquals("hello", cache.lookup("soon", 1000, 0));
        assertEquals("hello", cache.lookup("long", 1000, 0));
        assertEquals("optimised0", cache.lookup("line0", 1000, 0));
        assertEquals("optimised1199", cache.lookup("line1199", 1000, 0));
        
        cache.untilNextExpiration = 0;
        cache.expire();

        assertNull(cache.lookup("soon", 1000, 0));
        assertEquals("hello", cache.lookup("long", 1000, 0));
        assertNull(cache.lookup("line0", 1000, 0));
        assertEquals("optimised1199", cache.lookup("line1199", 1000, 0));
        assertNull(cache.lookup("line200", 1000, 0));
        assertEquals("optimised201", cache.lookup("line201", 1000, 0));
    }

    public void test3() throws Exception {
        OptimiserCache cache = new OptimiserCache();
        
        HashSet pastSet = new HashSet();
        OptimiserCacheLine pastLine = new OptimiserCacheLine("hello", 1000, 0, 1000, pastSet,
                "past");
        pastLine.expires = new Date((new Date()).getTime() - 100);
        cache.cacheLines.put("past", pastSet);
        pastSet.add(pastLine);
        cache.evictionQueue.put(new OptimiserCache.DateAndSequence(pastLine.getExpiry(), 0),
                pastLine);

        assertEquals("hello", cache.lookup("past", 1000, 0));

        cache.untilNextExpiration = 0;
        cache.expire();

        assertNull(cache.lookup("past", 1000, 0));
    }

    public void test4() throws Exception {
        OptimiserCache cache = new OptimiserCache();
        for (int i = 0; i < 12000; i++) {
            cache.addCacheLine("line" + i, "optimised" + i, 1000, 0, 1000);
        }

        assertEquals(1001, cache.cacheLines.size());
    }
}
