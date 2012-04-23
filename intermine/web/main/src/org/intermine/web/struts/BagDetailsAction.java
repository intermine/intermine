package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action that builds a PagedCollection to view a bag.
 * Redirects to results.do
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 *
 */
public class BagDetailsAction extends Action
{
    /**
     * Set up session attributes for the bag details page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        String bagName = request.getParameter("bagName");
        if (bagName == null) {
            bagName = request.getParameter("name");
        }
        String trail = null;
        if (request.getParameter("trail") != null) {
            trail = request.getParameter("trail");
        }

        String identifier = "bag." + bagName;

        PagedTable pt = SessionMethods.getResultsTable(session, identifier);
        if (pt != null) {
            if (trail != null) {
                trail += "|results." + pt.getTableid();
            } else {
                trail = "|results." + pt.getTableid();
            }
        }

        return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("bagName", bagName)
                        .addParameter("table", identifier)
                        .addParameter("trail", trail).forward();
    }
}
