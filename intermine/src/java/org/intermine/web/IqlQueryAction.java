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
import org.apache.struts.util.MessageResources;

import org.flymine.objectstore.query.fql.FqlQuery;
import org.flymine.objectstore.query.Query;

/**
 * Implementation of <strong>Action</strong> that runs a Query
 *
 * @author Andrew Varley
 */

public class FqlQueryAction extends LookupDispatchAction
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
    public ActionForward run(ActionMapping mapping,
                             ActionForm form,
                             HttpServletRequest request,
                             HttpServletResponse response)
        throws Exception {

        // Extract attributes we will need
        MessageResources messages = getResources(request);
        HttpSession session = request.getSession();

        FqlQueryForm queryform = (FqlQueryForm) form;

        Query q = new FqlQuery(queryform.getQuerystring(),
                               "org.flymine.model.testmodel").toQuery();
        session.setAttribute(Constants.QUERY, q);

        return (mapping.findForward("runquery"));

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
    public ActionForward view(ActionMapping mapping,
                              ActionForm form,
                              HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {

        // Extract attributes we will need
        MessageResources messages = getResources(request);
        HttpSession session = request.getSession();

        FqlQueryForm queryform = (FqlQueryForm) form;

        String queryString = queryform.getQuerystring();

        if (queryString == null || queryString.length() == 0) {
            session.setAttribute(Constants.QUERY, null);
        } else {
            Query q = new FqlQuery(queryString, "org.flymine.model.testmodel").toQuery();
            session.setAttribute(Constants.QUERY, q);
        }

        return (mapping.findForward("buildquery"));

    }

    /**
     * Distributes the actions to the necessary methods, by providing a Map from action to
     * the name of a method.
     *
     * @return a Map
     */
    protected Map getKeyMethodMap() {
        Map map = new HashMap();
        map.put("button.run", "run");
        map.put("button.view", "view");
        return map;
    }

}
