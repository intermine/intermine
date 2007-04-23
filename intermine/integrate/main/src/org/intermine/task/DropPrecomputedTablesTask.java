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


import java.sql.SQLException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.intermine.sql.Database;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.sql.precompute.PrecomputedTableManager;

/**
 * A Task that drops all precomputed tables in an ObjectStore.
 *
 * @author Kim Rutherford
 */

public class DropPrecomputedTablesTask extends Task
{
    protected String alias;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (alias == null) {
            throw new BuildException("alias attribute is not set");
        }

        ObjectStore objectStore;

        try {
            objectStore = ObjectStoreFactory.getObjectStore(alias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }

        if (objectStore instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) objectStore).getDatabase();

            try {
                PrecomputedTableManager ptm = PrecomputedTableManager.getInstance(db);
                ptm.dropEverything();
            } catch (SQLException e) {
                throw new BuildException("failed to drop precomputed tables", e);
            }
        } else {
            throw new BuildException("ObjectStore specified by \"" + alias + "\" is not an "
                                     + "instance of ObjectStoreInterMineImpl");
        }

    }
}
