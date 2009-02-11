package org.intermine.web.task;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;
import org.intermine.util.PropertiesUtil;
import org.intermine.web.ProfileManagerBinding;

/**
 * Sets userprofile field in intermine_metadata table in userprofile database.
 * 
 * @author Jakub Kulaviak
 */
public class SetUserProfileVersionTask extends Task {

    protected String database;
        
    /**
     * Sets the database alias
     * @param osname the database alias
     */
    public void setOsName(String osname) {
        this.database = PropertiesUtil.getProperties().getProperty(osname + ".db");
    }
    
    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        if (database == null) {
            throw new BuildException("database attribute is not set");
        }
        try {
            Database db = DatabaseFactory.getDatabase(database);
            MetadataManager.store(db, MetadataManager.PROFILE_FORMAT_VERSION,
                    "" + ProfileManagerBinding.ZERO_PROFILE_VERSION);
        } catch (Exception e) {
            if (e instanceof BuildException) {
                throw (BuildException) e;
            } else {
                throw new BuildException(e);
            }
        }
    }
}
