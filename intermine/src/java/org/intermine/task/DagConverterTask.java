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

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.DagConverter;


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
public class DagConverterTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(DagConverterTask.class);

    protected String file;
    protected String osName;
    protected String namespace;

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
     * Set the namespace
     *
     * @param namespace the namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (file == null) {
            throw new BuildException("database attribute is not set");
        }
        if (osName == null) {
            throw new BuildException("model attribute is not set");
        }
        if (namespace == null) {
            throw new BuildException("namespace attribute is not set");
        }

        try {
            ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            ItemWriter writer = new ObjectStoreItemWriter(osw);

            DagConverter converter = new DagConverter(writer, file, namespace);
            converter.process();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
