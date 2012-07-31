package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.intermine.dataconversion.FullXmlConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Load InterMine Items XML file(s) into a target items database.
 *
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class FullXmlConverterTask extends ConverterTask
{
    protected FileSet fileSet;
    protected String xmlRes, file, sourceName;

    /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * Set a file name to load from
     * @param file name of file to load
     */
    public void setFile(String file) {
        this.file = file;
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
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (getOsName() == null) {
            throw new BuildException("osName must be specified");
        }
        if (sourceName == null) {
            throw new BuildException("sourceName attribute is not set");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        File toRead = null;

        try {
            Model model = Model.getInstanceByName(getModelName());
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(getOsName());
            writer = new ObjectStoreItemWriter(osw);
            FullXmlConverter converter = new FullXmlConverter(writer, model);

            List<File> files = new ArrayList<File>();

            // read an InputStream from the classpath
            if (xmlRes != null) {
                InputStream is = getClass().getClassLoader().getResourceAsStream(xmlRes);
                if (is == null) {
                    throw new BuildException("Failed to find resource '" + xmlRes
                                             + "' on classpath.");
                }
                converter.process(new InputStreamReader(is));
            } else {
                if (file != null && !"".equals(file)) {
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
                    converter.process(new FileReader(toRead));
                }
            }
        } catch (Exception e) {
            if (toRead == null) {
                throw new BuildException("Exception in FullXmlConverterTask", e);
            } else {
                throw new BuildException("Exception while reading from: " + toRead, e);
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (osw != null) {
                    osw.close();
                }
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }

        try {
            doSQL(osw.getObjectStore());
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
