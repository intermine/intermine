package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Takes a parameter "type" with value "webapp" or "query" and prints "OK"
 * to the client if everything is ok, otherwise something else.
 *
 * @author Tom Riley
 * @author Richard Smith
 */
public class HeartbeatAction extends InterMineAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        String type = request.getParameter("type");
        if (type == null) {
            type = "webapp";
        }

        if ("webapp".equals(type)) {
            response.getOutputStream().print("OK");
        } else if ("query".equals(type)) {
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();
            Query q = new Query();
            QueryClass c = new QueryClass(InterMineObject.class);
            q.addFrom(c);
            q.addToSelect(c);
            // Add a unique value to the select to avoid caching the query
            QueryValue token = new QueryValue(System.currentTimeMillis());
            q.addToSelect(token);
            Results r = os.execute(q, 1, false, false, false);
            if (r.get(0) != null) {
                response.getOutputStream().print("OK");
            } else {
                response.getOutputStream().print("NO RESULTS");
            }
        }
        return null;
    }
}
