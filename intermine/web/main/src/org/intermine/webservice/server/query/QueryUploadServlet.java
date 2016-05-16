package org.intermine.webservice.server.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.core.NoServiceException;
import org.intermine.webservice.server.core.WebServiceServlet;

/**
 * Runs the query Upload Service to handle user query uploads.
 * @author Alexis Kalderimis
 *
 */
public class QueryUploadServlet extends WebServiceServlet
{

    private static final long serialVersionUID = -7802363779951976507L;

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        switch (method) {
            case POST:
                return new QueryUploadService(api);
            default:
                throw new NoServiceException();
        }
    }

}
