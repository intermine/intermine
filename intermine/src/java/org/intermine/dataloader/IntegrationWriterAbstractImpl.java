package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;

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
     * Search database for object matching the given object id
     *
     * @param id the object ID
     * @return the retrieved object
     * @throws ObjectStoreException if an error occurs retieving the object
     */
    public FlyMineBusinessObject getObjectById(Integer id) throws ObjectStoreException {
        return osw.getObjectById(id);
    }

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
     * @see org.flymine.objectstore.ObjectStoreWriter#getObjectStore
     */
    public ObjectStore getObjectStore() {
        return osw.getObjectStore();
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#execute
     */
    public Results execute(Query q) throws ObjectStoreException {
        return osw.execute(q);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#execute
     */
    public List execute(Query q, int start, int limit, boolean optimise)
            throws ObjectStoreException {
        return osw.execute(q, start, limit, optimise);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#prefetchObjectById
     */
    public void prefetchObjectById(Integer id) {
        osw.prefetchObjectById(id);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#invalidateObjectById
     */
    public void invalidateObjectById(Integer id) {
        osw.invalidateObjectById(id);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#cacheObjectById
     */
    public Object cacheObjectById(Integer id, FlyMineBusinessObject obj) {
        return osw.cacheObjectById(id, obj);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#flushObjectById
     */
    public void flushObjectById() {
        osw.flushObjectById();
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return osw.estimate(q);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#count
     */
    public int count(Query q) throws ObjectStoreException {
        return osw.count(q);
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#getModel
     */
    public Model getModel() {
        return osw.getModel();
    }

    /**
     * @see org.flymine.objectstore.ObjectStore#getObjectByExample
     */
    public FlyMineBusinessObject getObjectByExample(FlyMineBusinessObject o, List fieldNames)
            throws ObjectStoreException {
        return osw.getObjectByExample(o, fieldNames);
    }
}
