package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.lang.reflect.Method;

import org.apache.tools.ant.BuildException;

import org.intermine.task.ClassPathTask;

/**
 * Uses an IntegrationWriter to load data from XML format
 *
 * @author Richard Smith
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class XmlDataLoaderTask extends ClassPathTask
{
    protected String integrationWriter;
    protected File xmlFile;
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
     * Set the XML file to load data from.
     *
     * @param xmlFile the XML file
     */
    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
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
        if (this.xmlFile == null) {
            throw new BuildException("xmlFile attribute is not set");
        }
        if (this.sourceName == null) {
            throw new BuildException("sourceName attribute is not set");
        }

        try {
            Object driver = loadClass("org.intermine.dataloader.XmlDataLoaderDriver");

            // Have to execute the loadData method by reflection as
            // cannot cast to something that this class (which may use
            // a different ClassLoader) can see

            Method method = driver.getClass().getMethod("loadData", new Class[] {String.class,
                File.class, String.class});
            method.invoke(driver, new Object [] {integrationWriter, xmlFile, sourceName });
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }
}

