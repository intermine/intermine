package org.flymine.objectstore.ojb;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.TransactionAbortedException;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStoreWriterAbstractImpl;
import java.util.HashSet;

/**
 * Implementation of ObjectStoreWriter that uses OJB as its underlying store
 *
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class ObjectStoreWriterOjbImpl extends ObjectStoreWriterAbstractImpl
{
    protected HashSet transactionCacheHolder = null;
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
        if (!(os instanceof ObjectStoreOjbImpl)) {
            throw new ObjectStoreException("ObjectStoreWriterOjbImpl expected to be constructed "
                                           + "with an ObjectStoreOjbImpl");
        }
        try {
            pb = ((ObjectStoreOjbImpl) os).getPersistenceBroker();
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
    }
    
    /**
     * close the persistence broker so that it is returned to pool
     */
    protected void finalize() {
        if (pb != null) {
            pb.close();
        }
    }

    /**
     * @see org.flymine.objectstore.ObjectStoreWriter#store(Object)
     */
    public void store(Object o) throws ObjectStoreException {
        boolean valid = false;
        try {
            valid = hasValidKey(o);
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
        Object cacheKey = ((ObjectStoreAbstractImpl) os).cacheObjectByExample(o, o);
        if (transactionCacheHolder != null) {
            transactionCacheHolder.add(cacheKey);
        }
    }

    /**
     * @see org.flymine.objectstore.ObjectStoreWriter#delete(Object)
     */
    public void delete(Object o) throws ObjectStoreException {
        try {
            pb.delete(o);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
        Object cacheKey = ((ObjectStoreAbstractImpl) os).cacheObjectByExample(o, null);
        if (transactionCacheHolder != null) {
            transactionCacheHolder.add(cacheKey);
        }
    }

    /**
     * @see org.flymine.objectstore.ObjectStoreWriter#isInTransaction
     */
    public boolean isInTransaction() throws ObjectStoreException {
        try {
            return pb.isInTransaction();
        } catch (PersistenceBrokerException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * @see org.flymine.objectstore.ObjectStoreWriter#beginTransaction
     */
    public void beginTransaction() throws ObjectStoreException {
        transactionCacheHolder = new HashSet();
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
     * @see org.flymine.objectstore.ObjectStoreWriter#commitTransaction
     */
    public void commitTransaction() throws ObjectStoreException {
        transactionCacheHolder = null;
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
     * @see org.flymine.objectstore.ObjectStoreWriter#abortTransaction
     */
    public void abortTransaction() throws ObjectStoreException {
        transactionCacheHolder = null;
        os.flushObjectByExample();
        try {
            pb.abortTransaction();
        } catch (TransactionNotInProgressException e) {
            throw new ObjectStoreException("Cannot commit transaction: "
                                           + "there is no transaction is in progress");
        }
    }
}
