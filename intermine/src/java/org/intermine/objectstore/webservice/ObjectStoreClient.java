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

import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.flymine.objectstore.webservice.ser.SerializationUtil;
import org.flymine.objectstore.ObjectStoreAbstractImpl;
import org.flymine.objectstore.ObjectStoreException;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsInfo;
import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.TypeMapping;
import javax.xml.namespace.QName;

/**
 * ObjectStore implementation that accesses a remote ObjectStore via JAX-RPC.
 *
 * @author Andrew Varley
 */
public class ObjectStoreClient extends ObjectStoreAbstractImpl
{

    protected static Map instances = new HashMap();
    protected Call call;

    protected Map registeredQueries = new IdentityHashMap();

    /**
     * Construct an ObjectStoreClient pointing at an ObjectStore service on a remote URL
     *
     * @param url the URL of the remote ObjectStore
     * @param model the model used by this ObjectStore
     * @throws ObjectStoreException if any error with the remote service
     */
    protected ObjectStoreClient(URL url, Model model) throws ObjectStoreException {
        super(model);
        if (url == null) {
            throw new NullPointerException("url must not be null");
        }
        // Set up the service and call objects so that the session can be maintained
        try {
            Service service = new Service();
            call = (Call) service.createCall();
            call.setMaintainSession(true);
            call.setTargetEndpointAddress(url);

            TypeMapping tm = call.getTypeMapping();
            SerializationUtil.registerMappings(tm);
            Iterator iter = model.getClassDescriptors().iterator();
            while (iter.hasNext()) {
                Class cls = Class.forName(((ClassDescriptor) iter.next()).getName());
                SerializationUtil.registerDefaultMapping(tm, cls);
            }
        } catch (Exception e) {
            throw new ObjectStoreException("Calling remote service failed", e);
        }
    }

    /**
     * Gets a ObjectStoreClient instance for the given properties
     *
     * @param props The properties used to configure an ObjectStoreClient
     * @param model the metadata associated with this objectstore
     * @return the ObjectStoreClient for the given properties
     * @throws IllegalArgumentException if url is invalid
     * @throws ObjectStoreException if there is any problem with the underlying ObjectStore
     */
    public static ObjectStoreClient getInstance(Properties props, Model model)
        throws ObjectStoreException {
        String urlString = props.getProperty("url");
        if (urlString == null) {
            throw new ObjectStoreException("No 'url' property specified for"
                                           + " ObjectStoreClient (check properties file)");
        }
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new ObjectStoreException("URL (" + urlString + ") is invalid", e);
        }
        synchronized (instances) {
            if (!(instances.containsKey(urlString))) {
                instances.put(urlString, new ObjectStoreClient(url, model));
            }
        }
        return (ObjectStoreClient) instances.get(urlString);
    }

    /**
     * Execute a method on the remote ObjectStore web service
     *
     * @param methodName the name of the remote method
     * @param params the parameters to pass to that method
     * @return the resulting Object from the remote method
     * @throws ObjectStoreException if any error occurs
     */
    protected Object remoteMethod(String methodName, Object [] params) throws ObjectStoreException {
        try {
            call.setOperationName(new QName("http://soapinterop.org/", methodName));
            return (Object) call.invoke(params);
        } catch (Exception e) {
            throw new ObjectStoreException("Error communicating with remote server", e);
        }
    }

    /**
     * Get the id for the query if it has already been registered. If not, register it.
     *
     * @param q the Query to get the id for
     * @return the id of the query
     * @throws ObjectStoreException if an error occurs
     */
    protected int getQueryId(Query q) throws ObjectStoreException {
        synchronized (registeredQueries) {
            if (!registeredQueries.containsKey(q)) {
                Integer queryId =  (Integer) remoteMethod("registerQuery",
                                                          new Object [] {new FqlQuery(q)});
                registeredQueries.put(q, queryId);
            }
        }

        return ((Integer) registeredQueries.get(q)).intValue();
    }

    /**
     * Execute a Query on this ObjectStore, asking for a certain range of rows to be returned.
     * This will usually only be called by the Results object returned from
     * <code>execute(Query q)</code>.
     *
     * @param q the Query to execute
     * @param start the start row
     * @param limit the maximum number of rows to return
     * @param optimise true if the query should be optimised
     * @return a List of ResultRows
     * @throws ObjectStoreException if an error occurs during the running of the Query
     */
    public List execute(Query q, int start, int limit, boolean optimise)
        throws ObjectStoreException {
        int queryId = getQueryId(q);
        List results = (List) remoteMethod("execute", new Object [] {new Integer(queryId),
                                                                     new Integer(start),
                                                                     new Integer(limit)});
        try {
            Results.promoteProxies(results, this);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
        return results;
    }

    /**
     * Explain a Query with specified start and limit parameters.
     * This gives estimated time for a single 'page' of the query.
     *
     * @param q the query to explain
     * @return parsed results of EXPLAIN
     * @throws ObjectStoreException if an error occurs explaining the query
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        int queryId = getQueryId(q);
        return (ResultsInfo) remoteMethod("estimate", new Object [] {new Integer(queryId)});
    }

    /**
     * Counts the number of rows the query will produce
     *
     * @param q Flymine Query on which to count rows
     * @return the number of rows that will be produced by query
     * @throws ObjectStoreException if an error occurs with the remote ObjectStore
     */
    public int count(Query q) throws ObjectStoreException {
        int queryId = getQueryId(q);
        return ((Integer) remoteMethod("count", new Object [] {new Integer(queryId)})).intValue();
    }

    /**
     * @see ObjectStore#getObjectByExample
     */
    public Object getObjectByExample(Object obj) throws ObjectStoreException {
        Object object = remoteMethod("getObjectByExample", new Object[] {obj});
       try {
           Results.promoteProxiesInObject(object, this);
        } catch (Exception e) {
            throw new ObjectStoreException(e);
        }
        return object;
    }
}

