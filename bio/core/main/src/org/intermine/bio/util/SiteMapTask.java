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

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Output files to build sitemap xml file
 *
 * @author Julie Sullivan
 */
public class SiteMapTask extends Task
{
    private String objectStore;
    private String outputFile;

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
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }


    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {

        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }

        if (objectStore == null) {
            throw new BuildException("objectStore attribute is not set");
        }

        ObjectStore os = null;

        try {
            os = ObjectStoreFactory.getObjectStore(objectStore);
            CreateSiteMapLinkIns.createSiteMap(os, outputFile);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

