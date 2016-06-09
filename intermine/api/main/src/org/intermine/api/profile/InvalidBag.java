package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;

import org.apache.log4j.Logger;
import org.intermine.api.bag.UnknownBagTypeException;
import org.intermine.api.search.DeletionEvent;
import org.intermine.model.userprofile.SavedBag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.metadata.TypeUtil;

/**
 * A class representing a bag that is invalid because it does not conform to the current model.
 * @author Alex Kalderimis.
 *
 */
public class InvalidBag extends StorableBag
{
    // Static constants.
    protected static final Logger LOG = Logger.getLogger(InvalidBag.class);

    // Fields relating to the properties of the dearly departed.
    private final String name;
    private final String type;
    private final String description;
    private final Date dateCreated;

    // Immutable fields relating to the database
    private final ObjectStore os;
    private final ObjectStoreBag osb;
    private final ObjectStoreWriter uosw;

    // Whether or not it has been deleted yet.
    private boolean deleted = false;

    private static final String OS_IS_NULL = "Argument 'os' must not be null";
    private static final String UOSW_IS_NULL = "Argument 'userprofileObjectStore' must not be null";

    /**
     * Constructor.
     * @param name The name of this bag.
     * @param type The type it is meant to have.
     * @param desc A free text description of the bag.
     * @param createdAt When this list was first created.
     * @param os The production object store.
     * @param profileId The internal DB id of the profile of the user this list belongs to.
     * @param savedBagId The internal DB id of the saved bag this list represents.
     * @param osb The bag that represents the production DB level stored set of object ids.
     * @param userprofileObjectStore The user-profile object store writer.
     * @throws ObjectStoreException If a ObjectStoreBag needs to be created and there is a problem
     *                              doing so.
     */
    public InvalidBag(String name, String type, String desc, Date createdAt, ObjectStore os,
            Integer savedBagId, Integer profileId,
            ObjectStoreBag osb, ObjectStoreWriter userprofileObjectStore)
        throws ObjectStoreException {

        this.type = TypeUtil.unqualifiedName(type);
        this.name = name;
        this.description = desc;
        this.dateCreated = createdAt;

        checkArguments(os, userprofileObjectStore);
        this.os = os;
        if (osb == null) {
            this.osb = os.createObjectStoreBag();
        } else {
            this.osb = osb;
        }
        this.uosw = userprofileObjectStore;

        this.profileId = profileId;
        this.savedBagId = savedBagId;
    }

    private static void checkArguments(ObjectStore os, ObjectStoreWriter uosw) {
        if (os == null) {
            throw new IllegalArgumentException(OS_IS_NULL);
        }
        if (uosw == null) {
            throw new IllegalArgumentException(UOSW_IS_NULL);
        }
    }

    /**
     * Minimal constructor. This constructor only requires the minimal set of values.
     * @param name The name of the list.
     * @param type The erstwhile type of the list.
     * @param description A free text description of the list.
     * @param createdAt When the list was first created.
     * @param os A connection to the production object store.
     * @param uosw The writer to the user-profile object store.
     * @throws ObjectStoreException If there is a problem creating an ObjectStoreBag
     */
    public InvalidBag(String name, String type, String description, Date createdAt,
            ObjectStore os, ObjectStoreWriter uosw) throws ObjectStoreException {
        this(name, type, description, createdAt, os, null, null, null, uosw);
    }

    /**
     * Constructor callable by the ProfileManager.
     * @param savedBag The saved bag retrieved from the DB.
     * @param profileId The id of the user profile.
     * @param os The production object store.
     * @param userprofileObjectStore The userprofile object store.
     * @throws ObjectStoreException If there is a problem creating an ObjectStoreBag.
     */
    protected InvalidBag(SavedBag savedBag, Integer profileId,
            ObjectStore os, ObjectStoreWriter userprofileObjectStore)
        throws ObjectStoreException {

        this.type = TypeUtil.unqualifiedName(savedBag.getType());
        this.name = savedBag.getName();
        this.description = savedBag.getDescription();
        this.dateCreated = savedBag.getDateCreated();

        checkArguments(os, userprofileObjectStore);
        this.os = os;
        this.osb = os.createObjectStoreBag();
        this.uosw = userprofileObjectStore;

        this.profileId = profileId;
        this.savedBagId = savedBag.getId();
    }

    /**
     * Returns a new Invalid Bag identical to this one, but with the given name. The
     * underlying user-profile saved bag will be renamed at the same time, and this
     * object marked as deleted.
     *
     * @param newName The new name
     * @throws ObjectStoreException if there is a problem renaming the underlying bag.
     *         If that happens, this bag will not be marked as deleted, and a new bag
     *         will not be created.
     * @return A new invalid bag with the given name.
     */
    protected InvalidBag rename(String newName) throws ObjectStoreException {
        InvalidBag renamed = new InvalidBag(newName, type, description, dateCreated, os,
                savedBagId, profileId, osb, uosw);
        SavedBag sb = renamed.storeSavedBag();
        renamed.savedBagId = sb.getId();
        deleted = true;
        return renamed;
    }

    /** @return the bags's name **/
    public String getName() {
        return name;
    }

    /**
     * @return the bag's name
     */
    public String getTitle() {
        return name;
    }

    /** @return the number of items in this bag. **/
    public int getSize() {
        return getContents().size();
    }

    /** @return the bag's (now defunct) type **/
    public String getType() {
        return type;
    }

    /** @return the bags's DOB **/
    public Date getDateCreated() {
        return dateCreated;
    }

    /** @return an optional string describing the bag. Never null. **/
    public String getDescription() {
        return (description == null) ? "" : description;
    }

    /** @return the production DB level collection of object ids. */
    public ObjectStoreBag getOsb() {
        return osb;
    }

    /** @return The state of this bag. Always returns "NOT CURRENT" */
    public String getState() {
        return BagState.NOT_CURRENT.toString();
    }

    /** @return The connection to the user-profile DB **/
    public ObjectStoreWriter getUserProfileWriter() {
        return uosw;
    }

    /**
     * Delete this bag from the userprofile database, bag should not be used after this method has
     * been called. Delete the ids from the production database too.
     * @throws ObjectStoreException if problem deleting bag
     */
    @Override
    public void delete() throws ObjectStoreException {
        super.delete();
        if (!deleted) {
            SavedBag savedBag = (SavedBag) uosw.getObjectStore().getObjectById(savedBagId,
                    SavedBag.class);
            uosw.delete(savedBag);
            deleteAllBagValues();
        }
        this.deleted = true;
    }

    /**
     * Fix this bag by changing its type. If fixed, the DB will be corrected to store the new state,
     * and this bag will be marked as deleted.
     * @param newType The new type of this list. This must be a valid type in the current model.
     * @return A valid InterMineBag.
     * @throws UnknownBagTypeException If the new type is not in the current model.
     * @throws ObjectStoreException If there is a problem saving state to the DB.
     */
    public InterMineBag amendTo(String newType)
        throws UnknownBagTypeException, ObjectStoreException {
        if (os.getModel().getClassDescriptorByName(newType) == null) {
            throw new UnknownBagTypeException(newType + " is not a valid class name");
        }
        fireEvent(new DeletionEvent(this));
        InvalidBag amended = new InvalidBag(name, newType, description, dateCreated, os,
                savedBagId, profileId, osb, uosw);
        SavedBag sb = amended.storeSavedBag();
        deleted = true;
        return new InterMineBag(os, sb.getId(), uosw);
    }

}
