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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.lang.reflect.Constructor;

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.FileConverter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

import org.apache.log4j.Logger;

/**
 * Initiates retrieval and conversion of data from a source file.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Richard Smith
 * @author Matthew Wakeling
 * @author Thomas Riley
 */
public class FileConverterTask extends ConverterTask
{
    protected static final Logger LOG = Logger.getLogger(FileConverterTask.class);

    protected FileSet fileSet;
    protected String clsName;
    protected String param1 = null;
    protected String param2 = null;

    /**
     * Set the source specific subclass of FileConverter to run
     * @param clsName name of converter class to run
     */
    public void setClsName(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Set the data fileset
     * @param fileSet the fileset
     */
    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("fileSet must be specified");
        }
        if (clsName == null) {
            throw new BuildException("clsName attribute is not set");
        }
        if (getOsName() == null) {
            throw new BuildException("osName attribute is not set");
        }
        if (getModelName() == null) {
            throw new BuildException("modelName attribute is not set");
        }

        // Needed so that STAX can find its implementation classes
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(getOsName());
            writer = new ObjectStoreItemWriter(osw);

            Class c = Class.forName(clsName);
            if (!FileConverter.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException("Class (" + clsName + ") is not a subclass"
                                             + "of org.intermine.dataconversion.FileConverter.");
            }

            Constructor m = c.getConstructor(new Class[] {ItemWriter.class});
            FileConverter converter = (FileConverter) m.newInstance(new Object[] {writer});
            if (param1 != null) {
                System.err .println("Using param1 - please dynamic properties"
                        + "instead (see FileConverterTask)");
                converter.setParam1(param1);
            }
            if (param2 != null) {
                System.err .println("Using param2 - please dynamic properties"
                        + "instead (see FileConverterTask)");
                converter.setParam2(param2);
            }

            configureDynamicAttributes(converter);

            DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
            String[] files = ds.getIncludedFiles();
            for (int i = 0; i < files.length; i++) {
                File f = new File(ds.getBasedir(), files[i]);
                System.err .println("Processing file: " + f.getPath());
                converter.setCurrentFile(f);
                converter.process(new BufferedReader(new FileReader(f)));
            }
            converter.close();
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
            try {
                if (writer != null) {
                    writer.close();
                }
                if (writer != null) {
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
