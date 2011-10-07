package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.log4j.Logger;
import org.intermine.api.bag.IncompatibleTypesException;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.search.WebSearchable;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.sql.writebatch.Batch;
import org.intermine.sql.writebatch.BatchWriterPostgresCopyImpl;
import org.intermine.util.TypeUtil;


/**
 * An object that represents a bag of objects in our database for the webapp. It is backed by an
 * ObjectStoreBag object, but contains extra data such as name and description.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public class InterMineBag implements WebSearchable, Cloneable
{
    protected static final Logger LOG = Logger.getLogger(InterMineBag.class);
    /** name of bag values table */
    public static final String BAG_VALUES = "bagvalues";
    private Integer profileId;
    private Integer savedBagId;
    private String name;
    private String type;
    private String description;
    private Date dateCreated;
    private List<String> keyFieldNames = new ArrayList<String>();
    private BagState state;
    private ObjectStoreBag osb;
    private ObjectStore os;
    private ObjectStoreWriter uosw;

    private Set<ClassDescriptor> classDescriptors;

    /**
     * Constructs a new InterMineIdBag, and saves it in the UserProfile database.
     *
     * @param name the name of the bag
     * @param type the class of objects stored in the bag
     * @param description the description of the bag
     * @param dateCreated the Date when this bag was created
     * @param state the state of this bag
     * @param os the production ObjectStore
     * @param profileId the ID of the user in the userprofile database
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @throws ObjectStoreException if an error occurs
     */
    public InterMineBag(String name, String type, String description, Date dateCreated,
        BagState state, ObjectStore os, Integer profileId, ObjectStoreWriter uosw)
        throws ObjectStoreException {
        this.type = type;
        init(name, description, dateCreated, state, os, profileId, uosw);
    }

    /**
     * Constructs a new InterMineIdBag, and saves it in the UserProfile database.
     *
     * @param name the name of the bag
     * @param type the class of objects stored in the bag
     * @param description the description of the bag
     * @param dateCreated the Date when this bag was created
     * @param state the state of the bag
     * @param os the production ObjectStore
     * @param profileId the ID of the user in the userprofile database
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @param keyFieldNames the list of identifiers defined for this bag
     * @throws ObjectStoreException if an error occurs
     */
    public InterMineBag(String name, String type, String description, Date dateCreated,
        BagState state, ObjectStore os, Integer profileId, ObjectStoreWriter uosw,
        List<String> keyFieldNames) throws ObjectStoreException {
        this.type = type;
        init(name, description, dateCreated, state, os, profileId, uosw);
        this.keyFieldNames = keyFieldNames;
    }

    private void init(String name, String description, Date dateCreated, BagState state,
        ObjectStore os, Integer profileId, ObjectStoreWriter uosw) throws ObjectStoreException {
        checkAndSetName(name);
        this.description = description;
        this.dateCreated = dateCreated;
        this.state = state;
        this.os = os;
        this.profileId = profileId;
        this.osb = os.createObjectStoreBag();
        this.uosw = uosw;
        this.savedBagId = null;
        SavedBag savedBag = store();
        this.savedBagId = savedBag.getId();
        setClassDescriptors();
    }

    /**
     * Loads an InterMineBag from the UserProfile database.
     *
     * @param os the production ObjectStore
     * @param savedBagId the ID of the bag in the userprofile database
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @throws ObjectStoreException if something goes wrong
     */
    public InterMineBag(ObjectStore os, Integer savedBagId, ObjectStoreWriter uosw)
        throws ObjectStoreException {
        this(os, savedBagId, uosw, true);
    }

    /**
     * Loads an InterMineBag from the UserProfile database.
     *
     * @param os the production ObjectStore
     * @param savedBagId the ID of the bag in the userprofile database
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @param classDescriptor if true the classDescriptor will be setted
     * @throws ObjectStoreException if something goes wrong
     */
    public InterMineBag(ObjectStore os, Integer savedBagId, ObjectStoreWriter uosw, boolean classDescriptor)
        throws ObjectStoreException {
        this.os = os;
        this.uosw = uosw;
        this.savedBagId = savedBagId;
        ObjectStore uos = uosw.getObjectStore();
        SavedBag savedBag = (SavedBag) uos.getObjectById(savedBagId, SavedBag.class);
        checkAndSetName(savedBag.getName());
        this.type = savedBag.getType();
        this.description = savedBag.getDescription();
        this.dateCreated = savedBag.getDateCreated();
        this.profileId = savedBag.proxGetUserProfile().getId();
        setState(savedBag.getState());
        this.osb = new ObjectStoreBag(savedBag.getOsbId());
        if (classDescriptor) {
            setClassDescriptors();
        }
    }

    private void setClassDescriptors() {
        try {
            Class<?> cls = Class.forName(getQualifiedType());
            classDescriptors = os.getModel().getClassDescriptorsForClass(cls);
        } catch (ClassNotFoundException e) {
            throw new UnknownBagTypeException("bag type " + getQualifiedType() + " not known", e);
        }
    }

    private void setState(String savedBagStatus) {
        if (BagState.CURRENT.toString().equals(savedBagStatus)) {
            state = BagState.CURRENT;
        } else if (BagState.NOT_CURRENT.toString().equals(savedBagStatus)) {
            state = BagState.NOT_CURRENT;
        } else {
            state = BagState.TO_UPGRADE;
        }
    }

    private SavedBag store() throws ObjectStoreException {
        SavedBag savedBag = new SavedBag();
        savedBag.setId(savedBagId);
        if (profileId != null) {
            savedBag.setName(name);
            savedBag.setType(type);
            savedBag.setDescription(description);
            savedBag.setDateCreated(dateCreated);
            savedBag.proxyUserProfile(new ProxyReference(null, profileId, UserProfile.class));
            savedBag.setOsbId(osb.getBagId());
            savedBag.setState(state.toString());
            uosw.store(savedBag);
        }
        return savedBag;
    }

    /**
     * Delete this bag from the userprofile database, bag should not be used after this method has
     * been called. Delete the ids from the production database too.
     * @throws ObjectStoreException if problem deleting bag
     */
    protected void delete() throws ObjectStoreException {
        if (profileId != null) {
            SavedBag savedBag = (SavedBag) uosw.getObjectStore().getObjectById(savedBagId,
                    SavedBag.class);
            uosw.delete(savedBag);
            removeIdsFromBag(getContentsAsIds(), false);
            deleteAllBagValues();
            this.profileId = null;
            this.savedBagId = null;
        }
    }

    /**
     * Returns a List which contains the contents of this bag as Integer IDs.
     *
     * @return a List of Integers
     */
    public List<Integer> getContentsAsIds() {
        Query q = new Query();
        q.addToSelect(osb);
        q.setDistinct(false);
        SingletonResults res = os.executeSingleton(q, 1000, false, true, true);
        return ((List) res);
    }

    /**
     * Returns a List which contains the ids given in input and contained
     * in this bag as Integer IDs.
     * @return a List of Integers
     */
    public List<Integer> getIdsContained(Collection<Integer> ids) {
        Query q = new Query();
        q.setDistinct(false);
        try {
            Class<? extends InterMineObject> clazz = (Class<InterMineObject>) Class.forName(getQualifiedType());
            QueryClass qc = new QueryClass(clazz);
            QueryField idField = new QueryField(qc, "id");
            q.addToSelect(idField);
            q.addFrom(qc);

            BagConstraint constraint1 = new BagConstraint(idField, ConstraintOp.IN, ids);
            BagConstraint constraint2 = new BagConstraint(idField, ConstraintOp.IN, osb);
            ConstraintSet constraintSet = new ConstraintSet(ConstraintOp.AND);
            constraintSet.addConstraint(constraint1);
            constraintSet.addConstraint(constraint2);
            q.setConstraint(constraintSet);

        } catch (ClassNotFoundException nfe) {
            LOG.error("Error retriving class for bag: " + name, nfe);
        }

        SingletonResults res = os.executeSingleton(q, 1000, false, true, true);
        return ((List) res);
    }

    /**
     * Returns a List of BagValue (key field value and extra value) of the objects contained by this bag.
     * @return the list of BagValue
     */
    public List<BagValue> getContentsAsKeyFieldAndExtraValue() {
        if (isCurrent()) {
            return getContentsFromOsb();
        } else {
            //we are upgrading bags, the osbag_int is empty, we need to use bagvalues table
            Connection conn = null;
            Statement stm = null;
            ResultSet rs = null;
            List<BagValue> primaryIdentifiersList = new ArrayList<BagValue>();
            ObjectStoreInterMineImpl uos = null;
            try {
                uos = (ObjectStoreInterMineImpl) uosw.getObjectStore();
                conn = uos.getConnection();
                stm = conn.createStatement();
                String sql = "SELECT value, extra FROM " + BAG_VALUES + " WHERE savedbagid = "
                             + savedBagId;
                rs = stm.executeQuery(sql);
                while (rs.next()) {
                    primaryIdentifiersList.add(new BagValue(rs.getString(1),
                                               (rs.getString(2) != null) ? rs.getString(2) : ""));
                }
            } catch (SQLException sqe) {
                LOG.error("Connection problems during loadings primary identifiers fields for"
                          + "the bag " + name, sqe);
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                        if (stm != null) {
                            stm.close();
                        }
                    } catch (SQLException sqle) {
                        LOG.error("Problems closing resources in the method "
                            + "getContentsASPrimaryIdentifierValues for the bag " + name, sqle);
                    }
                }
                uos.releaseConnection(conn);
            }
            return primaryIdentifiersList;
        }
    }

    /**
     * Returns the values of the key field objects and extra attribute (if it exists) contained in the bag.
     * The values are retrieved using the objectstorebag.
     * @param ids the collection of id
     * @return the list of values
     */
    private List<BagValue> getContentsFromOsb() {
        return getContentsFromOsb(null);
    }

    /**
     * Returns the values of the key field objects and extra attribute (if it exists) having the id
     * specified in input and contained in the bag.The values are retrieved using the objectstorebag.
     * @param ids the collection of id
     * @return the list of values
     */
    private List<BagValue> getContentsFromOsb(Collection<Integer> ids) {
        List<BagValue> keyFieldValueList = new ArrayList<BagValue>();
        Properties bagProperties = new Properties();
        InputStream isBag = getClass().getClassLoader().getResourceAsStream("extraBag.properties");
        String extraClassName = null;
        String extraConnectField = null;
        String extraConstrainField = null;
        if (isBag != null) {
            try {
                bagProperties.load(isBag);
                extraConnectField = bagProperties.getProperty("extraBag.connectField");
                extraClassName = bagProperties.getProperty("extraBag.className");
                extraConstrainField = bagProperties.getProperty("extraBag.constrainField");
            } catch (IOException ioe) {
                ioe.printStackTrace();
                LOG.error("Problems loading extraBag.properties. ", ioe);
            }
        } else {
            System.out.println("Could not find extraBag.properties file");
            LOG.error("Could not find extraBag.properties file");
        }
        boolean hasExtraValue = false;
        if (extraClassName != null) {
            for (ClassDescriptor cd : classDescriptors) {
                FieldDescriptor fd = cd.getFieldDescriptorByName(extraConnectField);
                if (fd != null && fd instanceof ReferenceDescriptor) {
                    hasExtraValue = true;
                    break;
                }
            }
        }
        Query q = new Query();
        q.setDistinct(false);
        try {
            QueryClass qc = new QueryClass(Class.forName(getQualifiedType()));
            q.addFrom(qc);
            QueryClass qce = null;
            if (hasExtraValue) {
                qce = new QueryClass(Class.forName(extraClassName));
                q.addFrom(qce);
                q.addToSelect(new QueryField(qce, extraConstrainField));
            }
            if (keyFieldNames.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            for (String keyFieldName : keyFieldNames) {
                q.addToSelect(new QueryField(qc, keyFieldName));
            }
            QueryField idField = new QueryField(qc, "id");
            BagConstraint constraint;
            if (ids == null || ids.isEmpty()) {
                constraint = new BagConstraint(idField, ConstraintOp.IN, osb);
            } else {
                constraint = new BagConstraint(idField, ConstraintOp.IN, ids);
            }
            if (!hasExtraValue) {
                q.setConstraint(constraint);
            } else {
                ConstraintSet constraintSet = new ConstraintSet(ConstraintOp.AND);
                constraintSet.addConstraint(constraint);
                ContainsConstraint extraConstraint = new ContainsConstraint(
                                                       new QueryObjectReference(qc, extraConnectField),
                                                       ConstraintOp.CONTAINS,
                                                       qce);
                constraintSet.addConstraint(extraConstraint);
                q.setConstraint(constraintSet);
            }
            Results res = os.execute(q);
            for (Object rowObj : res) {
                ResultsRow<?> row = (ResultsRow<?>) rowObj;
                String value;
                String extra = "";
                int index = 0;
                if (hasExtraValue) {
                    extra = (String) row.get(0);
                    index++;
                }
                for (; index < row.size(); index++) {
                    value = (String) row.get(index);
                    if (value != null && !"".equals(value)) {
                        keyFieldValueList.add(new BagValue(value, extra));
                        break;
                    }
                }
            }
            return keyFieldValueList;
        } catch (ClassNotFoundException cne) {
            return new ArrayList<BagValue>();
        }
    }

    /**
     * Returns a List of BagValue (key field value and extra value) of the objects contained by this bag.
     * @return the list of BagValue
     */
    public List<BagValue> getContentsOrderByExtraValue() {
        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;
        List<BagValue> primaryIdentifiersList = new ArrayList<BagValue>();
        ObjectStoreInterMineImpl uos = null;
        try {
            uos = (ObjectStoreInterMineImpl) uosw.getObjectStore();
            conn = uos.getConnection();
            stm = conn.createStatement();
            String sql = "SELECT value, extra FROM " + BAG_VALUES + " WHERE savedbagid = "
                         + savedBagId + " ORDER BY extra DESC";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                primaryIdentifiersList.add(new BagValue(rs.getString(1),
                                          (rs.getString(2) != null) ? rs.getString(2) : ""));
            }
        } catch (SQLException sqe) {
            LOG.error("Connection problems during loadings primary identifiers fields for"
                      + "the bag " + name, sqe);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    if (stm != null) {
                        stm.close();
                    }
                } catch (SQLException sqle) {
                    LOG.error("Problems closing resources in the method "
                        + "getContentsASPrimaryIdentifierValues for the bag " + name, sqle);
                }
            }
            uos.releaseConnection(conn);
        }
        return primaryIdentifiersList;
    }

    /**
     * Upgrades the ObjectStoreBag with a new ObjectStoreBag containing the collection of elements
     * given in input
     * @param values the collection of elements to add
     * @param updateBagValues id true if we upgrade the bagvalues table
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public void upgradeOsb(Collection<Integer> values, boolean updateBagValues)
        throws ObjectStoreException {
        ObjectStoreWriter oswProduction = null;
        SavedBag savedBag = (SavedBag) uosw.getObjectById(savedBagId, SavedBag.class);
        try {
            oswProduction = os.getNewWriter();
            osb = oswProduction.createObjectStoreBag();
            oswProduction.addAllToBag(osb, values);
            savedBag.setOsbId(osb.getBagId());
            savedBag.setState(BagState.CURRENT.toString());
            state = BagState.CURRENT;
            uosw.store(savedBag);
            if (updateBagValues) {
                deleteAllBagValues();
                addBagValues();
            }
        } finally {
            if (oswProduction != null) {
                oswProduction.close();
            }
        }
    }

    /**
     * Returns the size of the bag.
     *
     * @return the number of elements in the bag
     * @throws ObjectStoreException if something goes wrong
     */
    public int size() throws ObjectStoreException {
        Query q = new Query();
        q.addToSelect(osb);
        q.setDistinct(false);
        return os.count(q, ObjectStore.SEQUENCE_IGNORE);
    }

    /**
     * Getter for size, just to make JSP happy.
     *
     * @return the number of elements in the bag
     * @throws ObjectStoreException if something goes wrong
     */
    public int getSize() throws ObjectStoreException {
        return size();
    }

    /**
     * Returns the ObjectStoreBag, so that elements can be added and removed.
     *
     * @return the ObjectStoreBag
     */
    public ObjectStoreBag getOsb() {
        return osb;
    }

    /**
     * Sets the ObjectStoreBag.
     *
     * @param osb the ObjectStoreBag
     */
    public void setOsb(ObjectStoreBag osb) {
        this.osb = osb;
    }

    /**
     * Sets the profileId - moves this bag from one profile to another.
     *
     * @param profileId the ID of the new userprofile
     * @throws ObjectStoreException if something goes wrong
     */
    public void setProfileId(Integer profileId)
        throws ObjectStoreException {
        this.profileId = profileId;
        SavedBag savedBag = store();
        this.savedBagId = savedBag.getId();
        addBagValues();
    }

    /**
     * Save the bag into the userprofile database
     *
     * @param profileId the ID of the userprofile
     * @param bagValues the list of the key field values of the objects contained by the bag
     * @throws ObjectStoreException if something goes wrong
     */
    public void saveWithBagValues(Integer profileId, Collection<BagValue> bagValues)
        throws ObjectStoreException {
        this.profileId = profileId;
        SavedBag savedBag = store();
        this.savedBagId = savedBag.getId();
        addBagValues(bagValues);
    }

    /**
     * Returns the value of name
     * @return the name of the bag
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     * @param name the bag name
     * @throws ObjectStoreException if something goes wrong
     */
    public void setName(String name) throws ObjectStoreException {
        checkAndSetName(name);
        store();
    }

    // Always set the name via this method to avoid saving bags with blank names
    private void checkAndSetName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("Attempt to create a list with a blank name.");
        }
        this.name = name;
    }

    /**
     * Return the description of this bag.
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Return the creation date that was passed to the constructor.
     * @return the creation date
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * @param description the description to set
     * @throws ObjectStoreException if something goes wrong
     */
    public void setDescription(String description)
        throws ObjectStoreException {
        this.description = description;
        store();
    }

    /**
     * Get the type of this bag (a class from InterMine model)
     * @return the type of objects in this bag
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     * @throws ObjectStoreException if something goes wrong
     */
    public void setType(String type)
        throws ObjectStoreException {
        if (os.getModel().getClassDescriptorByName(type) != null) {
            this.type = type;
            store();
        }
    }

    /**
     * Get the fully qualified type of this bag
     * @return the type of objects in this bag
     */
    public String getQualifiedType() {
        return os.getModel().getPackageName() + "." + type;
    }

    /**
     * Return the class descriptors for the type of this bag.
     * @return the set of class descriptors
     */
    public Set<ClassDescriptor> getClassDescriptors() {
        return classDescriptors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return getName();
    }

    /**
     * Return the savedbagId of this bag
     * @return savedbagId
     */
    public Integer getSavedBagId() {
        return savedBagId;
    }


    /**
     * Set the keyFieldNames
     * @param keyFieldNames the list of keyField names
     */
    public void setKeyFieldNames(List<String> keyFieldNames) {
        this.keyFieldNames = keyFieldNames;
    }

    /**
     * Return a list containing the keyFieldNames for the bag
     * @return keyFieldNames
     */
    public List<String> getKeyFieldNames() {
        return keyFieldNames;
    }

    /**
     * Return true if the status bag is current, otherwise false (status is not current
     * or to upgrade)
     * @return isCurrent
     */
    public boolean isCurrent() {
        if (BagState.CURRENT.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Return the bag state: current, not current, to upgrade
     * @return the status
     */
    public String getState() {
        return state.toString();
    }

    /**
     * Set bag state
     * @param state the state to set
     * @throws ObjectStoreException if something goes wrong
     */
    public void setState(BagState state) throws ObjectStoreException {
        this.state = state;
        store();
    }

    /**
     * Create copy of bag. Bag is saved to objectstore.
     * @return create bag
     */
    @Override
    public Object clone() {
        InterMineBag ret = cloneShallowIntermineBag();
        cloneInternalObjectStoreBag(ret);
        return ret;
    }

    private void cloneInternalObjectStoreBag(InterMineBag bag) {
        ObjectStoreWriter osw = null;
        try {
            osw = os.getNewWriter();
            ObjectStoreBag newBag = osw.createObjectStoreBag();
            Query q = new Query();
            q.addToSelect(this.osb);
            osw.addToBagFromQuery(newBag, q);
            bag.osb = newBag;
        } catch (ObjectStoreException e) {
            LOG.error("Clone failed.", e);
            throw new RuntimeException("Clone failed.", e);
        } finally {
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (ObjectStoreException e) {
                LOG.error("Closing object store failed.", e);
            }
        }
    }

    private InterMineBag cloneShallowIntermineBag() {
        // doesn't clone class descriptions and object store because they shouldn't change
        // -> cloned instance shares it with the original instance
        InterMineBag copy;
        try {
            copy = (InterMineBag) super.clone();
            copy.savedBagId = null;
            SavedBag savedBag = copy.store();
            copy.savedBagId = savedBag.getId();
            copy.keyFieldNames = keyFieldNames;
        } catch (ObjectStoreException ex) {
            throw new RuntimeException("Clone failed.", ex);
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Clone failed.", ex);
        }
        return copy;
    }

    /**
     * Sets date when bag was created.
     * @param date new date
     */
    public void setDate(Date date) {
        this.dateCreated = date;
    }

    /**
     * Add the given id to the bag, this updates the bag contents in the database. he type can
     * be a qualified or un-qualified class name.
     * @param id the id to add
     * @param type the type of ids being added
     * @throws ObjectStoreException if problem storing
     */
    public void addIdToBag(Integer id, String type) throws ObjectStoreException {
        addIdsToBag(Collections.singleton(id), type);
    }

    /**
     * Add the given ids to the bag, this updates the bag contents in the database.  The type can
     * be a qualified or un-qualified class name.
     * @param ids the ids to add
     * @param type the type of ids being added
     * @throws ObjectStoreException
     *             if problem storing
     */
    public void addIdsToBag(Collection<Integer> ids, String type)
        throws ObjectStoreException {
        if (!isOfType(type)) {
            throw new IncompatibleTypesException("Cannot add type " + type
                    + " to bag of type " + getType() + ".");
        }
        if (profileId != null) {
            //we add only the ids not already contained
            Collection<Integer> idsContained = getIdsContained(ids);
            for (Integer idContained : idsContained) {
                ids.remove(idContained);
            }
            if (!ids.isEmpty()) {
                addBagValuesFromIds(ids);
            }
        }
        ObjectStoreWriter oswProduction = null;
        try {
            oswProduction = os.getNewWriter();
            oswProduction.addAllToBag(osb, ids);
        } finally {
            if (oswProduction != null) {
                oswProduction.close();
            }
        }
    }

    /**
     * Test whether the given type can be added to this bag, type can be a
     * qualified or un-qualified string.
     * @param testType type to check
     * @return true if type can be added to the bag
     */
    public boolean isOfType(String testType) {
        Model model = os.getModel();
        // this method works with qualified and unqualified class names
        ClassDescriptor testCld = model.getClassDescriptorByName(testType);
        if (testCld == null) {
            throw new IllegalArgumentException("Class not found in model: " + testType);
        }
        Set<ClassDescriptor> clds = model.getClassDescriptorsForClass(testCld
                .getType());
        for (ClassDescriptor cld : clds) {
            String className = cld.getName();
            if (TypeUtil.unqualifiedName(className).equals(getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add elements to the bag from a query, this is able to operate entirely in the database
     * without needing to read objects into memory.  The query should have a single column on the
     * select list returning an object id.
     * @param query to select object ids
     * @throws ObjectStoreException if problem storing
     */
    public void addToBagFromQuery(Query query) throws ObjectStoreException {
        // query is checked in ObjectStoreWriter method
        ObjectStoreWriter oswProduction = null;
        try {
            oswProduction = os.getNewWriter();
            oswProduction.addToBagFromQuery(osb, query);
        } finally {
            if (oswProduction != null) {
                oswProduction.close();
            }
        }
        if (profileId != null) {
            updateBagValues();
        }
    }

    /**
     * Remove the given id from the bag, this updates the bag contents in the database
     * @param id the id to remove
     * @throws ObjectStoreException if problem storing
     */
    public void removeIdFromBag(Integer id) throws ObjectStoreException {
        removeIdsFromBag(Collections.singleton(id), true);
    }

    /**
     * Remove the given ids from the bag, this updates the bag contents in the database
     * @param ids the ids to remove
     * @param updateBagValues whether or not to update the values
     * @throws ObjectStoreException if problem storing
     */
    public void removeIdsFromBag(Collection<Integer> ids, boolean updateBagValues)
        throws ObjectStoreException {
        ObjectStoreWriter oswProduction = null;
        try {
            oswProduction = os.getNewWriter();
            oswProduction.removeAllFromBag(osb, ids);
        } finally {
            if (oswProduction != null) {
                oswProduction.close();
            }
        }
        if (profileId != null && updateBagValues) {
            updateBagValues();
        }
    }

    /**
     * Save the key field values associated to the bag into bagvalues table
     */
    public void addBagValues() {
        if (profileId != null) {
            List<BagValue> values = getContentsAsKeyFieldAndExtraValue();
            addBagValues(values);
        }
    }

    /**
     * Save the key field values identified by the ids given in input into bagvalues table
     */
    private void addBagValuesFromIds(Collection<Integer> ids) {
        if (profileId != null) {
            List<InterMineBag.BagValue> values = getContentsFromOsb(ids);
            addBagValues(values);
        }
    }

    /**
     * Save the values given in input into bagvalues table
     * @param bagValues the values to save
     */
    public void addBagValues(Collection<BagValue> bagValues) {
        Connection conn = null;
        Batch batch = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
                batch = new Batch(new BatchWriterPostgresCopyImpl());
                String[] colNames = new String[] {"savedbagid", "value", "extra"};
                for (BagValue bagValue : bagValues) {
                    batch.addRow(conn, BAG_VALUES, savedBagId, colNames,
                                new Object[] {savedBagId, bagValue.value, bagValue.extra});
                }
                batch.flush(conn);
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (SQLException sqle) {
            LOG.error("Exception committing bagValues for bag: " + savedBagId, sqle);
            try {
                conn.rollback();
                conn.setAutoCommit(true);
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

    /**Update the bagvalues table with the items contained in osb_int table
     *
     */
    private void updateBagValues() {
        deleteAllBagValues();
        addBagValues();
    }

    public void deleteBagValues(List<String> values) {
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();

            Collection<String> placeHolders = CollectionUtils.collect(values,
                                              new ConstantTransformer("?"));
            String valuesList = StringUtils.join(placeHolders, ", ");
            String sql = "DELETE FROM " + BAG_VALUES + " WHERE savedBagId = ? "
                + " AND value IN (" + valuesList + " )";
            stm = conn.prepareStatement(sql);
            stm.setInt(1, savedBagId);
            for (int i = 0; i < values.size(); i++) {
                stm.setString(i + 2, values.get(i));
            }
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Error deleting the " + values.size()
                    + " bagvalues of bag : " + savedBagId, sqle);
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Problem closing  resources in"
                                               + " deleteBagValuesByValue()", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
    }

    public void deleteAllBagValues() {
        Connection conn = null;
        PreparedStatement stm = null;
        try {
            conn = ((ObjectStoreWriterInterMineImpl) uosw).getConnection();
            String sql = "DELETE FROM " + BAG_VALUES + " WHERE savedBagId = ? ";
            stm = conn.prepareStatement(sql);
            stm.setInt(1, savedBagId);
            stm.executeUpdate();
        } catch (SQLException sqle) {
            throw new RuntimeException("Error deleting the bagvalues of bag : " + savedBagId, sqle);
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Problem closing  resources in"
                                               + " deleteAllBagValues()", e);
                }
            }
            ((ObjectStoreWriterInterMineImpl) uosw).releaseConnection(conn);
        }
    }
    public class BagValue {
        String value = null;
        String extra = null;

        public BagValue (String value, String extra) {
            this.value = value;
            this.extra = extra;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }
    }
}
