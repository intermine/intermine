package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

/**long
 * Class representing the track
 * @author dbutano
 */
public abstract class TrackAbstract implements Track
{
    private static final Logger LOG = Logger.getLogger(TrackAbstract.class);

    protected String userName;
    protected String sessionIdentifier;
    protected Timestamp timestamp;

    @Override
    public void store(Connection con) {
        String sql = "";
        Statement stm = null;
        StringBuffer valuesBuffer = new StringBuffer();
        Object[] values = getFormattedTrack();
        for (int index = 0; index < values.length; index++) {
            valuesBuffer = valuesBuffer.append("'" + values[index] + "',");
        }
        valuesBuffer = valuesBuffer.deleteCharAt(valuesBuffer.length() - 1);
        try {
            stm = con.createStatement();
            sql = "INSERT INTO " + getTableName()
                + " VALUES ( " + valuesBuffer + ")";
            stm.executeUpdate(sql);
        } catch (SQLException sqe) {
            LOG.error("Problem executing the statement: " + sql, sqe);
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    LOG.error("Problem closing  resources in TrackAbstract()", e);
                }
            }
        }
    }

    /**
     * Return the timestamp of the event
     * @return Timestamp the timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Return the user name
     * @return String user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Return the session id
     * @return String session id
     */
    public String getSessionIdentifier() {
        return sessionIdentifier;
    }
}
