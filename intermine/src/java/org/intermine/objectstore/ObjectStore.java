package org.flymine.objectstore;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;

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
     * @param end the end row
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public Results execute(Query q, int start, int end) throws ObjectStoreException;

}
