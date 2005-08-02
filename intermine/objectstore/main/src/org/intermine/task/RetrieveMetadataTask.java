package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.intermine.metadata.MetadataManager;
import org.intermine.metadata.Model;
import org.intermine.modelproduction.xml.InterMineModelParser;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

/**
 * Retrieve the model metadata from a database
 * @author Mark Woodbridge
 */
public class RetrieveMetadataTask extends Task
{
    protected File destDir;
    protected String database;

    /**
     * Sets the destination directory
     * @param destDir the destination directory
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }
    
    /**
     * Sets the database alias
     * @param database the database alias
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @see Task#execute
     */
    public void execute() throws BuildException {
        if (destDir == null) {
            throw new BuildException("destDir attribute is not set");
        }
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }

        try {
            Database db = DatabaseFactory.getDatabase(database);

            String modelXml = MetadataManager.retrieve(db, MetadataManager.MODEL);
            String keyDefs = MetadataManager.retrieve(db, MetadataManager.KEY_DEFINITIONS);
            //String classDescs = MetadataManager.retrieve(db, MetadataManager.CLASS_DESCRIPTIONS);
            
            Model model = new InterMineModelParser().process(new StringReader(modelXml));
            File localModel = new File(destDir,
                    MetadataManager.getFilename(MetadataManager.MODEL, model.getName()));
            
            if (localModel.exists()
                && IOUtils.contentEquals(new FileReader(localModel), new StringReader(modelXml))) {
                System.err .println("Model in database is identical to local model.");
                return;
            }
            
            MetadataManager.saveModel(model, destDir);
            MetadataManager.saveKeyDefinitions(keyDefs, destDir, model.getName());
            //MetadataManager.saveClassDescriptions(classDescs, destDir, model.getName());
        } catch (Exception e) {
            System.err .println("Failed to retrieve metadata - maybe you need to run build-db?");
            throw new BuildException(e);
        }
    }
}
