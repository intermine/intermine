package org.intermine.install.swing;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.install.properties.InterminePropertyKeys.ITEMS_ENCODING;
import static org.intermine.install.properties.InterminePropertyKeys.ITEMS_NAME;
import static org.intermine.install.properties.InterminePropertyKeys.ITEMS_PASSWORD;
import static org.intermine.install.properties.InterminePropertyKeys.ITEMS_SERVER;
import static org.intermine.install.properties.InterminePropertyKeys.ITEMS_USER_NAME;
import static org.intermine.install.properties.InterminePropertyKeys.PRODUCTION_ENCODING;
import static org.intermine.install.properties.InterminePropertyKeys.PRODUCTION_NAME;
import static org.intermine.install.properties.InterminePropertyKeys.PRODUCTION_PASSWORD;
import static org.intermine.install.properties.InterminePropertyKeys.PRODUCTION_SERVER;
import static org.intermine.install.properties.InterminePropertyKeys.PRODUCTION_USER_NAME;
import static org.intermine.install.properties.InterminePropertyKeys.PROFILE_ENCODING;
import static org.intermine.install.properties.InterminePropertyKeys.PROFILE_NAME;
import static org.intermine.install.properties.InterminePropertyKeys.PROFILE_PASSWORD;
import static org.intermine.install.properties.InterminePropertyKeys.PROFILE_SERVER;
import static org.intermine.install.properties.InterminePropertyKeys.PROFILE_USER_NAME;

import java.awt.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.intermine.common.swing.Messages;
import org.intermine.common.swing.ProgressDialog;
import org.intermine.install.database.DatabaseConnectionException;
import org.intermine.install.database.DatabaseCreationException;
import org.intermine.install.database.DatabaseUtil;
import org.intermine.install.database.PasswordException;


/**
 * Swing worker for creating the Intermine Postgres databases.
 */
class CreateDatabaseWorker extends SwingWorker<Boolean, String>
{
    /**
     * Name for the completed property change.
     */
    static final String COMPLETE = "completed";
    
    /**
     * Logger.
     */
    private Log logger = LogFactory.getLog(getClass());
    
    /**
     * The parent window that launched this worker.
     */
    private Window launcher;
    
    /**
     * The mine creation properties.
     */
    private Properties properties;
    
    /**
     * A progress monitor dialog for watching this worker's progress.
     */
    private ProgressDialog monitor;
    
    /**
     * The error raised if the database creation fails.
     */
    private Exception creationException;
    
    /**
     * Set of strings identifying the database that have been created or
     * checked.
     */
    private Set<String> doneDatabases = Collections.synchronizedSet(new HashSet<String>());
    
    
    /**
     * Initialise with a launching parent window and mine creation properties.
     * 
     * @param parent The parent Window.
     * @param props The mine properties.
     */
    CreateDatabaseWorker(Window parent, Properties props) {
        launcher = parent;
        properties = props;
        monitor = new ProgressDialog(parent, true, true);
        monitor.setTitle(Messages.getMessage("database.create.title"));
        monitor.setTaskProgressIndeterminate(true);
        monitor.setTask(this, true);
        monitor.setSize(400, monitor.getHeight());
        monitor.resetOverallProgress(0, 3);
    }

    /**
     * Get the exception raised if database creation or connection fails.
     * 
     * @return The exception from the failure, or <code>null</code> if there
     * was no failure.
     */
    public Exception getCreationException() {
        return creationException;
    }

    /**
     * Called in the Swing event thread to update the progress dialog according
     * to the databases checked or created.
     * 
     * @param chunks The databases completed since the last call to this method.
     * 
     * @see #publish
     */
    @Override
    protected void process(List<String> chunks) {
        
        doneDatabases.addAll(chunks);
        
        int progress = Math.min(doneDatabases.size(), 3);
        
        monitor.setInformationText(Messages.getMessage("database.create.progress." + progress));
        monitor.setOverallProgress(progress);
        
        if (progress <= 2) {
            if (!monitor.isVisible()) {
                monitor.positionOver(launcher);
                monitor.setVisible(true);
                monitor.toFront();
            }
        }
    }

