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

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.web.logic.help.HintManager;
import org.intermine.web.logic.results.WebState;
import org.intermine.web.logic.session.SessionMethods;


/**
 * Executed for the hints tile that displays text information at the top of webapp pages.  Looks for
 * an available hint and sets on the request.
 * @author Richard Smith
 */
public class HintsController extends TilesAction
{
    /**
     * @param context
     *            The Tiles ComponentContext
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if an error occurs
     */
    @Override
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Properties webProperties = SessionMethods.getWebProperties(request.getSession()
                .getServletContext());
        WebState webState = SessionMethods.getWebState(request.getSession());

        String pageName = (String) context.getAttribute("pageName");

        ActionMessages actionErrors = getErrors(request);
        ActionMessages actionMessages = getMessages(request);
        // Ticket #2449 - hide hints if messages are on a page
        if (actionErrors.isEmpty() && actionMessages.isEmpty()) {
	        HintManager hintManager = HintManager.getInstance(webProperties);
	        String hint = hintManager.getHintForPage(pageName, webState);
	        if (hint != null) {
	            request.setAttribute("hint", hint);
	        }
        }

        return null;
    }
}
