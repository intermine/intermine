package org.flymine.testing;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import junit.framework.*;

import org.flymine.sql.Database;

/**
 * TestCase for doing unit tests using an SQL database.
 *
 * @author Andrew Varley
 */
public abstract class DatabaseTestCase extends TestCase
{

    /**
     * Constructor
     *
     * @param name name of the Test
     */
    public DatabaseTestCase(String name) {
        super(name);
    }

    /**
     * Get the Database to be used for performing the tests
     *
     * @return the database to use
     * @throws Exception if error occurs
     */
    protected abstract Database getDatabase() throws Exception;

    /**
     * Asserts that two ResultSets are equal
     *
     * @param rs1 the first ResultSet
     * @param rs2 the second ResultSet
     */
    protected void assertEquals(ResultSet rs1, ResultSet rs2) {
        assertEquals(null, rs1, rs2);
    }

    /**
     * Asserts that two ResultSets are equal
     *
     * @param msg message to give on failure
     * @param rs1 the first ResultSet
     * @param rs2 the second ResultSet
     */
    protected void assertEquals(String msg, ResultSet rs1, ResultSet rs2) {

        try {
            // Number of columns should be the same
            ResultSetMetaData rsmd1 = rs1.getMetaData();
            ResultSetMetaData rsmd2 = rs2.getMetaData();
            assertEquals(msg, rsmd1.getColumnCount(), rsmd2.getColumnCount());

            // Names and types of columns should be the same
            for (int i = 1; i <= rsmd1.getColumnCount(); i++) {
                assertEquals(msg, rsmd1.getColumnName(i), rsmd2.getColumnName(i));
                assertEquals(msg, rsmd1.getColumnType(i), rsmd2.getColumnType(i));
            }

            // Contents should be the same
            while (rs1.next() && rs2.next()) {
                for (int i = 1; i <= rsmd1.getColumnCount(); i++) {
                    assertEquals(msg, rs1.getObject(i), rs2.getObject(i));
                }
            }
        } catch (SQLException e) {
            fail(msg + ": " + e.getMessage());
        }
    }

}
