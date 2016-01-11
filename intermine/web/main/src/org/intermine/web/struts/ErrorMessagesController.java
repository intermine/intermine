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
import org.apache.struts.action.ActionMessages;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.web.logic.Constants;


/**
 * Take the messages stored in the session which are not using ActionMessages or ActionErrors and
 * put them in the request
 *
 * @author "Xavier Watkins"
 */
public class ErrorMessagesController extends TilesAction
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
    public ActionForward execute(
            ComponentContext context,
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ActionMessages messages = (ActionMessages) session.getAttribute(Constants.LOOKUP_MSG);
        if (messages != null) {
            request.setAttribute(Constants.LOOKUP_MSG, messages);
            session.removeAttribute(Constants.LOOKUP_MSG);
        }
        ActionMessages messages2 = (ActionMessages) session.getAttribute(Constants.PORTAL_MSG);
        if (messages2 != null) {
            request.setAttribute(Constants.PORTAL_MSG, messages2);
            session.removeAttribute(Constants.PORTAL_MSG);
        }
        return null;
    }

}
