package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

//import java.io.PrintWriter;
//import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCreator;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.util.CacheMap;
import org.intermine.util.PropertiesUtil;
import org.intermine.metadata.MetaDataException;

import org.apache.log4j.Logger;

/**
 * Abstract implementation of the ObjectStore interface. Used to provide uniformity
 * between different ObjectStore implementations.
 *
 * @author Andrew Varley
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
    protected int sequenceNumber;

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
        synchronized (rand) {
            sequenceNumber = rand.nextInt();
        }
        LOG.info("Creating new " + getClass().getName() + " with sequence = " + sequenceNumber
                + ", model = \"" + model.getName() + "\"");
        cache = new CacheMap(getClass().getName() + " with sequence = " + sequenceNumber
                + ", model = \"" + model.getName() + "\" getObjectById cache");
    }

    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public Results execute(Query q) throws ObjectStoreException {
        return new Results(q, this, getSequence());
    }

    /**
     * @see ObjectStore#getObjectById(Integer)
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException {
        return getObjectById(id, InterMineObject.class);
    }

    /**
     * @see ObjectStore#getObjectById(Integer, Class)
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
     * @see ObjectStore#getObjectsByIds(Collection)
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
     * @see ObjectStore#prefetchObjectById(Integer)
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
     * @see ObjectStore#invalidateObjectById(Integer)
     */
    public void invalidateObjectById(Integer id) {
        synchronized (cache) {
            cache.remove(id);
            sequenceNumber++;
        }
    }

    /**
     * @see ObjectStore#cacheObjectById(Integer, InterMineObject)
     */
    public Object cacheObjectById(Integer id, InterMineObject obj) {
        synchronized (cache) {
            cache.put(id, obj);
        }
        return obj;
    }

    /**
     * @see ObjectStore#flushObjectById()
     */
    public void flushObjectById() {
        synchronized (cache) {
            cache.clear();
            sequenceNumber++;
        }
    }

    /**
     * @see ObjectStore#pilferObjectById(Integer)
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
     * @see ObjectStore#getModel()
     */
    public Model getModel() {
        return model;
    }

    /**
     * @see ObjectStore#getObjectByExample(InterMineObject, Set)
     */
    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames)
            throws ObjectStoreException {
        Query query = QueryCreator.createQueryForExampleObject(model, o, fieldNames);
        Results results = execute(query);
        results.setNoOptimise();

        if (results.size() > 1) {
            throw new IllegalArgumentException("More than one object in the database has "
                                               + "this primary key (" + results.size() + "): "
                                               + query.toString());
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
     * @param sequence an integer
     * @param q the Query that is to be run
     * @param message some description of the operation that is about to happen
     * @throws DataChangedException if the sequence numbers do not match
     */
    public void checkSequence(int sequence, Query q, String message) throws DataChangedException {
        if (sequence != getSequence()) {
            DataChangedException e = new DataChangedException("Sequence numbers do not match - was "
                    + "given " + sequence + " but needed " + getSequence() + " for operation \""
                    + message + q + "\"");
            throw e;
            /*e.fillInStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            String m = sw.toString();
            int index = m.indexOf("at junit.framework.TestCase.runBare");
            LOG.warn(index < 0 ? m : m.substring(0, index));*/
        }
    }

    /**
     * Returns the current sequence number.
     *
     * @return an integer
     */
    public int getSequence() {
        return sequenceNumber;
    }

    /**
     * @see ObjectStore#getMaxLimit
     */
    public int getMaxLimit() {
        return maxLimit;
    }

    /**
     * @see ObjectStore#getMaxOffset
     */
    public int getMaxOffset() {
        return maxOffset;
    }

    /**
     * @see ObjectStore#getMaxTime
     */
    public long getMaxTime() {
        return maxTime;
    }
}

