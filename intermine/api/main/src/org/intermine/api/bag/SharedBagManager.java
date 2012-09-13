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
import java.util.List;
import java.util.Map;

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
import org.intermine.model.userprofile.UserProfile;
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
    protected ObjectStoreWriter uosw;
    protected ProfileManager profileManager;
    private static final Logger LOG = Logger.getLogger(SharedBagManager.class);

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
     * @param profile
     * @return
     */
    public Map<String, InterMineBag> getSharedBags(Profile profile) {
        Map<String, InterMineBag> sharedBags = new HashMap<String, InterMineBag>();
        if (profile.getUsername() == null ||
            profileManager.getUserProfile(profile.getUsername()) == null) {
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
                InterMineBag bag = new InterMineBag(profileManager.getProductionObjectStore(),
                                                    bagId, uosw);
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
    public List<String> getUsersSharingBag(InterMineBag bag) {
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
    public void shareBagWithUser(InterMineBag bag, String userName)
        throws UserNotFoundException, BagDoesNotExistException {
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
            profileManager.getProfile(userName).getSearchRepository().receiveEvent(new CreationEvent(bag));
        }
    }

    /**
     * Delete the sharing between the user and the bag given in input
     */
    public void unshareBagWithUser(InterMineBag bag, String userName)
        throws UserNotFoundException, BagDoesNotExistException {
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
            profileManager.getProfile(userName).getSearchRepository().receiveEvent(new DeletionEvent(bag));
        }
    }

    /**
     * Delete the sharing between the bag and all the users sharing the bag.
     */
    public void unshareBagWithAllUsers(StorableBag bag)
        throws BagDoesNotExistException {
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
        }
}
