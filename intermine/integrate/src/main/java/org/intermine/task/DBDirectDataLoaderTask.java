package org.intermine.task;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tools.ant.BuildException;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * This task is a thin layer around DirectDataLoader to create objects and store them
 * directly into an ObjectStore using an IntegrationWriter when retrieving them from
 * a second database. We maintain object for second database and connection here.
 *
 * @author Joe Carlson
 */

public abstract class DBDirectDataLoaderTask extends DirectDataLoaderTask
{

    private String dbName;
    private Connection connection;
    private Database db = null;

    /**
     * Return the Database to read from, creating a new instance if needed.
     * @return the Database object
     */
    protected Database getDb() {
        if (db == null) {
            if (dbName == null ) {
                throw new BuildException("dbName is not set.");
            }
            try {
                db = DatabaseFactory.getDatabase(dbName);
            } catch (ClassNotFoundException | SQLException e) {
                throw new BuildException("Trouble getting database: " + e.getMessage());
            }
        }
        return db;
    }

    /**
     * Set the Connection to read from, creating a new instance if needed
     * @return the Connection object
     */
    public Connection getConnection() {
        if (connection == null ) {
            try {
                connection = getDb().getConnection();
            } catch (SQLException e) {
                throw new BuildException("Trouble getting database connection: " + e.getMessage());
            }
        }
        return connection;
    }

    /**
     * Set the Database to read from
     * @param dbName the database alias
     */

    public void setSourceDbName(String dbName) {
        if (dbName.startsWith("db.")) {
            this.dbName = dbName;
        } else {
            this.dbName = "db." + dbName;
        }
    }
}
