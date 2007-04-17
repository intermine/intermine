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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

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
    protected FileSet fileSet;
    protected String sourceName;
    protected boolean ignoreDuplicates = false;
    protected String file;
    
    /**
     * Set the IntegrationWriter.
     *
     * @param integrationWriter the name of the IntegrationWriter
     */
    public void setIntegrationWriter(String integrationWriter) {
        this.integrationWriter = integrationWriter;
    }

    /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }
    
    /**
     * Set XML resource name (to load data from classloader).
     * @param resName classloader resource name
     */
    public void setFile(String file) {
        this.file = file;
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
        if (sourceName == null) {
            throw new BuildException("sourceName attribute is not set");
        }
        XmlDataLoader loader = null;
        File toRead = null;
        try {
            IntegrationWriter iw = IntegrationWriterFactory.getIntegrationWriter(integrationWriter);
            iw.setIgnoreDuplicates(ignoreDuplicates);
            loader = new XmlDataLoader(iw);
            List<File> files = new ArrayList<File>();

            if (file != null && !file.equals("")) {
                files = new ArrayList<File>(Collections.singleton(new File(file)));
            } else {
                DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
                String[] fileArray = ds.getIncludedFiles();
                for (int i = 0; i < fileArray.length; i++) {
                    files.add(new File(ds.getBasedir(), fileArray[i]));
                }
                if (files.isEmpty()) {
                    throw new BuildException("No xml files read from: " + fileSet.toString());
                }
            }
            if (files.isEmpty()) {
                throw new BuildException("No files found to load for source: " + sourceName);
            }
            Iterator<File> fileIter = files.iterator();
            while (fileIter.hasNext()) {
                toRead = fileIter.next();              
                System.out .println("Processing file " + toRead.toString());
                loader.processXml(new FileInputStream(toRead),
                                  iw.getMainSource(sourceName),
                                  iw.getSkeletonSource(sourceName)); 
            }
        } catch (Exception e) {
            if (toRead == null) {
                throw new BuildException("Exception in XmlDataLoaderTask", e);
            } else {
                throw new BuildException("Exception while reading from: " + toRead, e);
            }
        } finally {
            try {
                loader.close();
            } catch (Exception e) {
                throw new BuildException("Exception while closing XmlDataLoader", e);
            }
        }
    }
}

