package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Changes the size of the results displayed.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
@SuppressWarnings("deprecation")
public class ChangeTableSizeAction extends InterMineAction
{
    /**
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        HttpSession session = request.getSession();
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));

        int pageSize = Integer.parseInt(request.getParameter("pageSize"));
        pt.setPageSize(pageSize);

        return new ForwardParameters(mapping.findForward("results"))
                .addParameter("table", request.getParameter("table"))
                .addParameter("page", "" + pt.getPage())
                .addParameter("trail", request.getParameter("trail")).forward();
    }
}
