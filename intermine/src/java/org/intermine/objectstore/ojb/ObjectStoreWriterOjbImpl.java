package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.TransactionAbortedException;

import org.flymine.objectstore.ObjectStoreWriterAbstractImpl;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStore;

import org.flymine.util.ModelUtil;

/**
 * Implementation of ObjectStoreWriter that uses OJB as its underlying store
 *
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class ObjectStoreWriterOjbImpl extends ObjectStoreWriterAbstractImpl
{
    protected PersistenceBroker pb = null;

    /**
     * No argument constructor for testing purposes
     */
    protected ObjectStoreWriterOjbImpl() {
    }

    /**
     * Constructs an ObjectStoreWriterOjbImpl interfacing with an OJB instance
     * NB There can be multiple ObjectStoreWriters per Database, each holding a PersistenceBroker
     *
     * @param os the ObjectStore that we wish to write to
     * @throws ObjectStoreException if there is any problem with the underlying ObjectStore
     */
    public ObjectStoreWriterOjbImpl(ObjectStore os) throws ObjectStoreException {
        super(os);
        try {
            pb = ((ObjectStoreOjbImpl) os).getPersistenceBroker();
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * @see ObjectStoreWriter#store
     */
    public void store(Object o) throws ObjectStoreException {
        boolean valid = false;
        try {
            valid = ModelUtil.hasValidKey(o, model);
        } catch (Exception e) {
            throw new ObjectStoreException("Error in checking primary key fields:" + e);
        }
        if (!valid) {
            throw new ObjectStoreException("Cannot store " + o + ": primary key is not set");
        }
        try {
            pb.store(o);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * @see ObjectStoreWriter#delete
     */
    public void delete(Object o) throws ObjectStoreException {
        try {
            pb.delete(o);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * @see ObjectStoreWriter#isInTransaction
     */
    public boolean isInTransaction() throws ObjectStoreException {
        boolean result;
        try {
            return pb.isInTransaction();
        } catch (PersistenceBrokerException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * @see ObjectStoreWriter#beginTransaction
     */
    public void beginTransaction() throws ObjectStoreException {
        try {
            pb.beginTransaction();
        } catch (TransactionInProgressException e) {
            throw new ObjectStoreException("Cannot begin transaction: "
                                           + "a transaction is already in progress");
        } catch (TransactionAbortedException e) {
            throw new ObjectStoreException("Cannot begin transaction: "
                                           + "transaction aborted");
        }
    }

    /**
     * @see ObjectStoreWriter#commitTransaction
     */
    public void commitTransaction() throws ObjectStoreException {
        try {
            pb.commitTransaction();
        } catch (TransactionNotInProgressException e) {
            throw new ObjectStoreException("Cannot commit transaction: "
                                           + "there is no transaction is in progress");
        } catch (TransactionAbortedException e) {
            throw new ObjectStoreException("Cannot abort transaction: "
                                           + "transaction aborted");
        }
    }

    /**
     * @see ObjectStoreWriter#abortTransaction
     */
    public void abortTransaction() throws ObjectStoreException {
        try {
            pb.abortTransaction();
        } catch (TransactionNotInProgressException e) {
            throw new ObjectStoreException("Cannot commit transaction: "
                                           + "there is no transaction is in progress");
        }
    }
}
