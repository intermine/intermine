package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.sql.*;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.sql.query.Query;
import org.intermine.sql.query.AbstractValue;
import org.intermine.sql.query.SelectValue;
import org.intermine.sql.query.Table;

import org.apache.log4j.Logger;

/**
 * Manages all the Precomputed tables in a given database.
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class PrecomputedTableManager
{
    private static final Logger LOG = Logger.getLogger(PrecomputedTableManager.class);

    protected TreeSet precomputedTables = new TreeSet();
    protected Map types = new HashMap();
    protected Database database = null;
    protected Connection conn = null;
    protected static final String TABLE_INDEX = "precompute_index";
    protected static Map instances = new HashMap();

    /**
     * Create a PrecomputedTableManager for a given Connection.
     *
     * @param conn the underlying database connection
     * @throws SQLException if an error occurs in the underlying database
     */
    protected PrecomputedTableManager(Connection conn) throws SQLException {
        if (conn == null) {
            throw new NullPointerException("conn cannot be null");
        }

        synchroniseWithDatabase(conn);

        this.conn = conn;
    }

    /**
     * Create a PrecomputedTableManager for the given underlying database.
     *
     * @param database the underlying database
     * @throws SQLException if an error occurs in the underlying database
     */
    protected PrecomputedTableManager(Database database) throws SQLException {
        if (database == null) {
            throw new NullPointerException("database cannot be null");
        }

        Connection con = null;
        try {
            con = database.getConnection();
            con.setAutoCommit(false);
            synchroniseWithDatabase(con);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
            }
        }
        this.database = database;
    }

    /**
     * Gets a PrecomputedTableManager instance for the given underlying Connection.
     *
     * @param conn the underlying database connection
     * @return the PrecomputedTableManager for this database connection
     * @throws IllegalArgumentException if connection is invalid
     * @throws SQLException if an error occurs in the underlying database
     */
    public static PrecomputedTableManager getInstance(Connection conn) throws SQLException {
        synchronized (instances) {
            if (!(instances.containsKey(conn))) {
                instances.put(conn, new PrecomputedTableManager(conn));
            }
        }
        return (PrecomputedTableManager) instances.get(conn);
    }

    /**
     * Gets a PrecomputedTableManager instance for the given underlying database.
     *
     * @param database the underlying database
     * @return the PrecomputedTableManager for this database
     * @throws IllegalArgumentException if database is invalid
     * @throws SQLException if an error occurs in the underlying database
     */
    public static PrecomputedTableManager getInstance(Database database) throws SQLException {
        synchronized (instances) {
            if (!(instances.containsKey(database))) {
                instances.put(database, new PrecomputedTableManager(database));
            }
        }
        return (PrecomputedTableManager) instances.get(database);
    }

    /**
     * Add a precomputed table to the underlying database.
     *
     * @param pt the PrecomputedTable to add
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if pt is null
     */
    public void add(PrecomputedTable pt) throws SQLException {
        add(pt, null);
    }

    /**
     * Add a precomputed table to the underlying database.
     *
     * @param pt the PrecomputedTable to add
     * @param indexes the extra fields to index - a Collection of Strings. Each String can be a
     * comma-separated list of fields that will be indexed as a multi-column index. The field
     * names should be names of columns in the precomputed table - so they are the aliases
     * specified in the PrecomputedTable
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if pt is null
     */
    public void add(PrecomputedTable pt, Collection indexes) throws SQLException {
        if (pt == null) {
            throw new NullPointerException("PrecomputedTable cannot be null");
        }
        String queryString = pt.getOriginalSql();
        Map queryStrings = (Map) types.get(pt.getCategory());
        if (queryStrings == null) {
            queryStrings = new HashMap();
            types.put(pt.getCategory(), queryStrings);
        }
        if (!queryStrings.containsKey(queryString)) {
            addTableToDatabase(pt, indexes, true);
            precomputedTables.add(pt);
            queryStrings.put(queryString, pt);
        }
    }

    /**
     * Deletes every single precomputed table. Use this when you have made a change to the database.
     *
     * @throws SQLException if something goes wrong
     */
    public void dropEverything() throws SQLException {
        Iterator iter = precomputedTables.iterator();
        while (iter.hasNext()) {
            PrecomputedTable pt = (PrecomputedTable) iter.next();
            deleteTableFromDatabase(pt.getName());
            iter.remove();
        }
        types.clear();
    }

    /**
     * Deletes all precomputed tables that would be affected by changes in any table in a given list
     * of table names.
     *
     * @param tablesAltered a Set of table names that may have alterations
     * @throws SQLException if something goes wrong
     */
    public void dropAffected(Set tablesAltered) throws SQLException {
        Iterator iter = precomputedTables.iterator();
        while (iter.hasNext()) {
            PrecomputedTable pt = (PrecomputedTable) iter.next();
            Query q = pt.getQuery();
            boolean drop = false;
            Iterator fromIter = q.getFrom().iterator();
            while ((!drop) && fromIter.hasNext()) {
                Object table = fromIter.next();
                if (table instanceof Table) {
                    if (tablesAltered.contains(((Table) table).getName())) {
                        drop = true;
                    }
                }
            }
            if (drop) {
                deleteTableFromDatabase(pt.getName());
                iter.remove();
                String queryString = pt.getOriginalSql();
                Map queryStrings = (Map) types.get(pt.getCategory());
                queryStrings.remove(queryString);
            }
        }
    }

    /**
     * Delete a precomputed table from the underlying database.
     *
     * @param pt the PrecomputedTable to delete
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if pt is null
     * @throws IllegalArgumentException if pt is not a valid table
     */
    public void delete(PrecomputedTable pt) throws SQLException {
        if (pt == null) {
            throw new NullPointerException("PrecomputedTable cannot be null");
        }
        if (!precomputedTables.contains(pt)) {
            throw new IllegalArgumentException("Table is not valid: " + pt);
        }

        deleteTableFromDatabase(pt.getName());
        precomputedTables.remove(pt);
        String queryString = pt.getOriginalSql();
        Map queryStrings = (Map) types.get(pt.getCategory());
        queryStrings.remove(queryString);
    }

    /**
     * Get all the precomputed tables in the underlying database.
     *
     * @return a Set of PrecomputedTables present in the database
     */
    public Set getPrecomputedTables() {
        return precomputedTables;
    }

    /**
     * Add a PrecomputedTable to the database.
     *
     * @param pt the PrecomputedTable to add
     * @param indexes a Collection of Strings that are indexes to create
     * @param record whether to record this table in the index
     * @throws SQLException if an error occurs in the underlying database
     */
    public void addTableToDatabase(PrecomputedTable pt,
            Collection indexes, boolean record) throws SQLException {
        Connection con = null;
        try {
            con = (conn == null ? database.getConnection() : conn);
            if (indexes == null) {
                indexes = new LinkedHashSet();
            }

            // Create the table
            Statement stmt = con.createStatement();
            String sql = pt.getSQLString();
            BestQuery bq = QueryOptimiser.optimise(sql, null, this, con,
                    QueryOptimiserContext.DEFAULT);
            sql = "CREATE TABLE " + pt.getName() + " AS " + bq.getBestQueryString();
            LOG.info("Creating new precomputed table " + sql);
            stmt.execute(sql);

            String orderByField = pt.getOrderByField();
            if (orderByField != null) {
                LOG.info("Creating orderby_field index on precomputed table " + pt.getName());
                indexes.add(orderByField);
            } else {
                List orderBy = pt.getQuery().getOrderBy();
                if (!orderBy.isEmpty()) {
                    AbstractValue firstOrderBy = ((AbstractValue) orderBy.get(0));
                    SelectValue firstOrderByValue = ((SelectValue) pt.getValueMap()
                            .get(firstOrderBy));
                    if (firstOrderByValue != null) {
                        LOG.info("Creating index on precomputed table " + pt.getName());
                        indexes.add(firstOrderByValue.getAlias());
                    }
                }
            }
            indexes = canonicaliseIndexes(indexes);

            Iterator indexIter = indexes.iterator();
            while (indexIter.hasNext()) {
                String indexName = (String) indexIter.next();
                addIndex(pt.getName(), indexName, con, (!indexName.equals(orderByField))
                        && (indexName.indexOf(",") == -1));
            }

            LOG.info("ANALYSEing precomputed table " + pt.getName());
            con.createStatement().execute("ANALYSE " + pt.getName());

            // Create the entry in the index table
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO "
                                                           + TABLE_INDEX + " VALUES(?,?,?)");
            pstmt.setString(1, pt.getName());
            pstmt.setString(2, pt.getOriginalSql());
            pstmt.setString(3, pt.getCategory());
            pstmt.execute();
            if (!con.getAutoCommit()) {
                con.commit();
            }
            LOG.info("Finished creating precomputed table " + pt.getName());
        } finally {
            if ((con != null) && (conn == null)) {
                con.close();
            }
        }
    }

    /**
     * Takes a collection of index strings, and removes redundant entries. An index string is a
     * list of SQL column names, separated by ", ", where the leftmost column is the most
     * significant in the btree order.
     *
     * @param indexes the Collection of index strings
     * @return a new Set of index strings
     */
    protected static Set canonicaliseIndexes(Collection indexes) {
        Set retval = new LinkedHashSet();
        Set indexesCovered = new HashSet();
        Iterator indexIter = indexes.iterator();
        while (indexIter.hasNext()) {
            String index = (String) indexIter.next();
            if (!indexesCovered.contains(index)) {
                String tmp = index;
                while (tmp != null) {
                    indexesCovered.add(tmp);
                    retval.remove(tmp);
                    int pos = tmp.lastIndexOf(", ");
                    if (pos >= 0) {
                        tmp = tmp.substring(0, pos);
                    } else {
                        tmp = null;
                    }
                }
                retval.add(index);
            }
        }
        return retval;
    }

    /**
     * Delete a PrecomputedTable from the database.
     *
     * @param name the name of the PrecomputedTable to delete
     * @throws SQLException if an error occurs in the underlying database
     */
    public void deleteTableFromDatabase(String name) throws SQLException {
        OptimiserCache oc = OptimiserCache.getInstance(database);
        oc.flush();
        Connection con = null;
        try {
            con = (conn == null ? database.getConnection() : conn);
            // Drop the entry from the index table
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM "
                                                           + TABLE_INDEX + " WHERE name = ?");
            pstmt.setString(1, name);
            pstmt.execute();

            // Drop the table
            Statement stmt = con.createStatement();
            stmt.execute("DROP TABLE " + name);
            if (!con.getAutoCommit()) {
                con.commit();
            }
            LOG.info("Dropped precomputed table " + name);
        } finally {
            if ((con != null) && (conn == null)) {
                con.close();
            }
        }
    }

    /**
     * Adds an index to the given table on the given field.
     *
     * @param table the name of the table
     * @param field the name of the field
     * @param con a Connection to use
     * @param nulls whether an index should be created for null values
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void addIndex(String table, String field, Connection con,
            boolean nulls) throws SQLException {
        String sql = "CREATE INDEX index" + table + "_field_" + field.replace(',', '_')
            .replace(' ', '_').replace('(', '_').replace(')', '_') + " ON "
            + table + " (" + field + ")";
        try {
            Statement stmt = con.createStatement();
            stmt.execute(sql);
            if (!con.getAutoCommit()) {
                con.commit();
            }
        } catch (SQLException e) {
            SQLException f = new SQLException(e.getMessage() + " when executing " + sql);
            f.setNextException(e);
            throw f;
        }
        if (nulls) {
            sql = "CREATE INDEX index" + table + "_field_" + field.replace(',', '_')
                .replace(' ', '_').replace('(', '_').replace(')', '_') + "_nulls"
                + " ON " + table + " ((" + field + " IS NULL))";
            try {
                Statement stmt = con.createStatement();
                stmt.execute(sql);
                if (!con.getAutoCommit()) {
                    con.commit();
                }
            } catch (SQLException e) {
                SQLException f = new SQLException(e.getMessage() + " when executing " + sql);
                f.setNextException(e);
                throw f;
            }
        }
    }

    /**
     * Synchronise with the underlying database
     *
     * @param con a Connection to the database we are synchronising with
     * @throws SQLException if there is a problem in the underlying database
     */
    protected void synchroniseWithDatabase(Connection con) throws SQLException {
        // Create index table if necessary
        if (!DatabaseUtil.tableExists(con, TABLE_INDEX)) {
            setupDatabase(con);
        }
        long start = System.currentTimeMillis();
        Statement stmt = con.createStatement();
        ResultSet res = stmt.executeQuery("SELECT name, statement, category FROM " + TABLE_INDEX);

        int failedCount = 0;
        while (res.next()) {
            String tableName = res.getString(1);
            String queryString = res.getString(2);
            String category = res.getString(3);
            try {
                PrecomputedTable pt = new PrecomputedTable(new Query(queryString, true),
                            queryString, tableName, category, con);
                precomputedTables.add(pt);
                Map queryStrings = (Map) types.get(category);
                if (queryStrings == null) {
                    queryStrings = new HashMap();
                    types.put(pt.getCategory(), queryStrings);
                }
                queryStrings.put(queryString, pt);
            } catch (IllegalArgumentException e) {
                // This would be a poor query string in the TABLE_INDEX
                failedCount++;
            }
        }
        LOG.info("Loaded " + precomputedTables.size() + " precomputed table descriptions (plus "
                + failedCount + " failed) in " + (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Sets up the database for storing precomputed tables
     *
     * @param con the Connection to use
     * @throws SQLException if there is a problem in the underlying database
     */
    protected void setupDatabase(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE " + TABLE_INDEX
                + "(name varchar(255), statement BYTEA, category varchar(255))");
        if (!con.getAutoCommit()) {
            con.commit();
        }
    }

    /**
     * Returns a PrecomputedTable object if one exists in the manager with the given category and
     * original SQL string.
     *
     * @param category a String
     * @param sql the original SQL string used to create the PrecomputedTable
     * @return a PrecomputedTable or null
     */
    public PrecomputedTable lookupSql(String category, String sql) {
        Map queryStrings = (Map) types.get(category);
        if (queryStrings != null) {
            return (PrecomputedTable) queryStrings.get(sql);
        }
        return null;
    }

    /**
     * Returns a Map from original SQL to PrecomputedTable for a given category in the manager.
     *
     * @param category a String
     * @return a Map
     */
    public Map lookupCategory(String category) {
        Map queryStrings = (Map) types.get(category);
        if (queryStrings == null) {
            queryStrings = new HashMap();
            types.put(category, queryStrings);
        }
        return queryStrings;
    }
}
