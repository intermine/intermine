package org.intermine.modelproduction;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.sql.Database;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;

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
    public static final String MODEL = "model";

    /**
     * Name of the key under which to store the serialized version of the key definitions
     */
    public static final String KEY_DEFINITIONS = "keyDefs";

    /**
     * The name of the key to use to store the class_keys.properties file.
     */
    public static final String CLASS_KEYS = "class_keys";

    /**
     * Name of the key under which to store the serialized version of the class descriptions
     */
    //public static final String CLASS_DESCRIPTIONS = "classDescs";

    /**
     * Store a (key, value) pair in the metadata table of the database
     * @param database the database
     * @param key the key
     * @param value the value
     * @throws SQLException if an error occurs
     */
    public static void store(Database database, String key, String value) throws SQLException {
        Connection connection = database.getConnection();
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(true);
            connection.createStatement().execute("DELETE FROM " + METADATA_TABLE + " where key = '"
                                                 + key + "'");
            connection.createStatement().execute("INSERT INTO " + METADATA_TABLE + " (key, value) "
                                                 + "VALUES('" + key + "', '"
                                                 + StringUtil.duplicateQuotes(value) + "')");
        } finally {
            connection.setAutoCommit(autoCommit);
            connection.close();
        }
    }

    /**
     * Retrieve the value for a given key from the metadata table of the database
     * @param database the database
     * @param key the key
     * @return the value
     * @throws SQLException if an error occurs
     */
    public static String retrieve(Database database, String key) throws SQLException {
        String value = null;
        Connection connection = database.getConnection();
        try {
            String sql = "SELECT value FROM " + METADATA_TABLE + " WHERE key='" + key + "'";
            ResultSet rs = connection.createStatement().executeQuery(sql);
            if (!rs.next()) {
                throw new RuntimeException("No value found in database for key " + key);
            }
            value = rs.getString(1);
        } finally {
            connection.close();
        }
        return value;
    }

    /**
     * Load a named model from the classpath
     * @param name the model name
     * @return the model
     */
    public static Model loadModel(String name) {
        String filename = getFilename(MODEL, name);
        InputStream is = Model.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new IllegalArgumentException("Model definition file '" + filename
                                               + "' cannot be found");
        }
        Model model = null;
        try {
            model = new InterMineModelParser().process(new InputStreamReader(is));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing model definition file '" + filename + "'", e);
        }
        return model;
    }

    /**
     * Save a model, in serialized form, to the specified directory
     * @param model the model
     * @param destDir the destination directory
     * @throws IOException if an error occurs
     */
    public static void saveModel(Model model, File destDir) throws IOException {
        write(model.toString(), new File(destDir, getFilename(MODEL, model.getName())));
    }

    /**
     * Load the key definitions file for the named model from the classpath
     * @param modelName the model name
     * @return the key definitions
     */
    public static Properties loadKeyDefinitions(String modelName) {
        return PropertiesUtil.loadProperties(getFilename(KEY_DEFINITIONS, modelName));
    }

    /**
     * Load the class key / key field definitions.
     * @param classKeys the prefix of the class_keys file.
     * @return the class key definitions
     */
    public static Properties loadClassKeyDefinitions(String classKeys) {
        return PropertiesUtil.loadProperties(getFilename(CLASS_KEYS, null));
    }
    
    /**
     * Save the key definitions, in serialized form, to the specified directory
     * @param properties the key definitions
     * @param destDir the destination directory
     * @param modelName the name of the associated model, used the generate the filename
     * @throws IOException if an error occurs
     */
    public static void saveKeyDefinitions(String properties, File destDir, String modelName)
        throws IOException {
        write(properties, new File(destDir, getFilename(KEY_DEFINITIONS, modelName)));
    }

    /**
     * Save the class keys, in serialized form, to the specified directory
     * @param properties the class keys
     * @param destDir the destination
     * @throws IOException if an error occurs
     */
    public static void saveClassKeys(String properties, File destDir) throws IOException {
        write(properties, new File(destDir, getFilename(CLASS_KEYS, null)));
    }
    
    /**
     * Load the class descriptions file for the named model from the classpath
     * @param modelName the model name
     * @return the class descriptions
     *
    public static Properties loadClassDescriptions(String modelName) {
        return PropertiesUtil.loadProperties(getFilename(CLASS_DESCRIPTIONS, modelName));
    }*/

    /**
     * Save the class descriptions, in serialized form, to the specified directory
     * @param properties the class descriptions
     * @param destDir the destination directory
     * @param modelName the name of the associated model, used the generate the filename
     * @throws IOException if an error occurs
     *
    public static void saveClassDescriptions(String properties, File destDir, String modelName)
        throws IOException {
        write(properties, new File(destDir, getFilename(CLASS_DESCRIPTIONS, modelName)));
    }*/

    /**
     * Given  a key and model name, return filename for reading/writing.
     * @param key key name
     * @param modelName the name of the model
     * @return name of file
     */
    public static String getFilename(String key, String modelName) {
        String filename;
        if (modelName == null) {
            filename = key;
        } else {
            filename = modelName + "_" + key;
        }
        if (MODEL.equals(key)) {
            return filename + ".xml";
        } else if (KEY_DEFINITIONS.equals(key)
                   || CLASS_KEYS.equals(key)
                   /* || CLASS_DESCRIPTIONS.equals(key)*/) {
            return filename + ".properties";
        }
        throw new IllegalArgumentException("Unrecognised key '" + key + "'");
    }

    private static void write(String string, File file) throws IOException {
        if (file.exists()
            && IOUtils.contentEquals(new FileReader(file), new StringReader(string))) {
            System.err .println("Not writing \"" + file.getName()
                                + "\" as version in database is identical to local copy");
            return;
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(string);
        writer.close();
    }
}
