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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.bag.IncompatibleTypesException;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.search.WebSearchable;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.SingletonResults;
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

    private Integer profileId;
    private Integer savedBagId;
    private String name;
    protected final String type;
    private String description;
    private Date dateCreated;
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
     * @param os the production ObjectStore
     * @param profileId the ID of the user in the userprofile database
     * @param uosw the ObjectStoreWriter of the userprofile database
     * @throws ObjectStoreException if an error occurs
     */
    public InterMineBag(String name, String type, String description, Date dateCreated,
            ObjectStore os, Integer profileId, ObjectStoreWriter uosw) throws ObjectStoreException {
        checkAndSetName(name);
        this.type = type;
        this.description = description;
        this.dateCreated = dateCreated;
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
        this.osb = new ObjectStoreBag(savedBag.getOsbId());
        setClassDescriptors();
    }

    private void setClassDescriptors() {
        try {
            Class<?> cls = Class.forName(getQualifiedType());
            classDescriptors = os.getModel().getClassDescriptorsForClass(cls);
        } catch (ClassNotFoundException e) {
            throw new UnknownBagTypeException("bag type " + getQualifiedType() + " not known", e);
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
            uosw.store(savedBag);
        }
        return savedBag;
    }

    /**
     * Delete this bag from the userprofile database, bag should not be used after this method has
     * been called.
     * @throws ObjectStoreException if problem deleting bag
     */
    protected void delete() throws ObjectStoreException {
        if (profileId != null) {
            SavedBag savedBag = (SavedBag) uosw.getObjectStore().getObjectById(savedBagId,
                    SavedBag.class);
            uosw.delete(savedBag);
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
     * Getter for size, just to make jsp happy.
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
    }

    /**
     * Returns the value of name
     * @return the name of the bag
     */
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
    public String getTitle() {
        return getName();
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
            throw new IllegalArgumentException("Class not found in model: " + type);
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
    }

    /**
     * Remove the given id from the bag, this updates the bag contents in the database
     * @param id the id to remove
     * @throws ObjectStoreException if problem storing
     */
    public void removeIdFromBag(Integer id) throws ObjectStoreException {
        removeIdsFromBag(Collections.singleton(id));
    }

    /**
     * Remove the given ids from the bag, this updates the bag contents in the database
     * @param ids the ids to remove
     * @throws ObjectStoreException if problem storing
     */
    public void removeIdsFromBag(Collection<Integer> ids) throws ObjectStoreException {
        ObjectStoreWriter oswProduction = null;
        try {
            oswProduction = os.getNewWriter();
            oswProduction.removeAllFromBag(osb, ids);
        } finally {
            if (oswProduction != null) {
                oswProduction.close();
            }
        }
    }
}
