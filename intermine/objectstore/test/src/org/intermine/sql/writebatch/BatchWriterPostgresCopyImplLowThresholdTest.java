package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Test for doing tests on the BatchWriterPostgresCopyImpl.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterPostgresCopyImplLowThresholdTest extends BatchWriterPostgresCopyImplTest
{
    public BatchWriterPostgresCopyImplLowThresholdTest(String arg) {
        super(arg);
    }

    public int getThreshold() {
        return 0;
    }

    public void testAnalyseLargeTable() throws Exception {
        // We don't need to run this test twice
    }

    public void testInsertOnly() throws Exception {
        // We don't need to run this test twice
    }

    public void testTypes() throws Exception {
        // We don't need to run this test twice
    }

    public void testUTF() throws Exception {
        // We don't need to run this test twice
    }

    public void testPartialFlush() throws Exception {
        // We don't need to run this test twice
    }
}
