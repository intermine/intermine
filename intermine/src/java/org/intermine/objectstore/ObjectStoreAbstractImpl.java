package org.flymine.objectstore;

import java.util.List;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;

import org.flymine.sql.query.ExplainResult;

/**
 * Abstract implementation of the ObjectStore interface. Used to provide uniformity
 * between different ObjectStore implementations.
 *
 * @author Andrew Varley
 */
public abstract class ObjectStoreAbstractImpl implements ObjectStore
{
    protected int maxOffset;
    protected int maxLimit;
    protected long maxTime;

    /**
     * No argument constructor
     */
    protected ObjectStoreAbstractImpl() {
    }

    /**
     * Execute a Query on this ObjectStore
     *
     * @param q the Query to execute
     * @return the results of the Query
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public abstract Results execute(Query q) throws ObjectStoreException;

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the start row
     * @param end the end row
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public abstract List execute(Query q, int start, int end) throws ObjectStoreException;


    /**
     * Runs an EXPLAIN on the query without a LIMIT or OFFSET.
     *
     * @param q the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public abstract ExplainResult estimate(Query q) throws ObjectStoreException;

    /**
     * Runs an EXPLAIN for the given query with specified start and end parameters.  This
     * gives estimated time for a single 'page' of the query.
     *
     * @param q the query to explain
     * @param start first row required, numbered from zero
     * @param end the number of the last row required, numbered from zero
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explining the query
     */
    public abstract ExplainResult estimate(Query q, int start, int end) throws ObjectStoreException;

    /**
     * Gets the maximum number of rows that the ObjectStore will return in the results of a query
     *
     * @return the maximum number of rows allowed in a query object from this ObjectStore
     */
    public int getMaxRowsInResult() {
        return maxOffset + maxLimit;
    }

}
