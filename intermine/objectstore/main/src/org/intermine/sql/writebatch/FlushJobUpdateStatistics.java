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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * An interface representing a job to be performed when actually flushing data to a database.
 *
 * @author Matthew Wakeling
 */
public class FlushJobUpdateStatistics implements FlushJob
{
    Map activity;
    BatchWriter batchWriter;
    Connection con;

    /**
     * Constructor for this class
     *
     * @param activity a Map from table name to amount of activity in this batch write
     * @param batchWriter a BatchWriter to use to update the statistics
     * @param con a Connection with which to perform the updates
     */
    public FlushJobUpdateStatistics(Map activity, BatchWriter batchWriter, Connection con) {
        this.activity = activity;
        this.batchWriter = batchWriter;
        this.con = con;
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws SQLException {
        batchWriter.updateStatistics(activity, con);
    }
}
