package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;


public class ModMineSearchResultsController extends TilesAction 
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());

        ModMineSearch.initModMineSearch(im);
        String searchTerm = (String) request.getParameter("searchTerm");
        request.setAttribute("searchTerm", "THE SEARCH TERM");
        if (searchTerm != null) {
            String identifier = ModMineSearch.SEARCH_KEY + searchTerm;
            PagedTable pagedResults = SessionMethods.getResultsTable(request.getSession(), identifier);
            request.setAttribute("searchTerm", searchTerm);
            request.setAttribute("pagedResults", pagedResults);
            request.setAttribute("scores", request.getAttribute("scores"));
        }
        return null;
    }
}