package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.results.TableHelper;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Takes a parameter "type" with value "webapp" or "query" and prints "OK"
 * to the client if everything is ok, otherwise something else.
 * 
 * @author tom riley
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
        if ("webapp".equals(type)) {
            response.getOutputStream().print("OK");
        } else if ("query".equals(type)) {
            ObjectStore os = (ObjectStore) request.getSession().getServletContext()
                .getAttribute(Constants.OBJECTSTORE);
            Query q = new Query();
            QueryClass c = new QueryClass(InterMineObject.class);
            q.addFrom(c);
            q.addToSelect(c);
            Results r = TableHelper.makeResults(os, q);
            TableHelper.initResults(r);
            int size = r.size();
            if (size > 0) {
                response.getOutputStream().print("OK");
            } else {
                response.getOutputStream().print("NO RESULTS");
            }
        }
        return null;
    }
}
