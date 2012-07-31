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

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.util.MessageResources;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.PageTableQueryMonitor;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Start a count in a separate thread of the number of rows in the PagedTable given by the
 * "resultsTable" request attribute.
 * @author Kim Rutherford
 */
public class CountTableController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form, HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        PagedTable pt = (PagedTable) request.getAttribute("resultsTable");
        PageTableQueryMonitor clientState
            = new PageTableQueryMonitor(Constants.QUERY_TIMEOUT_SECONDS * 1000, pt);
        String qid = SessionMethods.startPagedTableCount(clientState, session, messages);
        request.setAttribute("qid", qid);
        request.setAttribute("POLL_REFRESH_SECONDS", new Integer(Constants.POLL_REFRESH_SECONDS));
        return null;
    }
}
