package org.intermine.api.bag;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.DatabaseUtil;

/**
 * Singleton manager class for shared bags.
 * Implements retrieving, adding and deleting bag shared between users.
 * @author Daniela Butano
 */
public class SharedBagManager
{
    private static SharedBagManager sharedBagManager = null;
    public static final String SHARED_BAGS = "sharedbag";
    protected ObjectStore os;
    protected static ObjectStoreWriter uosw;
    private static final Logger LOG = Logger.getLogger(SharedBagManager.class);

    public static SharedBagManager getInstance(ProfileManager profileManager) {
        if (sharedBagManager == null) {
            sharedBagManager = new SharedBagManager(profileManager);
            Connection con = null;
            try {
                con = ((ObjectStoreWriterInterMineImpl) uosw).getDatabase()
                    .getConnection();
                if (!DatabaseUtil.tableExists(con, SHARED_BAGS)) {
                    con.createStatement().execute(getStatementCreatingTable());
                    con.createStatement().execute(getStatementCreatingIndex());
                }
            } catch (SQLException sqle) {
               LOG.error("Error trying to create the table " + SHARED_BAGS, sqle);
            }
        }
        return sharedBagManager;
    }
    /**
     * Constructor. Use TagManagerFactory for creating tag manager.
     * @param profileOsWriter user profile object store
     */
    private SharedBagManager(ProfileManager profileManager) {
        this.uosw = profileManager.getProfileObjectStoreWriter();
        this.os = profileManager.getProductionObjectStore(); 
    }

    /**
     * Return the sql query to create the table 'sharedbag'
     * @return the string containing the sql query
     */
    private static String getStatementCreatingTable() {
        return "CREATE TABLE " + SHARED_BAGS
             + "(bagid integer NOT NULL, userprofileid integer NOT NULL)";
    }

    /**
     * Return the sql query to create the index in the 'sharedbag'
     * @return the string containing the sql query
     */
    private static String getStatementCreatingIndex() {
        return "CREATE UNIQUE INDEX sharedbag_index1 ON " + SHARED_BAGS
                + "(bagid, userprofileid)";
    }

