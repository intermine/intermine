package org.flymine.util;

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
import java.util.Random;

public class CacheMapTest extends TestCase
{
    public CacheMapTest(String arg1) {
        super(arg1);
    }

    public void test() throws Exception {
        CacheMap cm = new CacheMap();
        Integer array[] = new Integer[100000];
        for (int i = 0; i < 100000; i++) {
            array[i] = new Integer(i);
            cm.put(array[i], null);
            cm.get(array[0]);
            cm.get(new Integer(3));
        }

        array = null;
        System.gc();

        assertTrue("Expected first two to be missing",!(cm.containsKey(new Integer(2)) && cm.containsKey(new Integer(1))));
        assertTrue("Expected last two to be present", cm.containsKey(new Integer(99998)) || cm.containsKey(new Integer(99997)));
        assertTrue("Expected number 0 to be present", cm.containsKey(new Integer(0)));
        assertTrue("Expected number 3 to be present", cm.containsKey(new Integer(3)));
    }

    public void test2() throws Exception {
        int maxInt = 36;
        Integer array[] = new Integer[maxInt];
        int count[] = new int[maxInt];
        int count2[] = new int[maxInt];
        int largeCount = 0;
        for (int i = 0; i < maxInt; i++) {
            array[i] = new Integer(i);
            count[i] = 0;
            count2[i] = 0;
        }

        Random rand = new Random(872341134); // Arbitrary random seed, for predictability.
        //Random rand = new Random();
        CacheMap.accessedRand = new Random(87623421);
        
        double maxDouble = Math.exp(10.0) - Math.exp(3.0) - 1.0;
        for (int i = 0; i < CacheMap.ACCESS_HOLDER_SMALL; i++) {
            CacheMap.small[i] = null;
        }
        if (CacheMap.large != null) {
            for (int i = 0; i < CacheMap.ACCESS_HOLDER_LARGE; i++) {
                ((Object []) CacheMap.large.get())[i] = null;
            }
        }
        for (int i = 0; i < 100000; i++) {
            double nextDouble = rand.nextDouble() * maxDouble;
            int n = 0;
            if (nextDouble < Math.exp(6.0) - Math.exp(3.0)) {
                n = (int) ((Math.log(nextDouble + Math.exp(3.0)) - 3.0) * 8.0);
            } else {
                n = (int) (Math.log(nextDouble + Math.exp(3.0)) * 3.0 + 6.0);
            }
            CacheMap.accessed(array[n]);
            count[n]++;
        }
        String ret = "\n";
        Object large[] = (Object []) CacheMap.large.get();
        for (int i = 0; i < CacheMap.ACCESS_HOLDER_LARGE; i++) {
            int n = (large[i] == null ? -1 : ((Integer) large[i]).intValue());
            ret += (i == 0 ? "" : ", ") + n;
            if (n >= 0) {
                count2[n]++;
                largeCount++;
            }
        }
        ret += "\n\n";

        double largeMultiplier = (Math.log(CacheMap.ACCESS_HOLDER_LARGE) - Math.log(CacheMap.ACCESS_HOLDER_LARGE - largeCount + 1)) / Math.log(2.0) + 1;
        for (int o = 0; o < CacheMap.ACCESS_HOLDER_SMALL; o++) {
            int n = ((Integer) CacheMap.small[o].get()).intValue();
            ret += "Number " + (CacheMap.small[o] == null ? "unfilled" : CacheMap.small[o].get() + ", accessed " + count[n] + " times, placed in large " + count2[n] + "(" + (((int) (count2[n] * 10.0 * largeMultiplier)) / 10.0) + ") times") + " in position " + o + "\n";
            //count2[n] = -1;
        }
        for (int o = 0; o < maxInt; o++) {
            if (count2[o] >= 0) {
                ret += "Number " + o + ", accessed " + count[o] + " times, placed in large " + count2[o] + "(" + (((int) (count2[o] * 10.0 * largeMultiplier)) / 10.0) + ") times\n";
            }
        }
        ret += "Large is " + ((largeCount * 100) / CacheMap.ACCESS_HOLDER_LARGE) + "% full - estimated real number of insertions in brackets";

        int expectedCount[] = {10, 19, 20, 14, 12, 25, 28, 33, 29, 38,
            43, 58, 45, 71, 88, 83, 80, 112, 115, 113,
            150, 171, 192, 211, 761, 1059, 1506, 1999, 2747, 3869,
            5327, 7351, 10428, 14607, 20079, 28507};
        int expectedCount2[] = {9, 13, 13, 10, 8, 19, 18, 23, 21, 24,
            28, 28, 27, 39, 36, 43, 41, 44, 43, 37,
            38, 36, 35, 40, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0};
        int expectedPositions[] = {32, 33, 35, 34, 29, 24, 31, 30, 26, 13,
            28, 16, 27, 14, 25, 18, 21, 23, 20, 12};
        for (int i = 0; i < maxInt; i++) {
            assertEquals("Count does not match for number " + i + ret, expectedCount[i], count[i]);
            assertEquals("Count2 does not match for number " + i + ret, expectedCount2[i], count2[i]);
        }
        for (int i = 0; i < CacheMap.ACCESS_HOLDER_SMALL; i++) {
            assertEquals("Position " + i + " does not match" + ret, expectedPositions[i], ((Integer) CacheMap.small[i].get()).intValue());
        }
        //fail(ret);
    }
}
