package org.flymine.objectstore.webservice;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.sql.query.ExplainResult;
import org.flymine.metadata.Model;
import org.flymine.util.PropertiesUtil;

/**
 * The server side of an ObjectStore webservice. This should be run in
 * session scope, ie. there is one example of this object per user.
 *
 * @author Andrew Varley
 */

public class ObjectStoreServer
{

    private int nextQueryId = 0;
    private Map registeredQueries = new HashMap();
    private ObjectStore os;

    /**
     * Construct an ObjectStoreServer that communicates with an ObjectStore
     * given by the objectstoreserver.os property
     *
     * @throws Exception if the property 'objectstoreserver.os' is missing or invalid
     */
    public ObjectStoreServer() throws Exception {
        // Configure from properties:
        // objectstoreserver.os = <name of objectstore to refer requests to>
         Properties props = PropertiesUtil.getPropertiesStartingWith("objectstoreserver");
         props = PropertiesUtil.stripStart("objectstoreserver", props);
         String osAlias = props.getProperty("os");
         if (osAlias == null) {
             throw new ObjectStoreException("No 'os' property specified for ObjectStoreServer"
                                            + " (check properties file)");
         }
         this.os = ObjectStoreFactory.getObjectStore(osAlias);
    }

    /**
     * Construct an ObjectStoreServer that communicates with the given ObjectStore
     *
     * @param os the ObjectStore to pass calls to
     */
    public ObjectStoreServer(ObjectStore os) {
        this.os = os;
    }


    /**
     * Register a query with this class. This is useful to avoid repeated
     * transfer of query objects across the network.
     *
     * @param query the FqlQuery to register
     * @return an id representing the query
     */
    public int registerQuery(FqlQuery query) {
        if (query == null) {
            throw new NullPointerException("query should not be null");
        }

        Query q = query.toQuery();
        synchronized (registeredQueries) {
            registeredQueries.put(new Integer(++nextQueryId), q);
        }
        return nextQueryId;
    }

    /**
     * Lookup a previously registered query
     *
     * @param queryId a query id
     * @return the previously registered query
     * @throws IllegalArgumentException if queryId has not been registered
     */
    protected Query lookupQuery(int queryId) {
        Integer key = new Integer(queryId);
        if (!registeredQueries.containsKey(key)) {
            throw new IllegalArgumentException("Query id " + queryId + " has not been registered");
        }
        return (Query) registeredQueries.get(key);
    }

    /**
     * Execute a registered query
     *
     * @param queryId the id of the query
     * @param start the start row
     * @param limit the maximum number of rows to return
     * @param optimise whether to optimise
     * @throws ObjectStoreException if an error occurs executing the query
     * @return a List of ResultRows
     */
    public List execute(int queryId, int start, int limit, boolean optimise)
            throws ObjectStoreException {
        return os.execute(lookupQuery(queryId), start, limit, optimise);
    }

    /**
     * Returns the number of row the query will produce
     *
     * @param queryId the id of the query on which to count rows
     * @return the number of rows to be produced by query
     * @throws ObjectStoreException if an error occurs
     */
    public int count(int queryId) throws ObjectStoreException {
        return os.count(lookupQuery(queryId));
    }

    /**
     * Explain a Query (give estimate for execution time and number of rows).
     *
     * @param queryId the id of the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ExplainResult estimate(int queryId) throws ObjectStoreException {
        return os.estimate(lookupQuery(queryId));
    }

    /**
     * Explain a Query with specified start and limit parameters.
     * This gives estimated time for a single 'page' of the query.
     *
     * @param queryId the query to explain
     * @param start first row required, numbered from zero
     * @param limit the maximum number og rows to return
     * @param optimise whether to optimise
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ExplainResult estimate(int queryId, int start, int limit, boolean optimise)
            throws ObjectStoreException {
        return os.estimate(lookupQuery(queryId), start, limit, optimise);
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
        return os.getObjectByExample(obj);
    }

    /**
     * Return the metadata associated with this ObjectStore
     *
     * @return the Model
     */
    public Model getModel() {
        return os.getModel();
    }
}
