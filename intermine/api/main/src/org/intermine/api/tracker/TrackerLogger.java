package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Runnable object providing insertion into the database. TrackerLogger is created
 * for a specific connection and table.
 * @author dbutano
 *
 */
public class TrackerLogger implements Runnable
{
    private static final Logger LOG = Logger.getLogger(TrackerLogger.class);
    private Connection connection;
    private String tableName;
    private String[] colNames;
    private Object[] values;

    /**
     * Construct a TrackerLogger for a specific connection and table
     * @param connection the connection to the database
     * @param tableName the name of the table where the value will be saved
     * @param colNames the names of the columns
     * @param values the values to be saved
     */
    public TrackerLogger(Connection connection, String tableName, String[] colNames,
                         Object[] values) {
        this.connection = connection;
        this.tableName = tableName;
        this.colNames = colNames;
        this.values = values;
    }

    public void run() {
        Statement stm = null;
        StringBuffer colNamesBuffer = new StringBuffer();
        StringBuffer valuesBuffer = new StringBuffer();
        for (int index = 0; index < colNames.length; index++) {
            colNamesBuffer = colNamesBuffer.append(colNames[index] + ",");
            valuesBuffer = valuesBuffer.append("'" + values[index] + "',");
        }
        colNamesBuffer = colNamesBuffer.deleteCharAt(colNamesBuffer.length() - 1);
        valuesBuffer = valuesBuffer.deleteCharAt(valuesBuffer.length() - 1);
        String sql = "";
        try {
            stm = connection.createStatement();
            sql = "INSERT INTO " + tableName
                        + " (" + colNamesBuffer + ") "
                        + " VALUES ( " + valuesBuffer + ")";
            stm.executeUpdate(sql);
        } catch (SQLException sqe) {
            LOG.error("Problem executing the statement: " + sql, sqe);
            LOG.error("The template execution has not been tracked.");
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    LOG.error("Problem closing  resources in TrackerLogger()", e);
                }
            }
        }
    }

}
