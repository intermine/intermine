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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flymine.FlyMineException;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.metadata.Model;

/**
 * The server side of an ObjectStore webservice. This should be run in
 * session scope, ie. there is one example of this object per user.
 *
 * @author Andrew Varley
 */
public class ObjectStoreServer
{
    private int nextQueryId = 0;
    private Map registeredResults = new HashMap();
    private ObjectStore os;

    /**
     * Construct an ObjectStoreServer that communicates with an ObjectStore
     * given by the objectstoreserver.os property
     *
     * @throws Exception if the property 'os.default' is missing or invalid
     */
    public ObjectStoreServer() throws Exception {
        this(ObjectStoreFactory.getObjectStore());
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
     * Register a query with this class. This is useful to avoid
     * repeated transfer of query objects across the network. We
     * actually store a Results object so that we can change the batch
     * size, rather than passing execute requests straight on the the
     * underlying ObjectStore.
     *
     * @param query the FqlQuery to register
     * @return an id representing the query
     * @throws ObjectStoreException if an error occurs with the underlying ObjectStore
     */
    public int registerQuery(FqlQuery query) throws ObjectStoreException {
        if (query == null) {
            throw new NullPointerException("query should not be null");
        }

        try {
            Query q = query.toQuery();
            synchronized (registeredResults) {
                registeredResults.put(new Integer(++nextQueryId), os.execute(q));
            }
        } catch (ObjectStoreException e) {
            StringWriter message = new StringWriter();
            PrintWriter pMessage = new PrintWriter(message);
            e.printStackTrace(pMessage);
            ObjectStoreException toThrow = new ObjectStoreException(message.toString());
            throw toThrow;
        } catch (RuntimeException e) {
            try {
                StringWriter message = new StringWriter();
                PrintWriter pMessage = new PrintWriter(message);
                e.printStackTrace(pMessage);
                Class c = e.getClass();
                Constructor cons = c.getConstructor(new Class[] {String.class});
                RuntimeException toThrow = (RuntimeException) cons.newInstance(
                        new Object[] {message.toString()});
                throw toThrow;
            } catch (NoSuchMethodException e2) {
                throw e;
            } catch (InstantiationException e2) {
                throw e;
            } catch (IllegalAccessException e2) {
                throw e;
            } catch (InvocationTargetException e2) {
                throw e;
            }
        }
        return nextQueryId;
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
     * @throws FlyMineException if an error occurs promoting proxies in the Results
     * @return a List of ResultRows
     */
    public List execute(int queryId, int start, int limit)
            throws ObjectStoreException, FlyMineException {
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
        } catch (RuntimeException e) {
            try {
                StringWriter message = new StringWriter();
                PrintWriter pMessage = new PrintWriter(message);
                e.printStackTrace(pMessage);
                Class c = e.getClass();
                Constructor cons = c.getConstructor(new Class[] {String.class});
                RuntimeException toThrow = (RuntimeException) cons.newInstance(
                        new Object[] {message.toString()});
                throw toThrow;
            } catch (NoSuchMethodException e2) {
                throw e;
            } catch (InstantiationException e2) {
                throw e;
            } catch (IllegalAccessException e2) {
                throw e;
            } catch (InvocationTargetException e2) {
                throw e;
            }
        }
        return rows;
    }

    /**
     * Returns the number of row the query will produce
     *
     * @param queryId the id of the query on which to count rows
     * @return the number of rows to be produced by query
     * @throws ObjectStoreException if an error occurs
     */
    public int count(int queryId) throws ObjectStoreException {
        return lookupResults(queryId).size();
    }

    /**
     * Explain a Query (give estimate for execution time and number of rows).
     *
     * @param queryId the id of the query to estimate rows for
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(int queryId) throws ObjectStoreException {
        return lookupResults(queryId).getInfo();
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
