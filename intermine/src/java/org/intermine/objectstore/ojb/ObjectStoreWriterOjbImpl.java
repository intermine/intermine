package org.flymine.objectstore.ojb;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.TransactionAbortedException;
import org.apache.ojb.broker.ta.PersistenceBrokerFactoryFactory;

import org.flymine.sql.Database;
import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;

/**
 * Implementation of ObjectStoreWriter that uses OJB as its underlying store
 *
 * @author Mark Woodbridge
 */
public class ObjectStoreWriterOjbImpl implements ObjectStoreWriter
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
     * @param db the database in which the model resides
     * @param model the name of the model
     * @throws ObjectStoreException if there is any problem with the underlying OJB instance
     */
    public ObjectStoreWriterOjbImpl(Database db, String model) throws ObjectStoreException {
        pb = ((PersistenceBrokerFactoryFlyMineImpl) PersistenceBrokerFactoryFactory.instance())
            .createPersistenceBroker(db, model);
    }

    /**
     * @see ObjectStoreWriter#store
     */
    public void store(Object o) throws ObjectStoreException {
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
