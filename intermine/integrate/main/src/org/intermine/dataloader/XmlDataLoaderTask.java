package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.apache.log4j.Logger;

/**
 * Uses an IntegrationWriter to load data from XML format
 *
 * @author Richard Smith
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public class XmlDataLoaderTask extends Task
{
    private static final Logger LOG = Logger.getLogger(XmlDataLoaderTask.class);
    protected String integrationWriter;
    protected File xmlFile;
    protected String sourceName;
    protected boolean ignoreDuplicates = false;
    protected String xmlRes;
    
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
     * Set XML resource name (to load data from classloader).
     * @param resName classloader resource name
     */
    public void setXmlResource(String resName) {
        this.xmlRes = resName;
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
     * Set the value of ignoreDuplicates for the IntegrationWriter
     * @param ignoreDuplicates the value of ignoreDuplicates
     */
    public void setIgnoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
        LOG.info("Setting ignoreDuplicates to " + ignoreDuplicates);
    }

    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (integrationWriter == null) {
            throw new BuildException("integrationWriter attribute is not set");
        }
        if (xmlFile == null && xmlRes == null) {
            throw new BuildException("neither xmlRes or xmlFile attributes set");
        }
        if (sourceName == null) {
            throw new BuildException("sourceName attribute is not set");
        }
        try {
            InputStream is = null;
            if (xmlRes != null) {
                is = getClass().getClassLoader().getResourceAsStream(xmlRes);
            } else {
                is = new FileInputStream(xmlFile);
            }
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriter);
            iw.setIgnoreDuplicates(ignoreDuplicates);
            new XmlDataLoader(iw).processXml(is,
                                             iw.getMainSource(sourceName),
                                             iw.getSkeletonSource(sourceName));
        } catch (Exception e) {
            throw new BuildException("Exception while reading from: "
                    + (xmlRes != null ? xmlRes : xmlFile.toString()) + " with source "
                                     + sourceName, e);
        }
    }
}

