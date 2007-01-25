package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.PropertiesUtil;

/**
 * Task to run ANALYSE on a table or whole database.
 *
 * @author Richard Smith
 * @author Andrew Varley
 */

public class AnalyseDbTask extends Task
{

    protected String database;
    protected boolean full = false;
    protected String clsName;
    protected String model;

    /**
     * Set the database alias
     * @param osName the database alias
     */
    public void setOsName(String osName) {
        this.database = PropertiesUtil.getProperties().getProperty(osName + ".db");
    }

    /**
     * Set an optional class name, must also set model name
     * @param clsName name of class to ANALYSE
     */
    public void setClassName(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Set model name, must be set if class name specified
     * @param model containing the class
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set true if a VACUUM FULL ANALYSE required
     * @param full true for full anaylse
     */
    public void setFull(boolean full) {
        this.full = full;
    }

    /**
     * @see Task#execute()
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }

        try {
            Database db = DatabaseFactory.getDatabase(database);

            if (clsName != null && !clsName.equals("")) {
                if (model == null) {
                    throw new BuildException("model attribute is not set");
                }
                Model m = Model.getInstanceByName(model);
                ClassDescriptor cld = m.getClassDescriptorByName(clsName);
                if (cld == null) {
                    throw new BuildException("class does not exist in model: " + clsName);
                }
                DatabaseUtil.analyse(db, cld, full);
            } else {
                DatabaseUtil.analyse(db, full);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
