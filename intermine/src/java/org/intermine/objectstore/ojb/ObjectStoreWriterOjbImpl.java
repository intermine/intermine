package org.flymine.objectstore.ojb;

import java.lang.reflect.Field;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.TransactionInProgressException;
import org.apache.ojb.broker.TransactionNotInProgressException;
import org.apache.ojb.broker.TransactionAbortedException;

import org.flymine.objectstore.ObjectStoreWriter;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryHelper;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.util.ModelUtil;


/**
 * Implementation of ObjectStoreWriter that uses OJB as its underlying store
 *
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class ObjectStoreWriterOjbImpl implements ObjectStoreWriter
{
    protected PersistenceBroker pb = null;
    protected ObjectStoreOjbImpl os = null;

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
        try {
            this.os = (ObjectStoreOjbImpl) os;
            pb = this.os.getPersistenceBroker();
        } catch (ClassCastException e) {
            throw new ObjectStoreException(e);
        }
    }

    /**
     * @see ObjectStoreWriter#store
     */
    public void store(Object o) throws ObjectStoreException {
        if (!ModelUtil.hasValidKey(o)) {
            throw new ObjectStoreException("Cannot store " + o
                                           + ": primary key is not set (key = "
                                           + ModelUtil.getKey(o.getClass()) + ")");
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

    /**
     * @see ObjectStoreWriter#getObjectByExample
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        Query q = QueryHelper.createQueryForExampleObject(obj);
        Results res = os.execute(q);

        if (res.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                                               + "this primary key");
        }
        if (res.size() == 1) {
            Object ret = ((ResultsRow) res.get(0)).get(0);
            return ret;
        }
        return null;
    }


    /**
     * Return an integer describing the type of relationship the given field represents,
     * where relationship types are 1:1, 1:N, N:1, M:N and "not a relationship".
     *
     * @param field object describing the field in querstion
     * @return int to describe the relationship type
     */
    public int describeRelation(Field field) {
        return ((PersistenceBrokerFlyMine) pb).describeRelation(field);
    }

}
