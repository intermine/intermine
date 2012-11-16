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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.intermine.api.bag.SharingInvite.NotFoundException;
import org.intermine.api.profile.BagDoesNotExistException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.StorableBag;
import org.intermine.api.profile.UserAlreadyShareBagException;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.api.search.ChangeEvent;
import org.intermine.api.search.CreationEvent;
import org.intermine.api.search.DeletionEvent;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.intermine.SQLOperation;
import org.intermine.sql.DatabaseUtil;

import static java.lang.String.format;

/**
 * Singleton manager class for shared bags.
 * Implements retrieving, adding and deleting bag shared between users.
 * @author Daniela Butano
 */
public class SharedBagManager
{
    private static final Map<ProfileManager, SharedBagManager> sharedBagManagers
        = new HashMap<ProfileManager, SharedBagManager>();
    /** the table name **/
    public static final String SHARED_BAGS = "sharedbag";
    /** The name of the table to persist offers to share a bag with others. **/
    public static final String BAG_INVITES = "baginvites";
    protected ObjectStoreWriterInterMineImpl uosw;
    protected ProfileManager profileManager;
    private static final Logger LOG = Logger.getLogger(SharedBagManager.class);

    /**
     * Return the singleton SharedBagManager instance
     * @param profileManager the profile manager
     * @return the instance
     */
    public static SharedBagManager getInstance(ProfileManager profileManager) {
        if (!sharedBagManagers.containsKey(profileManager)) {
            sharedBagManagers.put(profileManager, new SharedBagManager(profileManager));
        }
        return sharedBagManagers.get(profileManager);
    }

    /**
     * Constructor. Use TagManagerFactory for creating tag manager.
     * @param profileOsWriter user profile object store
     */
    private SharedBagManager(ProfileManager profileManager) {
        this.profileManager = profileManager;
        try {
            this.uosw = (ObjectStoreWriterInterMineImpl) profileManager.getProfileObjectStoreWriter();
        } catch (ClassCastException e) {
            throw new RuntimeException("Hey, that wasn't an intermine object store writer");
        }
        try {
            checkDBTablesExist();
        } catch (SQLException sqle) {
            LOG.error("Error trying to create extra tables", sqle);
        }
    }
    
