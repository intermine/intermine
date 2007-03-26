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

import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.DagConverter;
import org.intermine.dataconversion.OboConverter;


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

    private String file, dagName, osName, url, termClass;

    /**
     * Set the input file name
     * @param file the database name
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Set the name of the dag
     * @param dagName the name
     */
    public void setDagName(String dagName) {
        this.dagName = dagName;
    }

    /**
     * Set the objectstore name
     * @param osName the model name
     */
    public void setOsName(String osName) {
        this.osName = osName;
    }

    /**
     * Set the url for the source of the ontology
     *
     * @param url the URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Set the term class name
     *
     * @param termClass the class of the term
     */
    public void setTermClass(String termClass) {
        this.termClass = termClass;
    }

    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (file == null) {
            throw new BuildException("database attribute is not set");
        }
        if (dagName == null) {
            throw new BuildException("dagName attribute is not set");
        }
        if (osName == null) {
            throw new BuildException("model attribute is not set");
        }
        if (termClass == null) {
            throw new BuildException("termClass attribute is not set");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            writer = new ObjectStoreItemWriter(osw);

            DagConverter converter;
            if (file.endsWith(".ontology") || file.endsWith(".dag")) {
                converter = new DagConverter(writer, file, dagName, url, termClass);
            } else if (file.endsWith(".obo")) {
                converter = new OboConverter(writer, file, dagName, url, termClass);
            } else {
                throw new IllegalArgumentException("Don't know how to deal with file " + file);
            }
            converter.process();
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try {
                writer.close();
                osw.close();
            } catch (Exception e) {
                throw new BuildException(e);
            }
        }
    }
}