    /**
     * Execute the creation or checks of the databases. Calls <code>process</code>
     * as each database is completed.
     * 
     * @return <code>true</code> if all goes well, <code>false</code> if there is
     * a failure.
     * 
     * @see #process
     */
    @Override
    protected Boolean doInBackground() {
        
        doneDatabases.clear();
        
        publish();
        try {
            if (isCancelled()) {
                return Boolean.FALSE;
            }
            
            String server = properties.getProperty(PRODUCTION_SERVER);
            String dbName = properties.getProperty(PRODUCTION_NAME);
            String userName = properties.getProperty(PRODUCTION_USER_NAME);
            String password = properties.getProperty(PRODUCTION_PASSWORD);
            String encoding = properties.getProperty(PRODUCTION_ENCODING);
            if (!DatabaseUtil.checkDatabaseExists(server, dbName, userName, password)) {
                logger.info("Creating production database " + dbName + " on " + server);
                createDatabase(server, dbName, userName, password, encoding);
            }
            if (isCancelled()) {
                return Boolean.FALSE;
            }
            publish("production");
            
            server = properties.getProperty(ITEMS_SERVER);
            dbName = properties.getProperty(ITEMS_NAME);
            userName = properties.getProperty(ITEMS_USER_NAME);
            password = properties.getProperty(ITEMS_PASSWORD);
            encoding = properties.getProperty(ITEMS_ENCODING);
            if (!DatabaseUtil.checkDatabaseExists(server, dbName, userName, password)) {
                logger.info("Creating common target items database " + dbName + " on " + server);
                createDatabase(server, dbName, userName, password, encoding);
            }
            if (isCancelled()) {
                return Boolean.FALSE;
            }
            publish("items");
            
            server = properties.getProperty(PROFILE_SERVER);
            dbName = properties.getProperty(PROFILE_NAME);
            userName = properties.getProperty(PROFILE_USER_NAME);
            password = properties.getProperty(PROFILE_PASSWORD);
            encoding = properties.getProperty(PROFILE_ENCODING);
            if (!DatabaseUtil.checkDatabaseExists(server, dbName, userName, password)) {
                logger.info("Creating user profiles database " + dbName + " on " + server);
                createDatabase(server, dbName, userName, password, encoding);
            }
            if (!isCancelled()) {
                publish("profiles");
            }
            
            return Boolean.TRUE;
        } catch (InterruptedException e) {
            // Nothing, just end.
        } catch (IOException e) {
            creationException = e;
        } catch (DatabaseConnectionException e) {
            creationException = e;
        }
        return Boolean.FALSE;
    }

    /**
     * Called in the Swing event when the worker is finished. Fires the
     * "complete" property change event.
     */
    @Override
    protected void done() {
        monitor.setVisible(false);
        firePropertyChange(COMPLETE, false, true);
    }
    
    /**
     * Create a database.
     * 
     * @param server The database server.
     * @param databaseName The database name.
     * @param userName The database user name.
     * @param password The user password.
     * @param encoding The encoding for the database.
     * 
     * @throws IOException if there is a problem when executed as an external
     * <code>createdb</code> process ({@link #createDatabaseWithCreatedb}).
     *  
     * @throws InterruptedException if the thread is interrupted while running
     * as an external process ({@link #createDatabaseWithCreatedb}).
     * 
     * @throws DatabaseConnectionException if there is an issue with creating
     * or checking the database.
     */
    protected void createDatabase(String server, String databaseName,
                                  String userName, String password,
                                  String encoding)
    throws IOException, InterruptedException, DatabaseConnectionException {
        createDatabaseWithSQL(server, databaseName, userName, password, encoding);
    }
    
