package org.intermine.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.metadata.InterMineModelParser;
import org.intermine.metadata.Model;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.util.PropertiesUtil;

/**
 * Retrieve the model metadata from a database
 * @author Mark Woodbridge
 */
public class RetrieveMetadataTask extends Task
{
    private static final String FAILURE_MSG =
            "Failed to retrieve metadata from %s (%s) - maybe you need to run build-db?\n";
    protected File destDir;
    protected String database;
    protected String osname;
    private String keyToRetrieve;

    /**
     * Sets the destination directory
     * @param destDir the destination directory
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Sets the os alias
     * @param osname the os alias
     */
    public void setOsName(String osname) {
        this.osname = osname;
        this.database = PropertiesUtil.getProperties().getProperty(osname + ".db");
    }

    /**
     * Set a key to retrieve from database.  If not set, retrieve all property files that were
     * stored when the database was created.
     * @param keyToRetrieve the key
     */
    public void setKeyToRetreive(String keyToRetrieve) {
        this.keyToRetrieve = keyToRetrieve;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (destDir == null) {
            throw new BuildException("destDir attribute is not set");
        }
        if (osname == null) {
            throw new BuildException("osname attribute is not set");
        }
        if (database == null) {
            throw new BuildException("couldn't find database property: " + osname + ".db - "
                                     + "osName property is: " + osname);
        }
        PrintStream err = System.err;
        Database db;
        try {
            db = DatabaseFactory.getDatabase(database);
        } catch (SQLException e) {
            throw new BuildException("Could not connect to " + database, e);
        } catch (ClassNotFoundException e) {
            throw new BuildException("Configuration error.", e);
        }

        try {

            if (keyToRetrieve == null) {

                String modelXml = MetadataManager.retrieve(db, MetadataManager.MODEL);
                String keyDefs = MetadataManager.retrieve(db, MetadataManager.KEY_DEFINITIONS);
                // String classDescs =
                //   MetadataManager.retrieve(db, MetadataManager.CLASS_DESCRIPTIONS);

                Model model = new InterMineModelParser().process(new StringReader(modelXml));
                File localModel = new File(destDir,
                        MetadataManager.getFilename(MetadataManager.MODEL, model.getName()));

                if (keyDefs != null) {
                    MetadataManager.saveKeyDefinitions(keyDefs, destDir, model.getName());
                }

                /*if (classKeys != null) {
                    MetadataManager.saveClassKeys(classKeys, destDir);
                }*/

                if (localModel.exists()
                    && IOUtils.contentEquals(new FileReader(localModel),
                                             new StringReader(modelXml))) {
                    System.err .println("Model in database is identical to local model.");
                    return;
                }

                MetadataManager.saveModel(model, destDir);

            } else {
                String objectStoreSummary =
                    MetadataManager.retrieve(db, MetadataManager.OS_SUMMARY);
                MetadataManager.saveProperties(objectStoreSummary, destDir,
                                               "objectstoresummary.properties");
            }

            //MetadataManager.saveClassDescriptions(classDescs, destDir, model.getName());
        } catch (Exception e) {
            err.printf(FAILURE_MSG, database, db.getName());
            throw new BuildException(e);
        }
    }
}
