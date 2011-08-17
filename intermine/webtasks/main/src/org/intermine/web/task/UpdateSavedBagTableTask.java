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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.profile.BagState;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.DatabaseUtil;

/**
 * Task to rename the column 'intermine_current' into 'intermine_state' in savedbag table.
 * (from boolean to text)
 * @author dbutano
 */

public class UpdateSavedBagTableTask extends Task
{
    private String userProfileAlias;
    private static final String SAVEDBAG_TABLE = "savedbag";

    /**
     * Set the alias of the userprofile object store.
     * @param userProfileAlias the object store alias of the userprofile database
     */
    public void setUserProfileAlias(String userProfileAlias) {
        this.userProfileAlias = userProfileAlias;
    }

    /**
     * Execute the task - read the profiles.
     * @throws BuildException if there is a problem while reading from the file or writing to the
     * profiles.
     */
    public void execute() {

        if (userProfileAlias == null) {
            throw new BuildException("userProfileAlias parameter not set");
        }
        ObjectStoreWriter userProfileOS = null;
        Connection connection = null;
        try {
            userProfileOS =
                ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
            connection = ((ObjectStoreInterMineImpl) userProfileOS).getDatabase().getConnection();
            Statement stm = null;
            if (!DatabaseUtil.columnExists(connection, SAVEDBAG_TABLE, "intermine_state")
                && DatabaseUtil.columnExists(connection, SAVEDBAG_TABLE, "intermine_current")) {
                stm = connection.createStatement();
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
        } catch (ObjectStoreException ose) {
            ose.printStackTrace();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }  finally {
            ((ObjectStoreInterMineImpl) userProfileOS).releaseConnection(connection);
        }
    }
}
