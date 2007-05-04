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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.ObjectStoreBag;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.SingletonResults;

/**
 * Gets the Results of a Query from an underlying store.
 *
 * @author Andrew Varley
 */
public interface ObjectStore
{
    public static final Map<Object, Integer> SEQUENCE_IGNORE = Collections.emptyMap();

    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     */
    public Results execute(Query q);

    /**
     * Execute a Query on this ObjectStore, returning a SingletonResults
     *
     * @param q the Query to execute
     * @return the results of the Query
     */
    public SingletonResults executeSingleton(Query q);

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the start row
     * @param limit the maximum number of rows to return
     * @param optimise true if it is expected that optimising the query will improve performance
     * @param explain true if the ObjectStore should enforce maximum query running time constraints
     * @param sequence an object representing the state of the database corresponding to when the
     * action that resulted in this execute was started. This number must match the ObjectStore's
     * internal sequence number or a DataChangedException is thrown. The sequence number is
     * incremented each time the data in the objectstore is changed
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            Map<Object, Integer> sequence) throws ObjectStoreException;

    /**
     * Get an object from the ObjectStore by giving an ID.
     *
     * @param id the ID of the object to fetch
     * @return the object from the ObjectStore or cache, or null if none exists
     * @throws ObjectStoreException if an error occurs during retrieval of the object
     */
    public InterMineObject getObjectById(Integer id) throws ObjectStoreException;

    /**
     * Get an object from the ObjectStore by giving an ID and a hint of the Class of the object.
     * WARNING: If you provide the wrong class hint, this method will negatively cache the
     * non-presence of the object which will cause other getObjectById calls to return null even
     * if the object exists in another class.
     *
     * @param id the ID of the object to fetch
     * @param clazz a class of the object
     * @return the object from the ObjectStore or the cache, or null if none exists
     * @throws ObjectStoreException if an error occurs during the retrieval of the object
     */
    public InterMineObject getObjectById(Integer id, Class clazz) throws ObjectStoreException;

    /**
     * Get an objects from the ObjectStore that have the IDs in the ids colection
     *
     * @param ids the IDs of the objects to fetch
     * @return the objects from the ObjectStore or cache
     * @throws ObjectStoreException if an error occurs during retrieval of the object
     */
    public List getObjectsByIds(Collection ids) throws ObjectStoreException;

    /**
     * Prefetches an object into the objectstore getObjectById cache. This method doesn't
     * actually <u>have</u> to do anything - it is merely a hint to the objectstore that a
     * particular operation is likely to be required in the near future.
     *
     * <p>This method is provided primarily to help speed up our data loader. The method may block
     * until the prefetch has been completed. However, the prefetch can be done outside of any
     * synchronised areas of code, allowing the time-critical synchronised areas of code to access
     * the object from the cache.
     *
     * @param id the ID of the object to prefetch
     */
    public void prefetchObjectById(Integer id);

    /**
     * Removes an entry from the objectstore getObjectById cache. The objectstore must
     * guarantee that the next time this example object is requested by getObjectById, the
     * objectstore explicitly fetches the object from the database. Obviously, if the objectstore
     * does not have a getObjectById cache, this method will do nothing.
     *
     * @param id the ID of the object to invalidate
     */
    public void invalidateObjectById(Integer id);

    /**
     * Places an entry into the objectstore getObjectById cache. This method (like prefetch) is
     * merely a hint, and provides no guarantees. The method takes the object provided, and creates
     * a lookup in the getObjectById cache, so that subsequent requests for that object do not
     * access the database. If there is no cache, this method will do nothing.
     *
     * @param id the ID of the object
     * @param obj a fully populated object, as loaded from the database, or null to negatively
     * cache
     * @return an object which is softly-held part of the cache entry. This is useful to the caller
     * for the purpose of ensuring the entry does not expire from the cache. To endure this, the
     * caller merely needs to keep a strong reference to this returned value.
     */
    public Object cacheObjectById(Integer id, InterMineObject obj);

    /**
     * Completely empties the getObjectById cache. The objectstore must guarantee that the
     * next time any object is mentioned, it must not be taken from the cache.
     */
    public void flushObjectById();

    /**
     * Gets a object from the cache if it is present. If the object is not in the cache, then no
     * attempt is made to retrieve it from the database, and null is returned. A trivial
     * implementation may just return null always for this method.
     *
     * @param id the ID of the object
     * @return the object, or null
     */
    public InterMineObject pilferObjectById(Integer id);

    /**
     * Explain a Query (give estimate for execution time and number of rows).
     *
     * @param q the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException;

    /**
     * Counts the number of rows the query will produce
     *
     * @param q InterMine Query on which to count rows
     * @param sequence an object representing the state of the database corresponding to when the
     * action that resulted in this execute was started. This number must match the ObjectStore's
     * internal sequence number or a DataChangedException is thrown. The sequence number is
     * incremented each time the data in the objectstore is changed
     * @return the number of rows that will be produced by query
     * @throws ObjectStoreException if an error occurs counting the query
     */
    public int count(Query q, Map<Object, Integer> sequence) throws ObjectStoreException;

    /**
     * Return the metadata associated with this ObjectStore
     *
     * @return the Model
     */
    public Model getModel();

    /**
     * Return an object from the objectstore that has the fields mentioned in the list set to the
     * same values as the fields in the provided example object. If there are no objects in the
     * objectstore like that, then this method returns null. If there are more than one object, then
     * this method throws an IllegalArgumentException.
     *
     * @param o an example object
     * @param fieldNames a Set of fieldnames
     * @return a InterMineObject from the objectstore, or null if none fits
     * @throws ObjectStoreException if an underlying error occurs
     */
    public InterMineObject getObjectByExample(InterMineObject o, Set fieldNames)
        throws ObjectStoreException;

    /**
     * Return whether or not this ObjectStore gives a performance improvement when multiple
     * simultaneous are made. Note that ALL Objectstore must be multi-threading safe. If this
     * method returns true, then the ObjectStore probably handles multiple connections to the
     * database. The Results class uses this to work out whether or not to do prefetching.
     *
     * @return true if one should do multiple simultaneous operations
     */
    public boolean isMultiConnection();

    /**
     * Return the sequence number representing the state of the ObjectStore. This number is
     * incremented each time the data in the ObjectStore is changed.
     *
     * @param tables a Set of independent database components to get data for
     * @return an object representing the current database state
     */
    public Map<Object, Integer> getSequence(Set<Object> tables);

    /**
     * Get the maximum LIMIT that can be used in an SQL query without throwing an
     * ObjectStoreLimitReachedException
     * @return the maximum limit
     */
    public int getMaxLimit();

    /** 
     * Get the maximum range start index a that can be accessed in a Results object without throwing
     * an ObjectStoreLimitReachedException
     * @return the maximum offset
     */
    public int getMaxOffset();

    /**
     * Get the maximum time a query may take before throwing an ObjectStoreQueryDurationException
     * @return the maximum query time
     */
    public long getMaxTime();

    /**
     * Gets an ID number which is unique in the database.
     *
     * @return an Integer
     * @throws ObjectStoreException if a problem occurs
     */
    public Integer getSerial() throws ObjectStoreException;
    
    /**
     * Returns a new empty ObjectStoreBag object that is valid for this ObjectStore.
     *
     * @return an ObjectStoreBag
     * @throws ObjectStoreException if an error occurs fetching a new ID
     */
    public ObjectStoreBag createObjectStoreBag() throws ObjectStoreException;
}
