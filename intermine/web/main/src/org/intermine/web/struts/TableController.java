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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.SessionMethods;
import org.intermine.web.logic.TemplateQuery;
import org.intermine.web.logic.results.PageOutOfRangeException;
import org.intermine.web.logic.results.PagedTable;

/**
 * Implementation of <strong>TilesAction</strong>. Sets up PagedTable
 * for table tile.
 *
 * @author Thomas Riley
 */
public class TableController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(TableController.class);
    
    /**
     * Set up table tile.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String pageStr = request.getParameter("page");
        String sizeStr = request.getParameter("size");
        String trail = request.getParameter("trail");
        
//        if (trail != null) {
//            trail += request.getParameter("table");
//        } else {
//            trail = request.getParameter("table");
//        }
            
        
        request.setAttribute("trail", trail);
        
        SaveBagForm bagForm = (SaveBagForm) session.getAttribute("saveBagForm");
        if (bagForm != null) {
            bagForm.reset(mapping, request);
        }
        
        PagedTable pt = SessionMethods.getResultsTable(session, request.getParameter("table"));
        if (pt == null) {
            LOG.error("PagedTable for " + request.getParameter("table") + " is null");
            return null;
        }
        request.setAttribute("resultsTable", pt);

        if (session.getAttribute(Constants.QUERY) != null) {
            if (session.getAttribute(Constants.QUERY) instanceof TemplateQuery) {
                request.setAttribute("templateQuery", session.getAttribute(Constants.QUERY));
            }
        }
        
        int page = (pageStr == null ? 0 : Integer.parseInt(pageStr));
        int size = (sizeStr == null ? pt.getPageSize() : Integer.parseInt(sizeStr));
        
        try {
            pt.setPageAndPageSize(page, size);
        } catch (PageOutOfRangeException e) {
            ActionMessages actionMessages = getErrors(request);
            actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
                               new ActionMessage("results.maxoffsetreached"));
            saveErrors(request, actionMessages);
        }
        
        return null;
    }
}
