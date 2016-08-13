package org.intermine.api.bag;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.util.TextUtil;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.intermine.SQLOperation;

/**
 * A representation of an invitation to share a resource.
 *
 * @author Alex Kalderimis
 */
public class SharingInvite
{
    private static final class FetchInviteData extends SQLOperation<IntermediateRepresentation>
    {
        private final String token;

        private FetchInviteData(String token) {
            this.token = token;
        }

        @Override
        public IntermediateRepresentation run(PreparedStatement stm) throws SQLException {
            stm.setString(1, token);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                return toIntermediateReps(rs);
            }
            return null;
        }
    }

    /**
     * Exception thrown when we can't find a shared invitation.
     * @author Alex Kalderimis
     *
     */
    public static class NotFoundException extends Exception
    {

        private static final long serialVersionUID = 4508741600952344965L;

        /** @param msg the reason it could not be found **/
        NotFoundException(String msg) {
            super(msg);
        }
    }

    /** The DB table that holds the invitations **/
    public static final String TABLE_NAME = "baginvites";

    private static final String TABLE_DEFINITION =
        "CREATE TABLE " + TABLE_NAME + " ("
                + "bagid integer NOT NULL, "
                + "inviterid integer NOT NULL, "
                + "token char(20) UNIQUE NOT NULL, "
                + "createdat timestamp DEFAULT NOW(), "
                + "acceptedat timestamp,"
                + "accepted boolean, "
                + "invitee text NOT NULL)";

    /** @return the SQL needed to create the table **/
    public static String getTableDefinition() {
        return TABLE_DEFINITION;
    }

    private static final String FETCH_ALL_SQL =
        "SELECT bagid, inviterid, createdat, accepted, acceptedat, invitee, token FROM "
                + TABLE_NAME;

    private static final String FETCH_MINE_SQL =
        FETCH_ALL_SQL + " WHERE inviterid = ?";

    private static final String FETCH_SQL = FETCH_ALL_SQL + " WHERE token = ?";

    private static final String SAVE_SQL =
        "INSERT INTO " + TABLE_NAME + " (bagid, inviterid, token, invitee) "
                + "VALUES (?, ?, ?, ?)";

    private static final String FULL_SAVE_SQL =
        "INSERT INTO " + TABLE_NAME
            +   " (bagid, inviterid, token, invitee, createdat, acceptedat, accepted) "
            +   " VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String DELETE_SQL = "DELETE FROM " + TABLE_NAME + " WHERE token = ?";

    private static final String RECORD_ACCEPTANCE_SQL =
        "UPDATE " + TABLE_NAME + " SET acceptedat = ?, accepted = ? WHERE token = ?";

    private static final String RECORD_UNACCEPTANCE_SQL =
        "UPDATE " + TABLE_NAME + " SET acceptedat = NULL, accepted = NULL WHERE token = ?";

    private final InterMineBag bag;
    private final String invitee;
    private final String token;
    private final ObjectStoreWriterInterMineImpl os;
    private Date createdAt = null;
    private Date acceptedAt = null;
    private Boolean accepted = null;

    private boolean inDB = false;

    /**
     * Constructor.
     *
     * @param bag the bag to share
     * @param invitee the person to share the list with
     */
    protected SharingInvite(InterMineBag bag, String invitee) {
        this(bag, invitee, TextUtil.generateRandomUniqueString(20));
    }

    /**
     * Constructor.
     *
     * @param bag the bag to share
     * @param invitee the person to share the list with
     * @param token the User token
     */
    protected SharingInvite(
            InterMineBag bag,
            String invitee,
            String token) {
        this(bag, invitee, token, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param bag the bag to share
     * @param invitee the person to share the list with
     * @param token the User token
     * @param createdAt date created
     * @param acceptedAt date accepted
     * @param accepted true if accepted
     */
    protected SharingInvite(
            InterMineBag bag, String invitee, String token,
            Date createdAt, Date acceptedAt, Boolean accepted) {

        if (invitee == null) {
            throw new IllegalArgumentException("the invitee may not be null");
        }
        if (StringUtils.isBlank(invitee)) {
            throw new IllegalArgumentException("the invitee may not be blank");
        }
        if (StringUtils.isBlank(token)) {
            throw new IllegalArgumentException("the token must not be blank");
        }
        if (bag == null) {
            throw new IllegalArgumentException("the bag must not be null");
        }

        this.bag = bag;
        this.invitee = invitee;
        this.token = token;
        this.acceptedAt = acceptedAt;
        this.createdAt = createdAt;
        this.accepted = accepted;

        try {
            this.os = (ObjectStoreWriterInterMineImpl) bag.getUserProfileWriter();
        } catch (ClassCastException cce) {
            throw new RuntimeException("Hey, that isn't an intermine object-store!");
        }
    }

    /**
     * @throws SQLException database has wrong model
     * @throws NotFoundException invite not found
     */
    public void delete() throws SQLException, NotFoundException {
        if (!inDB) {
            throw new NotFoundException("This invite is not stored in the DB");
        }

        os.performUnsafeOperation(DELETE_SQL, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                stm.setString(0, token);
                stm.executeUpdate();
                inDB = false;
                return null;
            }
        });
    }

    /**
     *
     * @param wasAccepted true if accepted
     * @throws SQLException database has wrong model
     */
    protected void setAccepted(final Boolean wasAccepted) throws SQLException {
        if (acceptedAt != null) {
            throw new IllegalStateException("This invitation has already been accepted");
        }
        os.performUnsafeOperation(RECORD_ACCEPTANCE_SQL, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                Date now = new Date();
                stm.setDate(1, new java.sql.Date(now.getTime()));
                stm.setBoolean(2, wasAccepted);
                stm.setString(3, token);
                stm.executeUpdate();
                acceptedAt = now;
                accepted = wasAccepted;
                return null;
            }
        });
    }

    /**
    * @throws SQLException database has wrong model
    */

    protected void unaccept() throws SQLException {
        if (acceptedAt == null) {
            throw new IllegalStateException("This invitation has not been accepted");
        }
        os.performUnsafeOperation(RECORD_UNACCEPTANCE_SQL, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                stm.setString(1, token);
                stm.executeUpdate();
                return null;
            }
        });
    }

    /**
    * @throws SQLException database has wrong model
    */
    protected void save() throws SQLException {
        if (inDB) {
            return;
        }
        if (createdAt == null) { // New - we can go ahead and only save the interesting bits.
            os.performUnsafeOperation(SAVE_SQL, new SQLOperation<Void>() {
                @Override
                public Void run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, bag.getSavedBagId());
                    stm.setInt(2, bag.getProfileId());
                    stm.setString(3, token);
                    stm.setString(4, invitee);

                    stm.executeUpdate();

                    return null;
                }
            });
        } else { // Needs full serialisation.
            os.performUnsafeOperation(FULL_SAVE_SQL, new SQLOperation<Void>() {
                @Override
                public Void run(PreparedStatement stm) throws SQLException {
                    stm.setInt(1, bag.getSavedBagId());
                    stm.setInt(2, bag.getProfileId());
                    stm.setString(3, token);
                    stm.setString(4, invitee);
                    stm.setDate(5, new java.sql.Date(createdAt.getTime()));
                    if (acceptedAt == null) {
                        stm.setNull(6, java.sql.Types.DATE);
                    } else {
                        stm.setDate(6, new java.sql.Date(acceptedAt.getTime()));
                    }
                    if (accepted == null) {
                        stm.setNull(7, java.sql.Types.BOOLEAN);
                    } else {
                        stm.setBoolean(7, accepted);
                    }
                    stm.executeUpdate();
                    return null;
                }
            });

            inDB = true;
        }
    }

    /**
     * @param pm profile manager
     * @param inviter user who send the invite
     * @return collection of objects holding the invite data
     * @throws SQLException userprofile database doesn't have the correct model
     */
    public static Collection<IntermediateRepresentation> getInviteData(final ProfileManager pm,
            final Profile inviter)
        throws SQLException {
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
                pm.getProfileObjectStoreWriter();
        return osw.performUnsafeOperation(FETCH_MINE_SQL,
                new SQLOperation<Collection<IntermediateRepresentation>>() {
                @Override
                public Collection<IntermediateRepresentation> run(PreparedStatement stm)
                    throws SQLException {
                    stm.setInt(1, inviter.getUserId());
                    ResultSet rs = stm.executeQuery();

                    final List<IntermediateRepresentation> results
                        = new ArrayList<IntermediateRepresentation>();
                    while (rs.next()) {
                        results.add(toIntermediateReps(rs));
                    }
                    return results;
                }
            });
    }

    /**
     * Get the invitations this profile has made.
     * @param im The API of the data-warehouse
     * @param inviter The profile of the user that made the invitations.
     * @return A list of invitations
     * @throws SQLException If a connection cannot be established, or the SQL is bad.
     * @throws ObjectStoreException If the bag referenced by the invitation doesn't exist.
     */
    public static Collection<SharingInvite> getInvites(final InterMineAPI im, final Profile inviter)
        throws SQLException, ObjectStoreException {
        return getInvites(im.getProfileManager(), im.getBagManager(), inviter);
    }

    /**
     * Get the invitations this profile has made.
     * @param pm profile manager
     * @param bm bag manager
     * @param inviter The profile of the user that made the invitations.
     * @return A list of invitations
     * @throws SQLException If a connection cannot be established, or the SQL is bad.
     * @throws ObjectStoreException If the bag referenced by the invitation doesn't exist.
     */
    public static Collection<SharingInvite> getInvites(final ProfileManager pm, final BagManager bm,
            final Profile inviter)
        throws SQLException, ObjectStoreException {
        Collection<IntermediateRepresentation> rows = getInviteData(pm, inviter);
        List<SharingInvite> retval = new ArrayList<SharingInvite>();
        for (IntermediateRepresentation rep: rows) {
            retval.add(restoreFromRow(pm, bm, rep));
        }
        return retval;
    }

    private static SharingInvite restoreFromRow(
            ProfileManager pm, BagManager bm,
            IntermediateRepresentation rep) throws ObjectStoreException {
        ObjectStore os = pm.getProfileObjectStoreWriter();
        Profile inviter = pm.getProfile(rep.inviterId);
        SavedBag savedBag = (SavedBag) os.getObjectById(rep.bagId, SavedBag.class);
        InterMineBag bag = bm.getBag(inviter, savedBag.getName());
        return new SharingInvite(bag,
                rep.invitee, rep.token, rep.createdAt, rep.acceptedAt, rep.accepted);
    }

    /**
     * A structure for holding data we read from the DB.
     * @author Alex Kalderimis
     *
     */
    public static class IntermediateRepresentation
    {
        int bagId;
        int inviterId;
        String token;
        String invitee;
        Date acceptedAt;
        Date createdAt;
        Boolean accepted;

        /**
         * @return id of the bag shared
         */
        public int getBagId() {
            return bagId;
        }
        /**
         * @return id of user who sent invite
         */
        public int getInviterId() {
            return inviterId;
        }
        /**
         * @return user token
         */
        public String getToken() {
            return token;
        }
        /**
         * @return user invited to share the list
         */
        public String getInvitee() {
            return invitee;
        }
        /**
         * @return date invitee accepted the invitation
         */
        public Date getAcceptedAt() {
            return acceptedAt;
        }
        /**
         * @return data the user sent the invitation
         */
        public Date getCreatedAt() {
            return createdAt;
        }
        /**
         * @return true if the invitee accepted the invitation
         */
        public Boolean getAccepted() {
            return accepted;
        }
    }

    private static IntermediateRepresentation toIntermediateReps(final ResultSet rs)
        throws SQLException {
        IntermediateRepresentation rep = new IntermediateRepresentation();
        rep.bagId = rs.getInt("bagid");
        rep.inviterId = rs.getInt("inviterid");
        rep.token = rs.getString("token");
        rep.invitee = rs.getString("invitee");
        rep.acceptedAt = rs.getDate("acceptedat");
        rep.createdAt = rs.getDate("createdat");
        rep.accepted = rs.getBoolean("accepted");
        return rep;
    }

    /**
     * @param im API for databases
     * @param token user auth token
     * @return object representing the invite
     * @throws ObjectStoreException error storing the data
     * @throws SQLException database has wrong model
     * @throws NotFoundException invite not found
     */
    public static SharingInvite getByToken(final InterMineAPI im, final String token)
        throws SQLException, ObjectStoreException, NotFoundException {
        // Unpack what we want from the API.
        ProfileManager pm = im.getProfileManager();
        BagManager bm = im.getBagManager();
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            pm.getProfileObjectStoreWriter();

        IntermediateRepresentation row =
                osw.performUnsafeOperation(FETCH_SQL, new FetchInviteData(token));

        if (row == null) {
            throw new NotFoundException("token not found");
        }
        SharingInvite invite = restoreFromRow(pm, bm, row);

        invite.inDB = true;

        return invite;
    }


    /**
     * @return token
     */
    public String getToken() {
        return token;
    }

    /**
     * @return list being shared
     */
    public InterMineBag getBag() {
        return bag;
    }

    /**
     * @return user that received the invite
     */
    public String getInvitee() {
        return invitee;
    }

    /**
     * @return date invite created
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * @return data invite accepted by recipient
     */
    public Date getAcceptedAt() {
        return acceptedAt;
    }

    /**
     * @return true if recipient accepted the invite
     */
    public Boolean getAccepted() {
        return accepted;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof SharingInvite)) {
            return this.equals(other);
        }
        if (other == this) {
            return true;
        }
        SharingInvite oi = (SharingInvite) other;
        return token.equals(oi.getToken());
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(token);
    }

    @Override
    public String toString() {
        return String.format("[%s: token=%s bag=%s invitee=%s]",
            getClass().getName(), token, bag.getName(), invitee);
    }

}
