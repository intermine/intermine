package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.proxy.ProxyReference;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.web.logic.search.WebSearchable;


/**
 * An object that represents a bag of objects in our database for the webapp. It is backed by an
 * ObjectStoreBag object, but contains extra data such as name and description.
 *
 * @author Kim Rutherford
 * @author Matthew Wakeling
 */
public class InterMineBag implements WebSearchable
{
    protected static final Logger LOG = Logger.getLogger(InterMineBag.class);

    private Integer profileId;
    private Integer savedBagId;
    private String name;
    private final String type;
    private String description;
    private final Date dateCreated;
    private ObjectStoreBag osb;
    private ObjectStore os;
    
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
                        ObjectStore os, Integer profileId, ObjectStoreWriter uosw) 
      throws ObjectStoreException {
        this.name = name;
        this.type = type;
        this.description = description;
        this.dateCreated = dateCreated;
        this.os = os;
        this.profileId = profileId;
        this.osb = os.createObjectStoreBag();
        this.savedBagId = null;
        SavedBag savedBag = store(uosw);
        this.savedBagId = savedBag.getId();
    }

    private SavedBag store(ObjectStoreWriter uosw) throws ObjectStoreException {
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
        } else if (savedBagId != null) {
            uosw.delete(savedBag);
        }
        return savedBag;
    }

    /**
     * Loads an InterMineBag from the UserProfile database.
     *
     * @param os the production ObjectStore
     * @param savedBagId the ID of the bag in the userprofile database
     * @param uos the ObjectStore of the userprofile database
     * @throws ObjectStoreException if something goes wrong
     */
    public InterMineBag(ObjectStore os, Integer savedBagId, ObjectStore uos)
    throws ObjectStoreException {
        this.os = os;
        this.savedBagId = savedBagId;
        SavedBag savedBag = (SavedBag) uos.getObjectById(savedBagId, SavedBag.class);
        this.name = savedBag.getName();
        this.type = savedBag.getType();
        this.description = savedBag.getDescription();
        this.dateCreated = savedBag.getDateCreated();
        this.profileId = savedBag.proxGetUserProfile().getId();
        this.osb = new ObjectStoreBag(savedBag.getOsbId());
    }

    /**
     * Returns a List which contains the contents of this bag as Integer IDs.
     *
     * @return a List of Integers
     */
    public List getContentsAsIds() {
        Query q = new Query();
        q.addToSelect(osb);
        q.setDistinct(false);
        SingletonResults res = os.executeSingleton(q);
        res.setNoOptimise();
        return res;
    }

    /**
     * Returns a List which contains the contents of this bag as InterMineObjects.
     *
     * @return a List of InterMineObjects
     */
    public List getContentsAsObjects() {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        q.setConstraint(new BagConstraint(qc, ConstraintOp.IN, osb));
        q.setDistinct(false);
        SingletonResults res = os.executeSingleton(q);
        res.setNoOptimise();
        return res;
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
     * Returns the ID of the profile to which this bag is attached, or null if it is not logged in.
     *
     * @return an Integer
     */
    public Integer getProfileId() {
        return profileId;
    }

    /**
     * Sets the profileId - moves this bag from one profile to another.
     *
     * @param profileId the ID of the new userprofile
     * @param uosw an ObjectStoreWriter for the userprofile database
     * @throws ObjectStoreException if something goes wrong
     */
    public void setProfileId(Integer profileId, ObjectStoreWriter uosw)
    throws ObjectStoreException {
        this.profileId = profileId;
        SavedBag savedBag = store(uosw);
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
     * @param uosw an ObjectStoreWriter for the userprofile database
     * @throws ObjectStoreException if something goes wrong
     */
    public void setName(String name, ObjectStoreWriter uosw) throws ObjectStoreException {
        this.name = name;
        store(uosw);
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
     * @param uosw an ObjectStoreWriter for the userprofile database
     * @throws ObjectStoreException if something goes wrong
     */
    public void setDescription(String description, ObjectStoreWriter uosw)
    throws ObjectStoreException {
        this.description = description;
        store(uosw);
    }

    /**
     * Get the value of savedBagId
     * @return an Integer
     */
    public Integer getSavedBagId() {
        return savedBagId;
    }

    /**
     * Get the type of this bag (a class from InterMine model)
     * @return the type of objects in this bag
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the fully qualifie type of this bag
     * @return the type of objects in this bag
     */
    public String getQualifiedType() {
        return os.getModel().getPackageName() + "." + type;
    }

    /**
     * @see org.intermine.web.logic.search.WebSearchable#getTitle()
     * {@inheritDoc}
     */
    public String getTitle() {
       return getName();
    }
}
