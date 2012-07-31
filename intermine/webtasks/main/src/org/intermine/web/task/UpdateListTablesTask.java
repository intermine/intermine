package org.intermine.web.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.BagState;
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
import org.intermine.sql.DatabaseUtil;

/**
 * Task that rename the column 'intermine_current' into 'intermine_state' in savedbag table
 * (from boolean to text)
 * and add the column 'extra' (and its value) in the bagvalues table
 * @author dbutano
 */

public class UpdateListTablesTask extends Task
{
    private String osAlias;
    private String userProfileAlias;
    private static final String SAVEDBAG_TABLE = "savedbag";
    ObjectStore os;
    ObjectStore userProfileOS;


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
     * Execute the task.
     * @throws BuildException if there is a problem while reading from the file or writing to the
     * profiles.
     */
    public void execute() {

        if (userProfileAlias == null) {
            throw new BuildException("userProfileAlias parameter not set");
        }
        Connection connection = null;
        try {
            userProfileOS = ObjectStoreFactory.getObjectStore(userProfileAlias);
            os = ObjectStoreFactory.getObjectStore(osAlias);
            connection = ((ObjectStoreInterMineImpl) userProfileOS).getDatabase().getConnection();

            //rename the column intermine_current -> intermine_state
            if (!DatabaseUtil.columnExists(connection, SAVEDBAG_TABLE, "intermine_state")
                && DatabaseUtil.columnExists(connection, SAVEDBAG_TABLE, "intermine_current")) {
                log("Start replacing intermine_current with intermine_state in savedbag table.");
                addIntermineStateColumn(connection);
                log("Replacing successfully.");
            }
            //add the column extra and set the value
            if (DatabaseUtil.tableExists(connection, "bagvalues")
                && !DatabaseUtil.columnExists(connection, "bagvalues", "extra")) {
                DatabaseUtil.addColumn(connection, "bagvalues", "extra", DatabaseUtil.Type.text);
                String sqlDeleteIndex = "DROP INDEX bagvalues_index1";
                try {
                    connection.createStatement().execute(sqlDeleteIndex);
                } catch (SQLException sql) {
                }
                String sqlCreateIndex = "CREATE UNIQUE INDEX bagvalues_index1 ON bagvalues"
                                        + " (savedbagid, value, extra)";
                connection.createStatement().execute(sqlCreateIndex);
                log("Added column extra in bagvalues table.");
                setExtraValue();
            }
        } catch (ObjectStoreException ose) {
            ose.printStackTrace();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ((ObjectStoreInterMineImpl) userProfileOS).releaseConnection(connection);
        }
    }

    private void addIntermineStateColumn(Connection connection) throws SQLException {
        Statement stm = connection.createStatement();
        String sql1 = "ALTER TABLE savedbag ADD COLUMN intermine_state text";
        stm.executeUpdate(sql1);
        String sql2 = "SELECT id, intermine_current FROM savedbag";
        ResultSet rs = stm.executeQuery(sql2);
        Map<Integer, Boolean> intermineCurrentMap = new HashMap();
        while (rs.next()) {
            intermineCurrentMap.put(rs.getInt(1), rs.getBoolean(2));
        }
        Boolean intermineCurrentBoolean;
        String intermineCurrent = "";
        for (Entry<Integer, Boolean> entry : intermineCurrentMap.entrySet()) {
            intermineCurrentBoolean = entry.getValue();
            intermineCurrent = (intermineCurrentBoolean)
                               ? BagState.CURRENT.toString()
                               : BagState.NOT_CURRENT.toString();
            String sql3 = "UPDATE savedbag SET intermine_state='" + intermineCurrent
                + "' WHERE id=" + entry.getKey();
            stm.executeUpdate(sql3);
        }
        String sql4 = "ALTER TABLE savedbag ALTER intermine_current DROP NOT NULL;";
        stm.execute(sql4);
        String sql5 = "UPDATE savedbag SET intermine_current=null";
        stm.executeUpdate(sql5);
        String sql6 = "ALTER TABLE savedbag DROP COLUMN intermine_current";
        stm.executeUpdate(sql6);
    }

    private void setExtraValue() {
        Query q = new Query();
        QueryClass qc = new QueryClass(SavedBag.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Results bags = userProfileOS.execute(q, 1000, false, false, true);
        ObjectStoreWriter osw = null;
        try {
            osw = userProfileOS.getNewWriter();
        } catch (ObjectStoreException ose) {
            throw new BuildException("Problems retrieving the new writer", ose);
        }
        for (Iterator i = bags.iterator(); i.hasNext();) {
            ResultsRow row = (ResultsRow) i.next();
            SavedBag savedBag = (SavedBag) row.get(0);
            if (StringUtils.isBlank(savedBag.getName())) {
                log("Failed to load bag with blank name");
            } else {
                try {
                    InterMineBag bag = new InterMineBag(os, savedBag.getId(), osw);
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

                    if (bag.isCurrent()) {
                        log("Start setting extra values for list:" + bag.getName());
                        bag.deleteAllBagValues();
                        bag.addBagValues();
                        log("Extra values set.");
                    }
                } catch (ObjectStoreException ose) {
                    throw new BuildException("Exception while creating InterMineBag", ose);
                } catch (UnknownBagTypeException e) {
                    log("Skipping invalid bag '" + savedBag.getName() + "'");
                }
            }
        }
        if (osw != null) {
            try {
                osw.close();
            } catch (ObjectStoreException ose) {
                ose.printStackTrace();
            }
        }
    }
}
