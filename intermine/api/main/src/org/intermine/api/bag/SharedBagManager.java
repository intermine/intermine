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
import java.util.Date;
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
import org.intermine.api.profile.StorableBag;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.api.search.CreationEvent;
import org.intermine.api.search.DeletionEvent;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
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
    /** the table name **/
    public static final String SHARED_BAGS = "sharedbag";
    protected ObjectStoreWriter uosw;
    protected ProfileManager profileManager;
    private static final Logger LOG = Logger.getLogger(SharedBagManager.class);

    /**
     * Return the singleton SharedBagManager instance
     * @param profileManager the profile manager
     * @return the instance
     */
    public static SharedBagManager getInstance(ProfileManager profileManager) {
        if (sharedBagManager == null) {
            sharedBagManager = new SharedBagManager(profileManager);
        }
        return sharedBagManager;
    }

    /**
     * Constructor. Use TagManagerFactory for creating tag manager.
     * @param profileOsWriter user profile object store
     */
    private SharedBagManager(ProfileManager profileManager) {
        this.profileManager = profileManager;
        this.uosw = profileManager.getProfileObjectStoreWriter();
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
     * Return a map containing the bags that the user in input has access because shared by
     * someone else
     * @param profile the user profile
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getSharedBags(Profile profile) {
        Map<String, InterMineBag> sharedBags = new HashMap<String, InterMineBag>();
        if (profile.getUsername() == null
            || profile.getUserId() == null
            || profileManager.getUserProfile(profile.getUsername()) == null) {
            return sharedBags;
        }
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        Map<Integer, Map<String, String>> bagsMap = new HashMap<Integer, Map<String, String>>();
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "SELECT sb.id, sb.name, u.username "
                       + "FROM savedbag as sb, userprofile as u, "
                       + SHARED_BAGS + " as sharedbag WHERE "
                       + "sharedbag.bagid = sb.id "
                       + "AND sharedbag.userprofileid = " + profile.getUserId()
                       + " AND sb.userprofileid = u.id";
            stm = conn.prepareStatement(sql);
            rs = stm.executeQuery();
            Map<String, String> bagMap;
            while (rs.next()) {
                bagMap = new HashMap<String, String>();
                bagMap.put("bagName", rs.getString(2));
                bagMap.put("userName", rs.getString(3));
                bagsMap.put(rs.getInt(1), bagMap);
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("Error retrieving the shared bags "
                    + "for the user : " + profile.getUserId(), sqle);
        } finally {
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
        for (Map<String, String> bagValues : bagsMap.values()) {
            String userName = bagValues.get("userName");
            String bagName = bagValues.get("bagName");
            InterMineBag bag = profileManager.getProfile(userName).getSavedBags().get(bagName);
            if (bag != null) {
                sharedBags.put(bag.getName(), bag);
            }
        }
        return sharedBags;
    }

    /**
     * Return the users sharing the list given in input, not the owner
     * @param bag the bag the users share
     * @return the list of users sharing the bag
     */
    public List<String> getUsersSharingBag(StorableBag bag) {
        List<String> usersSharingBag = new ArrayList<String>();
        Connection conn = null;
        PreparedStatement stm = null;
        ResultSet rs = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "SELECT u.username FROM userprofile as u, sharedbag as sharebag "
                       + "WHERE u.id = sharebag.userprofileid AND sharebag.bagid = "
                       + bag.getSavedBagId();
            stm = conn.prepareStatement(sql);
            rs = stm.executeQuery();
            while (rs.next()) {
                usersSharingBag.add(rs.getString("username"));
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("Error retrieving the users sharing "
                    + "the bag : " + bag.getName(), sqle);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving the users sharing "
                    + "the bag : " + bag.getName(), e);
        } finally {
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
     * @param bag the bag to share
     * @param userName the user which the bag is shared with
     * @throws UserNotFoundException if the user does't exist
     * @throws UserAlreadyShareBagException if the user already shares the list
     */
    public void shareBagWithUser(InterMineBag bag, String userName)
        throws UserNotFoundException, UserAlreadyShareBagException {
        UserProfile userProfile = profileManager.getUserProfile(userName);
        if (userProfile == null) {
            throw new UserNotFoundException("User " + userName + " doesn't exist");
        }
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "INSERT INTO " + SHARED_BAGS + " VALUES(" + bag.getSavedBagId() + ", "
                       + userProfile.getId() + ")";
            stm = conn.prepareStatement(sql);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new UserAlreadyShareBagException("Error sharing the "
                    + " the bag : " + bag.getSavedBagId()
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
        if (profileManager.isProfileCached(userName)) {
            profileManager.getProfile(userName).getSearchRepository()
                          .receiveEvent(new CreationEvent(bag));
        }
    }

    /**
     * Perform a query to retrieve a bag's backing SavedBag
     * @param bagName the bagName
     * @param dateCreated the date when the bag has been created
     * @return the relevant SavedBag
     */
    public SavedBag getSavedBag(String bagName, String dateCreated) {
        SavedBag bag = new SavedBag();
        bag.setName(bagName);
        bag.setDateCreated(new Date(Long.parseLong(dateCreated)));
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("name");
        fieldNames.add("dateCreated");
        try {
            bag = (SavedBag) uosw.getObjectByExample(bag, fieldNames);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
        return bag;
    }

    /**
     * Share the bag given in input with user which userName is given in input
     * @param bagName the bag name to share
     * @param dateCreated the date when the bag has been created
     * @param userName the user which the bag is shared with
     * @throws UserNotFoundException if the user does't exist
     * @throws BagDoesNotExistException if the bag does't exist
     */
    public void shareBagWithUser(String bagName, String dateCreated, String userName)
        throws UserNotFoundException, BagDoesNotExistException {
        UserProfile userProfile = profileManager.getUserProfile(userName);
        if (userProfile == null) {
            throw new UserNotFoundException("User " + userName + " doesn't exist");
        }
        SavedBag bag = getSavedBag(bagName, dateCreated);
        if (bag == null) {
            throw new BagDoesNotExistException("The bag with name " + bagName + " doesn't exist");
        }
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "INSERT INTO " + SHARED_BAGS + " VALUES(" + bag.getId() + ", "
                       + userProfile.getId() + ")";
            stm = conn.prepareStatement(sql);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new UserAlreadyShareBagException("Error sharing the "
                    + " the bag : " + bag.getId()
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
     * @param bag the bag shared
     * @param userName the user name sharing the bag
     */
    public void unshareBagWithUser(InterMineBag bag, String userName) {
        UserProfile userProfile = profileManager.getUserProfile(userName);
        if (userProfile == null) {
            LOG.warn("User " + userName + " doesn't exist");
            return;
        }
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "DELETE FROM sharedbag WHERE userprofileid = " + userProfile.getId()
                       + " AND bagid = " + bag.getSavedBagId();
            stm = conn.prepareStatement(sql);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Error unsharing the "
                    + " the bag : " + bag.getName()
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
        if (profileManager.isProfileCached(userName)) {
            profileManager.getProfile(userName).getSearchRepository()
                          .receiveEvent(new DeletionEvent(bag));
        }
    }

    /**
     * Delete the sharing between the bag and all the users sharing the bag.
     * Method used when a bag is deleted.
     * @param bag the bag that has been shared by users
     */
    public void unshareBagWithAllUsers(StorableBag bag) {
        List<String> usersListSharingBag = getUsersSharingBag(bag);
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "DELETE FROM " + SHARED_BAGS + " WHERE bagid = " + bag.getSavedBagId();
            stm = conn.prepareStatement(sql);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Error unsharing the "
                    + " the bag : " + bag.getName() + " with all the users.", sqle);
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
        //update user repository for all users sharing tha bag
        for (String userName : usersListSharingBag) {
            if (profileManager.isProfileCached(userName)) {
                profileManager.getProfile(userName).getSearchRepository()
                              .receiveEvent(new DeletionEvent(bag));
            }
        }
    }
}
