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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.DatabaseUtil;

/**
 * Task to modify the type of the column 'timestamp' in the templatetrack table in the userprofile database.
 * (from bigint to timestamp)
 * @author dbutano
 */

public class UpdateTrackTableTask extends Task
{
    private String userProfileAlias;

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
            if (!DatabaseUtil.verifyColumnType(connection, TrackerUtil.TEMPLATE_TRACKER_TABLE,
                                              "timestamp", Types.TIMESTAMP)) {
                stm = connection.createStatement();
                String sql1 = "ALTER TABLE templatetrack ADD COLUMN timestamp_backup bigint";
                stm.executeUpdate(sql1);
                String sql2 = "UPDATE templatetrack SET timestamp_backup=timestamp, timestamp=null";
                stm.executeUpdate(sql2);
                String sql3 = "ALTER TABLE templatetrack DROP COLUMN timestamp";
                stm.executeUpdate(sql3);
                String sql4 = "ALTER TABLE templatetrack ADD COLUMN timestamp timestamp";
                stm.executeUpdate(sql4);
                String sql5 = "SELECT timestamp_backup FROM templatetrack";
                ResultSet rs = stm.executeQuery(sql5);
                List<Long> timestampList = new ArrayList<Long>();
                while (rs.next()) {
                    timestampList.add(rs.getLong(1));
                }
                for (Long timestamplong : timestampList) {
                    Timestamp timestamp = new Timestamp(timestamplong);
                    String sql6 = "UPDATE templatetrack SET timestamp='" + timestamp.toString()
                        + "' WHERE timestamp_backup=" + timestamplong;
                    stm.executeUpdate(sql6);
                }
                String sql7 = "UPDATE templatetrack SET timestamp_backup=null";
                stm.executeUpdate(sql7);
                String sql8 = "ALTER TABLE templatetrack DROP COLUMN timestamp_backup";
                stm.executeUpdate(sql8);
            }
            String[] tablesToVerify = {TrackerUtil.LIST_TRACKER_TABLE,
                TrackerUtil.LOGIN_TRACKER_TABLE, TrackerUtil.QUERY_TRACKER_TABLE,
                TrackerUtil.SEARCH_TRACKER_TABLE};

            for (String tableToVerify : tablesToVerify) {
                if (!DatabaseUtil.verifyColumnType(connection, tableToVerify, "timestamp", Types.TIMESTAMP)) {
                    String sql = "DROP TABLE " + tableToVerify;
                    if (stm == null) {
                        stm = connection.createStatement();
                    }
                    stm.executeUpdate(sql);
                }
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
