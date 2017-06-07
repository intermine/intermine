package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.intermine.objectstore.intermine.TorqueModelOutput.FORMAT_VERSION;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;

/**
 * Creates and runs a ModelOutput process to generate java or config files.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */
public class TorqueModelOutputTask extends Task
{
    protected File destFile;
    protected DatabaseSchema schema;

    /**
     * Sets the file to which the data should be written.
     *
     * @param destFile the file location
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Set the ObjectStore for which to generate the data.
     *
     * @param osName the ObjectStore name to be used
     */
    public void setOsName(String osName) {
        try {
            Properties props = PropertiesUtil.getPropertiesStartingWith(osName);
            props = PropertiesUtil.stripStart(osName, props);

            String missingTablesString = props.getProperty("missingTables");
            String truncatedClassesString = props.getProperty("truncatedClasses");
            String noNotXmlString = props.getProperty("noNotXml");

            Model osModel;
            String modelName = props.getProperty("model");
            osModel = Model.getInstanceByName(modelName);
            List<ClassDescriptor> truncatedClasses = new ArrayList<ClassDescriptor>();
            if (truncatedClassesString != null) {
                String[] classes = truncatedClassesString.split(",");
                for (int i = 0; i < classes.length; i++) {
                    ClassDescriptor truncatedClassDescriptor =
                        osModel.getClassDescriptorByName(classes[i]);
                    if (truncatedClassDescriptor == null) {
                        throw new ObjectStoreException("Truncated class " + classes[i]
                                                       + " does not exist in the model");
                    }
                    truncatedClasses.add(truncatedClassDescriptor);
                }
            }
            boolean noNotXml = false;
            if ("true".equals(noNotXmlString) || (noNotXmlString == null)) {
                noNotXml = true;
            } else if ("false".equals(noNotXmlString)) {
                noNotXml = false;
            } else {
                throw new ObjectStoreException("Invalid value for property noNotXml: "
                        + noNotXmlString);
            }
            HashSet<String> missingTables = new HashSet<String>();
            if (missingTablesString != null) {
                String[] tables = missingTablesString.split(",");
                for (int i = 0; i < tables.length; i++) {
                    missingTables.add(tables[i].toLowerCase());
                }
            }

            schema = new DatabaseSchema(osModel, truncatedClasses, noNotXml, missingTables,
                    FORMAT_VERSION, false, false);
        } catch (ClassCastException e) {
            throw new BuildException("Objectstore " + osName
                    + " is not an ObjectStoreInterMineImpl", e);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw new BuildException(e);
        } catch (Error e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (this.destFile == null) {
            throw new BuildException("destFile attribute is not set");
        }
        if (this.schema == null) {
            throw new BuildException("osName attribute is not set");
        }

        TorqueModelOutput mo = new TorqueModelOutput(schema, destFile);

        mo.process();
    }
}
