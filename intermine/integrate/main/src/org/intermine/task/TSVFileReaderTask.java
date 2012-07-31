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

import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.FormattedTextParser;
import org.intermine.util.TypeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import org.apache.tools.ant.BuildException;

/**
 * Read a file of tab separated values.  Use one column as the key to look up objects and use the
 * other columns to set fields in that object.
 *
 * @author Kim Rutherford
 */

public class TSVFileReaderTask extends FileDirectDataLoaderTask
{
    private File configurationFile = null;

    /**
     * Set the configuration file to use.
     * @param configurationFile the configuration File
     */
    public void setConfigurationFile(File configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Query all objects of the class given by the className specified in the configurationFile.
     * Set fields in the objects by using the tab separated files as input.
     * @param file the File to process
     * @throws BuildException if an ObjectStore method fails
     */
    public void processFile(File file) {
        if (configurationFile == null) {
            throw new BuildException("configurationFile not set");
        }

        try {
            Model model = getDirectDataLoader().getIntegrationWriter().getModel();

            DelimitedFileConfiguration dfc;

            try {
                dfc = new DelimitedFileConfiguration(model, new FileInputStream(configurationFile));
            } catch (Exception e) {
                throw new BuildException("unable to read configuration for "
                                         + this.getClass().getName(), e);
            }

            executeInternal(dfc, file);
        } catch (ObjectStoreException e) {
            throw new BuildException("ObjectStore problem while processing: " + file);
        }
    }

    /**
     * Does most of the work of execute().  This method exists to help with testing.
     * @param dfc the configuration of which fields to set and which field to use as a key
     * @param file The file to read from
     * @throws BuildException if an ObjectStore method fails
     */
    void executeInternal(DelimitedFileConfiguration dfc, File file) {
        String className = dfc.getConfigClassDescriptor().getName();

        System.err .println("Processing file: " + file.getName());

        Iterator tsvIter;
        try {
            tsvIter = FormattedTextParser.parseTabDelimitedReader(new FileReader(file));
        } catch (Exception e) {
            throw new BuildException("cannot parse file: " + file, e);
        }

        List fieldClasses = dfc.getColumnFieldClasses();

        while (tsvIter.hasNext()) {
            String[] thisRow = (String[]) tsvIter.next();

            InterMineObject o;
            try {
                o = getDirectDataLoader().createObject(className);
            } catch (ClassNotFoundException e) {
                throw new BuildException("cannot find class while reading: " + file, e);
            } catch (ObjectStoreException e) {
                throw new BuildException("exception while creating object of type: "
                                         + className, e);
            }

            for (int columnIndex = 0; columnIndex < thisRow.length; columnIndex++) {
                if (dfc.getColumnFieldDescriptors().size() <= columnIndex) {
                    // ignore - no configuration for this column
                    continue;
                }

                AttributeDescriptor columnAD =
                    (AttributeDescriptor) dfc.getColumnFieldDescriptors().get(columnIndex);

                if (columnAD == null) {
                    // ignore - no configuration for this column
                } else {
                    String rowValue = thisRow[columnIndex].trim();
                    if (rowValue.length() > 0) {
                        Class fieldClass = (Class) fieldClasses.get(columnIndex);
                        Object typedObject = TypeUtil.stringToObject(fieldClass, rowValue);
                        o.setFieldValue(columnAD.getName(), typedObject);
                    }
                }
            }

            try {
                getDirectDataLoader().store(o);
            } catch (ObjectStoreException e) {
                throw new BuildException("exception while storing: " + o, e);
            }
        }
    }
}
