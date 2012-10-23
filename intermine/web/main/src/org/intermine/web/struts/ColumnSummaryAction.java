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
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.results.WebTable;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Handles links from results pages when user clicks on "view all" in summary table.
 *
 * @author Julie Sullivan
 */

public class ColumnSummaryAction extends InterMineAction
{

    /**
     * Link-ins from other sites end up here (after some redirection).
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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        String tableName = request.getParameter("tableName");
        String summaryPath = request.getParameter("summaryPath");
        WebTable webTable = (SessionMethods.getResultsTable(session, tableName)).getWebTable();
        PathQuery q = webTable.getPathQuery().clone();
        q.clearOrderBy();
        q.clearView();
        q.clearDescriptions();
        q.clearOuterJoinStatus();
        q.addView(summaryPath);

        SessionMethods.loadQuery(q, session, response);
        String qid = SessionMethods.startQueryWithTimeout(request, false, q);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return new ForwardParameters(mapping.findForward("waiting"))
            .addParameter("qid", qid).forward();
    }
}
