package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.struts.tiles.actions.TilesAction;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * Implementation of <strong>TilesAction</strong> the sets up session
 * attributes for the saveQuery action.
 *
 * @author Kim Rutherford
 */

public class SaveQueryController extends TilesAction
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
     * @exception ServletException if a servlet error occurs
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws ServletException {
        HttpSession session = request.getSession();

        Map savedQueries = (Map) session.getAttribute(Constants.SAVED_QUERIES);
        if (savedQueries == null) {
            savedQueries = new HashMap();
            session.setAttribute(Constants.SAVED_QUERIES, savedQueries);
        }

        Map savedQueriesInverse = (Map) session.getAttribute(Constants.SAVED_QUERIES_INVERSE);
        if (savedQueriesInverse == null) {
            savedQueriesInverse = new IdentityHashMap();
            session.setAttribute(Constants.SAVED_QUERIES_INVERSE, savedQueriesInverse);
        }

        return null;
    }
}
