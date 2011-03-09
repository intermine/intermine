package org.intermine.bio.util;

/*
 * Copyright (C) 2002-2011 FlyMine
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
    private String objectStore, outputFile, taxonIds, paths, webappPages;
    private String sitePrefix, targetModel, defaultContext;

    /**
     * @param sitePrefix the sitePrefix to set
     */
    public void setSitePrefix(String sitePrefix) {
        this.sitePrefix = sitePrefix;
    }

    /**
     * @param targetModel the targetModel to set
     */
    public void setTargetModel(String targetModel) {
        this.targetModel = targetModel;
    }

    /**
     * @param defaultContext the defaultContext to set
     */
    public void setDefaultContext(String defaultContext) {
        this.defaultContext = defaultContext;
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
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }


    /**
     * {@inheritDoc}
     */
    public void execute() {

        if (outputFile == null) {
            throw new BuildException("outputFile attribute is not set");
        }

        if (objectStore == null) {
            throw new BuildException("objectStore attribute is not set");
        }

        ObjectStore os = null;

        try {
            os = ObjectStoreFactory.getObjectStore(objectStore);
            CreateSiteMaps.createSiteMap(os, outputFile, taxonIds, paths, webappPages,
                                         targetModel, sitePrefix, defaultContext);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * @param taxonIds the taxonIds to set
     */
    public void setTaxonIds(String taxonIds) {
        this.taxonIds = taxonIds;
    }

    /**
     * @param paths the paths to set
     */
    public void setPaths(String paths) {
        this.paths = paths;
    }

    /**
     * @param webappPages the webappPages to set
     */
    public void setWebappPages(String webappPages) {
        this.webappPages = webappPages;
    }
}

