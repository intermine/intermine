package org.intermine.objectstore.query;

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

public class ResultsInfoTest extends TestCase
{
    public ResultsInfoTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        ResultsInfo r1 = new ResultsInfo(0L, 0L, 50);
        ResultsInfo r2 = new ResultsInfo(0L, 0L, 50, 30, 1000);
        ResultsInfo r3 = new ResultsInfo(0L, 0L, 50, 50, 1000);
        ResultsInfo r4 = new ResultsInfo(0L, 0L, 50, 51, 1000);
        ResultsInfo r5 = new ResultsInfo(0L, 0L, 50, 0, 50);
        ResultsInfo r6 = new ResultsInfo(0L, 0L, 50, 0, 49);
        ResultsInfo r7 = new ResultsInfo(0L, 0L, 50, 50, 50);
        ResultsInfo r8 = new ResultsInfo(0L, 0L, 50, 49, 49);

        assertEquals(50, r1.getRows());
        assertEquals(50, r2.getRows());
        assertEquals(50, r3.getRows());
        assertEquals(51, r4.getRows());
        assertEquals(50, r5.getRows());
        assertEquals(49, r6.getRows());
        assertEquals(50, r7.getRows());
        assertEquals(49, r8.getRows());

        assertEquals(ResultsInfo.ESTIMATE, r1.getStatus());
        assertEquals(ResultsInfo.ESTIMATE, r2.getStatus());
        assertEquals(ResultsInfo.AT_LEAST, r3.getStatus());
        assertEquals(ResultsInfo.AT_LEAST, r4.getStatus());
        assertEquals(ResultsInfo.AT_MOST, r5.getStatus());
        assertEquals(ResultsInfo.AT_MOST, r6.getStatus());
        assertEquals(ResultsInfo.SIZE, r7.getStatus());
        assertEquals(ResultsInfo.SIZE, r8.getStatus());
    }
}

