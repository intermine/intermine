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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.rmi.RemoteException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import javax.xml.namespace.QName;

import org.intermine.metadata.Model;
import org.intermine.metadata.MetaDataException;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreAbstractImpl;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.ResultsInfo;
import org.intermine.objectstore.query.iql.IqlQuery;
import org.intermine.objectstore.webservice.ser.InterMineString;
import org.intermine.objectstore.webservice.ser.SerializationUtil;
import org.intermine.objectstore.webservice.ser.MappingUtil;

/**
 * ObjectStore implementation that accesses a remote ObjectStore via JAX-RPC.
 *
 * @author Andrew Varley
 * @author Mark Woodbridge
 */
public class ObjectStoreClient extends ObjectStoreAbstractImpl
{
    protected static Map instances = new HashMap();
    protected Call call;

    protected Map queryIds = new IdentityHashMap();

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
            call = (Call) new Service().createCall();
            call.setMaintainSession(true);
            call.setTargetEndpointAddress(url);

            MappingUtil.registerDefaultMappings(call.getTypeMapping());
        } catch (Exception e) {
            throw new ObjectStoreException("Calling remote service failed", e);
        }
    }
    
    /**
     * Gets a ObjectStoreClient instance for the given properties
     *
     * @param osAlias the alias of this objectstore
     * @param props The properties used to configure an ObjectStoreClient
     * @return the ObjectStoreClient for the given properties
     * @throws ObjectStoreException if there is any problem with the underlying ObjectStore
     */
    public static ObjectStoreClient getInstance(String osAlias, Properties props)
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
                Model classpathModel;
                try {
                    classpathModel = getModelFromClasspath(osAlias, props);
                } catch (MetaDataException metaDataException) {
                    throw new ObjectStoreException("Cannot load model", metaDataException);
                }
                instances.put(urlString, new ObjectStoreClient(url, classpathModel));
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
        call.setOperationName(new QName("", methodName));
        try {
            return call.invoke(params);
        } catch (AxisFault af) { //AxisFault extends RemoteException, but carries no cause
            af.printStackTrace();
            Exception real = null;
            try {
                String message = af.toString(); //<exceptionClassName>:<errorString>
                int firstColon = message.indexOf(':');
                if (firstColon == -1) { // probably a server error string
                    throw new ObjectStoreException(af);
                }
                Class c = Class.forName(message.substring(0, firstColon));
                Constructor cons = c.getConstructor(new Class[] {String.class});
                real = (Exception) cons.newInstance(new Object[] {message.substring(firstColon
                            + 1)});
            } catch (Exception e) {
                throw new ObjectStoreException(e);
            }
            if (real instanceof ObjectStoreException) {
                throw (ObjectStoreException) real;
            } else if (real instanceof RuntimeException) {
                throw (RuntimeException) real;
            } else {
                throw new ObjectStoreException(real);
            }
        } catch (RemoteException re) {
            throw new ObjectStoreException(re);
        }
    }

    /**
     * @see ObjectStore#execute(Query, int, int, boolean, boolean, int)
     */
    public List execute(Query q, int start, int limit, boolean optimise, boolean explain,
            int sequence) throws ObjectStoreException {
        checkStartLimit(start, limit, q);

        if (explain) {
            ResultsInfo estimate = estimate(q);
            if (estimate.getComplete() > getMaxTime()) {
                throw new ObjectStoreException("Estimated time to run query ("
                                               + estimate.getComplete()
                                               + ") greater than permitted maximum ("
                                               + getMaxTime() + ")");
            }
        }
        List results = (List) remoteMethod("execute", new Object [] {getQueryId(q),
                                                                     new Integer(start),
                                                                     new Integer(limit)});
        for (int i = 0; i < results.size(); i++) {
            results.set(i,
               new ResultsRow(SerializationUtil.collectionToObjects((List) results.get(i), this)));
        }
        return results;
    }
    
    /**
     * @see ObjectStore#estimate
     */
    public ResultsInfo estimate(Query q) throws ObjectStoreException {
        return (ResultsInfo) remoteMethod("estimate", new Object [] {getQueryId(q)});
    }

    /**
     * @see ObjectStore#count
     */
    public int count(Query q, int sequence) throws ObjectStoreException {
        return ((Integer) remoteMethod("count", new Object [] {getQueryId(q)})).intValue();
    }

    /**
     * @see ObjectStore#getObjectById
     */
    public InterMineObject internalGetObjectById(Integer id) throws ObjectStoreException {
        return SerializationUtil.stringToObject((InterMineString)
                                                remoteMethod("getObjectById", new Object[] {id}),
                                                this);
    }

    /**
     * Get the id for the query if it has already been registered. If not, register it.
     *
     * @param q the Query to get the id for
     * @return the id of the query
     * @throws ObjectStoreException if an error occurs
     */
    protected Integer getQueryId(Query q) throws ObjectStoreException {
        if (!queryIds.containsKey(q)) {
            IqlQuery fq = new IqlQuery(q);
            fq.setParameters(SerializationUtil.collectionToStrings(fq.getParameters(), model));
            queryIds.put(q,
                         (Integer) remoteMethod("registerQuery", new Object[] {fq}));
        }
        return (Integer) queryIds.get(q);
    }

    /**
     * @see ObjectStore#isMultiConnection
     */
    public boolean isMultiConnection() {
        return true;
    }

    /**
     * @see ObjectStore#getSequence
     */
    public int getSequence() {
        return 0;
    }

    /**
     * @see ObjectStore#getSerial
     */
    public Integer getSerial() throws ObjectStoreException {
        throw new ObjectStoreException("Cannot getSerial() on an ObjectStoreClient");
    }
}
