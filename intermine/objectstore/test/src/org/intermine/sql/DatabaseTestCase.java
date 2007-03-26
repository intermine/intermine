package org.intermine.sql;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import junit.framework.*;

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

        int row = 0;
        try {
            // Number of columns should be the same
            ResultSetMetaData rsmd1 = rs1.getMetaData();
            ResultSetMetaData rsmd2 = rs2.getMetaData();
            assertEquals(msg + "(difference in number of columns)",
                    rsmd1.getColumnCount(), rsmd2.getColumnCount());

            // Names and types of columns should be the same
            for (int i = 1; i <= rsmd1.getColumnCount(); i++) {
                try {
                    assertEquals(msg + "(difference in column names)",
                            rsmd1.getColumnLabel(i), rsmd2.getColumnLabel(i));
                } catch (com.mockobjects.util.NotImplementedException e) {
                    assertEquals(msg + "(difference in column names)",
                            rsmd1.getColumnName(i), rsmd2.getColumnName(i));
                }
                assertEquals(msg + "(difference in column types)",
                        rsmd1.getColumnType(i), rsmd2.getColumnType(i));
            }

            // Contents should be the same
            while (rs1.next() && rs2.next()) {
                row++;
                for (int i = 1; i <= rsmd1.getColumnCount(); i++) {
                    assertEquals(msg + "(difference in row " + row + ", column " + i + ")",
                                 rs1.getObject(i), rs2.getObject(i));
                }
            }
        } catch (SQLException e) {
            fail(msg + ": " + e.getMessage());
        }
    }

}
