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

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummaryGenerator;
import org.intermine.objectstore.ObjectStoreFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Summarise an ObjectStore into a particular properties file.  The summary contains:
 * <p> counts of the number of objects of each class
 * <p> for class fields that have a small number of value, a list of those values.
 * @author Kim Rutherford
 */

public class SummariseObjectStoreTask extends Task
{
    protected String alias, outputFile, inputFile;

    /**
     * Set the ObjectStore alias 
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the input file - a Properties file containing the names of the classes and fields to
     * summarise.
     * @see ObjectStoreSummaryGenerator#getAsProperties
     * @param inputFile the properties file
     */
    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Set the name of the file to write the summary into
     * @param outputFile the output file
     */
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        try {
            InputStream is = new FileInputStream(inputFile);
            Properties configProperties = new Properties();
            configProperties.load(is);
            ObjectStore os = ObjectStoreFactory.getObjectStore(alias);
            Properties outputProperties =
                ObjectStoreSummaryGenerator.getAsProperties(os, configProperties);
            FileOutputStream fos = new FileOutputStream(outputFile);
            outputProperties.store(fos, "automatically generated from " + alias
                                   + " with settings from " + inputFile);
        } catch (Exception e) {
            org.intermine.web.LogMe.log("i", e);
            throw new BuildException(e);
        }
    }
}
