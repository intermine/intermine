package org.flymine.objectstore;

/**
 * Store, update, and delete objects
 *
 * @author Mark Woodbridge
 */
public interface ObjectStoreWriter
{
    /**
     * Retrieve this Writer's ObjectStore
     *
     * @return the ObjectStore
     */
    public ObjectStore getObjectStore();

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
}
