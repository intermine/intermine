package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.util.DatabaseUtil;

/**
 * Task to run ANALYSE on a database.
 *
 * @author Richard Smith
 * @author Andrew Varley
 */

public class AnalyseDbTask extends Task
{

    protected String database;
    protected boolean full = false;

    /**
     * Set the database alias
     * @param database the database alias
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Set true if a VACUUM FULL ANALYSE required
     * @param full true for full anaylse
     */
    public void setFull(boolean full) {
        this.full = full;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }

        try {
            Database db = DatabaseFactory.getDatabase(database);
            DatabaseUtil.analyse(db, full);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
