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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.querybuilder.QueryBuilderSummaryHelper;
import org.intermine.web.logic.querybuilder.SummaryPath;
import org.intermine.web.logic.session.SessionMethods;
import org.jfree.util.Log;

/**
 * Controller for the QueryBuilder summary tile.
 *
 * @author Matthew Wakeling
 * @author Thomas Riley
 */
public class QueryBuilderSummaryController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        populateRequest(request, response);
        return null;
    }

    private static void populateRequest(HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PathQuery query = SessionMethods.getQuery(session);
        try {
            List<SummaryPath> summaryPaths = QueryBuilderSummaryHelper.getDisplaySummary(query);
            request.setAttribute("summaryPaths", summaryPaths);
        } catch (PathException e) {
            Log.error("Query is invalid: " + query, e);
        }
    }
}
