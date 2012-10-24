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
 * Test for doing tests on the BatchWriterPreparedStatementImpl.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterPreparedStatementImplTest extends BatchWriterTestCase
{
    public BatchWriterPreparedStatementImplTest(String arg) {
        super(arg);
    }

    public BatchWriter getWriter() {
        BatchWriterPreparedStatementImpl bw = new BatchWriterPreparedStatementImpl();
        bw.setThreshold(getThreshold());
        return bw;
    }
}


