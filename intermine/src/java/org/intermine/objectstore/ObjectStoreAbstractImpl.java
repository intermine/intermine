package org.flymine.objectstore;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;
import java.util.Iterator;
import java.lang.reflect.Field;

import org.flymine.metadata.Model;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryCreator;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.util.CacheMap;
import org.flymine.util.PropertiesUtil;
import org.flymine.util.TypeUtil;

import org.apache.log4j.Logger;

/**
 * Abstract implementation of the ObjectStore interface. Used to provide uniformity
 * between different ObjectStore implementations.
 *
 * @author Andrew Varley
 */
public abstract class ObjectStoreAbstractImpl implements ObjectStore
{
    protected static final Logger LOG = Logger.getLogger(ObjectStoreAbstractImpl.class);

    protected Model model;
    protected int maxOffset = Integer.MAX_VALUE;
    protected int maxLimit = Integer.MAX_VALUE;
    protected long maxTime = Long.MAX_VALUE;
    protected CacheMap cache = new CacheMap();

    protected int getObjectOps = 0;
    protected int getObjectHits = 0;
    protected int getObjectPrefetches = 0;

    /**
     * No-arg constructor for testing purposes
     */
    protected ObjectStoreAbstractImpl() {
    }

    /**
     * Construct an ObjectStore with some metadata
     * @param model the name of the model
     */
    protected ObjectStoreAbstractImpl(Model model) {
        this.model = model;
        Properties props = PropertiesUtil.getPropertiesStartingWith("os.query");
        props = PropertiesUtil.stripStart("os.query", props);
        maxLimit = Integer.parseInt((String) props.get("max-limit"));
        maxOffset = Integer.parseInt((String) props.get("max-offset"));
        maxTime = Long.parseLong((String) props.get("max-time"));
    }

    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public Results execute(Query q) throws ObjectStoreException {
        return new Results(q, this);
    }

    /**
     * @see ObjectStore#getObjectByExample
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        getObjectOps++;
        if (getObjectOps % 1000 == 0) {
            LOG.error("getObjectByExample called " + getObjectOps + " times. Cache hits: "
                    + getObjectHits + ". Prefetches: " + getObjectPrefetches);
        }
        boolean contains = true;
        Object cached = null;
        Object cacheKey = QueryCreator.createQueryForExampleObject(obj, model).toString();
        synchronized (cache) {
            cached = cache.get(cacheKey);
            if (cached == null) {
                contains = cache.containsKey(cacheKey);
            }
        }
        if (contains) {
            getObjectHits++;
            return cached;
        }
        Object fromDb = internalGetObjectByExample(obj);
        synchronized (cache) {
            cached = cache.get(cacheKey);
            if (cached == null) {
                contains = cache.containsKey(cacheKey);
            }
            if (contains) {
                fromDb = cached;
            } else {
                cache.put(cacheKey, fromDb);
            }
        }
        return fromDb;
    }

    /**
     * Internal service method for getObjectByExample.
     *
     * @param obj the object to get
     * @return an object from the database
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    protected Object internalGetObjectByExample(Object obj) throws ObjectStoreException {
        Results results = execute(QueryCreator.createQueryForExampleObject(obj, model));
        results.setNoOptimise();

        if (results.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                                               + "this primary key");
        }
        if (results.size() == 1) {
            Object o = ((Object []) results.get(0))[0];
            try {
                promoteProxies(o);
            } catch (Exception e) {
                throw new ObjectStoreException("Problem promoting proxies", e);
            }
            return o;
        }
        return null;
    }

    /**
     * @see ObjectStore#prefetchObjectByExample
     */
    public void prefetchObjectByExample(Object obj) {
        getObjectPrefetches++;
        try {
            getObjectByExample(obj);
        } catch (Exception e) {
            // We can ignore this - it's only a hint.
        }
    }

    /**
     * @see ObjectStore#invalidateObjectByExample
     */
    public void invalidateObjectByExample(Object obj) {
        Object key = QueryCreator.createQueryForExampleObject(obj, model).toString();
        synchronized (cache) {
            cache.remove(key);
        }
    }

    /**
     * @see ObjectStore#cacheObjectByExample
     */
    public Object cacheObjectByExample(Object obj, Object obj2) {
        Object key = QueryCreator.createQueryForExampleObject(obj, model).toString();
        synchronized (cache) {
            cache.put(key, obj2);
        }
        return key;
    }

    /**
     * @see ObjectStore#flushObjectByExample
     */
    public void flushObjectByExample() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * Checks the start and limit to see whether they are inside the
     * hard limits for this ObjectStore
     *
     * @param start the start row
     * @param limit the number of rows
     * @throws ObjectStoreLimitReachedException if the start is greater than the
     * maximum start allowed or the limit greater than the maximum
     * limit allowed
     */
    protected void checkStartLimit(int start, int limit) throws ObjectStoreLimitReachedException {
        if (start > maxOffset) {
            throw (new ObjectStoreLimitReachedException("offset parameter (" + start
                                            + ") is greater than permitted maximum ("
                                            + maxOffset + ")"));
        }
        if (limit > maxLimit) {
            throw (new ObjectStoreLimitReachedException("number of rows required (" + limit
                                            + ") is greater than permitted maximum ("
                                            + maxLimit + ")"));
        }
    }

    /**
     * @see ObjectStore#getModel
     */
    public Model getModel() {
        return model;
    }

    /**
     * Takes an Object, and promotes all the proxies in it.
     *
     * @param obj an Object to process
     * @throws ObjectStoreException if something goes wrong
     */
    protected void promoteProxies(Object obj) throws ObjectStoreException {
        if (obj == null) {
            return;
        }
        Class cls = obj.getClass();
        for (Iterator fieldIter = TypeUtil.getFields(cls).iterator(); fieldIter.hasNext();) {
            String fieldName = ((Field) fieldIter.next()).getName();
            try {
                Object fieldValue = TypeUtil.getFieldValue(obj, fieldName);
                if (fieldValue instanceof LazyReference) {
                    ((LazyReference) fieldValue).setObjectStore(this);
                } else if (fieldValue instanceof LazyCollection) {
                    Query query = ((LazyCollection) fieldValue).getQuery();
                    TypeUtil.setFieldValue(obj, fieldName, new SingletonResults(query, this));
                }
            } catch (IllegalAccessException e) {
                throw new ObjectStoreException(e);
            }
        }
    }
}

