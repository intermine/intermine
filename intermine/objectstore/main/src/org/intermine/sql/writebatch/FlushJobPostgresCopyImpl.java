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

import java.io.ByteArrayInputStream;
import java.sql.SQLException;

import org.postgresql.copy.CopyManager;

/**
 * An implementation of the FlushJob interface that represents a batch created in a Statement.
 *
 * @author Matthew Wakeling
 */
public class FlushJobPostgresCopyImpl implements FlushJob
{
    CopyManager copyManager;
    String sql;
    byte data[];

    /**
     * Constructor for this class
     *
     * @param copyManager the CopyManager to use
     * @param sql the SQL String containing the COPY command
     * @param data a byte array of COPY data
     */
    public FlushJobPostgresCopyImpl(CopyManager copyManager, String sql, byte data[]) {
        this.copyManager = copyManager;
        this.sql = sql;
        this.data = data;
    }
    
    /**
     * @see FlushJob#flush
     */
    public void flush() throws SQLException {
        try {
            copyManager.copyInQuery(sql, new ByteArrayInputStream(data));
            copyManager = null;
            sql = null;
            data = null;
        } catch (SQLException e) {
            SQLException e2 = new SQLException("Error writing to database, running statement "
                    + sql);
            e2.initCause(e);
            throw e2;
        }
    }
}

