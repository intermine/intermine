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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Copy a Model in the intermine_metadata table of an database
 *
 * @author Kim Rutherford
 */
public class InsertModelTask extends Task
{
    protected String modelName;
    protected Database database;

    /**
     * Sets the name of the model.
     *
     * @param modelName the model name
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

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
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.modelName == null) {
            throw new BuildException("modelName attribute is not set");
        }
        if (this.database == null) {
            throw new BuildException("database attribute is not set");
        }

        try {
            Model model = Model.getInstanceByName(modelName);
            Connection connection = null;
            try {
                connection = database.getConnection();
                connection.setAutoCommit(false);
                Statement statement = connection.createStatement();
                statement.execute("INSERT INTO intermine_metadata (key, value) "
                                  + "VALUES('model', '" + model + "')");
                connection.commit();
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        } catch (MetaDataException e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        }
    }
}
