package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;

/**
 * Store, update, and delete objects
 *
 * @author Mark Woodbridge
 */
public interface ObjectStoreWriter extends ObjectStore
{
    /**
     * Retrieve this Writer's ObjectStore. This ObjectStoreWriter is a read-write extension to the
     * read-only ObjectStore. This ObjectStoreWriter uses a single database connection in order to
     * allow proper transaction support - use the ObjectStore for general read-only access.
     *
     * @return the ObjectStore
     */
    ObjectStore getObjectStore();

    /**
     * Store an object in this ObjectStore.
     * If the ID of this object is not set, it will be set to a previously unused value.
     * If the ID matches the ID of an object already in the objectstore, then an update operation is
     * performed - otherwise a store operation is performed.
     * <br>
     * Attributes and references of the stored object will be set to those of the provided object.
     * Collections of the stored object will contain the union of the contents of any pre-existing
     * stored object, and the contents of the provided object. There is currently no way to remove
     * entries from a collection of a stored object.
     * <br>
     * Any objects referred to by this object will have their ID set in a similar way to this
     * object. This object will be stored with references and collections using those IDs, however
     * those objects will not be stored themselves. Therefore, the objectstore will be inconsistent
     * until those objects are also stored.
     * <br>
     * If bidirectional relationships are not consistent in the objects being stored, then the
     * behaviour of the store method is not defined. Specifically, one-to-one relationships must be
     * stored on both sides. For example, if A has a relationship with B1 in the database, and we
     * store a new A that has a relationship with B2, then we must also store B2 with a relationship
     * back to A (to complete the relationship), and store B1 (to completely break the old
     * relationship),
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    void store(Object o) throws ObjectStoreException;

    /**
     * Delete an object from this ObjectStore
     *
     * @param o the object to delete
     * @throws ObjectStoreException if an error occurs during deletion of the object
     */
    void delete(InterMineObject o) throws ObjectStoreException;

    /**
     * Deletes a set of objects from this ObjectStore.
     *
     * @param qc a QueryClass for the class of objects to delete
     * @param c a Constraint based on the QueryClass to filter the objects to delete, or null to
     * delete all objects
     * @throws ObjectStoreException if an error occurs while deleting the objects
     */
    void delete(QueryClass qc, Constraint c) throws ObjectStoreException;

    /**
     * Place an object in a many-to-many collection of another object.
     * This method provides a way to place an object into a collection in another object without
     * having either object loaded in memory. Only the IDs of the two objects are required, along
     * with the name of the collection.
     *
     * @param hasId the ID of the object that has the collection
     * @param clazz the class of the object that has the collection
     * @param fieldName the name of the collection
     * @param hadId the ID of the object to place in the collection
     * @throws ObjectStoreException if a problem occurs
     */
    void addToCollection(Integer hasId, Class<?> clazz, String fieldName, Integer hadId)
        throws ObjectStoreException;

    /**
     * Adds an element to an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param element an Integer to add to the bag
     * @throws ObjectStoreException if an error occurs
     */
    void addToBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException;

    /**
     * Adds a collection of elements to an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if an error occurs
     */
    void addAllToBag(ObjectStoreBag osb,
            Collection<Integer> coll) throws ObjectStoreException;

    /**
     * Removes an element from an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param element an Integer to add to the bag
     * @throws ObjectStoreException if an error occurs
     */
    void removeFromBag(ObjectStoreBag osb, Integer element) throws ObjectStoreException;

    /**
     * Removes a collection of elements from an ObjectStoreBag.
     *
     * @param osb an ObjectStoreBag
     * @param coll a Collection of Integers
     * @throws ObjectStoreException if an error occurs
     */
    void removeAllFromBag(ObjectStoreBag osb, Collection<Integer> coll) throws ObjectStoreException;

    /**
     * Adds elements to an ObjectStoreBag from the results of a Query. The data may not be loaded
     * into Java, so this is a performance improvement method. For example, in SQL this method
     * may issue a command like "INSERT INTO bag SELECT ...".
     *
     * @param osb an ObjectStoreBag
     * @param q an objectstore Query, which contains only one result column, which is a suitable
     * type for insertion into the given bag
     * @throws ObjectStoreException if something goes wrong
     */
    void addToBagFromQuery(ObjectStoreBag osb, Query q) throws ObjectStoreException;

    /**
     * Replaces the contents of the given Clob with the given String.
     *
     * @param clob the Clob to write to
     * @param text the text to write to the Clob
     * @throws ObjectStoreException if something goes wrong
     */
    void replaceClob(Clob clob, String text) throws ObjectStoreException;

    /**
     * Check whether the ObjectStoreWriter is performing a transaction
     *
     * @return true if in a transaction, false otherwise
     * @throws ObjectStoreException if an error occurs
     */
    boolean isInTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStoreWriter begins a transaction
     *
     * @throws ObjectStoreException if a transaction is in progress, or is aborted
     */
    void beginTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStoreWriter commits and closes the transaction
     *
     * @throws ObjectStoreException if a transaction is not in progress, or is aborted
     */
    void commitTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStoreWriter aborts and closes the transaction
     *
     * @throws ObjectStoreException if a transaction is not in progress
     */
    void abortTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStoreWriter commits and closes the transaction and then opens a new
     * one, without guaranteeing that the operation is finished before this method returns. Note
     * that writes that are made AFTER this method may be treated as if they were BEFORE this method
     * and be committed in the transaction.
     *
     * @throws ObjectStoreException if an error occurs
     */
    void batchCommitTransaction() throws ObjectStoreException;

    /**
     * Closes the connection associated with this ObjectStoreWriter
     *
     * @throws ObjectStoreException if something goes wrong
     */
    void close() throws ObjectStoreException;
}
