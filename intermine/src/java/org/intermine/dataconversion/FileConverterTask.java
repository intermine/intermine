package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;

import org.flymine.objectstore.ObjectStoreWriterFactory;
import org.flymine.objectstore.ObjectStoreWriter;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.apache.log4j.Logger;

/**
 * Initiates retrieval and conversion of data from a source database
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class FileConverterTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(FileConverterTask.class);

    protected String source;
    protected String file;
    protected String osName;

    /**
     * Set the data source ('orthologue', 'mage' or 'rnai') and thus the expected input file format
     * @param source the source
     */
    public void setSource(String source) {
        this.source = source;
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
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (source == null) {
            throw new BuildException("source attribute is not set");
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

            FileConverter converter = null;
            if ("orthologue".equals(source)) {
                converter = new OrthologueConverter(reader, writer);
            } else if ("mage".equals(source)) {
                converter = new MageConverter(reader, writer);
            } else if ("rnai".equals(source)) {
                converter = new RNAiConverter(reader, writer);
            } else {
                throw new IllegalArgumentException("'source' parameter has an invalid value");
            }
            converter.process();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
