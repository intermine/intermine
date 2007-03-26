package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Project;

import org.apache.torque.task.TorqueSQLExec;
import org.apache.torque.task.TorqueSQLTask;

import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.SQLException;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;

import org.apache.log4j.Logger;

/**
 * Generates and inserts SQL given database name, schema and temporary directory
 *
 * @author Mark Woodbridge
 */
public class BuildDbTask extends Task
{
    private static final Logger LOG = Logger.getLogger(BuildDbTask.class);
    protected static final String SERIAL_SEQUENCE_NAME = "serial";
    protected File tempDir;
    protected Database database;
    protected String databaseAlias;
    protected String schemaFile;

    /**
     * Sets the objectstore
     * @param os String used to identify objectstore and therefore database
     */
    public void setOsName(String os) {
        try {
            this.databaseAlias = PropertiesUtil.getProperties().getProperty(os + ".db");
            this.database = DatabaseFactory.getDatabase(databaseAlias);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the directory for temporary files including sql output
     * @param tempDir the directory location
     */
    public void setTempdir(File tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * Adds the schemafile to be processed.
     * @param schemafile to be processed
     */
    public void setSchemafile(String schemafile) {
        this.schemaFile = schemafile;
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (tempDir == null) {
            throw new BuildException("tempDir attribute is not set");
        }
        if (database == null) {
            throw new BuildException("database attribute is not set or database is not present");
        }
        if (schemaFile == null) {
            throw new BuildException("schemaFile attribute is not set");
        }
       
        {
            Connection c = null;
            try {
                c = database.getConnection();
                c.setAutoCommit(true);
                DatabaseUtil.removeAllTables(c);
            } catch (SQLException e) {
                LOG.error("Failed to remove all tables from database: " + e);
            } finally {
                if (c != null) {
                    try {
                        c.close();
                    } catch (SQLException e) {
                        // ignore
                    }
                }
            }
        }

        SQL sql = new SQL();
        sql.setControlTemplate("sql/base/Control.vm");
        sql.setOutputDirectory(tempDir);
        sql.setUseClasspath(true);
        //sql.setBasePathToDbProps("sql/base/");
        sql.setSqlDbMap(tempDir + "/sqldb.map");
        sql.setOutputFile("report.sql.generation");
        sql.setTargetDatabase(database.getPlatform().toLowerCase()); // "postgresql"
        InputStream schemaFileInputStream =
            getClass().getClassLoader().getResourceAsStream(schemaFile);

        if (schemaFileInputStream == null) {
            throw new BuildException("cannot open schema file (" + schemaFile + ")");
        }
        
        File tempFile;

        try {
            tempFile = File.createTempFile("schema", "xml", tempDir);

            PrintWriter writer = new PrintWriter(new FileWriter(tempFile));
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(schemaFileInputStream));
            
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                } else {
                    writer.println(line);
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new BuildException("cannot create temporary file for BuildDbTask: "
                                     + e.getMessage());
        }

        sql.setXmlFile(tempFile.getPath());

        sql.execute();

        InsertSQL isql = new InsertSQL();
        isql.setDriver(database.getDriver()); // "org.postgresql.Driver"
        isql.setUrl(database.getURL()); // "jdbc:postgresql://localhost/test"
        isql.setUserid(database.getUser()); // "mark"
        isql.setPassword(database.getPassword()); // ""
        isql.setAutocommit(true);
        TorqueSQLExec.OnError ea = new TorqueSQLExec.OnError();
        ea.setValue("continue"); // "abort", "continue" or "stop"
        isql.setOnerror(ea);
        isql.setSqlDbMap(tempDir + "/sqldb.map");
        isql.setSrcDir(tempDir.toString());
        try {
            isql.execute();
        } catch (BuildException e) {
            throw new BuildException(e.getMessage() + " - for database: " + databaseAlias,
                                     e.getCause());
        }

        ea.setValue("abort"); // "abort", "continue" or "stop"
        isql.execute();
        // TODO: properly

        Connection c = null;
        try {
            c = database.getConnection();
            c.setAutoCommit(true);
            c.createStatement().execute("CREATE SEQUENCE " + SERIAL_SEQUENCE_NAME);
        } catch (SQLException e) {
            // probably happens because the SEQUENCE already exists
            LOG.info("Failed to create SEQUENCE: " + e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }

        c = null;

        try {
            c = database.getConnection();
            c.setAutoCommit(true);
            c.createStatement().execute("CREATE SEQUENCE "
                                        + ObjectStoreInterMineImpl.UNIQUE_INTEGER_SEQUENCE_NAME);
        } catch (SQLException e) {
            // probably happens because the SEQUENCE already exists
            LOG.info("Failed to create SEQUENCE: " + e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }

        tempFile.delete();
    }
}

/**
 * Class to initialise sql generation task in absence of ant
 *
 * @author Mark Woodbridge
 */
class SQL extends TorqueSQLTask
{
    /**
     * Default constructor
     */
    public SQL() {
            project = new Project();
            project.init();
            target = new Target();
            taskName = "torque-sql";
    }
}

/**
 * Class to initialise sql insertion task in absence of ant
 *
 * @author Mark Woodbridge
 */
class InsertSQL extends TorqueSQLExec 
{
    /**
     * Default constructor
     */
    public InsertSQL() {
            project = new Project();
            project.init();
            target = new Target();
            taskName = "torque-insert-sql";
    }
}
