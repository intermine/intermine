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

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryHelper;

/**
 * Implementation of <strong>Action</strong> that changes part of a Query
 *
 * @author Richard Smith
 * @author Kim Rutherford
 */

public class QueryAliasChangeAction extends DispatchAction
{
    /**
     * Remove an alias from the current query.
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
        
        String alias = request.getParameter("alias");
        Query q = (Query) session.getAttribute(Constants.QUERY);

        if (q == null) {
            return mapping.findForward("buildquery");
        }

        QueryClass qc = (QueryClass) q.getReverseAliases().get(alias);

        if (qc != null) {
            if (q != null) {
                QueryHelper.removeFromQuery(q, qc);

                if (q.getFrom().size() == 0) {
                    session.removeAttribute(Constants.QUERY);
                }
                
                session.removeAttribute("queryClass");
            }
        }

        return mapping.findForward("buildquery");
    }

    /**
     * Edit an alias from the current query.
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
    public ActionForward edit(ActionMapping mapping,
                              ActionForm form,
                              HttpServletRequest request,
                              HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        
        String alias = request.getParameter("alias");
        Query q = (Query) session.getAttribute(Constants.QUERY);
        
        if (q == null) {
            return mapping.findForward("buildquery");
        }
        
        QueryClass qc = (QueryClass) q.getReverseAliases().get(alias);

        session.setAttribute("queryClass", qc);

        return mapping.findForward("buildquery");
    }
}

