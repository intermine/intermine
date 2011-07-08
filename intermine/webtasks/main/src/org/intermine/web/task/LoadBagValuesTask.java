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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;

/**
 * Task to load bagvalues table in the userprofile database.
 * If in the table savedbag, the column 'intermine_current' doesn't exist , add it.
 *
 * @author dbutano
 */

public class LoadBagValuesTask extends Task
{
    private String osAlias;
    private String userProfileAlias;

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
     * Execute the task - load bagvalues table.
     * @throws BuildException if there is a problem while
     */
    public void execute() {
        ObjectStore os = null;
        ObjectStore uos = null;
        ObjectStoreWriter uosw = null;
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
            uos = ObjectStoreFactory.getObjectStore(userProfileAlias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }
        if (uos instanceof ObjectStoreInterMineImpl) {
            Connection conn = null;
            Database db = ((ObjectStoreInterMineImpl) uos).getDatabase();
            try {
                conn = ((ObjectStoreInterMineImpl) uos).getConnection();
                if (!DatabaseUtil.columnExists(conn, "savedbag", "intermine_current")) {
                    DatabaseUtil.addColumn(db, "savedbag", "intermine_current", DatabaseUtil.Type.boolean_type);
                    DatabaseUtil.updateColumnValue(db, "savedbag", "intermine_current", Boolean.TRUE);
                }
            } catch (SQLException sqle) {
                throw new BuildException("Problems connecting bagvalues table", sqle);
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException sqle) {
                }
            }
        }
        try {
            uosw = uos.getNewWriter();
        } catch (ObjectStoreException ose) {
            throw new BuildException("Problems retrieving the new writer", ose);
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Results bags = uos.execute(q, 1000, false, false, true);
        for (Iterator i = bags.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            if (StringUtils.isBlank(savedBag.getName())) {
                System.out.println("Failed to load bag with blank name");
            } else {
                try {
                    InterMineBag bag = new InterMineBag(os, savedBag.getId(), uosw);
                    System.out.println("Start loading bag: " + bag.getName() + " - id: " + bag.getSavedBagId());
                    Properties classKeyProps = new Properties();
                    try {
                        classKeyProps.load(this.getClass().getClassLoader()
                                .getResourceAsStream("class_keys.properties"));
                    } catch (Exception e) {
                        System.out.println("Error loading class descriptions.");
                        e.printStackTrace();
                    }
                    Map<String, List<FieldDescriptor>>  classKeys =
                        ClassKeyHelper.readKeys(os.getModel(), classKeyProps);

                    List<String> keyFielNames = (List<String>) ClassKeyHelper.getKeyFieldNames(
                            classKeys, bag.getType());
                    bag.setKeyFieldNames(keyFielNames);
                    bag.addBagValues();
                    bag.setCurrent(true);
                    System.out.println("Loaded bag: " + bag.getName() + " - id: " + bag.getSavedBagId());
                } catch (UnknownBagTypeException e) {
                    System.out.println("Ignoring a bag '" + savedBag.getName() + " because type: "
                             + savedBag.getType() + " is not in the model.");
                    e.printStackTrace();
                } catch (ObjectStoreException ose) {
                    throw new BuildException("Exception while creating InterMineBag", ose);
                }
            }
        }
    }
}