    /**
     * Create a database via SQL <code>CREATE DATABASE</code> statements through
     * an JDBC connection. 
     * 
     * @param server The database server.
     * @param databaseName The database name.
     * @param userName The database user name.
     * @param password The user password.
     * @param encoding The encoding for the database.
     * 
     * @throws DatabaseCreateException if there is a problem when creating the database.
     * @throws PasswordException if the user name and password are rejected by Postgres.
     * @throws DatabaseConnectionException if there are other problems connecting
     * to Postgres.
     */
    protected void createDatabaseWithSQL(String server, String databaseName,
                                         String userName, String password,
                                         String encoding)
    throws DatabaseConnectionException {

        String url = DatabaseUtil.formJdbcUrl(server, "postgres");
        
        try {
            Connection conn = DriverManager.getConnection(url, userName, password);
            try {
                StringBuilder sql = new StringBuilder();
                sql.append("create database \"").append(databaseName).append("\" with owner ");
                sql.append(userName).append(" template template0 encoding '");
                sql.append(encoding).append("'");
                
                logger.debug(sql);
                conn.createStatement().execute(sql.toString());
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            
            String message = e.getMessage();
            if (DatabaseUtil.ALREADY_EXISTS_PATTERN.matcher(message).matches()) {
                logger.info("Database '" + databaseName + "' already exists.");
            } else if (DatabaseUtil.PASSWORD_PATTERN.matcher(message).matches()) {
                throw new PasswordException(databaseName,
                        "Password authentication failed for user '" + userName + "'.");
            } else {
                throw new DatabaseCreationException(e);
            }
        }
    }
    
    /**
     * Create a database using the Postgres <code>createdb</code> program.
     * 
     * @param server The database server.
     * @param databaseName The database name.
     * @param userName The database user name.
     * @param password The user password.
     * @param encoding The encoding for the database.
     * 
     * @throws IOException if there is a problem running <code>createdb</code>.
     *  
     * @throws InterruptedException if the thread is interrupted while running
     * the <code>createdb</code>.
     * 
     * @throws DatabaseCreationException if the process completes but failed
     * to create the database.
     */
    protected void createDatabaseWithCreatedb(String server, String databaseName,
                                              String userName, String password,
                                              String encoding)
    throws IOException, InterruptedException, DatabaseCreationException {

        String[] commands = {
                "/usr/bin/createdb",
                "-h",
                server,
                "-E",
                encoding,
                "-O",
                userName,
                "-W",
                "-T",
                "template0",
                databaseName
            };
        
        if (logger.isDebugEnabled()) {
            StringBuilder command = new StringBuilder();
            for (int i = 0; i < commands.length; i++) {
                if (i > 0) {
                    command.append(' ');
                }
                command.append(commands[i]);
            }
            logger.debug(command);
        }
        
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        Integer exitCode = null;
        Process p = Runtime.getRuntime().exec(commands);
        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            boolean passwordSet = false;
            while (true) {
                try {
                    while (stdin.ready()) {
                        char c = (char) stdin.read();
                        output.append(c);
                    }
                    while (stderr.ready()) {
                        char c = (char) stderr.read();
                        errorOutput.append(c);
                    }
                    
                    if (!passwordSet && errorOutput.indexOf("Password:") >= 0) {
                        PrintStream out = new PrintStream(p.getOutputStream(), true);
                        out.println(password);
                        passwordSet = true;
                    }
                    
                    exitCode = p.exitValue();
                    // If this succeeds, we're done.
                    break;
                } catch (IllegalThreadStateException e) {
                    // Process not done, so wait and continue.
                    Thread.sleep(250);
                }
            }
        } finally {
            try {
                p.exitValue();
            } catch (IllegalThreadStateException e) {
                // Not finished, but something has failed.
                p.destroy();
            }
        }
        
        if (errorOutput.length() > 0) {
            throw new DatabaseCreationException(
                    exitCode, output.toString(), errorOutput.toString());
        }
        
        if (exitCode != 0) {
            throw new DatabaseCreationException(
                    "Return code from createdb = " + exitCode,
                    exitCode, output.toString(), errorOutput.toString());
        }
    }
}
