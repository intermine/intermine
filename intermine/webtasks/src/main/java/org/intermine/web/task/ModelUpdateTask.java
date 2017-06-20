package org.intermine.web.task;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;


import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.profile.ModelUpdate;
import org.intermine.metadata.InterMineModelParser;
import org.intermine.metadata.Model;
import org.intermine.metadata.ModelParserException;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathException;

/**
 * Task updating savedquery, savedtemplatequery and savedbag in the userprofile (identified by
 * the userProfileAlias) when the model has been changed
 * @author butano
 */
public class ModelUpdateTask extends Task
{
    private String osAlias;
    private String userProfileAlias;
    private String oldModelLocation;
    private ObjectStore os = null;
    private ObjectStoreWriter uosw = null;

    /**
     * Set the alias of the main object store.
     * @param osAlias the object store alias
     */
    public void setOSAlias(String osAlias) {
        this.osAlias = osAlias;
    }

    /**
     * Set the alias of the userprofile object store.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Set the location  of the previous model.
     * @param oldModel the location
     */
    public void setOldModelLocation(String oldModel) {
        this.oldModelLocation = oldModel;
    }

    /**
     * Update savedquery, savedtemplatequery and savedbag in the userprofile
     * through ModelUpdate object
     */
    public void execute() {
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
            uosw = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }
        InterMineModelParser imModelParser = new InterMineModelParser();
        Model oldModel = null;
        try {
            Reader fileReader = new FileReader(oldModelLocation);
            oldModel = imModelParser.process(fileReader);
        } catch (FileNotFoundException fnfe) {
            throw new BuildException("File of the previous model not found ", fnfe);
        } catch (ModelParserException mpe) {
            throw new BuildException("Problems parsing the previous model ", mpe);
        }
        ModelUpdate modelUpdate = new ModelUpdate(os, uosw, oldModel);
        log("Read the updates in the modelUpdate.properties file");
        log("Classes deleted: " + modelUpdate.getDeletedClasses().toString());
        log("Classes renamed: " + modelUpdate.getRenamedClasses().toString());
        log("Fields renamed: " + modelUpdate.getRenamedFields().toString());
        log("Start updating...");
        try {
            modelUpdate.update();
        } catch (PathException pe) {
            throw new BuildException("Exception while updating", pe);
        }
    }

}
