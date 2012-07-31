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
import java.util.Random;

import org.intermine.metadata.Model;
import org.intermine.modelproduction.MetadataManager;
import static org.intermine.objectstore.intermine.TorqueModelOutput.FORMAT_VERSION;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.util.PropertiesUtil;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Store model metadata to a database
 * @author Kim Rutherford
 */
public class StoreMetadataTask extends Task
{
    protected String modelName;
    protected String database;
    protected String osName;

    /**
     * Sets the model name
     *
     * @param modelName the model name
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * Sets the database alias
     * @param osname the database alias
     */
    public void setOsName(String osname) {
        this.database = PropertiesUtil.getProperties().getProperty(osname + ".db");
        this.osName = osname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (modelName == null) {
            throw new BuildException("modelName attribute is not set");
        }
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        try {
            Database db = DatabaseFactory.getDatabase(database);

            Model model = MetadataManager.loadModel(modelName);
            MetadataManager.store(db, MetadataManager.MODEL, model.toString());

            Properties keys = MetadataManager.loadKeyDefinitions(modelName);
            if (keys == null) {
                throw new BuildException("no keys for " + modelName
                                         + " model found to store in the ObjectStore");
            }
            MetadataManager.store(db, MetadataManager.KEY_DEFINITIONS,
                                  PropertiesUtil.serialize(keys));

            /*Properties classKeys =
                MetadataManager.loadClassKeyDefinitions();
            MetadataManager.store(db, MetadataManager.CLASS_KEYS,
                                  PropertiesUtil.serialize(classKeys));*/

            /*Properties descriptions = MetadataManager.loadClassDescriptions(modelName);
            if (descriptions != null) {
                MetadataManager.store(db, MetadataManager.CLASS_DESCRIPTIONS,
                                      PropertiesUtil.serialize(descriptions));
            }*/

            Properties props = PropertiesUtil.getPropertiesStartingWith(osName);
            props = PropertiesUtil.stripStart(osName, props);

            String missingTablesString = props.getProperty("missingTables");
            String truncatedClassesString = props.getProperty("truncatedClasses");
            String noNotXmlString = props.getProperty("noNotXml");

            boolean noNotXml = false;
            if ("true".equals(noNotXmlString) || (noNotXmlString == null)) {
                noNotXml = true;
            } else if ("false".equals(noNotXmlString)) {
                noNotXml = false;
            } else {
                throw new BuildException("Invalid value for property noNotXml: "
                        + noNotXmlString);
            }

            MetadataManager.store(db, MetadataManager.OS_FORMAT_VERSION, "" + FORMAT_VERSION);
            if (truncatedClassesString != null) {
                MetadataManager.store(db, MetadataManager.TRUNCATED_CLASSES,
                        truncatedClassesString);
            }
            if (missingTablesString != null) {
                MetadataManager.store(db, MetadataManager.MISSING_TABLES, missingTablesString);
            }
            MetadataManager.store(db, MetadataManager.NO_NOTXML, "" + noNotXml);
            MetadataManager.store(db, MetadataManager.SERIAL_NUMBER, Long.toString(new Random().nextLong()));
        } catch (Exception e) {
            if (e instanceof BuildException) {
                throw (BuildException) e;
            } else {
                throw new BuildException(e);
            }
        }
    }
}
