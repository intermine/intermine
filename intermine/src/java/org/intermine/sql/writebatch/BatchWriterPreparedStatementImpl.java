package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * An implementation of the BatchWriter interface that uses JDBC PreparedStatement addBatch() and
 * executeBatch() methods.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterPreparedStatementImpl extends BatchWriterSimpleImpl
{
    protected static final Logger LOG = Logger.getLogger(BatchWriterPreparedStatementImpl.class);

    /**
     * @see BatchWriter#write
     */
    public void write(Connection con, Map tables) throws SQLException {
        this.con = con;
        long start = System.currentTimeMillis();
        simpleBatch = con.createStatement();
        simpleBatchSize = 0;
        Iterator tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            String name = (String) tableEntry.getKey();
            TableBatch table = (TableBatch) tableEntry.getValue();
            doDeletes(name, table);
        }
        if (simpleBatchSize > 0) {
            long beforeFlush = System.currentTimeMillis();
            simpleBatch.executeBatch();
            long now = System.currentTimeMillis();
            LOG.info("Flushing simpleBatch (size = " + simpleBatchSize + ", total time = "
                    + (now - start) + " ms, of which " + (now - beforeFlush) + " for flush)");
        }
        simpleBatch = null;
        tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            String name = (String) tableEntry.getKey();
            TableBatch table = (TableBatch) tableEntry.getValue();
            doInserts(name, table);
        }
    }

    /**
     * @see BatchWriterSimpleImpl#doInserts
     */
    protected void doInserts(String name, TableBatch table) throws SQLException {
        long start = System.currentTimeMillis();
        String colNames[] = table.getColNames();
        if ((colNames != null) && (!table.getIdsToInsert().isEmpty())) {
            StringBuffer sqlBuffer = new StringBuffer("INSERT INTO ").append(name).append(" (");
            for (int i = 0; i < colNames.length; i++) {
                if (i > 0) {
                    sqlBuffer.append(", ");
                }
                sqlBuffer.append(colNames[i]);
            }
            sqlBuffer.append(") VALUES (");
            for (int i = 0; i < colNames.length; i++) {
                if (i > 0) {
                    sqlBuffer.append(", ");
                }
                sqlBuffer.append("?");
            }
            sqlBuffer.append(")");
            String sql = sqlBuffer.toString();
            PreparedStatement prepS = con.prepareStatement(sql);
            int insertCount = 0;
            Iterator insertIter = table.getIdsToInsert().entrySet().iterator();
            while (insertIter.hasNext()) {
                Map.Entry insertEntry = (Map.Entry) insertIter.next();
                Object values[] = (Object[]) insertEntry.getValue();
                for (int i = 0; i < colNames.length; i++) {
                    prepS.setObject(i + 1, values[i]);
                }
                prepS.addBatch();
                insertCount++;
            }
            long beforeFlush = System.currentTimeMillis();
            prepS.executeBatch();
            long now = System.currentTimeMillis();
            LOG.info("Flushing PreparedStatement batch for table " + name + " (" + insertCount
                    + " inserts, total time = " + (now - start) + " ms, of which "
                    + (now - beforeFlush) + " for flush)");
            table.getIdsToInsert().clear();
        }
    }
}
