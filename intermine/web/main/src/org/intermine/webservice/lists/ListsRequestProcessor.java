package org.intermine.webservice.lists;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

/**
 * Request processor for ListsService that process request, validates it and returns 
 * parsed input as a parameter object.
 * @author Jakub Kulaviak
 **/
public class ListsRequestProcessor
{
    private HttpServletRequest request;
    /** Name of parameter with id of object. **/
    static final public String OBJECT_ID_PARAMETER = "objectId";

    /**
     * ListsRequestProcessor constructor.
     * @param request request
     */
    public ListsRequestProcessor(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns parameters of web service as a parameter object. 
     * @return ListsService input
     */
    public ListsServiceInput getInput() {
        ListsServiceInput ret = new ListsServiceInput();
        Integer objectId = null;
        try {
            objectId = Integer.parseInt(
                    request.getParameter(ListsRequestProcessor.OBJECT_ID_PARAMETER));
        } catch (Throwable t) {
            ret.addError("invalid parameter: " + ListsRequestProcessor.OBJECT_ID_PARAMETER);
        }
        ret.setObjectId(objectId);
        return ret;
    }
}
