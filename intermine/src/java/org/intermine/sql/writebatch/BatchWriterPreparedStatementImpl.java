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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    private static final Logger LOG = Logger.getLogger(BatchWriterPreparedStatementImpl.class);

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
        }
        if (simpleBatchSize > 0) {
            retval.add(new FlushJobStatementBatchImpl(simpleBatch));
        }
        simpleBatch = null;
        tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            String name = (String) tableEntry.getKey();
            TableBatch table = (TableBatch) tableEntry.getValue();
            doInserts(name, table);
        }
        return retval;
    }

    /**
     * @see BatchWriterSimpleImpl#doInserts
     */
    protected void doInserts(String name, TableBatch table) throws SQLException {
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
            Iterator insertIter = table.getIdsToInsert().entrySet().iterator();
            while (insertIter.hasNext()) {
                Map.Entry insertEntry = (Map.Entry) insertIter.next();
                Object inserts = insertEntry.getValue();
                if (inserts instanceof Object[]) {
                    Object values[] = (Object[]) inserts;
                    for (int i = 0; i < colNames.length; i++) {
                        prepS.setObject(i + 1, values[i]);
                    }
                    prepS.addBatch();
                } else {
                    Iterator iter = ((List) inserts).iterator();
                    while (iter.hasNext()) {
                        Object values[] = (Object[]) iter.next();
                        for (int i = 0; i < colNames.length; i++) {
                            prepS.setObject(i + 1, values[i]);
                        }
                        prepS.addBatch();
                    }
                }
            }
            retval.add(new FlushJobStatementBatchImpl(prepS));
            table.getIdsToInsert().clear();
        }
    }
}
