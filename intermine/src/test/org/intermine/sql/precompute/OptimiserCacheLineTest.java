package org.flymine.sql.precompute;

import junit.framework.*;
import java.util.Date;

public class OptimiserCacheLineTest extends TestCase
{
    public OptimiserCacheLineTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        OptimiserCacheLine l1 = new OptimiserCacheLine("optimised query", 1000, 0, 1000);
        assertTrue(!l1.isExpired());
        l1.expires = new Date();
        assertTrue("Score is " + l1.score(1000,0), 0.0 == l1.score(1000, 0));
        assertTrue(0.0 == l1.score(1000, 100));
        assertTrue("Score is " + l1.score(250, 0), Math.abs(1.0 - l1.score(250, 0)) < 0.0001);
        assertTrue("Score is " + l1.score(4000, 0), Math.abs(1.0 - l1.score(4000, 0)) < 0.0001);
        assertTrue(1.0 == l1.score(1000, 225));
        Thread.sleep(100);
        assertTrue(l1.isExpired());
    }

    public void test2() throws Exception {
        OptimiserCacheLine l1 = new OptimiserCacheLine("optimised query", 1000, 500, 1000);
        assertTrue("Score is " + l1.score(1000,0), 3.2 == l1.score(1000, 0));
        assertTrue(2.4 == l1.score(1000, 100));
    }

}
