package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.util.PropertiesUtil;
import org.apache.tools.ant.BuildException;
import org.intermine.postprocess.PostProcessor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Summarise an ObjectStore into a particular properties file.  The summary contains:
 * <p> counts of the number of objects of each class
 * <p> for class fields that have a small number of value, a list of those values.
 * @author Kim Rutherford
 */
public class SummariseObjectstoreProcess extends PostProcessor
{
    /**
     * Create a new instance
     *
     * @param osw object store writer
     */
    public SummariseObjectstoreProcess(ObjectStoreWriter osw) {
        super(osw);
    }

    /**
     * {@inheritDoc}
     */
    public void postProcess()
            throws ObjectStoreException {
        System.out .println("summarising objectstore ...");
        ObjectStore os = osw.getObjectStore();
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream(
                "objectstoresummary.config.properties"));
        } catch (IOException e) {
            throw new BuildException("Could not open the class keys");
        } catch (NullPointerException e) {
            throw new BuildException("Could not find the class keys");
        }
        try {
            ObjectStoreSummary oss = new ObjectStoreSummary(os, props);
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            MetadataManager.store(db, MetadataManager.OS_SUMMARY,
                    PropertiesUtil.serialize(oss.toProperties()));
        } catch (ClassNotFoundException e) {
            throw new BuildException("Could not find the class keys" + e);
        } catch (IOException e) {
            throw new BuildException("Could not open the class keys");
        } catch (SQLException e) {
            throw new BuildException("Could not find the class keys " + e);
        }
    }
}
