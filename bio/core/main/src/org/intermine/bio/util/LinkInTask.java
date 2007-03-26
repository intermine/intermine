package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2007 FlyMine
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

/**
 * Output files to send to other database providers to link in to
 * FlyMine.
 *
 * @author Richard Smith
 */
public class LinkInTask extends Task
{
    private String objectStore, database;
    private File outputFile;

    /**
     * Sets the value of database. Can be: "flybase"
     *
     * @param database the database to create link-ins for
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Sets the value of objectStore
     *
     * @param objectStore an objectStore alias for operations that require one
     */
    public void setObjectStore(String objectStore) {
        this.objectStore = objectStore;
    }

    /**
     * Sets the value of outputFile
     *
     * @param outputFile an output file for operations that require one
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }


    /**
     * @see Task#execute()
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }

        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }

        if (objectStore == null) {
            throw new BuildException("objectStore attribute is not set");
        }

        ObjectStore os = null;

        try {
            os = ObjectStoreFactory.getObjectStore(objectStore);
            if ("flybase".equals(database)) {
                CreateFlyBaseLinkIns.createLinkInFile(os, outputFile);
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

