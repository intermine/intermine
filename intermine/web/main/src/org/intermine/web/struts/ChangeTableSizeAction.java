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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import org.intermine.web.logic.results.PageOutOfRangeException;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Changes the size of the results displayed.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class ChangeTableSizeAction extends InterMineAction
{
    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception ServletException if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws ServletException {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        
        try {
            pt.setPageSize(Integer.parseInt(request.getParameter("pageSize")));
        } catch (PageOutOfRangeException e) {
            recordMessage(new ActionMessage("results.maxoffsetreached"), request);
        }
        
        return new ForwardParameters(mapping.findForward("results"))
                .addParameter("table", request.getParameter("table"))
                .addParameter("page", "" + pt.getPage())
                .addParameter("size", "" + pt.getPageSize())
                .addParameter("trail", request.getParameter("trail")).forward();
    }
}
