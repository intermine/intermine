package org.intermine.api.tracker.track;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

/**
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
        PreparedStatement stm = null;
        StringBuffer valuesBuffer = new StringBuffer();
        Object[] values = getFormattedTrack();
        int valuesSize = values.length;
        for (int index = 0; index < valuesSize; index++) {
            valuesBuffer = valuesBuffer.append("?,");
        }
        valuesBuffer = valuesBuffer.deleteCharAt(valuesBuffer.length() - 1);
        try {
            sql = "INSERT INTO " + getTableName() + " VALUES(" + valuesBuffer + ")";
            stm = con.prepareStatement(sql);
            Object value = null;
            for (int index = 0; index < valuesSize; ) {
                value = values[index];
                if (value instanceof Integer) {
                    stm.setInt(++index, (Integer) value);
                } else if (value instanceof Timestamp) {
                    stm.setTimestamp(++index, (Timestamp) value);
                } else {
                    stm.setString(++index, value.toString());
                }
            }
            stm.executeUpdate();
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
