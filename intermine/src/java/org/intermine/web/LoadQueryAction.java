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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import java.util.Map;

/**
 * Implementation of <strong>Action</strong> that set the current Query for
 * the session from a saved Query.
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */

public class LoadQueryAction extends Action
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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        LoadQueryForm rqForm = (LoadQueryForm) form;
        String queryName = rqForm.getQueryName();

        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);

        if ((savedQueries != null) && savedQueries.containsKey(queryName)) {
            session.setAttribute("query", savedQueries.get(queryName));
            session.removeAttribute("queryClass");
            session.removeAttribute("ops");
            session.removeAttribute("constraints");
        }

        return (mapping.findForward("buildquery"));
    }
}
