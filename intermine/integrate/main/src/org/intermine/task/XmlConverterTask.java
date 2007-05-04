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

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;

import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.XmlConverter;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.metadata.Model;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * Task in invoke XML conversion
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 */
public class XmlConverterTask extends ConverterTask
{
    protected FileSet fileSet;
    protected File schema;

    /**
     * Set the xsd schema file
     * @param schema the file
     */
    public void setSchema(File schema) {
        this.schema = schema;
    }

    /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (schema == null) {
            throw new BuildException("schema must be specified");
        }
        if (model == null) {
            throw new BuildException("model must be specified");
        }
        if (osName == null) {
            throw new BuildException("osName must be specified");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        File toRead = null;
        
        try {
            Model m = Model.getInstanceByName(model);
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            writer = new ObjectStoreItemWriter(osw);
            XmlConverter converter = new XmlConverter(m,
                                                      new BufferedReader(new FileReader(schema)),
                                                      writer);
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            if (files.length == 0) {
                throw new BuildException("No .xml files found in: " + fileSet.getDir(getProject()));
            }
            for (int i = 0; i < files.length; i++) {
                toRead = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file " + toRead.toString());
                converter.process(new BufferedReader(new FileReader(toRead)));
            }
        } catch (Exception e) {
            if (e instanceof BuildException) {
                throw (BuildException) e;
            } else if (toRead == null) {
                throw new BuildException("Exception in XmlConverterTask", e);
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
