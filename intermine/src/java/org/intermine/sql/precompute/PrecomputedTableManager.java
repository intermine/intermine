package org.flymine.sql.precompute;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.sql.*;
import org.flymine.util.DatabaseUtil;
import org.flymine.sql.Database;
import org.flymine.sql.query.Query;
import org.flymine.sql.query.AbstractValue;
import org.flymine.sql.query.SelectValue;

/**
 * Manages all the Precomputed tables in a given database.
 *
 * @author Andrew Varley
 */
public class PrecomputedTableManager
{

    protected Set precomputedTables = new HashSet();
    protected Database database = null;
    protected static final String TABLE_INDEX = "precompute_index";
    protected static Map instances = new HashMap();

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
            con = database.getConnection();
            con.setAutoCommit(false);

            // Create the table
            Statement stmt = con.createStatement();
            stmt.execute(pt.getSQLString());

            List orderBy = pt.getQuery().getOrderBy();
            if (!orderBy.isEmpty()) {
                AbstractValue firstOrderBy = ((AbstractValue) orderBy.get(0));
                SelectValue firstOrderByValue = ((SelectValue) pt.getValueMap().get(firstOrderBy));
                if (firstOrderByValue != null) {
                    addIndex(pt.getName(), firstOrderByValue.getAlias(), con);
                }
            }

            // Create the entry in the index table
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO "
                                                           + TABLE_INDEX + " VALUES(?,?)");
            pstmt.setString(1, pt.getName());
            pstmt.setString(2, pt.getQuery().getSQLString());
            pstmt.execute();

            con.commit();
        } finally {
            if (con != null) {
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
        Connection con = null;
        try {
            con = database.getConnection();
            con.setAutoCommit(false);
            // Drop the entry from the index table
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM "
                                                           + TABLE_INDEX + " WHERE name = ?");
            pstmt.setString(1, name);
            pstmt.execute();

            // Drop the table
            Statement stmt = con.createStatement();
            stmt.execute("DROP TABLE " + name);

            con.commit();
        } finally {
            if (con != null) {
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
        con.commit();
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
                precomputedTables.add(new PrecomputedTable(new Query(queryString), tableName));
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
        con.commit();
    }
}
