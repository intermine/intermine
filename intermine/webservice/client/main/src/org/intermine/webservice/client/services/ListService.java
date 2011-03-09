package org.intermine.webservice.client.services;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

import org.intermine.webservice.client.core.ContentType;
import org.intermine.webservice.client.core.RequestImpl;
import org.intermine.webservice.client.core.Service;
import org.intermine.webservice.client.core.TabTableResult;
import org.intermine.webservice.client.core.Request.RequestType;
import org.intermine.webservice.client.util.HttpConnection;

/**
 * The ListService provides some information about public lists in InterMine like
 * which lists contain specified object.
 *
 * @author Jakub Kulaviak
 **/
public class ListService extends Service
{

    private static final String SERVICE_RELATIVE_URL = "listswithobject";

    /**
     * Use {@link ServiceFactory} instead of constructor for creating this service .
     * @param rootUrl root URL
     * @param applicationName application name
     */
    public ListService(String rootUrl, String applicationName) {
        super(rootUrl, SERVICE_RELATIVE_URL, applicationName);
    }

    private static class ListRequest extends RequestImpl
    {

        public ListRequest(RequestType type, String serviceUrl, ContentType contentType) {
            super(type, serviceUrl, contentType);
        }

        public void setPublicId(String publicId) {
            setParameter("publicId", publicId);
        }

        public void setObjectType(String type) {
            setParameter("type", type);
        }
    }

    /**
     * Returns names of all InterMine public lists containing object with specified publicId.
     * @param publicId id of object of interest, object can have different id in different public
     * databases and InterMine know some of them and searches in all of them contained in
     * InterMine. If there are more corresponding objects for this publicId, then exception
     * is thrown.
     * @param type type of Object - Gene ...
     * @return names of public lists containing specified object
     */
    public List<String> getPublicListsWithObject(String publicId, String type) {
        ListRequest request = new ListRequest(RequestType.POST, getUrl(),
                ContentType.TEXT_TAB);
        request.setPublicId(publicId);
        request.setObjectType(type);
        HttpConnection connection = executeRequest(request);
        List<List<String>> result = new TabTableResult(connection).getData();
        List<String> ret = new ArrayList<String>();
        for (List<String> row : result) {
            if (row.size() > 0) {
                ret.add(row.get(0));
            }
        }
        return ret;
    }
}
