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

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.metadata.Model;

/**
 * Copy a Model in the intermine_metadata table of an database
 *
 * @author Kim Rutherford
 */
public class InsertModelTask extends Task
{
    protected String modelName;
    protected String database;

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
        this.database = database;
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (modelName == null) {
            throw new BuildException("modelName attribute is not set");
        }
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        try {
            Model model = Model.getInstanceByName(modelName);
            Database db = DatabaseFactory.getDatabase(database);
            MetadataManager.storeModel(model.toString(), db);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