    /**
     * Return the userprofile given the username. 
     * @param userName
     * @return the userprofile
     */
    private UserProfile getUserProfile(String userName) {
        if (userName == null) {
            return null;
        }
        UserProfile profile = new UserProfile();
        profile.setUsername(userName);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("username");
        try {
            profile = (UserProfile) uosw.getObjectByExample(profile, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
        return profile;
    }

    private Integer getBagId(String bagName, int bagOwnerId) {
        UserProfile ownerUserProfile = new UserProfile();
        ownerUserProfile.setId(bagOwnerId);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("id");
        try {
            ownerUserProfile = (UserProfile) uosw.getObjectByExample(ownerUserProfile, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
        SavedBag bag = new SavedBag();
        bag.setName(bagName);
        bag.setUserProfile(ownerUserProfile);
        fieldNames = new HashSet<String>();
        fieldNames.add("name");
        fieldNames.add("userProfile");
        try {
            bag = (SavedBag) uosw.getObjectByExample(bag, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load the bag with name " + bagName, e);
        }
        return bag.getId();
    }

    /**
     * Return a map containing the bags that the user in input has access because shared by
     * someone else
     * @param profile
     * @return
     */
    public Map<String, InterMineBag> getSharedBags(Profile profile) {
        Map<String, InterMineBag> sharedBags = new HashMap<String, InterMineBag>();
        if ( getUserProfile(profile.getUsername()) == null) {
            return sharedBags;
        }
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        String clause = "userprofileid = ?";
        Integer bagId = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "SELECT bagid FROM " + SHARED_BAGS + " WHERE " + clause;
            stm = conn.prepareStatement(sql);
            stm.setInt(1, profile.getUserId());
            rs = stm.executeQuery();
            while (rs.next()) {
                bagId = rs.getInt("bagid");
                InterMineBag bag = new InterMineBag(os, bagId, uosw);
                sharedBags.put(bag.getName(), bag);
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("Error retrieving the shared bags "
                    + "for the user : " + profile.getUserId(), sqle);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving the bag "
                    + "with bagId : " + bagId, e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                    if (stm != null) {
                        stm.close();
                    }
                } catch (SQLException e) {
                        throw new RuntimeException("Problem closing resources", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
        return sharedBags;
    }

    /**
     * Return the users sharing the list given in input, not the owner
     */
    public List<String> getUsersSharingBag(String bagName, int bagOwnerId) {
        List<String> usersSharingBag = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "SELECT username FROM userprofile as u, sharedbag as sharebag, savedbag as sb "
                       + "WHERE u.id = sharebag.userprofileid AND sharebag.bagid = sb.id "
                       + "AND sb.name = '" + bagName + "'" + "AND sb.userprofileid = " + bagOwnerId;
            stm = conn.prepareStatement(sql);
            rs = stm.executeQuery();
            while (rs.next()) {
                usersSharingBag.add(rs.getString("username"));
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("Error retrieving the users sharing "
                    + "the bag : " + bagName, sqle);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving the users sharing "
                    + "the bag : " + bagName, e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                    if (stm != null) {
                        stm.close();
                    }
                } catch (SQLException e) {
                        throw new RuntimeException("Problem closing resources", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
        return usersSharingBag;
    }

    /**
     * Share the bag given in input with user which userName is given in input
     */
    public void shareBagWithUser(String bagName, int bagOwnerId, String userName)
        throws UserNotFoundException, BagDoesNotExistException {
        UserProfile userProfile = getUserProfile(userName);
        if (userProfile == null) {
            throw new UserNotFoundException("User " + userName + " doesn't exist");
        }
        Integer bagId = getBagId(bagName, bagOwnerId);
        if (bagId == null) {
            throw new BagDoesNotExistException("The bag with name " + bagName + "does not exist.");
        }
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String subSelect = "SELECT id FROM savedbag WHERE name='" + bagName + "'"
                             + " AND userprofileid = "+ bagOwnerId;
            String sql = "INSERT INTO " + SHARED_BAGS + " VALUES((" + subSelect + "), " +
                       + userProfile.getId() + ")";
            stm = conn.prepareStatement(sql);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new UserAlreadyShareBagException("Error sharing the "
                    + " the bag : " + bagId
                    + " with the user " + userProfile.getId(), sqle);
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Problem closing resources", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
    }

    /**
     * Delete the sharing between the user and the bag given in input
     */
    public void unshareBagWithUser(String bagName, int bagOwnerId, String userName)
        throws UserNotFoundException, BagDoesNotExistException {
        UserProfile userProfile = getUserProfile(userName);
        if (userProfile == null) {
            LOG.warn("User " + userName + " doesn't exist");
            return;
        }
        Integer bagId = getBagId(bagName, bagOwnerId);
        if (bagId == null) {
            LOG.warn("The bag with name " + bagName + "does not exist.");
            return;
        }
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "DELETE FROM sharedbag WHERE userprofileid = " + userProfile.getId()
                       + " AND bagid = (SELECT id FROM savedbag WHERE name='" + bagName + "'"
                       + " AND userprofileid = "+ bagOwnerId + ")";
            stm = conn.prepareStatement(sql);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Error unsharing the "
                    + " the bag : " + bagId
                    + " with the user " + userProfile.getId(), sqle);
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Problem closing resources", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
    }

    /**
     * Delete the sharing between the bag and all the users sharing the bag.
     */
    public void unshareBagWithAllUsers(String bagName, int bagOwnerId)
        throws BagDoesNotExistException {
            Integer bagId = getBagId(bagName, bagOwnerId);
            if (bagId == null) {
                LOG.warn("The bag with name " + bagName + "does not exist.");
                return;
            }
            Connection conn = null;
            PreparedStatement stm = null;
            try {
                conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
                String sql = "DELETE FROM " + SHARED_BAGS + " WHERE bagid = " + bagId;
                stm = conn.prepareStatement(sql);
                stm.executeUpdate();
            } catch (SQLException sqle) {
                throw new RuntimeException("Error unsharing the "
                        + " the bag : " + bagId + " with all the users.", sqle);
            } finally {
                if (stm != null) {
                    try {
                        stm.close();
                    } catch (SQLException e) {
                        throw new RuntimeException("Problem closing resources", e);
                    }
                }
                ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
            }
        }
}
