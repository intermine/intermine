package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import org.apache.struts.actions.LookupDispatchAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.Model;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.objectstore.query.*;


/**
 * Implementation of <strong>Action</strong> that runs a Query
 *
 * @author Richard Smith
 * @author Mark Woodbridge
 */
public class QueryBuildAction extends LookupDispatchAction
{
    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward submit(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Query query = (Query) session.getAttribute("query");
        if (query == null) {
            query = new Query();
        }

        QueryClass qc = (QueryClass) session.getAttribute("queryClass");
        session.removeAttribute("queryClass");

        if (qc != null) {
            Model model = ((DisplayModel) session.getAttribute("model")).getModel();
            QueryBuildForm queryBuildForm = (QueryBuildForm) form;

            QueryHelper.addToQuery(query, qc, queryBuildForm.getFieldValues(),
                                    queryBuildForm.getFieldOps(), model);
            session.setAttribute("query", query);
        }
        return mapping.findForward("buildquery");
    }


    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward remove(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        Query query = (Query) session.getAttribute("query");
        if (query == null) {
            query = new Query();
        }

        QueryClass qc = (QueryClass) session.getAttribute("queryClass");
        session.removeAttribute("queryClass");

        if (qc != null) {
            Model model = ((DisplayModel) session.getAttribute("model")).getModel();

            QueryHelper.removeFromQuery(query, qc);

            if (query.getFrom().size() > 0) {
                session.setAttribute("query", query);
            } else {
                session.removeAttribute("query");
            }
        }
        return mapping.findForward("buildquery");
    }


    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("button.submit", "submit");
        map.put("button.remove", "remove");
        return map;
    }
}

