package org.flymine.objectstore.webservice;

import java.util.List;

import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.sql.query.ExplainResult;
import org.flymine.metadata.Model;

/**
 * The server side of an ObjectStore webservice. This should be run in
 * session scope, ie. there is one example of this object per user.
 */

public class ObjectStoreServer {

    /**
     * Register a query with this class. This is useful to avoid repeated
     * transfer of query objects across the network.
     *
     * @param query the Query to register
     * @return an id representing the query
     */
    public int registerQuery(Query query) {
        return 0;
    }
    /**
     * Register a query (as a String) with this class. This is useful
     * to avoid repeated transfer of query objects across the network.
     *
     * @param query the Query to register
     * @return an id representing the query
     */
    public int registerQuery(String query) {
        return 0;
    }

    /**
     * Execute a registered query
     *
     * @param queryId the id of the query
     * @param start the start row
     * @param limit the maximum number of rows to return
     * @return a List of ResultRows
     */
    public List execute(int queryId, int start, int end) {
        return null;
    }

    /**
     * Returns the number of row the query will produce
     *
     * @param queryId the id of the query on which to count rows
     * @return the number of rows to be produced by query
     */
    public int count(int queryId) {
        return 72;
    }

    /**
     * Explain a Query (give estimate for execution time and number of rows).
     *
     * @param queryId the id of the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ExplainResult estimate(int queryId) {
        return null;
    }

    /**
     * Explain a Query with specified start and limit parameters.
     * This gives estimated time for a single 'page' of the query.
     *
     * @param queryId the query to explain
     * @param start first row required, numbered from zero
     * @param limit the maximum number og rows to return
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ExplainResult estimate(int queryId, int start, int limit) throws ObjectStoreException {
        return null;
    }

    /**
     * Get an object from the ObjectStore by giving an example. The returned object
     * (if present) will have the same primary keys as the example object.
     *
     * @param obj an example object
     * @return the equivalent object from the ObjectStore, or null if none exists
     * @throws ObjectStoreException if an error occurs during retrieval of the object
     * @throws IllegalArgumentException if obj does not have all its primary key fields set
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        return null;
    }

    /**
     * Return the metadata associated with this ObjectStore
     *
     * @return the Model
     */
    public Model getModel() {
        return null;
    }
}
