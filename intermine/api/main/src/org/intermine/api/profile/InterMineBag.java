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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.bag.IncompatibleTypesException;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.search.PropertyChangeEvent;
import org.intermine.api.search.WebSearchable;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectPathExpression;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.TypeUtil;


/**
 * An object that represents a bag of objects in our database for the webapp. It is backed by an
 * ObjectStoreBag object, but contains extra data such as name and description.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public class InterMineBag extends StorableBag implements WebSearchable, Cloneable
{
    protected static final Logger LOG = Logger.getLogger(InterMineBag.class);
    /** name of bag values table */
    public static final String BAG_VALUES = "bagvalues";
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
        throws UnknownBagTypeException, ObjectStoreException {
        this.type = TypeUtil.unqualifiedName(type);
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
        List<String> keyFieldNames)
        throws UnknownBagTypeException, ObjectStoreException {
        this.type = TypeUtil.unqualifiedName(type);
        init(name, description, dateCreated, state, os, profileId, uosw);
        this.keyFieldNames = keyFieldNames;
    }

    /**
     * Constructs a new InterMineBag, and saves it in the UserProfile database.
     *
     * @param name the name of the bag
     * @param type the class of objects stored in the bag
     * @param description the description of the bag
     * @param dateCreated the Date when this bag was created
     * @param state the state of the bag
     * @param os the production ObjectStore
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @param keyFieldNames the list of identifiers defined for this bag
     * @throws ObjectStoreException if an error occurs
     */
    public InterMineBag(String name, String type, String description, Date dateCreated,
        BagState state, ObjectStore os, ObjectStoreWriter uosw,
        List<String> keyFieldNames)
        throws UnknownBagTypeException, ObjectStoreException {
        this(name, type, description, dateCreated, state, os, null, uosw, keyFieldNames);
    }

    private void init(String name, String description, Date dateCreated, BagState state,
        ObjectStore os, Integer profileId, ObjectStoreWriter uosw)
        throws UnknownBagTypeException, ObjectStoreException {
        checkAndSetName(name);
        this.description = description;
        this.dateCreated = dateCreated;
        this.state = state;
        this.os = os;
        this.profileId = profileId;
        this.osb = os.createObjectStoreBag();
        this.uosw = uosw;
        this.savedBagId = null;
        SavedBag savedBag = storeSavedBag();
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
        throws UnknownBagTypeException, ObjectStoreException {
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
    public InterMineBag(ObjectStore os, Integer savedBagId, ObjectStoreWriter uosw,
        boolean classDescriptor)
        throws UnknownBagTypeException, ObjectStoreException {
        this.os = os;
        this.uosw = uosw;
        this.savedBagId = savedBagId;
        ObjectStore uos = uosw.getObjectStore();
        SavedBag savedBag = (SavedBag) uos.getObjectById(savedBagId, SavedBag.class);
        checkAndSetName(savedBag.getName());
        this.type = TypeUtil.unqualifiedName(savedBag.getType());
        this.description = savedBag.getDescription();
        this.dateCreated = savedBag.getDateCreated();
        this.profileId = savedBag.proxGetUserProfile().getId();
        setState(savedBag.getState());
        this.osb = new ObjectStoreBag(savedBag.getOsbId());
        if (classDescriptor) {
            setClassDescriptors();
        }
    }



    /**
     * Declare that this bag is invalid, and return its InvalidBag representation.
     * @return An InvalidBag version of the database record.
     */
    protected InvalidBag invalidate() {
        try {
            return new InvalidBag(name, type, description, dateCreated, os,
                savedBagId, profileId, osb, uosw);
        } catch (ObjectStoreException e) {
            // Shouldn't happen, unless this.osb == null, and then things are really screwy.
            throw new RuntimeException("Unexpected error:", e);
        }
    }

    private void setClassDescriptors() throws UnknownBagTypeException {
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

    /**
     * Delete this bag from the userprofile database, bag should not be used after this method has
     * been called. Delete the ids from the production database too.
     * @throws ObjectStoreException if problem deleting bag
     */
    @Override
    public void delete() throws ObjectStoreException {
        super.delete();
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
     * Returns a List of BagValue (key field value and extra value) of the objects contained
     * by this bag.
     * @return the list of BagValue
     */
    @Override
    public List<BagValue> getContents() {
        if (isCurrent()) {
            return getContentsFromOsb();
        } else {
            return super.getContents();
        }
    }

    /**
     * Returns the values of the key field objects and extra attribute (if it exists)
     * contained in the bag. The values are retrieved using the objectstorebag.
     * @param ids the collection of id
     * @return the list of values
     */
    private List<BagValue> getContentsFromOsb() {
        return getContentsFromOsb(null);
    }

    /**
     * Returns the values of the key field objects and extra attribute (if it exists) having the id
     * specified in input and contained in the bag.The values are retrieved using the objectstorebag
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
            if (hasExtraValue) {
                QueryObjectPathExpression qope = new QueryObjectPathExpression(qc,
                                                 extraConnectField);
                qope.addToSelect(qope.getDefaultClass());
                q.addToSelect(qope);
                q.addToSelect(qc);
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
            q.setConstraint(constraint);
            Results res = os.execute(q);
            for (Object rowObj : res) {
                ResultsRow<?> row = (ResultsRow<?>) rowObj;
                String value;
                String extra = "";
                int index = 0;
                if (hasExtraValue) {
                    InterMineObject imObj = (InterMineObject) row.get(0);
                    if (imObj != null) {
                        extra = (String) imObj.getFieldValue(extraConstrainField);
                    } else {
                        extra = "";
                    }
                    index = index + 2; //row.get(1) contains the class of the bag type
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
        } catch (IllegalAccessException iae) {
            return new ArrayList<BagValue>();
        }
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
                updateBagValues();
            }
        } finally {
            if (oswProduction != null) {
                oswProduction.close();
            }
        }
    }


    @Override
    public int getSize() throws ObjectStoreException {
        Query q = new Query();
        q.addToSelect(osb);
        q.setDistinct(false);
        return os.count(q, ObjectStore.SEQUENCE_IGNORE);
    }

    /** @return the user-profile object store writer **/
    public ObjectStoreWriter getUserProfileWriter() {
        return uosw;
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
        SavedBag savedBag = storeSavedBag();
        this.savedBagId = savedBag.getId();
        addBagValues();
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
        storeSavedBag();
        fireEvent(new PropertyChangeEvent(this));
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
        storeSavedBag();
        fireEvent(new PropertyChangeEvent(this));
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
            storeSavedBag();
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
        storeSavedBag();
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
            SavedBag savedBag = copy.storeSavedBag();
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
            List<BagValue> values = getContents();
            addBagValues(values);
        }
    }

    /**
     * Save the key field values identified by the ids given in input into bagvalues table
     */
    private void addBagValuesFromIds(Collection<Integer> ids) {
        if (profileId != null) {
            List<BagValue> values = getContentsFromOsb(ids);
            addBagValues(values);
        }
    }

    /**Update the bagvalues table with the items contained in osb_int table
     *
     */
    private void updateBagValues() {
        deleteAllBagValues();
        addBagValues();
    }

    /**
     * Delete a given set of bag values from the bag value table. If an empty list is passed in,
     * no values will be deleted. If null is passed in, that is an error, and an
     * IllegalArgumentException will be raised.
     * @param values The values to delete. May not be <code>null</code>.
     */
    public void deleteBagValues(List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException("values cannot be null");
        }
        deleteSomeBagValues(values);
    }

    @Override
    public void deleteAllBagValues() {
        deleteSomeBagValues(null);
    }
}
