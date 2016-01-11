package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class OptimiserCacheLineTest extends TestCase
{
    public OptimiserCacheLineTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        OptimiserCacheLine l1 = new OptimiserCacheLine("optimised query", 1000, "hello");
        assertTrue("Score is " + l1.score(1000), 0.0 == l1.score(1000));
        assertTrue("Score is " + l1.score(250), Math.abs(1.0 - l1.score(250)) < 0.0001);
        assertTrue("Score is " + l1.score(4000), Math.abs(1.0 - l1.score(4000)) < 0.0001);
    }
}
