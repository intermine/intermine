package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
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

import org.flymine.sql.Database;

/**
 * A reasonably dumb implementation of the DBReader interface.
 *
 * @author Matthew Wakeling
 */
public class DirectDBReader implements DBReader
{
    private static final int BATCH_SIZE = 1000;
    private Database db;
    private DBBatch batch;
    private int queryNo;
    private String sql;
    private String idField;

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
    public Iterator sqlIterator(String sql, String idField) {
        queryNo++;
        batch = null;
        this.sql = sql;
        this.idField = idField;
        return new SqlIterator(queryNo);
    }

    /**
     * @see DBReader#execute
     */
    public List execute(String sql) throws SQLException {
        Connection c = db.getConnection();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery(sql);
        List rows = new ArrayList();
        ResultSetMetaData rMeta = r.getMetaData();
        int columnCount = rMeta.getColumnCount();
        while (r.next()) {
            Map row = new HashMap();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rMeta.getColumnName(i);
                row.put(columnName, r.getObject(i));
            }
            rows.add(row);
        }
        c.close();
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
        if (previous != null) {
            int whereIndex = sql.toUpperCase().indexOf(" WHERE ");
            tempSql = tempSql + (whereIndex == -1 ? " WHERE " : " AND ") + idField + " > "
                + previous.getLastId();
        }
        tempSql = tempSql + " ORDER BY " + idField + " LIMIT " + BATCH_SIZE;
        Connection c = db.getConnection();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery(tempSql);
        List rows = new ArrayList();
        ResultSetMetaData rMeta = r.getMetaData();
        int columnCount = rMeta.getColumnCount();
        while (r.next()) {
            Map row = new HashMap();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rMeta.getColumnName(i);
                row.put(columnName, r.getObject(i));
            }
            rows.add(row);
        }
        c.close();
        return new DBBatch((previous == null ? 0 : previous.getOffset()
                    + previous.getRows().size()), rows, null, idField);
    }

    /**
     * Nested class to provide the sqlIterator service.
     */
    private class SqlIterator implements Iterator
    {
        private int thisQueryNo;
        private String sql;
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
