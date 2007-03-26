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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.intermine.objectstore.ObjectStoreSummary;
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
    protected String alias;
    protected File outputFile, inputFile;

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
     * @param inputFile the properties file
     */
    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Set the name of the file to write the summary into
     * @param outputFile the output file
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (outputFile.exists() && outputFile.lastModified() > inputFile.lastModified()) {
            System
                .out.println("Summarisation is newer than config. Skipping summarisation.");
            return;
        }
        try {
            Properties config = new Properties();
            config.load(new FileInputStream(inputFile));
            String header = "Automatically generated for " + alias + " using config " + inputFile;
            new ObjectStoreSummary(ObjectStoreFactory.getObjectStore(alias), config)
                .toProperties().store(new FileOutputStream(outputFile), header);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}
