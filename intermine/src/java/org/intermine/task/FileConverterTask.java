package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.FileConverter;


import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.apache.log4j.Logger;

/**
 * Initiates retrieval and conversion of data from a source database
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class FileConverterTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(FileConverterTask.class);

    protected String clsName;
    protected String file;
    protected String osName;
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
     * Set the input file name
     * @param file the database name
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Set the objectstore name
     * @param osName the model name
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Set param1
     * @param param1 the model name
     */
    public void setParam1(String param1) {
        this.param1 = param1;
    }

    /**
     * Set param2
     * @param param2 the model name
     */
    public void setParam2(String param2) {
        this.param2 = param2;
    }

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (clsName == null) {
            throw new BuildException("clsName attribute is not set");
        }
        if (file == null) {
            throw new BuildException("database attribute is not set");
        }
        if (osName == null) {
            throw new BuildException("model attribute is not set");
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            ItemWriter writer = new ObjectStoreItemWriter(osw);

            Class c = Class.forName(clsName);
            if (!FileConverter.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException("Class (" + clsName + ") is not a subclass"
                                             + "of org.intermine.dataconversion.FileConverter.");
            }

            Constructor m = c.getConstructor(new Class[] {BufferedReader.class, ItemWriter.class});
            FileConverter converter = (FileConverter) m.newInstance(new Object[] {reader, writer});
            if (param1 != null) {
                converter.setParam1(param1);
            }
            if (param2 != null) {
                converter.setParam2(param2);
            }
            converter.process();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
