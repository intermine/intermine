package org.intermine.sql.precompute;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.sql.*;
import org.intermine.util.DatabaseUtil;
import org.intermine.sql.Database;
import org.intermine.sql.query.Query;
import org.intermine.sql.query.AbstractValue;
import org.intermine.sql.query.SelectValue;

import org.apache.log4j.Logger;

/**
 * Manages all the Precomputed tables in a given database.
 *
 * @author Andrew Varley
 */
public class PrecomputedTableManager
{
    private static final Logger LOG = Logger.getLogger(PrecomputedTableManager.class);

    protected Set precomputedTables = new HashSet();
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
        if (pt == null) {
            throw new NullPointerException("PrecomputedTable cannot be null");
        }
        addTableToDatabase(pt);
        precomputedTables.add(pt);
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
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void addTableToDatabase(PrecomputedTable pt) throws SQLException {
        Connection con = null;
        try {
            con = (conn == null ? database.getConnection() : conn);

            // Create the table
            Statement stmt = con.createStatement();
            LOG.info("Creating new precomputed table " + pt.getName());
            stmt.execute(pt.getSQLString());

            String orderByField = pt.getOrderByField();
            if (orderByField != null) {
                LOG.info("Creating orderby_field index on precomputed table " + pt.getName());
                addIndex(pt.getName(), orderByField, con);
            } else {
                List orderBy = pt.getQuery().getOrderBy();
                if (!orderBy.isEmpty()) {
                    AbstractValue firstOrderBy = ((AbstractValue) orderBy.get(0));
                    SelectValue firstOrderByValue = ((SelectValue) pt.getValueMap()
                            .get(firstOrderBy));
                    if (firstOrderByValue != null) {
                        LOG.info("Creating index on precomputed table " + pt.getName());
                        addIndex(pt.getName(), firstOrderByValue.getAlias(), con);
                    }
                }
            }

            LOG.info("ANALYSEing precomputed table " + pt.getName());
            con.createStatement().execute("ANALYSE " + pt.getName());

            // Create the entry in the index table
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO "
                                                           + TABLE_INDEX + " VALUES(?,?)");
            pstmt.setString(1, pt.getName());
            pstmt.setString(2, pt.getQuery().getSQLString());
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
     * Delete a PrecomputedTable from the database.
     *
     * @param name the name of the PrecomputedTable to delete
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void deleteTableFromDatabase(String name) throws SQLException {
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
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void addIndex(String table, String field, Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE INDEX index" + table + "_field_" + field + " ON " + table + " ("
                + field + ")");
        if (!con.getAutoCommit()) {
            con.commit();
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
        Statement stmt = con.createStatement();
        ResultSet res = stmt.executeQuery("SELECT * FROM " + TABLE_INDEX);

        while (res.next()) {
            String tableName = res.getString(1);
            String queryString = res.getString(2);
            try {
                precomputedTables.add(new PrecomputedTable(new Query(queryString), tableName, con));
            } catch (IllegalArgumentException e) {
                // This would be a poor query string in the TABLE_INDEX
            }
        }
    }

    /**
     * Sets up the database for storing precomputed tables
     *
     * @param con the Connection to use
     * @throws SQLException if there is a problem in the underlying database
     */
    protected void setupDatabase(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        stmt.execute("CREATE TABLE " + TABLE_INDEX + "(name varchar(255), statement BYTEA)");
        if (!con.getAutoCommit()) {
            con.commit();
        }
    }
}