    private static final SQLOperation<Boolean> createSavedBagTable = new SQLOperation<Boolean>() {
        @Override
        public Boolean run(PreparedStatement stm) throws SQLException {
            if (!DatabaseUtil.tableExists(stm.getConnection(), SHARED_BAGS)) {
                LOG.info("Creating shared bag table");
                stm.execute();
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
    };
    private static final SQLOperation<Void> createInvitesTable = new SQLOperation<Void>() {
        @Override
        public Void run(PreparedStatement stm) throws SQLException {
            if (!DatabaseUtil.tableExists(stm.getConnection(), SharingInvite.TABLE_NAME)) {
                LOG.info("Creating shared bag invite table");
                stm.execute();
            }
            return null;
        }
    };

    private void checkDBTablesExist() throws SQLException {
        final Boolean createdTable = uosw.performUnsafeOperation(
                getStatementCreatingTable(), createSavedBagTable);
        uosw.performUnsafeOperation(getStatementCreatingIndex(), new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                if (createdTable) {
                    LOG.info("Creating shared bag table index");
                    stm.execute();
                }
                return null;
            }
        });
        uosw.performUnsafeOperation(SharingInvite.getTableDefinition(), createInvitesTable);
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
    
    private static final String GET_SHARED_BAGS_SQL =
        "SELECT bag.name as bagname, u.username as sharer"
        + " FROM savedbag as bag, userprofile as u, " + SHARED_BAGS + " as share"
        + " WHERE share.bagid = bag.id AND share.userprofileid = ? AND bag.userprofileid = u.id";

    /**
     * Return a map containing the bags that the user in input has access because shared by
     * someone else
     * @param profile the user profile
     * @return a map from bag name to bag
     */
    public Map<String, InterMineBag> getSharedBags(final Profile profile) {
        if (profile == null || !profile.isLoggedIn()) {
            return Collections.emptyMap();
        }
        // We have to loop over things twice, because otherwise we end up in 
        // the dreaded ObjectStore deadlock, since this DB has only a single
        // connection.
        Map<String, Set<String>> whatTheSharersShared;
        try {
            whatTheSharersShared = uosw.performUnsafeOperation(GET_SHARED_BAGS_SQL, new SQLOperation<Map<String, Set<String>>>() {
                @Override
                public Map<String, Set<String>> run(PreparedStatement stm) throws SQLException {
                    final Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
                    stm.setInt(1, profile.getUserId());
                    ResultSet rs = stm.executeQuery();
                    while (rs.next()) {
                        String bagName = rs.getString("sharer");
                        if (!ret.containsKey(bagName)) {
                            ret.put(bagName, new HashSet<String>());
                        }
                        ret.get(bagName).add(rs.getString("bagname"));
                    }
                    return ret;
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving the shared bags "
                    + "for the user : " + profile.getUserId(), e);
        }
        Map<String, InterMineBag> ret = new HashMap<String, InterMineBag>();
        for (Entry<String, Set<String>> sharerAndTheirBags: whatTheSharersShared.entrySet()) {
            Profile sharer = profileManager.getProfile(sharerAndTheirBags.getKey());
            for (String bagName: sharerAndTheirBags.getValue()) {
                InterMineBag bag = sharer.getSavedBags().get(bagName);
                if (bag == null) {
                    LOG.warn("Shared bag doesn't exist: " + bagName);
                } else {
                    ret.put(bagName, bag);
                }    
            }    
        }
        return ret;
    }
    
    private final static String USERS_WITH_ACCESS_SQL =
        "SELECT u.username"
        + " FROM userprofile as u, sharedbag as share"
        + " WHERE u.id = share.userprofileid AND share.bagid = ?"
        + " ORDER BY u.username ASC";

    /**
     * Return the users this bag is shared with.
     * 
     * This set does not include the name of the owner of the bag, and it doesn't take
     * global sharing into account.
     * 
     * @param bag the bag the users share
     * @return the list of users sharing the bag
     */
    public Set<String> getUsersWithAccessToBag(final StorableBag bag) {
        try {
            return uosw.performUnsafeOperation(USERS_WITH_ACCESS_SQL, new SQLOperation<Set<String>>() {
                @Override
                public Set<String> run(PreparedStatement stm) throws SQLException {
                    final Set<String> usersWithAccess = new LinkedHashSet<String>();
                    stm.setInt(1, bag.getSavedBagId());
                    ResultSet rs = stm.executeQuery();
                    while (rs.next()) {
                        usersWithAccess.add(rs.getString(1));
                    }
                    return usersWithAccess;
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving the users sharing "
                    + "the bag : " + bag.getName(), e);
        }
    }
    
    /**
     * Generate an invitation to share a bag.
     * 
     * The invitation is a record of the invitation to share a bag, and records the
     * bag that is shared and whom it is meant to be shared with. This method generates a new
     * invitation, stores it in the persistent data-store that the bag is stored in,
     * and returns an object that represents that invitation.
     *
     * @param bag The list that we mean to share with someone.
     * @param userEmail An email address we are sending this invitation to.
     * @return An invitation.
     */
    public static SharingInvite inviteToShare(InterMineBag bag, String userEmail) {
        SharingInvite invite = new SharingInvite(bag, userEmail);
        try {
            invite.save();
        } catch (SQLException e) {
            throw new RuntimeException("SQL error. Possible token collision.", e);
        }
        return invite;
    }
    
    public void resolveInvitation(SharingInvite invitation, Profile accepter, boolean accepted)
            throws UserNotFoundException, UserAlreadyShareBagException, NotFoundException {
        if (accepted) {
            acceptInvitation(invitation, accepter);
        } else {
            rejectInvitation(invitation);
        }
        resolveInvitation(invitation, accepter, true);
    }
    
    public void rejectInvitation(SharingInvite invitation)
            throws UserNotFoundException, UserAlreadyShareBagException, NotFoundException {
        try { // Try this first, as we don't want to share unless this worked.
            invitation.setAccepted(false);
        } catch (SQLException e) {
            throw new RuntimeException("Error rejecting invitation", e);
        }
    }
    
    public void acceptInvitation(
            SharingInvite invitation,
            Profile accepter)
        throws UserNotFoundException, UserAlreadyShareBagException, NotFoundException {
        
        try { // Try this first, as we don't want to share unless this worked.
            invitation.setAccepted(true);
        } catch (SQLException e) {
            throw new RuntimeException("Error accepting invitation", e);
        }
        try {
            shareBagWithUser(invitation.getBag(), accepter.getUsername());
        } catch (UserNotFoundException e) {
            // Probably a temporary (non-persistent) user. Revert the invitation acceptance.
            try {
                invitation.unaccept();
            } catch (SQLException sqle) {
                throw new RuntimeException(
                    "Error accepting invitation. This invitation is no longer valid");
            }
            throw new NotFoundException("This is not a permanent user. Please log in");
        }
    }

    /**
     * Share the bag given in input with user which userName is given in input
     * @param bag the bag to share
     * @param userName the user which the bag is shared with
     * @throws UserNotFoundException if the user doesn't exist
     * @throws UserAlreadyShareBagException if the user already shares the list
     */
    public void shareBagWithUser(InterMineBag bag, String userName)
        throws UserNotFoundException, UserAlreadyShareBagException {
        UserProfile userProfile = profileManager.getUserProfile(userName);
        if (userProfile == null) {
            throw new UserNotFoundException("User " + userName + " doesn't exist");
        }
        storeShare(bag, userProfile);
    }
    
    private static final String STORE_SHARE_SQL = "INSERT INTO " + SHARED_BAGS + " VALUES(?, ?)";

    private void storeShare(final InterMineBag bag, final UserProfile sharedWith) 
        throws UserAlreadyShareBagException {
        final String userName = sharedWith.getUsername();
        try {
            uosw.performUnsafeOperation(STORE_SHARE_SQL, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, bag.getSavedBagId());
                    stm.setInt(2, sharedWith.getId());
                    return stm.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new UserAlreadyShareBagException("Error sharing the "
                    + " the bag : " + bag.getSavedBagId()
                    + " with the user " + sharedWith.getId(), e);
        }
        informProfileOfChange(userName, new CreationEvent(bag));
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
     * To be used ONLY when deserialising the user-profile from XML.
     * @param bagName the bag name to share
     * @param dateCreated the date when the bag has been created
     * @param userName the user which the bag is shared with
     * @throws UserNotFoundException if the user does't exist
     * @throws BagDoesNotExistException if the bag does't exist
     */
    public void shareBagWithUser(String bagName, String dateCreated, String userName)
        throws UserNotFoundException, BagDoesNotExistException {
        final UserProfile sharedWith = profileManager.getUserProfile(userName);
        if (sharedWith == null) {
            throw new UserNotFoundException("User " + userName + " doesn't exist");
        }
        final SavedBag bag = getSavedBag(bagName, dateCreated);
        if (bag == null) {
            throw new BagDoesNotExistException("There is not bag named '" + bagName + "'");
        }
        try {
            uosw.performUnsafeOperation(STORE_SHARE_SQL, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, bag.getId());
                    stm.setInt(2, sharedWith.getId());
                    return stm.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new UserAlreadyShareBagException(bag, sharedWith);
        }
    }

    private static final String DELETE_SHARE_SQL =
        "DELETE FROM sharedbag WHERE userprofileid = ? AND bagid = ?";

    private static final String NOT_ALREADY_SHARED_MSG =
        "This bag (%s) was not shared with this user (%s)";
    
    private static final String UNSHARING_ERROR_MSG =
        "Error unsharing this bag (%s:%d) from this user (%s:%d)";

    /**
     * Delete the sharing between the user and the bag given in input
     * @param bag the bag shared
     * @param userName the user name sharing the bag
     */
    public void unshareBagWithUser(final InterMineBag bag, final String userName) {
        final UserProfile userProfile = profileManager.getUserProfile(userName);
        if (userProfile == null) {
            LOG.warn("User " + userName + " doesn't exist");
            return;
        }
        try {
            Integer deleted = uosw.performUnsafeOperation(DELETE_SHARE_SQL, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, userProfile.getId());
                    stm.setInt(2, bag.getSavedBagId());
                    return stm.executeUpdate();
                }
            });
            if (deleted > 0) {
                informProfileOfChange(userName, new DeletionEvent(bag));
            } else {
                LOG.warn(format(NOT_ALREADY_SHARED_MSG, bag, userName));
            }
        } catch (SQLException e) {
            throw new RuntimeException(format(UNSHARING_ERROR_MSG,
                bag.getName(), bag.getSavedBagId(),
                userProfile.getUsername(), userProfile.getId()), e);
        }
    }
    
    private void informProfileOfChange(final String name, final ChangeEvent evt) {
        if (profileManager.isProfileCached(name)) {
            profileManager.getProfile(name).getSearchRepository().receiveEvent(evt);
        }
    }

    private static final String UNSHARE_BAG_SQL = 
        "DELETE FROM " + SHARED_BAGS + " WHERE bagid = ?";

    private static final String UNSHARE_BAG_ERROR_MSG =
        "Error removing all shares of this bag: %s:%d";
    
    /**
     * Delete the sharing between the bag and all the users sharing the bag.
     * Method used when a bag is deleted.
     * @param bag the bag that has been shared by users
     */
    public void unshareBagWithAllUsers(final StorableBag bag) {
        Collection<String> usersWithAccess = getUsersWithAccessToBag(bag);
        if (usersWithAccess.isEmpty()) {
            return;
        }
        try {
            uosw.performUnsafeOperation(UNSHARE_BAG_SQL, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, bag.getSavedBagId());
                    return stm.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(
                format(UNSHARE_BAG_ERROR_MSG, bag.getName(), bag.getSavedBagId()),
                e
            );
        }
        //update user repository for all users sharing that bag
        ChangeEvent evt = new DeletionEvent(bag);
        for (String userName : usersWithAccess) {
            informProfileOfChange(userName, evt);
        }
    }

    private static final String REMOVE_USERS_INVITES_SQL =
        "DELETE FROM " + SharingInvite.TABLE_NAME + " WHERE inviterid = ?";

    public void removeAllInvitesBy(final Integer userId) {
        if (userId == null) {
            LOG.warn("I can't remove invites when the user-id is null");
            return;
        }
        try {
            uosw.performUnsafeOperation(REMOVE_USERS_INVITES_SQL, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, userId.intValue());
                    return stm.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException("Errors removing invites", e);
        }
    }
    
    private static final String DELETE_SHARES_WITH =
        "DELETE FROM " + SHARED_BAGS + " WHERE userprofileid = ?";

    public void removeAllSharesInvolving(final Integer userId) {
        if (userId == null) {
            return;
        }
        try {
            uosw.performUnsafeOperation(DELETE_SHARES_WITH, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, userId.intValue());
                    return stm.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException("Errors removing shares", e);
        }
    }
    
    private static final String DELETE_USERS_SHARES_WITH = DELETE_SHARES_WITH +
        " AND bagid IN (SELECT b.id FROM savedbag AS b WHERE b.userprofileid = ?)";

    public void unshareAllBagsFromUser(final Profile owner, final Profile recipient) {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("recipient must not be null");
        }
        try {
            uosw.performUnsafeOperation(DELETE_USERS_SHARES_WITH, new SQLOperation<Integer>() {
                @Override
                public Integer run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, recipient.getUserId());
                    stm.setInt(2, owner.getUserId());
                    return stm.executeUpdate();
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException("Error removing shares", e);
        }
    }
}
