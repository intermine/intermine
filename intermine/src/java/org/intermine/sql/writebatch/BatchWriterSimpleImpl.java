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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                int activity = 0;
                Table table = (Table) tableEntry.getValue();
                if (table instanceof TableBatch) {
                    activity += 2 * doDeletes(name, (TableBatch) table);
                    activity += doInserts(name, (TableBatch) table);
                } else {
                    activity += 2 * doIndirectionDeletes(name, (IndirectionTableBatch) table);
                    activity += doIndirectionInserts(name, (IndirectionTableBatch) table);
                }
                table.clear();
                if (activity > 0) {
                    activityMap.put(name, new Integer(activity));
                }
            }
        }
        if (simpleBatchSize > 0) {
            retval.add(new FlushJobStatementBatchImpl(simpleBatch));
        }
        if (!activityMap.isEmpty()) {
            retval.add(new FlushJobUpdateStatistics(activityMap, this, con));
        }
        simpleBatch = null;
        return retval;
    }

    /**
     * Performs all the inserts for the given table name and table batch.
     *
     * @param name the name of the table
     * @param table the table batch
     * @return the number of rows inserted
     * @throws SQLException if an error occurs
     */
    protected int doInserts(String name, TableBatch table) throws SQLException {
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
            return table.getIdsToInsert().size();
        }
        return 0;
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
     * @return the number of rows deleted
     * @throws SQLException if an error occurs
     */
    protected int doDeletes(String name, TableBatch table) throws SQLException {
        String idField = table.getIdField();
        if ((idField != null) && (!table.getIdsToDelete().isEmpty())) {
            StringBuffer sqlBuffer = new StringBuffer("DELETE FROM ").append(name).append(" WHERE ")
                .append(idField).append(" IN (");
            boolean needComma = false;
            int statementSize = 0;
            Iterator iter = table.getIdsToDelete().iterator();
            while (iter.hasNext()) {
                Object idValue = iter.next();
                if (needComma) {
                    sqlBuffer.append(", ");
                }
                needComma = true;
                sqlBuffer.append(DatabaseUtil.objectToString(idValue));
                statementSize++;
                if (statementSize >= 500) {
                    statementSize = 0;
                    sqlBuffer.append(")");
                    addToSimpleBatch(sqlBuffer.toString());
                    sqlBuffer = new StringBuffer("DELETE FROM ").append(name).append(" WHERE ")
                        .append(idField).append(" IN (");
                    needComma = false;
                }
            }
            if (statementSize > 0) {
                sqlBuffer.append(")");
                addToSimpleBatch(sqlBuffer.toString());
            }
            return table.getIdsToDelete().size();
        }
        return 0;
    }

    /**
     * Performs all the delete operations for the given IndirectionTableBatch and name.
     *
     * @param name the name of the table
     * @param table the IndirectionTableBatch
     * @return the number of rows deleted
     * @throws SQLException if an error occurs
     */
    protected int doIndirectionDeletes(String name,
            IndirectionTableBatch table) throws SQLException {
        Set rows = new CombinedSet(table.getRowsToDelete(), table.getRowsToInsert());
        if (!rows.isEmpty()) {
            StringBuffer sql = new StringBuffer("DELETE FROM ").append(name).append(" WHERE (");
            boolean needComma = false;
            int statementSize = 0;
            Iterator dIter = rows.iterator();
            while (dIter.hasNext()) {
                Row row = (Row) dIter.next();
                if (needComma) {
                    sql.append(" OR ");
                }
                sql.append("(").append(table.getLeftColName()).append(" = ")
                    .append(row.getLeft()).append(" AND ").append(table.getRightColName())
                    .append(" = ").append(row.getRight()).append(")");
                needComma = true;
                statementSize++;
                if (statementSize >= 500) {
                    statementSize = 0;
                    sql.append(")");
                    addToSimpleBatch(sql.toString());
                    sql = new StringBuffer("DELETE FROM ").append(name).append(" WHERE (");
                    needComma = false;
                }
            }
            if (statementSize > 0) {
                sql.append(")");
                addToSimpleBatch(sql.toString());
            }
        }
        return table.getRowsToDelete().size();
    }

    /**
     * Performs all the insert operations for the given IndirectionTableBatch and name.
     *
     * @param name the name of the table
     * @param table the IndirectionTableBatch
     * @return the number of rows inserted
     * @throws SQLException if an error occurs
     */
    protected int doIndirectionInserts(String name,
            IndirectionTableBatch table) throws SQLException {
        if (!table.getRowsToInsert().isEmpty()) {
            String preamble = "INSERT INTO " + name + " (" + table.getLeftColName() + ", "
                + table.getRightColName() + ") VALUES (";
            Iterator insertIter = table.getRowsToInsert().iterator();
            while (insertIter.hasNext()) {
                Row row = (Row) insertIter.next();
                StringBuffer sql = new StringBuffer(preamble).append(row.getLeft()).append(", ")
                    .append(row.getRight()).append(")");
                addToSimpleBatch(sql.toString());
            }
        }
        return table.getRowsToInsert().size();
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

    private static class CombinedSet extends AbstractSet
    {
        private Set setA, setB;

        public CombinedSet(Set setA, Set setB) {
            this.setA = setA;
            this.setB = setB;
        }

        public int size() {
            return setA.size() + setB.size();
        }

        public Iterator iterator() {
            return new CombinedIterator();
        }

        private class CombinedIterator implements Iterator
        {
            private boolean state = true;
            private Iterator iter = setA.iterator();

            public boolean hasNext() {
                if (state) {
                    if (iter.hasNext()) {
                        return true;
                    } else {
                        state = false;
                        iter = setB.iterator();
                        return iter.hasNext();
                    }
                } else {
                    return iter.hasNext();
                }
            }
            
            public Object next() {
                if (state && (!iter.hasNext())) {
                    iter = setB.iterator();
                }
                return iter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    /*
     * All code above this comment is called by the thread that calls into the Batch.
     * All code below this comment is called by the Batch writer thread.
     * They do not access any common instance variables, so they need no synchronisation.
     */

    protected Map stats = new HashMap();

    /**
     * @see BatchWriter#updateStatistics
     */
    public void updateStatistics(Map activity, Connection conn) throws SQLException {
        Iterator iter = activity.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String name = (String) entry.getKey();
            int amount = ((Integer) entry.getValue()).intValue();
            Statistic stat = (Statistic) stats.get(name);
            if (stat == null) {
                stat = new Statistic(name, getTableSize(name, conn), amount);
                stats.put(name, stat);
            }
            boolean doAnalyse = stat.addActivity(amount);
            if (doAnalyse) {
                long start = System.currentTimeMillis();
                doAnalyse(name, conn);
                int tableSize = getTableSize(name, conn);
                stat.setTableSize(tableSize);
                long end = System.currentTimeMillis();
                LOG.info("Analysing table " + name + " took " + (end - start) + "ms ("
                        + tableSize + " rows)");
            }
        }
    }

    /**
     * Returns the approximate number of rows in a table.
     *
     * @param name the name of the table
     * @param conn a Connection to use
     * @return an int
     * @throws SQLException if there is a problem
     */
    protected int getTableSize(String name, Connection conn) throws SQLException {
        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + name);
        if (r.next()) {
            int returnValue = r.getInt(1);
            if (r.next()) {
                throw new SQLException("Too many results");
            }
            return returnValue;
        } else {
            throw new SQLException("No results");
        }
    }

    /**
     * Performs an ANALYSE of a table.
     *
     * @param name the name of the table
     * @param conn a Connection to use
     * @throws SQLException if something goes wrong
     */
    protected void doAnalyse(String name, Connection conn) throws SQLException {
        Statement s = conn.createStatement();
        s.execute("ANALYSE VERBOSE " + name);
        SQLWarning e = s.getWarnings();
        while (e != null) {
            LOG.debug("ANALYSE WARNING: " + e.toString());
            e = e.getNextWarning();
        }
    }

    private static class Statistic
    {
        private String name;
        private int tableSize, activity;

        public Statistic(String name, int tableSize, int activity) {
            this.name = name;
            this.tableSize = tableSize - activity; // Yes, this is a hack. It works.
            this.activity = 0;
            LOG.debug("Statistics: " + name + " created: tableSize = " + tableSize
                    + " (hacked down to " + this.tableSize + " for unaccounted-for activity)");
        }

        public boolean addActivity(int activity) {
            LOG.debug("Statistics: " + name + ", tableSize = " + tableSize + ", activity "
                    + this.activity + " --> tableSize = " + tableSize + ", activity = "
                    + (this.activity + activity) + "    - Activity of " + activity + " rows");
            this.activity += activity;
            return this.activity > tableSize + 1000;
        }

        public void setTableSize(int tableSize) {
            LOG.debug("Statistics: " + name + ", tableSize = " + this.tableSize + ", activity "
                    + this.activity + " --> tableSize = " + tableSize
                    + ", activity = 0   - New table size");
            this.tableSize = tableSize;
            this.activity = 0;
        }
    }
}
