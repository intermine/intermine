package org.flymine.objectstore.safe;

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

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryCloner;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.metadata.Model;

/**
 * Provides a safe implementation of an objectstore - that is, an implementation that works
 * correctly if passed a Query object that has been modified since being used previously.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreSafeImpl implements ObjectStore
{
    private ObjectStore os;
    
    /**
     * Creates an instance, from another ObjectStore instance.
     *
     * @param os an ObjectStore object to use
     */
    public ObjectStoreSafeImpl(ObjectStore os) {
        this.os = os;
    }

    /**
     * @see ObjectStore#execute(Query)
     */
    public Results execute(Query q) throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q));
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean)
     */
    public List execute(Query q, int start, int limit, boolean optimise)
            throws ObjectStoreException {
        return os.execute(QueryCloner.cloneQuery(q), start, limit, optimise);
    }

    /**
     * @see ObjectStore#getObjectByExample
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        return os.getObjectByExample(obj);
    }

    /**
     * @see ObjectStore#prefetchObjectByExample
     */
    public void prefetchObjectByExample(Object obj) {
        os.prefetchObjectByExample(obj);
    }

    /**
     * @see ObjectStore#invalidateObjectByExample
     */
    public void invalidateObjectByExample(Object obj) {
        os.invalidateObjectByExample(obj);
    }

    /**
     * @see ObjectStore#cacheObjectByExample
     */
    public Object cacheObjectByExample(Object obj, Object obj2) {
        return os.cacheObjectByExample(obj, obj2);
    }

    /**
     * @see ObjectStore#flushObjectByExample
     */
    public void flushObjectByExample() {
        os.flushObjectByExample();
    }

    /**
     * @see ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return os.estimate(QueryCloner.cloneQuery(q));
    }

    /**
     * @see ObjectStore#count
     */
    public int count(Query q) throws ObjectStoreException {
        return os.count(QueryCloner.cloneQuery(q));
    }

    /**
     * @see ObjectStore#getModel
     */
    public Model getModel() {
        return os.getModel();
    }
}
