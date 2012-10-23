package org.intermine.api.profile;

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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.search.OriginatingEvent;
import org.intermine.api.search.DeletionEvent;
import org.intermine.api.search.WebSearchWatcher;
import org.intermine.api.search.WebSearchable;
import org.intermine.api.tag.TagTypes;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;

/**
 * Base class for representations of user's collections of objects stored in a database.
 *
 * @author Alex Kalderimis
 *
 */
public abstract class StorableBag implements WebSearchable
{

    private static final Logger LOG = Logger.getLogger(StorableBag.class);

    private enum Order {
        BY_VALUE("value asc"), BY_EXTRA("extra desc");

        private final String fragment;

        private Order(String sqlFragment) {
            this.fragment = sqlFragment;
        }

        @Override
        public String toString() {
            return fragment;
        }
    }

    protected Integer profileId;
    protected Integer savedBagId;

    /**
     * Returns the size of the bag.
     *
     * @return the number of elements in the bag
     * @throws ObjectStoreException if something goes wrong
     */
    public abstract int getSize() throws ObjectStoreException;

    /**
     * Returns the number of elements in the bag.
     * @return the number of elements in the bag
     * @throws ObjectStoreException if something goes wrong
     */
    public int size() throws ObjectStoreException {
        return getSize();
    }

    @Override
    public abstract String getName();

    @Override
    public abstract String getDescription();

    /**
     * Get the date that this bag was created at.
     * @return A date.
     */
    public abstract Date getDateCreated();

    /**
     * Get the unqualified name of class that the objects in this bag represent.
     * @return A name of a class.
     */
    public abstract String getType();

    /** @return the id of the profile belonging to the user this list belongs to */
    public Integer getProfileId() {
        return profileId;
    }

    /** @return the id of the saved bag this list represents */
    public Integer getSavedBagId() {
        return savedBagId;
    }

    /**
     * Return a reference to the object store bag contained in the production database.
     * @return A reference to the backing store of objects.
     */
    protected abstract ObjectStoreBag getOsb();

    /**
     * Return a string representing the state of this bag (eg. CURRENT).
     * @return A valid representation of a BagState
     */
    public abstract String getState();

    /**
     * Get a  reference to a connection to the database where user data is persisted.
     * @return An object capable of storing information in the user data store.
     */
    protected abstract ObjectStoreWriter getUserProfileWriter();

    /**
     * Delete this bag.
     *
     * Contrary to appearances, implementations MUST override this method in order to actually
     * delete the bag. They MUST ALSO call this method (as super.delete()). This is to ensure that
     * bag deletion events are registered correctly.
     *
     * @throws ObjectStoreException 
     */
    public void delete() throws ObjectStoreException {
        fireEvent(new DeletionEvent(this));
    }

    /**
     * Save the bag into the userprofile database, along with information about what kinds of values
     * this bag contains. These bag values can then be used later to reconstruct the contents of the
     * bag if when the bag is used with a different production database.
     *
     * @param profileId the ID of the userprofile
     * @param bagValues the list of the key field values of the objects contained by the bag
     * @throws ObjectStoreException if something goes wrong when inserting data into the database.
     */
    public void saveWithBagValues(Integer profileId, Collection<BagValue> bagValues)
        throws ObjectStoreException {
        if (profileId == null) {
            throw new NullPointerException("profileId may not be null");
        }
        this.profileId = profileId;
        SavedBag savedBag = storeSavedBag();
        this.savedBagId = savedBag.getId();
        addBagValues(bagValues);
    }

    /**
     * Perform the actual insertion of data into the userprofile database.
     * @return The object that represents the database record for this bag.
     * @throws ObjectStoreException If we cannot store the bag.
     */
    protected SavedBag storeSavedBag() throws ObjectStoreException {
        SavedBag savedBag = new SavedBag();
        savedBag.setId(getSavedBagId());
        if (profileId != null) {
            savedBag.setName(getName());
            savedBag.setType(getType());
            savedBag.setDescription(getDescription());
            savedBag.setDateCreated(getDateCreated());
            savedBag.proxyUserProfile(new ProxyReference(null, profileId, UserProfile.class));
            savedBag.setOsbId(getOsb().getBagId());
            savedBag.setState(getState());
            getUserProfileWriter().store(savedBag);
        }
        return savedBag;
    }

    /** Remove all the values from the bag-value table. **/
    public void deleteAllBagValues() {
        deleteSomeBagValues(null);
    }

