package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2003 FlyMine
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
public class BatchWriterPostgresCopyImplTest extends BatchWriterTestCase
{
    public BatchWriterPostgresCopyImplTest(String arg) {
        super(arg);
    }

    public BatchWriter getWriter() {
        return new BatchWriterPostgresCopyImpl();
    }
}



