package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the BatchWriter interface that uses JDBC PreparedStatement addBatch() and
 * executeBatch() methods.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterPreparedStatementImpl extends BatchWriterSimpleImpl
{
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected int doInserts(String name, TableBatch table, List<FlushJob> batches)
        throws SQLException {
        String[] colNames = table.getColNames();
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
            for (Map.Entry<Object, Object> insertEntry : table.getIdsToInsert().entrySet()) {
                Object inserts = insertEntry.getValue();
                if (inserts instanceof Object[]) {
                    Object[] values = (Object[]) inserts;
                    for (int i = 0; i < colNames.length; i++) {
                        Object value = values[i];
                        if (value instanceof CharSequence) {
                            value = ((CharSequence) value).toString();
                        }
                        prepS.setObject(i + 1, value);
                    }
                    prepS.addBatch();
                } else {
                    for (Object[] values : ((List<Object[]>) inserts)) {
                        for (int i = 0; i < colNames.length; i++) {
                            Object value = values[i];
                            if (value instanceof CharSequence) {
                                value = ((CharSequence) value).toString();
                            }
                            prepS.setObject(i + 1, value);
                        }
                        prepS.addBatch();
                    }
                }
            }
            batches.add(new FlushJobStatementBatchImpl(prepS));
            return table.getIdsToInsert().size();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int doIndirectionInserts(String name,
            IndirectionTableBatch table, List<FlushJob> batches) throws SQLException {
        if (!table.getRowsToInsert().isEmpty()) {
            String sql = "INSERT INTO " + name + " (" + table.getLeftColName() + ", "
                + table.getRightColName() + ") VALUES (?, ?)";
            PreparedStatement prepS = con.prepareStatement(sql);
            for (Row row : table.getRowsToInsert()) {
                prepS.setInt(1, row.getLeft());
                prepS.setInt(2, row.getRight());
                prepS.addBatch();
            }
            batches.add(new FlushJobStatementBatchImpl(prepS));
        }
        return table.getRowsToInsert().size();
    }

    /**
     * Adds a statement to the postDeleteBatch.
     *
     * @param sql the statement
     * @throws SQLException if an error occurs
     */
    @Override
    protected void addToPostDeleteBatch(String sql) throws SQLException {
        if (postDeleteBatch == null) {
            postDeleteBatch = con.createStatement();
        }
        postDeleteBatch.addBatch(sql);
    }

    /**
     * Adds a statement to the lastBatch.
     *
     * @param sql the statement
     * @throws SQLException if an error occurs
     */
    @Override
    protected void addToLastBatch(String sql) throws SQLException {
        if (lastBatch == null) {
            lastBatch = con.createStatement();
        }
        lastBatch.addBatch(sql);
    }
}
