package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;

import org.flymine.task.ClassPathTask;

/**
 * Uses an IntegrationWriter to load data from another ObjectStore
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class ObjectStoreDataLoaderTask extends ClassPathTask
{
    protected String integrationWriter;
    protected String source;
    protected String sourceName;

    /**
     * Set the IntegrationWriter.
     *
     * @param integrationWriter the name of the IntegrationWriter
     */
    public void setIntegrationWriter(String integrationWriter) {
        this.integrationWriter = integrationWriter;
    }

    /**
     * Set the ObjectStore that is the data source.
     *
     * @param source the name of the objectstore that is the source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Set the source name, as used by primary key priority config.
     *
     * @param sourceName the name of the data source
     */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.integrationWriter == null) {
            throw new BuildException("integrationWriter attribute is not set");
        }
        if (this.source == null) {
            throw new BuildException("source attribute is not set");
        }

        try {
            Object driver = loadClass("org.flymine.dataloader.ObjectStoreDataLoaderDriver");

            // Have to execute the loadData method by reflection as
            // cannot cast to something that this class (which may use
            // a different ClassLoader) can see

            Method method = driver.getClass().getMethod("loadData", new Class[] {String.class,
                String.class, String.class});
            method.invoke(driver, new Object [] {integrationWriter, source, sourceName });
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

}

