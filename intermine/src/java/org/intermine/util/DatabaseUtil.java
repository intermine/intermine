package org.flymine.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Collection of commonly used Database utilities
 *
 * @author Andrew Varley
 */
public class DatabaseUtil
{
    private DatabaseUtil() {
    }

    /**
     * Tests if a table exists in the database
     *
     * @param con a connection to a database
     * @param tableName the name of a table to test for
     * @return true if the table exists, false otherwise
     * @throws SQLException if an error occurs in the underlying database
     * @throws NullPointerException if tableName is null
     */

    public static boolean tableExists(Connection con, String tableName) throws SQLException {
        if (tableName == null) {
            throw new NullPointerException("tableName cannot be null");
        }

        ResultSet res = con.getMetaData().getTables(null, null, tableName, null);

        while (res.next()) {
            if (res.getString(3).equals(tableName)) {
                return true;
            }
        }
        return false;

    }
}
