package org.intermine.dataconversion;

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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.intermine.sql.Database;

import org.apache.log4j.Logger;

/**
 * A reasonably dumb implementation of the DBReader interface.
 *
 * @author Matthew Wakeling
 */
public class DirectDBReader implements DBReader
{
    private static final Logger LOG = Logger.getLogger(DirectDBReader.class);

    protected static final int BATCH_SIZE = 20000;
    protected static final int MAX_SIZE = 10000000;
    protected Database db;
    protected DBBatch batch;
    protected int queryNo;
    protected String sql;
    protected String idField;
    protected String tableName;

    protected long iteratorTime = 0;
    protected long oobTime = 0;

    /**
     * Constructs a new DirectDBReader.
     *
     * @param db the Database to access
     */
    public DirectDBReader(Database db) {
        this.db = db;
        batch = null;
        queryNo = 0;
    }
    
    /**
     * @see DBReader#sqlIterator
     */
    public Iterator sqlIterator(String sql, String idField, String tableName) {
        queryNo++;
        batch = null;
        this.sql = sql;
        this.idField = idField;
        this.tableName = tableName;
        return new SqlIterator(queryNo);
    }

    /**
     * @see DBReader#execute
     */
    public List execute(String sql) throws SQLException {
        long start = System.currentTimeMillis();
        Connection c = db.getConnection();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery(sql);
        List rows = new ArrayList();
        ResultSetMetaData rMeta = r.getMetaData();
        int columnCount = rMeta.getColumnCount();
        String columnNames[] = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = rMeta.getColumnName(i + 1);
        }
        while (r.next()) {
            Map row = new HashMap();
            for (int i = 1; i <= columnCount; i++) {
                row.put(columnNames[i - 1], r.getObject(i));
            }
            rows.add(row);
        }
        c.close();
        long end = System.currentTimeMillis();
        oobTime += end - start;
        if (oobTime / 100000 > (oobTime - end + start) / 100000) {
            LOG.info("Spent " + oobTime + " ms on out-of-band queries like (" + (end - start)
                    + " ms) " + sql);
        }
        return rows;
    }

    /**
     * Creates a new DBBatch for a given offset.
     *
     * @param previous the previous batch, or null if this is the first
     * @return a DBBatch
     * @throws SQLException if the database has a problem
     */
    protected DBBatch getBatch(DBBatch previous) throws SQLException {
        String tempSql = sql;
        String tempSizeQuery = null;
        String sizeQuery = null;
        Connection c = null;
        try {
            c = db.getConnection();
            if (previous != null) {
                if (previous.getRows().isEmpty()) {
                    return previous;
                }
                long start = System.currentTimeMillis();
                sizeQuery = previous.getSizeQuery();
                int whereIndex = sql.toUpperCase().indexOf(" WHERE ");
                tempSizeQuery = sizeQuery + (whereIndex == -1 ? " WHERE " : " AND ")
                    + idField + " > " + previous.getLastId() + " ORDER BY " + idField + " LIMIT "
                    + BATCH_SIZE;
                Statement s = c.createStatement();
                ResultSet r = null;
                try {
                    r = s.executeQuery(tempSizeQuery);
                } catch (SQLException e) {
                    LOG.error("Problem running query \"" + tempSizeQuery + "\"");
                    throw e;
                }
                int rowCount = 0;
                int sizeSum = 0;
                while (r.next() && (sizeSum <= MAX_SIZE)) {
                    sizeSum += r.getInt("size");
                    rowCount++;
                }
                if (rowCount <= 0) {
                    rowCount = 1;
                }
                long end = System.currentTimeMillis();
                iteratorTime += end - start;
                if (iteratorTime / 100000 > (iteratorTime - end + start) / 100000) {
                    LOG.info("Spent " + iteratorTime + " ms on iterator queries like ("
                            + (end - start) + " ms) " + tempSizeQuery);
                }
                tempSql = tempSql + (whereIndex == -1 ? " WHERE " : " AND ") + idField + " > "
                    + previous.getLastId() + " ORDER BY " + idField + " LIMIT " + rowCount;
            } else {
                tempSql = tempSql + " ORDER BY " + idField + " LIMIT 1";
            }
            long start = System.currentTimeMillis();
            Statement s = c.createStatement();
            ResultSet r = null;
            try {
                r = s.executeQuery(tempSql);
            } catch (SQLException e) {
                LOG.error("Problem running query \"" + tempSql + "\"");
                throw e;
            }
            long afterExecute = System.currentTimeMillis();
            List rows = new ArrayList();
            ResultSetMetaData rMeta = r.getMetaData();
            int columnCount = rMeta.getColumnCount();
            String columnNames[] = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = rMeta.getColumnName(i + 1);
            }
            while (r.next()) {
                Map row = new HashMap();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(columnNames[i - 1], r.getObject(i));
                }
                rows.add(row);
            }
            long end = System.currentTimeMillis();
            iteratorTime += end - start;
            if (iteratorTime / 100000 > (iteratorTime - end + start) / 100000) {
                LOG.info("Spent " + iteratorTime + " ms on iterator queries like ("
                        + (afterExecute - start) + " + " + (end - afterExecute) + " ms) "
                        + tempSql);
            }
            if (sizeQuery == null) {
                sizeQuery = "SELECT " + idField + ", 30";
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rMeta.getColumnName(i);
                    if (rMeta.getColumnType(i) != java.sql.Types.BIT) {
                        sizeQuery += " + char_length(" + columnName + ")";
                    }
                }
                sizeQuery += " AS size";
                int whereIndex = sql.toUpperCase().indexOf(" FROM ");
                sizeQuery += sql.substring(whereIndex);
            }
            return new DBBatch((previous == null ? 0 : previous.getOffset()
                        + previous.getRows().size()), rows, new HashMap(), idField, sizeQuery);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * @see DBReader#close
     */
    public void close() {
    }

    /**
     * Nested class to provide the sqlIterator service.
     */
    private class SqlIterator implements Iterator
    {
        private int thisQueryNo;
        private int cursor;

        /**
         * Constructor
         *
         * @param queryNo the query number, to match against DirectDBReader.queryNo
         */
        public SqlIterator(int queryNo) {
            this.thisQueryNo = queryNo;
            cursor = 0;
            try {
                batch = getBatch(null);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * @see Iterator#hasNext
         */
        public boolean hasNext() {
            if (thisQueryNo != queryNo) {
                throw new IllegalStateException("Cannot use an SqlIterator once a new one has"
                        + " been created");
            }
            if (cursor < batch.getOffset() + batch.getRows().size()) {
                return true;
            } else {
                if (batch.getRows().size() == 0) {
                    return false;
                } else {
                    try {
                        batch = getBatch(batch);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return hasNext();
                }
            }
        }

        /**
         * @see Iterator#next
         */
        public Object next() {
            if (hasNext()) {
                return batch.getRows().get((cursor++) - batch.getOffset());
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * @see Iterator#remove
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
