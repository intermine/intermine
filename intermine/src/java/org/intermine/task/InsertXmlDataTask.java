package org.flymine.task;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * Uses an IntegrationWriterWriter to insert XML data from a file
 *
 * @author Andrew Varley
 */
public class InsertXmlDataTask extends ClassPathTask
{

    protected String integrationWriter;
    protected File file;

    /**
     * Set the IntegrationWriter
     *
     * @param integrationWriter the name of the IntegrationWriter
     */
    public void setIntegrationWriter(String integrationWriter) {
        this.integrationWriter = integrationWriter;
    }

    /**
     * Set the XML file to be inserted
     *
     * @param file the name of the file
     */
    public void setFile(File file) {
        this.file = file;
    }


    /**
     * @see Task#execute
     * @throws BuildException
     */
    public void execute() throws BuildException {
        if (this.integrationWriter == null) {
            throw new BuildException("integrationWriter attribute is not set");
        }
        if (this.file == null) {
            throw new BuildException("file attribute is not set");
        }

        try {
            Object driver = loadClass("org.flymine.task.XmlDataLoaderDriver");

            // Have to execute the loadData method by reflection as
            // cannot cast to something that this class (which may use
            // a different ClassLoader) can see

            Method method = driver.getClass().getMethod("loadData", new Class[] {String.class,
                                                                                 File.class });
            method.invoke(driver, new Object [] {integrationWriter,
                                                 file });
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }

}
