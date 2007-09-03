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

/**
 * A FlushJob that commits and re-opens a transaction on the database.
 *
 * @author Matthew Wakeling
 */
public class FlushJobBatchCommit implements FlushJob
{
    Connection con;

    /**
     * Constructor for this class
     *
     * @param con a Connection with which to perform the updates
     */
    public FlushJobBatchCommit(Connection con) {
        this.con = con;
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws SQLException {
        con.commit();
    }
}

