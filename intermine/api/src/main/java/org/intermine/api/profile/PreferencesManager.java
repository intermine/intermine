package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.intermine.SQLOperation;
import org.intermine.sql.DatabaseUtil;

/**
 * Class encapsulating the logic for reading user preferences in and out of the DB.
 *
 * @author Alex Kalderimis
 */
public class PreferencesManager
{

    private static final Logger LOG = Logger.getLogger(PreferencesManager.class);
    private final ObjectStoreWriterInterMineImpl osw;

    private static final String TABLE_NAME = "userpreferences";
    private static final String TABLE_DEFINITION =
            "CREATE TABLE " + TABLE_NAME + " ("
            + "userprofileid integer NOT NULL, "
            + "preferencename text NOT NULL, "
            + "preferencevalue text, "
            + "PRIMARY KEY (userprofileid, preferencename))";

    private static final String FETCH_PREFS_FOR_USER =
            "SELECT preferencename, preferencevalue "
                    + " FROM " + TABLE_NAME
                    + " WHERE userprofileid = ?";

    /**
     * Constructor
     *
     * @param osw objectstore writer
     */
    protected PreferencesManager(ObjectStoreWriter osw) {
        try {
            this.osw = (ObjectStoreWriterInterMineImpl) osw;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("This is not a suitable osw");
        }
        try {
            checkTables();
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialise DB", e);
        }
    }

    private void checkTables() throws SQLException {
        osw.performUnsafeOperation(TABLE_DEFINITION, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                if (!DatabaseUtil.tableExists(stm.getConnection(), TABLE_NAME)) {
                    LOG.info("Creating preferences table");
                    stm.execute();
                }
                return null;
            }
        });
    }

    /**
     * @param profile userprofile
     * @return user preferences
     * @throws SQLException if userprofile is unavailable
     */
    protected Map<String, String> getPreferences(final Profile profile) throws SQLException {
        return osw.performUnsafeOperation(FETCH_PREFS_FOR_USER,
                new SQLOperation<Map<String, String>>() {
                @Override
                public Map<String, String> run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, profile.getUserId());
                    ResultSet rs = stm.executeQuery();
                    Map<String, String> ret = new HashMap<String, String>();
                    while (rs.next()) {
                        ret.put(rs.getString(1), rs.getString(2));
                    }
                    return ret;
                }
            });
    }

    private static final String UPDATE_PREFERENCE_SQL = "UPDATE " + TABLE_NAME
            + " SET preferencevalue = ? WHERE userprofileid = ? AND preferencename = ?";

    private static final String INSERT_PREFERENCE_SQL = "INSERT INTO " + TABLE_NAME
            + " VALUES (?, ?, ?)";

    /**
     * @param profile userprofile
     * @param key key
     * @param value value
     * @throws SQLException if something goes wrong
     */
    protected synchronized void setPreference(final Profile profile, final String key,
            final String value) throws SQLException {
        Integer updated = osw.performUnsafeOperation(UPDATE_PREFERENCE_SQL,
                new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setString(1, value);
                    stm.setInt(2, profile.getUserId());
                    stm.setString(3, key);
                    return stm.executeUpdate();
                }
            });

        if (updated < 1) { // WHERE clause didn't match. Do insert.
            Integer inserted = osw.performUnsafeOperation(INSERT_PREFERENCE_SQL,
                    new SQLOperation<Integer>() {
                    @Override
                    public Integer run(PreparedStatement stm) throws SQLException {
                        stm.setInt(1, profile.getUserId());
                        stm.setString(2, key);
                        stm.setString(3, value);
                        return stm.executeUpdate();
                    }
                });
            if (inserted != 1) {
                throw new SQLException("Expected to insert one row, but actually inserted "
                        + inserted);
            }
        }
    }

    private static final String DELETE_PREFERENCE_SQL =
        "DELETE FROM " + TABLE_NAME + " WHERE userprofileid = ? AND preferencename = ?";

    /**
     * @param profile userprofile
     * @param key key
     * @throws SQLException if something goes wrong
     */
    protected void deletePreference(final Profile profile, final String key)
        throws SQLException {
        osw.performUnsafeOperation(DELETE_PREFERENCE_SQL, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                stm.setInt(1, profile.getUserId());
                stm.setString(2, key);
                stm.executeUpdate();
                return null;
            }
        });
    }

    private static final String DELETE_ALL_PREFERENCES_SQL =
            "DELETE FROM " + TABLE_NAME + " WHERE userprofileid = ?";

    /**
     * @param profile userprofile
     * @throws SQLException if something goes wrong
     */
    public void deleteAllPreferences(final Profile profile) throws SQLException {
        osw.performUnsafeOperation(DELETE_ALL_PREFERENCES_SQL, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                stm.setInt(1, profile.getUserId());
                stm.executeUpdate();
                return null;
            }
        });
    }

    private static final String FIND_MAPPING_SQL =
            "SELECT COUNT(*) FROM " + TABLE_NAME
            + " WHERE preferencename = ? AND preferencevalue = ?";

    /**
     * @param key key
     * @param value value
     * @return true if mapping exists
     * @throws SQLException if something goes wrong
     */
    public synchronized boolean mappingExists(final String key, final String value)
        throws SQLException {
        return osw.performUnsafeOperation(FIND_MAPPING_SQL, new SQLOperation<Boolean>() {
            @Override
            public Boolean run(PreparedStatement stm) throws SQLException {
                stm.setString(1, key);
                stm.setString(2, value);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return Boolean.FALSE;
            }
        });
    }

    private static final String FIND_USER_WITH_MAPPING =
            "SELECT userprofileid FROM " + TABLE_NAME
            + " WHERE preferencename = ? AND preferencevalue = ?";

    /**
     * @param key key
     * @param value value
     * @return user id
     * @throws SQLException something goes wrong
     */
    public Integer getUserWithUniqueMapping(final String key, final String value)
        throws SQLException {
        return osw.performUnsafeOperation(FIND_USER_WITH_MAPPING, new SQLOperation<Integer>() {
            @Override
            public Integer run(PreparedStatement stm) throws SQLException {
                stm.setString(1, key);
                stm.setString(2, value);
                ResultSet rs = stm.executeQuery();
                Set<Integer> matches = new HashSet<Integer>();
                while (rs.next()) {
                    matches.add(rs.getInt(1));
                }
                if (matches.isEmpty()) {
                    return null;
                } else if (matches.size() > 1) {
                    throw new DuplicateMappingException(key, value);
                } else {
                    return matches.iterator().next();
                }
            }
        });
    }

}
