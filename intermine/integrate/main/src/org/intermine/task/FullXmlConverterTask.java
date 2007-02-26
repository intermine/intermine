package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import org.intermine.dataconversion.FullXmlConverter;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * Task in invoke XML conversion.
 *
 * @author Matthew Wakeling
 */
public class FullXmlConverterTask extends ConverterTask
{
    protected FileSet fileSet;

    /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (osName == null) {
            throw new BuildException("osName must be specified");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        File toRead = null;
        
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            writer = new ObjectStoreItemWriter(osw);
            FullXmlConverter converter = new FullXmlConverter(writer);
            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                toRead = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file " + toRead.toString());
                converter.process(new BufferedReader(new FileReader(toRead)));
            }
        } catch (Exception e) {
            if (toRead == null) {
                throw new BuildException("Exception in FullXmlConverterTask", e);
            } else {
                throw new BuildException("Exception in FullXmlConverterTask while reading from: "
                        + toRead, e);
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
