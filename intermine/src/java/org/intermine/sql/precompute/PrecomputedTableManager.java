package org.flymine.sql.precompute;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.sql.*;
import org.flymine.util.DatabaseUtil;
import org.flymine.sql.Database;
import org.flymine.sql.query.Query;

/**
 * Manages all the Precomputed tables in a given database.
 *
 * @author Andrew Varley
 */
public class PrecomputedTableManager
{

    protected Map precomputedTables = new HashMap();
    protected Database database = null;
    protected static final String TABLE_INDEX = "precompute_index";
    protected static Map instances = new HashMap();
    Date lastChecked;

    /**
     * Create a PrecomputedTableManager for the given underlying database
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
     * Gets a PrecomputedTableManager instance for the given underlying database
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
     * Add a precomputed table to the underlying database
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
        precomputedTables.put(pt.getName(), pt);
    }


    /**
     * Delete a precomputed table from the underlying database
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
        delete(pt.getName());
    }

    /**
     * Delete a precomputed table from the underlying database
     *
     * @param name the name of the table to delete
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if name does not refer to a valid table
     */
    public void delete(String name) throws SQLException {
        if (name == null) {
            throw new NullPointerException("Table name cannot be null");
        }
        if (!(precomputedTables.containsKey(name))) {
            throw new IllegalArgumentException("Table name is not valid: " + name);
        }
        deleteTableFromDatabase(name);
        precomputedTables.remove(name);
    }

    /**
     * Get all the precomputed tables in the underlying database
     *
     * @return a Collection of PrecomputedTables present in the database
     */
    public Collection getPrecomputedTables() {
        return precomputedTables.values();
    }

    /**
     * Add a PrecomputedTable to the database
     *
     * @param pt the PrecomputedTable to add
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void addTableToDatabase(PrecomputedTable pt) throws SQLException {
        Connection con = null;
        try {
            con = database.getConnection();

            // Create the table
            Statement stmt = con.createStatement();
            stmt.execute(pt.getSQLString());

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
     * Delete a PrecomputedTable from the database
     *
     * @param name the name of the PrecomputedTable to delete
     * @throws SQLException if an error occurs in the underlying database
     */
    protected void deleteTableFromDatabase(String name) throws SQLException {
        Connection con = null;
        try {
            con = database.getConnection();
            // Drop the table
            Statement stmt = con.createStatement();
            stmt.execute("DROP TABLE " + name);

            // Drop the entry from the index table
            PreparedStatement pstmt = con.prepareStatement("DELETE FROM "
                                                           + TABLE_INDEX + " WHERE name = ?");
            pstmt.setString(1, name);
            pstmt.execute();

            con.commit();
        } finally {
            if (con != null) {
                con.close();
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
        Statement stmt = con.createStatement();
        ResultSet res = stmt.executeQuery("SELECT * FROM " + TABLE_INDEX);

        while (res.next()) {
            String tableName = res.getString(1);
            String queryString = res.getString(2);
            try {
                precomputedTables.put(tableName,
                                      new PrecomputedTable(new Query(queryString), tableName));
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
        stmt.execute("CREATE TABLE precompute_index(name varchar(255), statement BYTEA)");
        con.commit();
    }

}
