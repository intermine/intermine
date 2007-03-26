package org.intermine.objectstore.webservice;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.InterMineException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.webservice.ser.InterMineString;
import org.intermine.objectstore.webservice.ser.SerializationUtil;
import org.intermine.util.Util;
import org.intermine.util.PropertiesUtil;


/**
 * The server side of an ObjectStore webservice. This should be run in
 * session scope, ie. there is one example of this object per user.
 *
 * @author Andrew Varley
 */
public class ObjectStoreServer
{
    private int nextQueryId = 0;
    private ObjectStore os;
    private Map registeredResults = new HashMap();


    /**
     * Construct an ObjectStoreServer that communicates with an ObjectStore
     * given by the webservice.os property
     *
     * @throws Exception if the property 'os.webservice' is missing or invalid
     */
    public ObjectStoreServer() throws Exception {
        Properties props = PropertiesUtil.getPropertiesStartingWith("webservice");
        props = PropertiesUtil.stripStart("webservice", props);
        String osAlias = props.getProperty("os");
        if (osAlias == null) {
            throw new ObjectStoreException("No 'webservice.os' property found, must be "
                                           + "set to initialise ObjectStoreServer.");
        }
        this.os = ObjectStoreFactory.getObjectStore(osAlias);
    }


    /**
     * Register a query with this class. This is useful to avoid
     * repeated transfer of query objects across the network. We
     * actually store a Results object so that we can change the batch
     * size, rather than passing execute requests straight on the the
     * underlying ObjectStore.
     *
     * @param query the IqlQuery to register - NOTE: this IqlQuery should not be a normal IqlQuery.
     * Instead, it should be a mangled invalid IqlQuery as produced by ObjectStoreClient for
     * the purposes of sending over the network.
     * @return an id representing the query
     * @throws ObjectStoreException if an error occurs with the underlying ObjectStore
     */
    public int registerQuery(IqlQuery query) throws ObjectStoreException {
        if (query == null) {
            throw new NullPointerException("query should not be null");
        }
        try {
            query.setParameters(SerializationUtil.collectionToObjects(query.getParameters(), os));
            synchronized (registeredResults) {
                registeredResults.put(new Integer(++nextQueryId), os.execute(query.toQuery()));
            }
            return nextQueryId;
        } catch (ObjectStoreException e) {
            throw (ObjectStoreException) Util.verboseException(e);
        } catch (RuntimeException e) {
            throw (RuntimeException) Util.verboseException(e);
        }
    }

    /**
     * Lookup a previously registered Results object
     *
     * @param queryId a query id
     * @return the previously registered Results
     * @throws IllegalArgumentException if queryId has not been registered
     */
    protected Results lookupResults(int queryId) {
        Integer key = new Integer(queryId);
        if (!registeredResults.containsKey(key)) {
            throw new IllegalArgumentException("Query id " + queryId + " has not been registered");
        }
        return (Results) registeredResults.get(key);
    }

    /**
     * Execute a registered query.
     * In this implementation we actually return rows from the
     * registered Results object. This allows us to change the batch
     * size.
     *
     * @param queryId the id of the query
     * @param start the start row
     * @param limit the maximum number of rows to return
     * @throws ObjectStoreException if an error occurs executing the query
     * @throws InterMineException if an error occurs promoting proxies in the Results
     * @return a List of ResultRows
     */
    public List execute(int queryId, int start, int limit)
            throws ObjectStoreException, InterMineException {
        // Results.range() can throw an IndexOutOfBoundsException if end is off the
        // end of the results set. Here we will catch it and then call range again
        // with size() (which is now known to the results set).

        Results results = lookupResults(queryId);
        List rows = null;
        try {
            try {
                rows = results.subList(start, start + limit);
            } catch (IndexOutOfBoundsException e) {
                //assume start + limit > size and try again (may still fail)
                rows = results.subList(start, results.size());
            }
            // note that serialization will reconstruct resultsrows as plain lists
            for (int i = 0; i < rows.size(); i++) {
                rows.set(i, SerializationUtil.collectionToStrings((List) rows.get(i), getModel()));
            }
            return rows;
        } catch (RuntimeException e) {
            throw (RuntimeException) Util.verboseException(e);
        }
    }

    /**
     * Returns the number of row the query will produce
     *
     * @param queryId the id of the query on which to count rows
     * @return the number of rows to be produced by query
     * @throws ObjectStoreException if an error occurs performing the count
     */
    public int count(int queryId) throws ObjectStoreException {
        try {
            return lookupResults(queryId).size();
        } catch (RuntimeException e) {
            throw (RuntimeException) Util.verboseException(e);
        }
    }

    /**
     * Explain a Query (give estimate for execution time and number of rows).
     *
     * @param queryId the id of the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(int queryId) throws ObjectStoreException {
        try {
            return lookupResults(queryId).getInfo();
        } catch (ObjectStoreException e) {
            throw (ObjectStoreException) Util.verboseException(e);
        } catch (RuntimeException e) {
            throw (RuntimeException) Util.verboseException(e);
        }
    }

    /**
     * Get an object from the ObjectStore by giving an ID.
     *
     * @param id an ID
     * @return the object from the ObjectStore, or null if none exists
     * @throws ObjectStoreException if an error occurs during retrieval of the object
     */
    public InterMineString getObjectById(Integer id) throws ObjectStoreException {
        try {
            return SerializationUtil.objectToString(os.getObjectById(id), getModel());
        } catch (ObjectStoreException e) {
            throw (ObjectStoreException) Util.verboseException(e);
        } catch (RuntimeException e) {
            throw (RuntimeException) Util.verboseException(e);
        }
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
