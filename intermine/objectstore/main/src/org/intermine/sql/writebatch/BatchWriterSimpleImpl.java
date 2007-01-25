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

import org.intermine.sql.DatabaseUtil;

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
    protected int deleteTempTableSize = 200;

    protected Connection con;

    protected Statement preDeleteBatch;
    protected List deleteBatches;
    protected Statement postDeleteBatch;
    protected List addBatches;
    protected Statement lastBatch;

    /**
     * This sets the threshold above which a temp table will be used for deletes.
     *
     * @param deleteTempTableSize the threshold
     */
    public void setThreshold(int deleteTempTableSize) {
        this.deleteTempTableSize = deleteTempTableSize;
    }

    /**
     * @see BatchWriter#write
     */
    public List write(Connection con, Map tables, Set filter) throws SQLException {
        this.con = con;
        // Initialise object for action. Note this code is NOT re-entrant.
        preDeleteBatch = null;
        deleteBatches = new ArrayList();
        postDeleteBatch = null;
        addBatches = new ArrayList();
        lastBatch = null;
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
                    activity += doInserts(name, (TableBatch) table, addBatches);
                } else {
                    activity += 2 * doIndirectionDeletes(name, (IndirectionTableBatch) table);
                    activity += doIndirectionInserts(name, (IndirectionTableBatch) table,
                            addBatches);
                }
                table.clear();
                if (activity > 0) {
                    activityMap.put(name, new Integer(activity));
                }
            }
        }
        List retval = new ArrayList();
        if (preDeleteBatch != null) {
            retval.add(new FlushJobStatementBatchImpl(preDeleteBatch));
        }
        retval.addAll(deleteBatches);
        if (postDeleteBatch != null) {
            retval.add(new FlushJobStatementBatchImpl(postDeleteBatch));
        }
        retval.addAll(addBatches);
        if (lastBatch != null) {
            retval.add(new FlushJobStatementBatchImpl(lastBatch));
        }
        if (!activityMap.isEmpty()) {
            retval.add(new FlushJobUpdateStatistics(activityMap, this, con));
        }
        // Help the garbage collector
        preDeleteBatch = null;
        deleteBatches = null;
        postDeleteBatch = null;
        addBatches = null;
        lastBatch = null;
        return retval;
    }

    /**
     * Performs all the inserts for the given table name and table batch.
     *
     * @param name the name of the table
     * @param table the table batch
     * @param batches the List of batches into which new flushjobs should be placed
     * @return the number of rows inserted
     * @throws SQLException if an error occurs
     */
    protected int doInserts(String name, TableBatch table, List batches) throws SQLException {
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
                    addToLastBatch(insertString(preamble, colNames.length, (Object[]) inserts));
                } else {
                    Iterator iter = ((List) inserts).iterator();
                    while (iter.hasNext()) {
                        Object values[] = (Object[]) iter.next();
                        addToLastBatch(insertString(preamble, colNames.length, values));
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
            if (table.getIdsToDelete().size() > deleteTempTableSize) {
                String tempTableName = "deletes_from_" + name;
                addToPreDeleteBatch("CREATE TABLE " + tempTableName + " (value integer)");
                TableBatch tableBatch = new TableBatch();
                String colNames[] = new String[] {"value"};
                Iterator iter = table.getIdsToDelete().iterator();
                while (iter.hasNext()) {
                    tableBatch.addRow(null, colNames, new Object[] {iter.next()});
                }
                doInserts(tempTableName, tableBatch, deleteBatches);
                addToPostDeleteBatch("DELETE FROM " + name + " WHERE " + idField
                        + " IN (SELECT value FROM " + tempTableName + ")");
                addToPostDeleteBatch("DROP TABLE " + tempTableName);
            } else {
                StringBuffer sqlBuffer = new StringBuffer("DELETE FROM ").append(name)
                    .append(" WHERE ").append(idField).append(" IN (");
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
                        addToPostDeleteBatch(sqlBuffer.toString());
                        sqlBuffer = new StringBuffer("DELETE FROM ").append(name).append(" WHERE ")
                            .append(idField).append(" IN (");
                        needComma = false;
                    }
                }
                if (statementSize > 0) {
                    sqlBuffer.append(")");
                    addToPostDeleteBatch(sqlBuffer.toString());
                }
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
            if (rows.size() > deleteTempTableSize) {
                String tempTableName = "deletes_from_" + name;
                addToPreDeleteBatch("CREATE TABLE " + tempTableName + " (a integer, b integer)");
                IndirectionTableBatch tableBatch = new IndirectionTableBatch("a", "b", rows);
                doIndirectionInserts(tempTableName, tableBatch, deleteBatches);
                addToPostDeleteBatch("DELETE FROM " + name + " WHERE (" + table.getLeftColName()
                        + ", " + table.getRightColName() + ") IN (SELECT a, b FROM "
                        + tempTableName + ")");
                addToPostDeleteBatch("DROP TABLE " + tempTableName);
            } else {
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
                        addToPostDeleteBatch(sql.toString());
                        sql = new StringBuffer("DELETE FROM ").append(name).append(" WHERE (");
                        needComma = false;
                    }
                }
                if (statementSize > 0) {
                    sql.append(")");
                    addToPostDeleteBatch(sql.toString());
                }
            }
        }
        return table.getRowsToDelete().size();
    }

    /**
     * Performs all the insert operations for the given IndirectionTableBatch and name.
     *
     * @param name the name of the table
     * @param table the IndirectionTableBatch
     * @param batches the List of flushjobs to add further actions to
     * @return the number of rows inserted
     * @throws SQLException if an error occurs
     */
    protected int doIndirectionInserts(String name,
            IndirectionTableBatch table, List batches) throws SQLException {
        if (!table.getRowsToInsert().isEmpty()) {
            String preamble = "INSERT INTO " + name + " (" + table.getLeftColName() + ", "
                + table.getRightColName() + ") VALUES (";
            Iterator insertIter = table.getRowsToInsert().iterator();
            while (insertIter.hasNext()) {
                Row row = (Row) insertIter.next();
                StringBuffer sql = new StringBuffer(preamble).append(row.getLeft()).append(", ")
                    .append(row.getRight()).append(")");
                addToLastBatch(sql.toString());
            }
        }
        return table.getRowsToInsert().size();
    }

    /**
     * Adds a statement to the preDeleteBatch.
     *
     * @param sql the statement
     * @throws SQLException if an error occurs
     */
    protected void addToPreDeleteBatch(String sql) throws SQLException {
        if (preDeleteBatch == null) {
            preDeleteBatch = con.createStatement();
        }
        preDeleteBatch.addBatch(sql);
    }

    /**
     * Adds a statement to the postDeleteBatch.
     *
     * @param sql the statement
     * @throws SQLException if an error occurs
     */
    protected void addToPostDeleteBatch(String sql) throws SQLException {
        // Note that this is a fudge - in subclasses you wil almost certainly want to override this
        // method to make it actually do what the prototype says. The fudge exists in order to speed
        // up the SimpleImpl by using only one Statement batch.
        addToPreDeleteBatch(sql);
    }

    /**
     * Adds a statement to the lastBatch.
     *
     * @param sql the statement
     * @throws SQLException if an error occurs
     */
    protected void addToLastBatch(String sql) throws SQLException {
        // Note that this is a fudge - in subclasses you wil almost certainly want to override this
        // method to make it actually do what the prototype says. The fudge exists in order to speed
        // up the SimpleImpl by using only one Statement batch.
        addToPreDeleteBatch(sql);
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
        private int tableSize, totalActivity;

        public Statistic(String name, int tableSize, int activity) {
            this.name = name;
            this.tableSize = tableSize - activity; // Yes, this is a hack. It works.
            this.totalActivity = 0;
            LOG.debug("Statistics: " + name + " created: tableSize = " + tableSize
                    + " (hacked down to " + this.tableSize + " for unaccounted-for activity)");
        }

        // Return true (i.e. do analyse) if total number of rows written is greater than some
        // threshold.  Currently if aprrox 50% of original table size.
        public boolean addActivity(int activity) {
            LOG.debug("Statistics: " + name + ", tableSize = " + tableSize + ", activity "
                    + this.totalActivity + " --> tableSize = " + tableSize + ", activity = "
                    + (this.totalActivity + activity) + "    - Activity of " + activity + " rows");
            this.totalActivity += activity;
            return this.totalActivity > (tableSize / 2) + 1000;
        }

        public void setTableSize(int tableSize) {
            LOG.debug("Statistics: " + name + ", tableSize = " + this.tableSize + ", activity "
                    + this.totalActivity + " --> tableSize = " + tableSize
                    + ", activity = 0   - New table size");
            this.tableSize = tableSize;
            this.totalActivity = 0;
        }
    }
}
