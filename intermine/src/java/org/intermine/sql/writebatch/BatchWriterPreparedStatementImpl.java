package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List write(Connection con, Map tables, Set filter) throws SQLException {
        retval = new ArrayList();
        this.con = con;
        simpleBatch = con.createStatement();
        simpleBatchSize = 0;
        Map activityMap = new HashMap();
        Iterator tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            String name = (String) tableEntry.getKey();
            if ((filter == null) || filter.contains(name)) {
                int activity;
                Table table = (Table) tableEntry.getValue();
                if (table instanceof TableBatch) {
                    activity = 2 * doDeletes(name, (TableBatch) table);
                } else {
                    activity = 2 * doIndirectionDeletes(name, (IndirectionTableBatch) table);
                }
                if (activity > 0) {
                    activityMap.put(name, new Integer(activity));
                }
            }
        }
        if (simpleBatchSize > 0) {
            retval.add(new FlushJobStatementBatchImpl(simpleBatch));
        }
        simpleBatch = null;
        tableIter = tables.entrySet().iterator();
        while (tableIter.hasNext()) {
            Map.Entry tableEntry = (Map.Entry) tableIter.next();
            String name = (String) tableEntry.getKey();
            if ((filter == null) || filter.contains(name)) {
                int activity;
                Table table = (Table) tableEntry.getValue();
                if (table instanceof TableBatch) {
                    activity = doInserts(name, (TableBatch) table);
                } else {
                    activity = doIndirectionInserts(name, (IndirectionTableBatch) table);
                }
                table.clear();
                if (activity > 0) {
                    Integer oldActivity = (Integer) activityMap.get(name);
                    if (oldActivity != null) {
                        activity += oldActivity.intValue();
                    }
                    activityMap.put(name, new Integer(activity));
                }
            }
        }
        if (!activityMap.isEmpty()) {
            retval.add(new FlushJobUpdateStatistics(activityMap, this, con));
        }
        return retval;
    }

    /**
     * @see BatchWriterSimpleImpl#doInserts
     */
    protected int doInserts(String name, TableBatch table) throws SQLException {
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
            return table.getIdsToInsert().size();
        }
        return 0;
    }

    /**
     * @see BatchWriterSimpleImpl#doIndirectionInserts
     */
    protected int doIndirectionInserts(String name,
            IndirectionTableBatch table) throws SQLException {
        if (!table.getRowsToInsert().isEmpty()) {
            String sql = "INSERT INTO " + name + " (" + table.getLeftColName() + ", "
                + table.getRightColName() + ") VALUES (?, ?)";
            PreparedStatement prepS = con.prepareStatement(sql);
            Iterator insertIter = table.getRowsToInsert().iterator();
            while (insertIter.hasNext()) {
                Row row = (Row) insertIter.next();
                prepS.setInt(1, row.getLeft());
                prepS.setInt(2, row.getRight());
                prepS.addBatch();
            }
            retval.add(new FlushJobStatementBatchImpl(prepS));
        }
        return table.getRowsToInsert().size();
    }
}
