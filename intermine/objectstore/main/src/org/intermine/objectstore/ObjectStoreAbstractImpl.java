package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.Clob;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCreator;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;

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
    // Optimiser will use a default query parse time if none is provided from properties
    protected Long maxQueryParseTime = null;
    protected CacheMap<Integer, InterMineObject> cache;

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

        if (props.get("max-limit") != null) {
            maxLimit = Integer.parseInt((String) props.get("max-limit"));
        }

        if (props.get("max-offset") != null) {
            maxOffset = Integer.parseInt((String) props.get("max-offset"));
        }

        if (props.get("max-time") != null) {
            maxTime = Long.parseLong((String) props.get("max-time"));
        }

        if (props.get("max-query-parse-time") != null) {
            maxQueryParseTime = Long.parseLong((String) props.get("max-query-parse-time"));
        }

        LOG.info("Creating new " + getClass().getName() + " with sequence = " + sequenceNumber
                + ", model = \"" + model.getName() + "\"");
        cache = new CacheMap<Integer, InterMineObject>(getClass().getName() + " with sequence = "
                + sequenceNumber + ", model = \"" + model.getName() + "\" getObjectById cache");
    }

    /**
     * {@inheritDoc}
     */
    public ObjectStoreWriter getNewWriter() throws ObjectStoreException {
        throw new UnsupportedOperationException("This ObjectStore does not have a writer");
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q) {
        Results retval = new Results(q, this, getSequence(getComponentsForQuery(q)));
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public Results execute(Query q, int batchSize, boolean optimise, boolean explain,
            boolean prefetch) {
        Results retval = new Results(q, this, getSequence(getComponentsForQuery(q)));
        retval.setBatchSize(batchSize);
        if (!optimise) {
            retval.setNoOptimise();
        }
        if (!explain) {
            retval.setNoExplain();
        }
        if (!prefetch) {
            retval.setNoPrefetch();
        }
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q) {
        SingletonResults retval = new SingletonResults(q, this, getSequence(getComponentsForQuery(
                        q)));
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public SingletonResults executeSingleton(Query q, int batchSize, boolean optimise,
            boolean explain, boolean prefetch) {
        SingletonResults retval = new SingletonResults(q, this, getSequence(getComponentsForQuery(
                        q)));
        retval.setBatchSize(batchSize);
        if (!optimise) {
            retval.setNoOptimise();
        }
        if (!explain) {
            retval.setNoExplain();
        }
        if (!prefetch) {
            retval.setNoPrefetch();
        }
        retval.setImmutable();
        return retval;
    }

    /**
     * {@inheritDoc}
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
    public InterMineObject getObjectById(Integer id, Class<? extends InterMineObject> clazz)
        throws ObjectStoreException {
        getObjectOps++;
        if (getObjectOps % 10000 == 0) {
            LOG.info("getObjectById called " + getObjectOps + " times. Cache hits: "
                    + getObjectHits + ". Prefetches: " + getObjectPrefetches);
        }
        boolean contains = true;
        InterMineObject cached = null;
        synchronized (cache) {
            cached = cache.get(id);
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
            cached = cache.get(id);
            if (cached == null) {
                contains = cache.containsKey(id);
            }
            if (contains) {
                fromDb = cached;
            } else {
                cacheObjectById(id, fromDb);
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
            Class<? extends InterMineObject> clazz) throws ObjectStoreException {
        Results results = new Results(QueryCreator.createQueryForId(id, clazz), this,
                SEQUENCE_IGNORE);
        results.setBatchSize(2);
        results.setNoOptimise();
        results.setNoExplain();
        results.setNoPrefetch();

        if (results.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                                               + "this primary key");
        }
        if (results.size() == 1) {
            InterMineObject o = (InterMineObject) ((ResultsRow<?>) results.get(0)).get(0);
            return o;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "cast", "unchecked", "rawtypes" })
    public List<InterMineObject> getObjectsByIds(Collection<Integer> ids)
        throws ObjectStoreException {
        Results results = executeSingleton(QueryCreator.createQueryForIds(ids,
                        InterMineObject.class), 1000, false, false, false);

        return (List<InterMineObject>) ((List) results);
    }

    /**
     * Read the Model from the classpath.
     *
     * @param osAlias the alias of the ObjectStore properties to get the model name from.
     * @param properties the Properties object containing the model name
     * @return a Model
     * @throws MetaDataException if the model can't be read
     */
    protected static Model getModelFromClasspath(String osAlias, Properties properties)
        throws MetaDataException {
        String modelName = properties.getProperty("model");
        if (modelName == null) {
            throw new MetaDataException(osAlias + " does not have a model specified ("
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
            return cache.get(id);
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

    @Override
    public <T extends InterMineObject> T getObjectByExample(T o, Set<String> fieldNames)
        throws ObjectStoreException {
        Query query = QueryCreator.createQueryForExampleObject(model, o, fieldNames);
        List<ResultsRow<Object>> results = execute(query, 0, 2, false, false, SEQUENCE_IGNORE);

        if (results.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                    + "this primary key (" + results.size() + "): " + query.toString());
        }
        if (results.size() == 1) {
            @SuppressWarnings("unchecked")
            T j = (T) results.get(0).get(0);
            return j;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends InterMineObject> Collection<T> getObjectsByExample(
            T o,
            Set<String> fieldNames)
        throws ObjectStoreException {
        Query query = QueryCreator.createQueryForExampleObject(model, o, fieldNames);
        List<ResultsRow<Object>> results = execute(query, 0, 2, false, false, SEQUENCE_IGNORE);
        List<T> ret = new ArrayList<T>();

        for (ResultsRow<Object> row: results) {
            ret.add((T) row.get(0));
        }
        return ret;
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
                sequenceKeys.put(key, new WeakReference<Object>(key));
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
    public synchronized void changeSequence(Set<Object> tables) {
        for (Object key : tables) {
            WeakReference<Object> keyRef = sequenceKeys.get(key);
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
     * {@inheritDoc}
     */
    public Long getMaxQueryParseTime() {
        return maxQueryParseTime;
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

    /**
     * Creates a new empty Clob that is valid for this ObjectStore.
     *
     * @return a Clob
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public Clob createClob() throws ObjectStoreException {
        return new Clob(getSerial().intValue());
    }
}