    /**
     * Delete a given set of bag values from the bag value table. If an empty list is passed in,
     * no values will be deleted. If null is passed in all values will be deleted.
     * @param values The values to delete. <code>null</code> is understood
     *               as <code>ALL VALUES.</code>.
     */
    protected void deleteSomeBagValues(final List<String> values) {
        Connection conn = null;
        PreparedStatement stm = null;
        ObjectStoreWriter uosw = getUserProfileWriter();
        List<String> clauses = new ArrayList<String>(Arrays.asList("savedBagId = ?"));

        if (values != null) {
            Collection<String> placeHolders = CollectionUtils.collect(values,
                    new ConstantTransformer("?"));
            String valuesList = StringUtils.join(placeHolders, ", ");
            if (!valuesList.isEmpty()) {
                clauses.add("value IN (" + valuesList + ")");
            }
        }

        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "DELETE FROM " + InterMineBag.BAG_VALUES
                + " WHERE " + StringUtils.join(clauses, " AND ");
            stm = conn.prepareStatement(sql);
            stm.setInt(1, savedBagId);
            for (int i = 0; values != null && i < values.size(); i++) {
                stm.setString(i + 2, values.get(i));
            }
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Error deleting the "
                    + (values == null ? "" : values.size() + " ")
                    + "bagvalues of bag : " + savedBagId, sqle);
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
     * Returns a List of BagValue (key field value and extra value) of the objects contained
     * by this bag.
     * @return the values of the bag.
     */
    public List<BagValue> getContents() {
        return getContents(Order.BY_VALUE);
    }

    /** @return the contents of this list, ordered by their extra-value. **/
    public List<BagValue> getContentsOrderByExtraValue() {
        return getContents(Order.BY_EXTRA);
    }

    /**
     * Get the contents of this list, in a specified order.
     * @param order How to order the items when fetching from the database.
     * @return A list of the contents of the bag.
     */
    private List<BagValue> getContents(Order order) {
        String name = getName();
        ObjectStoreWriter uosw = getUserProfileWriter();
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        ObjectStoreInterMineImpl uos = null;
        List<BagValue> ret = new ArrayList<BagValue>();

        try {
            uos = (ObjectStoreInterMineImpl) uosw.getObjectStore();
            conn = uos.getConnection();
            stm = conn.createStatement();
            String sql = "SELECT value, extra FROM " + InterMineBag.BAG_VALUES
                         + " WHERE savedbagid = " + savedBagId
                         + " ORDER BY " + order.toString();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                String value = rs.getString("value");
                String extra = rs.getString("extra") == null ? "" : rs.getString("extra");
                ret.add(new BagValue(value, extra));
            }
        } catch (SQLException sqe) {
            LOG.error("Connection problem while loading primary fields for " + name, sqe);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    if (stm != null) {
                        stm.close();
                    }
                } catch (SQLException sqle) {
                    LOG.error("While releasing resources in the method "
                        + "getContentsASPrimaryIdentifierValues for the bag " + name, sqle);
                }
            }
            uos.releaseConnection(conn);
        }
        return ret;
    }

    /**
     * Save the values given in input into bagvalues table
     * @param bagValues the values to save
     */
    protected void addBagValues(Collection<BagValue> bagValues) {
        Connection conn = null;
        Batch batch = null;
        Boolean oldAuto = null;
        ObjectStoreWriter uosw = getUserProfileWriter();
        Integer sbid = getSavedBagId();
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            oldAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);
            batch = new Batch(new BatchWriterPostgresCopyImpl());
            String[] colNames = new String[] {"savedbagid", "value", "extra"};
            for (BagValue bagValue : bagValues) {
                batch.addRow(conn, InterMineBag.BAG_VALUES, sbid, colNames,
                            new Object[] {sbid, bagValue.value, bagValue.extra});
            }
            batch.flush(conn);
            conn.commit();
            conn.setAutoCommit(oldAuto);
        } catch (SQLException sqle) {
            LOG.error("Exception committing bagValues for bag: " + sbid, sqle);
            try {
                conn.rollback();
                if (oldAuto != null) {
                    conn.setAutoCommit(oldAuto);
                }
            } catch (SQLException sqlex) {
                throw new RuntimeException("Error aborting transaction", sqlex);
            }
        } finally {
            try {
                batch.close(conn);
            } catch (Exception e) {
                LOG.error("Exception caught when closing Batch while addbagValues", e);
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
    }

    // WebSearchable Implementation //
    private final Set<WebSearchWatcher> observers = new HashSet<WebSearchWatcher>();

    @Override
    public void addObserver(WebSearchWatcher wsw) {
        observers.add(wsw);
    }

    @Override
    public void removeObserver(WebSearchWatcher wsw) {
        observers.remove(wsw);
    }

    @Override
    public String getTagType() {
        return TagTypes.BAG;
    }

    @Override
    public void fireEvent(OriginatingEvent e) {
        if (observers != null) { // Can be due the order of initialisation of static fields...
            Collection<WebSearchWatcher> watchers = new ArrayList<WebSearchWatcher>(observers);
            for (WebSearchWatcher wsw: watchers) {
                wsw.receiveEvent(e);
            }
        }
    }


}
