package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.SQLException;
import java.sql.Statement;

/**
 * An implementation of the FlushJob interface that represents a batch created in a Statement.
 *
 * @author Matthew Wakeling
 */
public class FlushJobStatementBatchImpl implements FlushJob
{
    Statement batch;

    /**
     * Constructor for this class
     *
     * @param batch the Statement containing the batch
     */
    public FlushJobStatementBatchImpl(Statement batch) {
        this.batch = batch;
    }
    
    /**
     * {@inheritDoc}
     */
    public void flush() throws SQLException {
        batch.executeBatch();
        batch.clearBatch();
    }
}
