package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

import org.flymine.xml.full.FullRenderer;
import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.metadata.Model;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import org.apache.log4j.Logger;

/**
 * Initiates retrieval and conversion of Chado data
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ChadoRetrieverTask extends Task
{
    protected static final Logger LOG = Logger.getLogger(ChadoRetrieverTask.class);
    
    protected String database;
    protected String model;
    protected File destFile;

    /**
     * Set the database name
     * @param database the database name
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * Set the model name
     * @param model the model name
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Set the destination file
     * @param destFile the destination file
     */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }
    
    /**
     * Run the task
     * @throws BuildException if a problem occurs
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        if (model == null) {
            throw new BuildException("model attribute is not set");
        }
        if (destFile == null) {
            throw new BuildException("destFile attribute is not set");
        }

        BufferedWriter writer = null;
        try {
            Database db = DatabaseFactory.getDatabase(database);
            Model m = Model.getInstanceByName(model);
            
            writer = new BufferedWriter(new FileWriter (destFile));
            new ChadoConvertor(m, db).process(writer);
        } catch (Exception e) {            
            throw new BuildException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }
}        
