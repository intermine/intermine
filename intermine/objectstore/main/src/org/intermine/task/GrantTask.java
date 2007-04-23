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

import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.sql.DatabaseUtil;

/**
 * Task to grant permissions on all tables in a database to a given user.
 *
 * @author Andrew Varley
 * @author Richard Smith
 */

public class GrantTask extends Task
{

    protected String database;
    protected String user;
    protected String perm;


    /**
     * Set the database alias
     * @param database the database alias
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Set user to grant permissions to
     * @param user a username
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * The permission to grant: SELECT, INSERT, UPDATE, DELETE, RULE,
     * REFERENCES, TRIGGER, ALL
     * @param perm the permission to set
     */
    public void setPerm(String perm) {
        this.perm = perm;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        if (user == null) {
            throw new BuildException("user attribute is not set");
        }
        if (perm == null) {
            throw new BuildException("perm attribute is not set");
        }

        try {
            Database db = DatabaseFactory.getDatabase(database);
            DatabaseUtil.grant(db, user, perm);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
