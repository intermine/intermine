package org.intermine.api.bag;

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

public class SharingInvite {

    public static class NotFoundException extends Exception {

        private static final long serialVersionUID = 4508741600952344965L;

        public NotFoundException(String msg) {
            super(msg);
        }

    }

    public static final String TABLE_NAME = "baginvites";
    
    private static final String TABLE_DEFINITION =
        "CREATE TABLE " + TABLE_NAME + " (" +
          "bagid integer NOT NULL, " +
          "inviterid integer NOT NULL, " +
          "token char(20) UNIQUE NOT NULL, " +
          "createdat timestamp DEFAULT NOW(), " +
          "acceptedat timestamp," +
          "accepted boolean, " +
          "invitee text NOT NULL)";
    
    public static String getTableDefinition() {
        return TABLE_DEFINITION;
    }
    
    private static final String FETCH_ALL_SQL = 
        "SELECT bagid, inviterid, createdat, accepted, acceptedat, invitee, token FROM " + TABLE_NAME;

    private static final String FETCH_MINE_SQL =
        FETCH_ALL_SQL + " WHERE inviterid = ?";

    private static final String FETCH_SQL = FETCH_ALL_SQL + " WHERE token = ?";
    
    private static final String SAVE_SQL =
        "INSERT INTO " + TABLE_NAME + " (bagid, inviterid, token, invitee) " +
        "VALUES (?, ?, ?, ?)";
    
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

    
    protected SharingInvite(InterMineBag bag, String invitee) {
        this(bag, invitee, TextUtil.generateRandomUniqueString(20));
    }

    protected SharingInvite(InterMineBag bag, String invitee, String token) {
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
        try {
            this.os = (ObjectStoreWriterInterMineImpl) bag.getUserProfileWriter();
        } catch (ClassCastException cce) {
            throw new RuntimeException("Hey, that isn't an intermine object-store!");
        }
    }
    
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
    
    protected void save() throws SQLException {
        if (inDB) {
            return;
        }
        os.performUnsafeOperation(SAVE_SQL, new SQLOperation<Void>() {
            @Override
            public Void run(PreparedStatement stm) throws SQLException {
                stm.setInt(1, bag.getSavedBagId());
                stm.setInt(2, bag.getProfileId());
                stm.setString(3, token);
                stm.setString(4, invitee);
                
                stm.executeUpdate();
                
                inDB = true;
                return null;
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
        // Unpack what we want from the API.
        ProfileManager pm = im.getProfileManager();
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            pm.getProfileObjectStoreWriter();
        
        Collection<IntermediateRepresentation> rows = osw.performUnsafeOperation(FETCH_MINE_SQL, new SQLOperation<Collection<IntermediateRepresentation>>() {
            @Override
            public Collection<IntermediateRepresentation> run(PreparedStatement stm) throws SQLException {
                stm.setInt(1, inviter.getUserId());
                ResultSet rs = stm.executeQuery();
        
                final List<IntermediateRepresentation> results = new ArrayList<IntermediateRepresentation>();
                while (rs.next()) {
                    results.add(toIntermediateReps(rs));
                }
                
                return results;
            }
        });
        List<SharingInvite> retval = new ArrayList<SharingInvite>();
        for (IntermediateRepresentation rep: rows) {
            retval.add(restoreFromRow(im, rep));
        }
        return retval;
    }
    
    private static SharingInvite restoreFromRow(InterMineAPI im,
            IntermediateRepresentation rep) throws ObjectStoreException{
        ProfileManager pm = im.getProfileManager();
        BagManager bm = im.getBagManager();
        ObjectStore os = pm.getProfileObjectStoreWriter();
        Profile inviter = pm.getProfile(rep.inviterId);
        SavedBag savedBag = (SavedBag) os.getObjectById(rep.bagId, SavedBag.class);
        InterMineBag bag = bm.getBag(inviter, savedBag.getName());
        SharingInvite invite = new SharingInvite(bag, rep.invitee, rep.token);

        // TODO: move these into a constructor...
        invite.acceptedAt = rep.acceptedAt;
        invite.createdAt = rep.createdAt;
        invite.accepted = rep.accepted;
        return invite;
    }

    private static class IntermediateRepresentation {
        int bagId;
        int inviterId;
        String token;
        String invitee;
        Date acceptedAt;
        Date createdAt;
        Boolean accepted;
        IntermediateRepresentation() {
            
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
    
    public static SharingInvite getByToken(final InterMineAPI im, final String token)
        throws SQLException, ObjectStoreException, NotFoundException {
        // Unpack what we want from the API.
        ProfileManager pm = im.getProfileManager();
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            pm.getProfileObjectStoreWriter();

        IntermediateRepresentation row =  osw.performUnsafeOperation(FETCH_SQL, new SQLOperation<IntermediateRepresentation>() {
            @Override
            public IntermediateRepresentation run(PreparedStatement stm) throws SQLException {
                stm.setString(1, token);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    return toIntermediateReps(rs);
                };
                return null;
            }
        });

        if (row == null) {
            throw new NotFoundException("token not found");
        }
        SharingInvite invite = restoreFromRow(im, row);
        
        invite.inDB = true;
        
        return invite;
    }
    
    public String getToken() {
        return token;
    }
    
    public InterMineBag getBag() {
        return bag;
    }
    
    public String getInvitee() {
        return invitee;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public Date getAcceptedAt() {
        return acceptedAt;
    }
    
    public Boolean getAccepted() {
        return accepted;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof SharingInvite)) {
            return other.equals(this);
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
