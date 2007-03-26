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
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.intermine.dataconversion.ObjectStoreItemWriter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.FullXmlConverter;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.apache.tools.ant.BuildException;

/**
 * Task in invoke XML conversion.
 *
 * @author Matthew Wakeling
 */
public class FullXmlConverterTask extends ConverterTask
{
    protected File xmlFile;
    protected String xmlRes;

    /**
     * Set the XML file to load data from.
     *
     * @param xmlFile the XML file
     */
    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
     * Set XML resource name (to load data from classloader).
     * @param resName classloader resource name
     */
    public void setXmlResource(String resName) {
        this.xmlRes = resName;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (xmlFile == null && xmlRes == null) {
            throw new BuildException("neither xmlRes nor xmlFile attributes set");
        }
        if (xmlFile != null && xmlRes != null) {
            throw new BuildException("both xmlRes and xmlFile attributes set");
        }
        if (osName == null) {
            throw new BuildException("osName must be specified");
        }

        ObjectStoreWriter osw = null;
        ItemWriter writer = null;
        File toRead = null;

        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter(osName);
            writer = new ObjectStoreItemWriter(osw);
            FullXmlConverter converter = new FullXmlConverter(writer);
            System.err .println("Processing file " + xmlFile);
            if (xmlRes != null) {
                converter.process(new BufferedReader(new InputStreamReader(getClass()
                                .getClassLoader().getResourceAsStream(xmlRes))));
            } else {
                converter.process(new BufferedReader(new FileReader(xmlFile)));
            }
        } catch (Exception e) {
            if (xmlFile == null) {
                throw new BuildException("Exception in FullXmlConverterTask while reading from "
                        + xmlRes, e);
            } else {
                throw new BuildException("Exception in FullXmlConverterTask while reading from "
                        + xmlFile, e);
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (osw != null) {
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
