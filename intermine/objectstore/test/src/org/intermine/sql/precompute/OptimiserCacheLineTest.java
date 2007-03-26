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

import java.util.Date;
import java.util.HashSet;

import junit.framework.TestCase;

public class OptimiserCacheLineTest extends TestCase
{
    public OptimiserCacheLineTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        OptimiserCacheLine l1 = new OptimiserCacheLine("optimised query", 1000, 0, 1000, new HashSet(), "hello");
        assertTrue(!l1.isExpired());
        l1.expires = new Date();
        assertTrue("Score is " + l1.score(1000,0), 0.0 == l1.score(1000, 0));
        assertTrue(0.0 == l1.score(1000, 1000));
        assertTrue("Score is " + l1.score(250, 0), Math.abs(1.0 - l1.score(250, 0)) < 0.0001);
        assertTrue("Score is " + l1.score(4000, 0), Math.abs(1.0 - l1.score(4000, 0)) < 0.0001);
        assertTrue(1.0 == l1.score(1000, 1125));
        Thread.sleep(100);
        assertTrue(l1.isExpired());
    }

    public void test2() throws Exception {
        OptimiserCacheLine l1 = new OptimiserCacheLine("optimised query", 10000, 5000, 10000, new HashSet(), "hello");
        assertTrue("Score is " + l1.score(10000,0), 3.2 == l1.score(10000, 0));
        assertTrue(2.4 == l1.score(10000, 1000));
    }

}
