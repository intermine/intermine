package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.TabTableResult;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.exceptions.ServiceException;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * The QueryService is service that provides some methods for flexible querying InterMine data.
 *  
 * <p>
 * The basic tool for querying data in InterMine is PathQuery object. See examples to see how 
 * to construct PathQuery.  
 * </p> 
 * 
 * @author Jakub Kulaviak
 **/
public class QueryService extends Service
{
    
    private static final String SERVICE_RELATIVE_URL = "query/results";

    /**
     * Use {@link ServiceFactory} instead for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public QueryService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    private static class QueryRequest extends RequestImpl 
    {

        public QueryRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        public void setStart(int start) {
            setParameter("start", start + "");
        }
        
        public void setMaxCount(int maxCount) {
            setParameter("size", maxCount + "");
        }
        
        public void setQueryXml(String xml) {
            setParameter("query", xml);
        }
    }
    
    
    /**
     * Constructs PathQuery from its XML representation. You can use this method
     * for creating PathQuery, modifying it a bit and executing afterwards.
     * @param queryXml PathQuery represented as a XML string
     * @return created PathQuery    
     */
    public PathQuery createPathQuery(String queryXml) {
        ModelService modelService = new ModelService(getRootUrl(), getApplicationName());
        Model model = modelService.getModel();
        Model.addModel(model.getName(), model);
        return PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml), 
                new HashMap<String, List<FieldDescriptor>>());
    }

    /**
     * Returns number of results of specified query.
     * @param query query
     * @return number of results of specified query.
     */
    public int getCount(PathQuery query) {
        return getCount(query.toXml());
    }

    /**
     * Returns number of results of specified query.
     * @param queryXml PathQuery represented as a XML string
     * @return number of results of specified query.
     */
    public int getCount(String queryXml) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), 
                ContentType.TEXT_TAB);
        request.setQueryXml(queryXml);
        request.setParameter("tcount", "");
        HttpConnection connection = executeRequest(request);
        String body = connection.getResponseBodyAsString().trim();
        if (body.length() == 0) {
            throw new ServiceException("Service didn't return any result");
        }
        try {
            return Integer.parseInt(body);
        }  catch (NumberFormatException e) {
            throw new ServiceException("Service returned invalid result. It is not number: " 
                    + body, e);
        }        
    }

    /**
     * Returns results of specified PathQuery. If you expect a lot of results 
     * use getResultIterator() method.
     * @param query query
     * @param start index of first returned result, indexes starts at 1 
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public List<List<String>> getResult(PathQuery query, int start, int maxCount) {
        return getResultInternal(query.toXml(), start, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery as iterator. Use this method if you expects a lot 
     * of results and you would run out of memory. 
     * @param query query
     * @param start index of first returned result, indexes starts at 1 
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */
    public Iterator<List<String>> getResultIterator(PathQuery query, int start, int maxCount) {
        return getResultInternal(query.toXml(), start, maxCount).getIterator();
    }
 
    /**
     * Returns results of specified PathQuery. If you expect a lot of results 
     * use getResultIterator() method.
     * @param queryXml PathQuery represented as a XML string
     * @param start index of first returned result, indexes starts at 1 
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */    
    public List<List<String>> getResult(String queryXml, int start, int maxCount) {
        return getResultInternal(queryXml, start, maxCount).getData();
    }

    /**
     * Returns results of specified PathQuery. Use this method if you expects a lot 
     * of results and you would run out of memory. 
     * @param queryXml PathQuery represented as a XML string
     * @param start index of first returned result, indexes starts at 1 
     * @param maxCount maximum number of returned results
     * @return results of specified PathQuery
     */    
    public Iterator<List<String>> getResultIterator(String queryXml, int start, int maxCount) {
        return getResultInternal(queryXml, start, maxCount).getIterator();
    }
    
    private TabTableResult getResultInternal(String queryXml, int start, int maxCount) {
        QueryRequest request = new QueryRequest(RequestType.POST, getUrl(), 
                ContentType.TEXT_TAB);
        request.setStart(start);
        request.setMaxCount(maxCount);
        request.setQueryXml(queryXml);
        HttpConnection connection = executeRequest(request);
        return new TabTableResult(connection);
    }
}
