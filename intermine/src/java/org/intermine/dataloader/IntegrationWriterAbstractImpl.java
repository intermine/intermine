package org.flymine.dataloader;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Abstract implementation of ObjectStoreIntegrationWriter.  To retain
 * O/R mapping independence concrete subclasses should delegate writing to
 * a mapping tool specific implementation of ObjectStoreWriter.
 *
 * @author Richard Smith
 * @author Matthew Wakeling
 */

public abstract class IntegrationWriterAbstractImpl implements IntegrationWriter
{
    protected String dataSource;
    protected ObjectStoreWriter osw;

    /**
     * Constructs a new instance of an IntegrationWriter
     *
     * @param dataSource the name of the data source
     * @param osw an instance of an ObjectStoreWriter, which we can use to access the database
     */
    public IntegrationWriterAbstractImpl(String dataSource, ObjectStoreWriter osw) {
        this.dataSource = dataSource;
        this.osw = osw;
    }

    /**
     * Search database for object matching the given example object (i.e. primary key search)
     *
     * @param o the example object
     * @return the retieved object
     * @throws ObjectStoreException if an error occurs retieving the object
     */
    public Object getObjectByExample(Object o) throws ObjectStoreException {
        return osw.getObjectStore().getObjectByExample(o);
    }

    /**
     * Given a new object from a data source find whether corresponding object exists in
     * ObjectStore and if so which fields the current data source has permission to write to.
     *
     * @param obj new object from datasource
     * @return details of object in database and which fields can be overridden
     * @throws ObjectStoreException if anything goes wrong retrieving object
     */
    public abstract IntegrationDescriptor getByExample(Object obj) throws ObjectStoreException;

    /**
     * Store an object in this ObjectStore, abstract.
     *
     * @param o the object to store
     * @param skeleton is this a skeleton object?
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public abstract void store(Object o, boolean skeleton) throws ObjectStoreException;

    /**
     * Store an object in this ObjectStore, delegates to internal ObjectStoreWriter.
     *
     * @param o the object to store
     * @throws ObjectStoreException if an error occurs during storage of the object
     */
    public void store(Object o) throws ObjectStoreException {
        osw.store(o);
    }

    /**
     * Delete an object from this ObjectStore, delegate to internal ObjectStoreWriter.
     *
     * @param o the object to delete
     * @throws ObjectStoreException if an error occurs during deletion of the object
     */
    public void delete(Object o) throws ObjectStoreException {
        osw.delete(o);
    }

    /**
     * Check whether the ObjectStore is performing a transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @return true if in a transaction, false otherwise
     * @throws ObjectStoreException if an error occurs the check
     */
    public boolean isInTransaction() throws ObjectStoreException {
        return osw.isInTransaction();
    }

    /**
     * Request that the ObjectStore begins a transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is in progress, or is aborted
     */
    public void beginTransaction() throws ObjectStoreException {
        osw.beginTransaction();
    }

    /**
     * Request that the ObjectStore commits and closes the transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is not in progress, or is aborted
     */
    public void commitTransaction() throws ObjectStoreException {
        osw.commitTransaction();
    }

    /**
     * Request that the ObjectStore aborts and closes the transaction, delegate to internal
     * ObjectStoreWriter.
     *
     * @throws ObjectStoreException if a transaction is not in progress
     */
    public void abortTransaction() throws ObjectStoreException {
        osw.abortTransaction();
    }

    /**
     * @see ObjectStoreWriter.getObjectStore
     */
    public ObjectStore getObjectStore() {
        return osw.getObjectStore();
    }
}
