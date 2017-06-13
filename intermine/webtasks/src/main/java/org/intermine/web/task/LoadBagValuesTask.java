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

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.BagState;
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
 * If in the table savedbag, the column 'intermine_state' doesn't exist , add it.
 *
 * @author dbutano
 */

public class LoadBagValuesTask extends Task
{
    private String osAlias;
    private String userProfileAlias;
    private ObjectStore uos = null;
    private ObjectStore os = null;

    private static final Logger LOG = Logger.getLogger(LoadBagValuesTask.class);

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
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
            uos = ObjectStoreFactory.getObjectStore(userProfileAlias);
        } catch (Exception e) {
            throw new BuildException("Exception while creating ObjectStore", e);
        }
        updateUserProfileDatabase();
        ObjectStoreWriter uosw = null;
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
        if (bags.size() == 0) {
            log("There are no users's saved list.");
            return;
        }
        if (!verifyProductionDatabase(bags)) {
            log("The task will not be executed. Verify to use the same production database that"
                    + " created the users's saved lists.");
            return;
        }

        //start loading bagvalues
        for (Iterator<?> i = bags.iterator(); i.hasNext();) {
            @SuppressWarnings("rawtypes")
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            if (StringUtils.isBlank(savedBag.getName())) {
                log("Failed to load bag with blank name");
            } else {
                try {
                    InterMineBag bag = new InterMineBag(os, savedBag.getId(), uosw);
                    log("Start loading bag: " + bag.getName() + " - id: "
                            + bag.getSavedBagId());
                    Properties classKeyProps = new Properties();
                    try {
                        classKeyProps.load(this.getClass().getClassLoader()
                                .getResourceAsStream("class_keys.properties"));
                    } catch (Exception e) {
                        log("Error loading class descriptions.");
                        e.printStackTrace();
                    }

                    Map<String, List<FieldDescriptor>>  classKeys =
                        ClassKeyHelper.readKeys(os.getModel(), classKeyProps);

                    List<String> keyFielNames = (List<String>) ClassKeyHelper.getKeyFieldNames(
                            classKeys, bag.getType());
                    bag.setKeyFieldNames(keyFielNames);
                    bag.addBagValues();
                    log("Loaded bag: " + bag.getName() + " - id: "
                            + bag.getSavedBagId());
                } catch (UnknownBagTypeException e) {
                    log("Ignoring a bag because type: is not in the model.");
                    e.printStackTrace();
                } catch (ObjectStoreException ose) {
                    throw new BuildException("Exception while creating InterMineBag", ose);
                }
            }
        }
        try {
            uosw.close();
        } catch (ObjectStoreException ose) {
            throw new BuildException("Problems closing the writer", ose);
        }
    }

    private void updateUserProfileDatabase() {
        if (uos instanceof ObjectStoreInterMineImpl) {
            Connection conn = null;
            PrintStream out = System.out;
            Database db = ((ObjectStoreInterMineImpl) uos).getDatabase();
            try {
                conn = ((ObjectStoreInterMineImpl) uos).getConnection();

                if (!DatabaseUtil.columnExists(conn, "savedbag", "intermine_state")
                    && DatabaseUtil.columnExists(conn, "savedbag", "intermine_current")) {
                    out.println("You must not execute the task load-bagvalues-table."
                            + " Run the task update-savedbag-table task.");
                    return;
                }
                if (!DatabaseUtil.columnExists(conn, "savedbag", "intermine_state")) {
                    DatabaseUtil.addColumn(db, "savedbag", "intermine_state",
                            DatabaseUtil.Type.text);
                    DatabaseUtil.updateColumnValue(db, "savedbag", "intermine_state",
                            BagState.CURRENT.toString());
                }

                if (!DatabaseUtil.tableExists(conn, "bagvalues")) {
                    DatabaseUtil.createBagValuesTables(conn);
                }

            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw new BuildException("Problems creating bagvalues table", sqle);
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException sqle) {
                }
            }
        }
    }

    private boolean verifyProductionDatabase(Results bags) {
        //verify that we are pointing out the production database
        //that created the users's saved lists.
        //select osbid from savedbag
        int totalBags = bags.size();
        int bagsMatching = 0;
        StringBuffer osbids = new StringBuffer();
        for (Iterator<?> i = bags.iterator(); i.hasNext();) {
            @SuppressWarnings("rawtypes")
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            osbids.append(savedBag.getOsbId() + ",");
        }
        LOG.info("BAGVAL - userprofile osbids:" + totalBags);
        LOG.info("BAGVAL - userprofile ids:" + osbids);
        if (!"".equals(osbids)) {
            osbids.deleteCharAt(osbids.length() - 1);
            Connection conn = null;
            try {
                conn = ((ObjectStoreInterMineImpl) os).getDatabase().getConnection();
                String sqlCountBagsMatching =
                        "SELECT COUNT(DISTINCT bagid)"
                        + " FROM osbag_int"
                        + " WHERE bagid IN (" + osbids + ")";
                ResultSet result = conn.createStatement().executeQuery(sqlCountBagsMatching);
                result.next();
                bagsMatching = result.getInt(1);
                LOG.info("BAGVAL - found in production: " + bagsMatching);
                LOG.info("BAGVAL - bagsMatching / (float) totalBags = "
                        + bagsMatching / (float) totalBags);
                if (bagsMatching / (float) totalBags < 0.8) {
                    return false;
                }
                return true;
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw new BuildException("Exception while connecting ", sqle);
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException sqle) {
                }
            }
        } else {
            return true;
        }
    }
}
