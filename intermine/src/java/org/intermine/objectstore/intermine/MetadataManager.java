package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.intermine.sql.Database;

/**
 * Class to handle persistence of an intermine objectstore's metadata to the objectstore's database
 * @author Mark Woodbridge
 */
public class MetadataManager
{
    /**
     * Name of the metadata table (created by TorqueModelOutput)
     */
    public static final String METADATA_TABLE = "intermine_metadata";
    /**
     * Name of the key under which to store the serialized version of the model
     */
    public static final String MODEL_KEY = "model";

    /**
     * Store the model in the database. This is used by by the InsertModelTask
     * @param model the serialized version of the model
     * @param database the database in which to save the model
     * @throws SQLException if an error occurs
     */
    public static void storeModel(String model, Database database) throws SQLException {
        Connection connection = database.getConnection();
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(true);
            connection.createStatement().execute("INSERT INTO " + METADATA_TABLE + " (key, value) "
                                                 + "VALUES('" + MODEL_KEY + "', '" + model + "')");
        } finally {
            connection.setAutoCommit(autoCommit);
            connection.close();
        }
    }

    /**
     * Retrieve the model from the database. This is used by intermine objectstores.
     * @param database the database from which to retrieve the model
     * @return the serialized version of the model
     * @throws SQLException if an error occurs
     */
    public static String retrieveModel(Database database) throws SQLException {
        String model = null;
        Connection connection = database.getConnection();
        try {
            String sql = "SELECT value FROM " + METADATA_TABLE + " WHERE key='" + MODEL_KEY + "'";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            if (!rs.next()) {
                throw new RuntimeException("No model found in database");
            }
            model = rs.getString(1);
        } finally {
            connection.close();
        }
        return model;
    }
}