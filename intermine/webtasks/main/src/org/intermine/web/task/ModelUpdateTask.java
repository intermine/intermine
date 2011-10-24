package org.intermine.web.task;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.profile.ModelUpdate;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.pathquery.PathException;

public class ModelUpdateTask extends Task {
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

    public void execute() {
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
            uosw = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }
        ModelUpdate modelUpdate = new ModelUpdate(os, uosw, oldModelLocation);
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
