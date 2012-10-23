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
import java.lang.reflect.Constructor;

import org.apache.tools.ant.BuildException;
import org.intermine.dataconversion.DirectoryConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

/**
 * Initiates retrieval and conversion of data from a source directory.
 *
 * @author Julie Sullivan
 */
public class DirectoryConverterTask extends ConverterTask
{
    protected String dataDir;
    protected String clsName;

    /**
     * Set the source specific subclass of DirectoryConverter to run
     * @param clsName name of converter class to run
     */
    public void setClsName(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Set the dataDir.
     *
     * @param dataDir the name of a directory with data input files
     */
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Run the task.
     *
     * @throws BuildException if a problem occurs
     */
    public void execute() {
        if (dataDir == null) {
            throw new BuildException("dataDir must be specified");
        }
        File dir = new File(dataDir);
        String directoryName = dir.getName();
        if (!dir.isDirectory()) {
            throw new BuildException("dataDir is not a directory:" + directoryName);
        }
        if (dir.listFiles().length == 0) {
            throw new BuildException("dataDir contains no files:" + directoryName);
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
            Model model = Model.getInstanceByName(getModelName());
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(getOsName());
            writer = new ObjectStoreItemWriter(osw);

            Class c = Class.forName(clsName);
            if (!DirectoryConverter.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException("Class (" + clsName + ") is not a subclass"
                                             + "of org.intermine.dataconversion.Directory.");
            }

            Constructor m = c.getConstructor(new Class[] {ItemWriter.class, Model.class});
            DirectoryConverter converter =
                (DirectoryConverter) m.newInstance(new Object[] {writer, model});

            try {
                configureDynamicAttributes(converter);

                File file = new File(dataDir);
                if (file.isDirectory()) {
                    converter.process(file);
                } else {
                    throw new IllegalArgumentException(dataDir + " is not a directory");
                }
            } finally {
                converter.close();
            }
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
