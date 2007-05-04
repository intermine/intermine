package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCreator;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;
import org.intermine.metadata.MetaDataException;

import org.apache.log4j.Logger;

/**
 * Abstract implementation of the ObjectStore interface. Used to provide uniformity
 * between different ObjectStore implementations.
 *
 * @author Andrew Varley
 * @author Matthew Wakeling
 */
public abstract class ObjectStoreAbstractImpl implements ObjectStore
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreAbstractImpl.class);

    protected static Random rand = new Random();

    protected Model model;
    protected int maxOffset = Integer.MAX_VALUE;
    protected int maxLimit = Integer.MAX_VALUE;
    protected long maxTime = Long.MAX_VALUE;
    protected CacheMap cache;

    protected int getObjectOps = 0;
    protected int getObjectHits = 0;
    protected int getObjectPrefetches = 0;
    protected Map<Object, Integer> sequenceNumber = new WeakHashMap<Object, Integer>();
    protected Map<Object, WeakReference<Object>> sequenceKeys
        = new WeakHashMap<Object, WeakReference<Object>>();

    /**
     * No-arg constructor for testing purposes
     */
    protected ObjectStoreAbstractImpl() {
        // empty
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
        LOG.info("Creating new " + getClass().getName() + " with sequence = " + sequenceNumber
                + ", model = \"" + model.getName() + "\"");
        cache = new CacheMap(getClass().getName() + " with sequence = " + sequenceNumber
                + ", model = \"" + model.getName() + "\" getObjectById cache");
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q) {
        return new Results(q, this, getSequence(getComponentsForQuery(q)));
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q) {
        return new SingletonResults(q, this, getSequence(getComponentsForQuery(q)));
    }

    /**
     * Returns a Set of independent components that affect the results of the given Query.
     *
     * @param q a Query
     * @return a Set of objects
     */
    public abstract Set<Object> getComponentsForQuery(Query q);

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return getObjectById(id, InterMineObject.class);
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectById(Integer id, Class clazz) throws ObjectStoreException {
        getObjectOps++;
        if (getObjectOps % 10000 == 0) {
            LOG.info("getObjectById called " + getObjectOps + " times. Cache hits: "
                    + getObjectHits + ". Prefetches: " + getObjectPrefetches);
        }
        boolean contains = true;
        InterMineObject cached = null;
        synchronized (cache) {
            cached = (InterMineObject) cache.get(id);
            if (cached == null) {
                contains = cache.containsKey(id);
            }
        }
        if (contains) {
            getObjectHits++;
            return cached;
        }
        InterMineObject fromDb = internalGetObjectById(id, clazz);
        synchronized (cache) {
            cached = (InterMineObject) cache.get(id);
            if (cached == null) {
                contains = cache.containsKey(id);
            }
            if (contains) {
                fromDb = cached;
            } else {
                cache.put(id, fromDb);
            }
        }
        return fromDb;
    }

    /**
     * Internal service method for getObjectById.
     *
     * @param id the ID of the object to get
     * @param clazz a class of the object
     * @return an object from the database
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    protected InterMineObject internalGetObjectById(Integer id,
            Class clazz) throws ObjectStoreException {
        Results results = execute(QueryCreator.createQueryForId(id, clazz));
        results.setNoOptimise();
        results.setNoExplain();

        if (results.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                                               + "this primary key");
        }
        if (results.size() == 1) {
            InterMineObject o = (InterMineObject) ((ResultsRow) results.get(0)).get(0);
            return o;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List getObjectsByIds(Collection ids) throws ObjectStoreException {
        Results results = execute(QueryCreator.createQueryForIds(ids, InterMineObject.class));
        results.setNoOptimise();
        results.setNoExplain();

        return results;
    }
    
    /**
     * Read the Model from the classpath.
     * @param osAlias the alias of the ObjectStore properties to get the model name from.
     * @param properties the Properties object containing the model name
     * @return a Model
     * @throws MetaDataException if the model can't be read
     */
    protected static Model getModelFromClasspath(String osAlias, Properties properties)
        throws MetaDataException {
        String modelName = properties.getProperty("model");
        if (modelName == null) {
            throw new MetaDataException(osAlias
                                        + " does not have a model specified ("
                                        + modelName + ") - check properties");
        }
        Model classpathModel = Model.getInstanceByName(modelName);
        if (classpathModel == null) {
            throw new MetaDataException("Model is null despite load from classpath");
        }
        return classpathModel;
    }

    /**
     * {@inheritDoc}
     */
    public void prefetchObjectById(Integer id) {
        getObjectPrefetches++;
        try {
            getObjectById(id);
        } catch (Exception e) {
            // We can ignore this - it's only a hint.
        }
    }

    /**
     * {@inheritDoc}
     */
    public void invalidateObjectById(Integer id) {
        synchronized (cache) {
            cache.remove(id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object cacheObjectById(Integer id, InterMineObject obj) {
        synchronized (cache) {
            cache.put(id, obj);
        }
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    public void flushObjectById() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject pilferObjectById(Integer id) {
        synchronized (cache) {
            return (InterMineObject) cache.get(id);
        }
    }

    /**
     * Checks the start and limit to see whether they are inside the
     * hard limits for this ObjectStore
     *
     * @param start the start row
     * @param limit the number of rows
     * @param query the current Query (for adding to error messages)
     * @throws ObjectStoreLimitReachedException if the start is greater than the
     * maximum start allowed or the limit greater than the maximum
     * limit allowed
     */
    protected void checkStartLimit(int start, int limit, Query query)
        throws ObjectStoreLimitReachedException {
        if (start > maxOffset) {
            throw (new ObjectStoreLimitReachedException("offset parameter (" + start
                                            + ") is greater than permitted maximum ("
                                            + maxOffset + ") for query " + query));
        }
        if (limit > maxLimit) {
            throw (new ObjectStoreLimitReachedException("number of rows required (" + limit
                                            + ") is greater than permitted maximum ("
                                            + maxLimit + ") for query " + query));
        }
    }

    /**
     * {@inheritDoc}
     */
    public Model getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames)
            throws ObjectStoreException {
        Query query = QueryCreator.createQueryForExampleObject(model, o, fieldNames);
        List results = execute(query, 0, 2, false, false, SEQUENCE_IGNORE);

        if (results.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                    + "this primary key (" + results.size() + "): " + query.toString());
        }
        if (results.size() == 1) {
            InterMineObject j = (InterMineObject) ((ResultsRow) results.get(0)).get(0);
            return j;
        }
        return null;
    }

    /**
     * Checks a number against the sequence number, and throws an exception if they do not match.
     *
     * @param sequence a Map representing a database state
     * @param q the Query that is to be run
     * @param message some description of the operation that is about to happen
     * @throws DataChangedException if the sequence numbers do not match
     */
    public synchronized void checkSequence(Map<Object, Integer> sequence, Query q, String message)
    throws DataChangedException {
        for (Map.Entry<Object, Integer> entry : sequence.entrySet()) {
            Object key = entry.getKey();
            if (!entry.getValue().equals(sequenceNumber.get(key))) {
                throw new DataChangedException("Sequence numbers do not match - was given " + key
                        + " = " + entry.getValue() + " but needed " + key + " = "
                        + sequenceNumber.get(key) + " for operation \"" + message + q + "\"");
            }
        }
    }

    /**
     * Returns an object representing the current state of the database, for fail-fast concurrency
     * control.
     *
     * @param tables a Set of objects representing independent components of the database
     * @return a Map containing sequence data
     */
    public synchronized Map<Object, Integer> getSequence(Set<Object> tables) {
        Map<Object, Integer> retval = new HashMap<Object, Integer>();
        for (Object key : tables) {
            WeakReference<Object> keyRef = sequenceKeys.get(key);
            Integer s = null;
            if (keyRef != null) {
                Object keyCandidate = keyRef.get();
                if (keyCandidate != null) {
                    key = keyCandidate;
                    s = sequenceNumber.get(key);
                }
            }
            if (s == null) {
                synchronized (rand) {
                    s = new Integer(rand.nextInt());
                }
                sequenceNumber.put(key, s);
                sequenceKeys.put(key, new WeakReference(key));
            }
            retval.put(key, s);
        }
        return retval;
    }

    /**
     * Increments the sequence numbers for the given set of database components.
     *
     * @param tables a Set of objects representing independent components of the database
     */
    public synchronized void changeSequence(Set tables) {
        for (Object key : tables) {
            WeakReference keyRef = sequenceKeys.get(key);
            if (keyRef != null) {
                Object realKey = keyRef.get();
                Integer value = sequenceNumber.get(key);
                if (realKey != null) {
                    sequenceNumber.put(realKey, new Integer(value.intValue() + 1));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxLimit() {
        return maxLimit;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxOffset() {
        return maxOffset;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxTime() {
        return maxTime;
    }

    /**
     * Creates a new empty ObjectStoreBag object that is valid for this ObjectStore.
     *
     * @return an ObjectStoreBag
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public ObjectStoreBag createObjectStoreBag() throws ObjectStoreException {
        return new ObjectStoreBag(getSerial().intValue());
    }
}
