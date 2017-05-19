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

import java.util.Properties;

import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.util.PropertiesUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.StringInputStream;


/**
 * Summarise an ObjectStore into a particular properties file.  The summary contains:
 * <p> counts of the number of objects of each class
 * <p> for class fields that have a small number of value, a list of those values.
 * @author Kim Rutherford
 */

public class SummariseObjectStoreTask extends Task
{
    protected String alias, configFileName;
    protected File outputFile;

    /**
     * Set the ObjectStore alias
     * @param alias the ObjectStore alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Set the name of the Properties file containing the names of the classes and fields to
     * summarise.
     * @param configFileName the properties file name
     */
    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    /**
     * Set the name of the file to write the summary into
     * @param outputFile the output file
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() {
        try {
            ObjectStore os = ObjectStoreFactory.getObjectStore(alias);
            if (os instanceof ObjectStoreInterMineImpl) {
                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
                String objectSummaryString =
                    MetadataManager.retrieve(db, MetadataManager.OS_SUMMARY);

                ObjectStoreSummary oss;

                if (objectSummaryString == null) {
                    // not there so summarise
                    ClassLoader classLoader = SummariseObjectStoreTask.class.getClassLoader();
                    InputStream configStream = classLoader.getResourceAsStream(configFileName);
                    if (configStream == null) {
                        throw new RuntimeException("can't find resource: " + configFileName);
                    }
                    Properties config = new Properties();
                    config.load(configStream);
                    System.out .println("summarising objectstore...");
                    oss = new ObjectStoreSummary(os, config);
                    MetadataManager.store(db, MetadataManager.OS_SUMMARY,
                                          PropertiesUtil.serialize(oss.toProperties()));

                } else {
                    Properties objectStoreSummaryProperties = new Properties();
                    InputStream objectStoreSummaryPropertiesStream =
                        new StringInputStream(objectSummaryString);

                    objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
                    oss = new ObjectStoreSummary(objectStoreSummaryProperties);
                }

                String header = "Automatically generated for " + alias + " using config "
                    + configFileName;
                oss.toProperties().store(new FileOutputStream(outputFile), header);

            } else {
                throw new RuntimeException("can't read summary from " + alias
                                           + " - not an instance of ObjectStoreInterMineImpl");
            }
        } catch (Exception e) {
            throw new BuildException("failed to get the object store summary", e);
        }
    }
}
