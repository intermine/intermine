package org.intermine.task;

/*
 * Copyright (C) 2002-2003 FlyMine
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
import org.apache.tools.ant.types.FileSet;

import org.apache.torque.task.TorqueSQLExec;
import org.apache.torque.task.TorqueSQLTask;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * Generates and inserts SQL given database name, schema fileset and destination directory
 *
 * @author Mark Woodbridge
 */
public class BuildDbTask extends Task
{
    protected static final String SEQUENCE_NAME = "serial";
    protected List filesets = new ArrayList();
    protected File destDir;
    protected Database database;

    /**
     * Sets the database
     * @param database String used to identify Database (usually dataSourceName)
     */
    public void setDatabase(String database) {
        try {
            this.database = DatabaseFactory.getDatabase(database);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the directory for temporary files including sql output
     * @param destDir the directory location
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Adds a set of xml database schema files to be processed
     * @param fileset a Set of xml schema files
     */
    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.destDir == null) {
            throw new BuildException("destDir attribute is not set");
        }
        if (filesets.size() == 0) {
            throw new BuildException("fileset attribute is not set");
        }
        if (database == null) {
            throw new BuildException("database attribute is not set or database is not present");
        }
       
        SQL sql = new SQL();
        sql.setControlTemplate("sql/base/Control.vm");
        sql.setOutputDirectory(destDir);
        sql.setUseClasspath(true);
        //sql.setBasePathToDbProps("sql/base/");
        sql.setSqlDbMap(destDir + "/sqldb.map");
        sql.setOutputFile("report.sql.generation");
        sql.setTargetDatabase(database.getPlatform().toLowerCase()); // "postgresql"
        Iterator iter = filesets.iterator();
        while (iter.hasNext()) {
            sql.addFileset((FileSet) iter.next());
        }
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
        isql.setSqlDbMap(destDir + "/sqldb.map");
        isql.setSrcDir(destDir.toString());
        isql.execute();

        try {
            Connection c = database.getConnection();
            c.setAutoCommit(true);
            c.createStatement().execute("create sequence " + SEQUENCE_NAME);
            c.close();
        } catch (SQLException e) {
        }
    }

//     public static void main(String[] args) {
//         BuildDbTask task = new BuildDbTask();
//         FileSet f = new FileSet();
//         f.setDir(new File("schema"));
//         f.setIncludes("*-schema.xml");
//         task.addFileset(f);
//         task.setDestdir(new File("sql"));
//         task.execute();
//     }
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
