package org.intermine.install.database;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Class for shared methods for handling the Intermine databases.
 * This class provides methods to assemble JDBC URLs and to check a
 * database exists and can be connected to.
 */
public class DatabaseUtil
{
    /**
     * A regular expression to recognise an error from Postgres in English
     * indicating a database already exists.
     */
    public static final Pattern ALREADY_EXISTS_PATTERN;
    
    /**
     * A regular expression to recognise an error from Postgres in English
     * indicating a database does not exist.
     */
    public static final Pattern NON_EXISTANT_PATTERN;
    
    /**
     * A regular expression to recognise an error from Postgres in English
     * indicating the credentials given do not work for the database.
     */
    public static final Pattern PASSWORD_PATTERN;

    
    /**
     * Logger.
     */
    private static Log logger = LogFactory.getLog(DatabaseUtil.class);
    
    /**
     * Static initialiser to create the public patterns for recognising Postgres errors.
     * A resource bundle is used to account for other languages.
     */
    static
    {
        ResourceBundle postgresPatterns = null;
        try {
            postgresPatterns = ResourceBundle.getBundle("org/intermine/install/postgres_messages");
        } catch (MissingResourceException e) {
            logger.fatal("Cannot load Postgres message bundle \"postgres_messages\" for locale "
                         + Locale.getDefault().getDisplayName());
        }
        
        String existsPatternString = "ERROR: database \"[\\w-]+\" already exists";
        String notExistsPatternString = "FATAL: database \"[\\w-]+\" does not exist";
        String passwordWrongString = "FATAL: password authentication failed for user \"[\\w-]+\"";
        
        if (postgresPatterns != null) {
            try {
                existsPatternString = postgresPatterns.getString("database.exists.pattern");
                notExistsPatternString = postgresPatterns.getString("database.not.exists.pattern");
                passwordWrongString = postgresPatterns.getString("password.failed.pattern");
                
            } catch (MissingResourceException e) {
                logger.fatal("Postgres message bundle \"postgres_messages\" for locale "
                             + Locale.getDefault().getDisplayName() + " is missing message "
                             + e.getKey());
            }
        }
        
        ALREADY_EXISTS_PATTERN = Pattern.compile(existsPatternString);
        NON_EXISTANT_PATTERN = Pattern.compile(notExistsPatternString);
        PASSWORD_PATTERN = Pattern.compile(passwordWrongString);
    }
    
    
    /**
     * Check that the database given is accessible on the server with the given credentials.
     * 
     * @param server The name of the database server.
     * @param databaseName The name of the database.
     * @param userName The database user name.
     * @param password The user's password.
     * 
     * @return <code>true</code> if the database is accessible, or <code>false</code> if
     * it does not exist. Any other problem results in a <code>DatabaseConnectionException</code>.
     * 
     * @throws PasswordException if the connection fails due to incorrect credentials.
     * 
     * @throws DatabaseConnectionException if the connection fails for any other reason.
     */
    public static boolean checkDatabaseExists(String server, String databaseName,
                                              String userName, String password)
    throws PasswordException, DatabaseConnectionException {
        
        String url = formJdbcUrl(server, databaseName);
        
        try {
            Connection conn = DriverManager.getConnection(url, userName, password);
            try {
                conn.createStatement().execute("select 1");
                
                return true;
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            
            String message = e.getMessage();
            if (NON_EXISTANT_PATTERN.matcher(message).matches()) {
                // Nothing - simply return false.
            } else if (PASSWORD_PATTERN.matcher(message).matches()) {
                throw new PasswordException(databaseName,
                        "Password authentication failed for user '" + userName + "'.");
            } else {
                throw new DatabaseConnectionException(e);
            }
        }
        
        return false;
    }
    
    /**
     * Create a Postgres JDBC string for the given database.
     * 
     * @param server The database server name.
     * @param databaseName The name of the database.
     * 
     * @return The JDBC URL for the database.
     * 
     * @throws DatabaseConnectionException if the Postgres JDBC drivers are not available.
     */
    public static String formJdbcUrl(String server, String databaseName)
    throws DatabaseConnectionException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.fatal("Postgres JDBC driver is not on the class path.");
            throw new DatabaseConnectionException(
                    "Postgres JDBC driver is not on the class path.", e);
        }
        
        StringBuilder url = new StringBuilder();
        url.append("jdbc:postgresql://").append(server).append('/');
        url.append(databaseName);
        
        return url.toString();
    }
}
