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

import java.util.List;

import org.flymine.metadata.Model;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;

/**
 * Gets the Results of a Query from an underlying store.
 *
 * @author Andrew Varley
 */
public interface ObjectStore
{
    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public Results execute(Query q) throws ObjectStoreException;

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the start row
     * @param limit the maximum number of rows to return
     * @param optimise true if it is expected that optimising the query will improve performance
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit, boolean optimise)
        throws ObjectStoreException;

    /**
     * Get an object from the ObjectStore by giving an example. The returned object
     * (if present) will have the same primary keys as the example object.
     *
     * @param id the ID of the object to fetch
     * @return the object from the ObjectStore or cache, or null if none exists
     * @throws ObjectStoreException if an error occurs during retrieval of the object
     */
    public FlyMineBusinessObject getObjectById(Integer id) throws ObjectStoreException;

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
     * @return an object which is the lookup key for the cache entry. This is useful to the caller
     * for the purpose of ensuring the entry does not expire from the cache. To endure this, the
     * caller merely needs to keep a strong reference to this returned value.
     */
    public Object cacheObjectById(Integer id, FlyMineBusinessObject obj);

    /**
     * Completely empties the getObjectById cache. The objectstore must guarantee that the
     * next time any object is mentioned, it must not be taken from the cache.
     */
    public void flushObjectById();

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
     * @param q Flymine Query on which to count rows
     * @return the number of rows that will be produced by query
     * @throws ObjectStoreException if an error occurs counting the query
     */
    public int count(Query q) throws ObjectStoreException;

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
     * this method throws an exception.
     *
     * @param o an example object
     * @param fieldnames a List of fieldnames
     * @return a FlyMineBusinessObject from the objectstore, or null if none fits
     * @throws ObjectStoreException if there are too many matches, or some other error occurs
     */
    public FlyMineBusinessObject getObjectByExample(FlyMineBusinessObject o, List fieldNames)
        throws ObjectStoreException;
}
