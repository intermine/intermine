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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.util.DatabaseUtil;

import org.apache.log4j.Logger;

/**
 * An implementation of the BatchWriter interface that uses simple JDBC addBatch() and
 * executeBatch() methods.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterSimpleImpl implements BatchWriter
{
    private static final Logger LOG = Logger.getLogger(BatchWriterSimpleImpl.class);

    protected Connection con;
    protected Statement simpleBatch;
    protected int simpleBatchSize;
    protected List retval;

    /**
     * @see BatchWriter#write
     */
    public List write(Connection con, Map tables) throws SQLException {
        retval = new ArrayList();
        this.con = con;
        simpleBatch = con.createStatement();
        simpleBatchSize = 0;
        Iterator tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            String name = (String) tableEntry.getKey();
            TableBatch table = (TableBatch) tableEntry.getValue();
            doDeletes(name, table);
            doInserts(name, table);
        }
        if (simpleBatchSize > 0) {
            retval.add(new FlushJobStatementBatchImpl(simpleBatch));
        }
        simpleBatch = null;
        return retval;
    }

    /**
     * Performs all the inserts for the given table name and table batch.
     *
     * @param name the name of the table
     * @param table the table batch
     * @throws SQLException if an error occurs
     */
    protected void doInserts(String name, TableBatch table) throws SQLException {
        String colNames[] = table.getColNames();
        if ((colNames != null) && (!table.getIdsToInsert().isEmpty())) {
            StringBuffer preambleBuffer = new StringBuffer("INSERT INTO ").append(name)
                .append(" (");
            for (int i = 0; i < colNames.length; i++) {
                if (i > 0) {
                    preambleBuffer.append(", ");
                }
                preambleBuffer.append(colNames[i]);
            }
            preambleBuffer.append(") VALUES (");
            String preamble = preambleBuffer.toString();
            Iterator insertIter = table.getIdsToInsert().entrySet().iterator();
            while (insertIter.hasNext()) {
                Map.Entry insertEntry = (Map.Entry) insertIter.next();
                Object inserts = insertEntry.getValue();
                if (inserts instanceof Object[]) {
                    addToSimpleBatch(insertString(preamble, colNames.length, (Object[]) inserts));
                } else {
                    Iterator iter = ((List) inserts).iterator();
                    while (iter.hasNext()) {
                        Object values[] = (Object[]) iter.next();
                        addToSimpleBatch(insertString(preamble, colNames.length, values));
                    }
                }
            }
            table.getIdsToInsert().clear();
        }
    }

    private static String insertString(String preamble, int colCount, Object values[]) {
        StringBuffer sqlBuffer = new StringBuffer((int) (TableBatch.sizeOfArray(values)
                    * 1.01 + 1000)).append(preamble);
        for (int i = 0; i < colCount; i++) {
            if (i > 0) {
                sqlBuffer.append(", ");
            }
            sqlBuffer.append(DatabaseUtil.objectToString(values[i]));
        }
        sqlBuffer.append(")");
        return sqlBuffer.toString();
    }

    /**
     * Performs all the deletes for the given table name and table batch.
     *
     * @param name the name of the table
     * @param table the table batch
     * @throws SQLException if an error occurs
     */
    protected void doDeletes(String name, TableBatch table) throws SQLException {
        String idField = table.getIdField();
        if ((idField != null) && (!table.getIdsToDelete().isEmpty())) {
            StringBuffer sqlBuffer = new StringBuffer("DELETE FROM ").append(name).append(" WHERE ")
                .append(idField).append(" IN (");
            boolean needComma = false;
            Iterator iter = table.getIdsToDelete().iterator();
            while (iter.hasNext()) {
                Object idValue = iter.next();
                if (needComma) {
                    sqlBuffer.append(", ");
                }
                needComma = true;
                sqlBuffer.append(DatabaseUtil.objectToString(idValue));
            }
            sqlBuffer.append(")");
            addToSimpleBatch(sqlBuffer.toString());
            table.getIdsToDelete().clear();
        }
    }

    /**
     * Adds a statement to the simpleBatch, and flushes if it is too big.
     *
     * @param sql the statement
     * @throws SQLException if an error occurs
     */
    protected void addToSimpleBatch(String sql) throws SQLException {
        //LOG.debug("Batching " + sql);
        simpleBatch.addBatch(sql);
        simpleBatchSize += sql.length();
        if (simpleBatchSize > 10000000) {
            retval.add(new FlushJobStatementBatchImpl(simpleBatch));
            simpleBatchSize = 0;
        }
    }
}
