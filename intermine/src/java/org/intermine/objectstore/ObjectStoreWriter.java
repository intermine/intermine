package org.flymine.objectstore;

import java.lang.reflect.Field;

/**
 * Store, update, and delete objects
 *
 * @author Mark Woodbridge
 */
public interface ObjectStoreWriter
{
    /**
     * Store an object in this ObjectStore
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(Object o) throws ObjectStoreException;

    /**
     * Delete an object from this ObjectStore
     *
     * @param o the object to delete
     * @throws ObjectStoreException if an error occurs during deletion of the object
     */
    public void delete(Object o) throws ObjectStoreException;

    /**
     * Check whether the ObjectStore is performing a transaction
     *
     * @return true if in a transaction, false otherwise
     * @throws ObjectStoreException if an error occurs the check
     */
    public boolean isInTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStore begins a transaction
     *
     * @throws ObjectStoreException if a transaction is in progress, or is aborted
     */
    public void beginTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStore commits and closes the transaction
     *
     * @throws ObjectStoreException if a transaction is not in progress, or is aborted
     */
    public void commitTransaction() throws ObjectStoreException;

    /**
     * Request that the ObjectStore aborts and closes the transaction
     *
     * @throws ObjectStoreException if a transaction is not in progress
     */
    public void abortTransaction() throws ObjectStoreException;

    /**
     * Get an object from the ObjectStore by giving an example. The returned object
     * (if present) will have the same primary keys as the example object.
     *
     * @param obj an example object
     * @return the equivalent object from the ObjectStore, or null if none exists
     * @throws ObjectStoreException if an error occurs during retrieval of the object
     * @throws IllegalArgumentException if obj does not have all its primary key fields set
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException ;

    /**
     * Return an integer describing the type of relationship the given field represents,
     * where relationship types are 1:1, 1:N, N:1, M:N and "not a relationship".
     *
     * @param field object describing the field in querstion
     * @return int to describe the relationship type
     */
    public int describeRelation(Field field);

}
