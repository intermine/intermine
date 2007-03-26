package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * This action collapses/expands sections of the user interface.
 *
 * @author Thomas Riley
 */
public class CollapseAction extends InterMineAction
{
    /**
     * Looks for three request parameters "forward", "id" and "state". Updates
     * COLLAPSED session map with new collapsed state for element "id" and
     * redirects back to page specified by "forward" attribute.
     * 
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        String forward = request.getParameter("forward");        
        String id = request.getParameter("id");
        Map collapsed = SessionMethods.getCollapsedMap(session);
        Boolean b = (Boolean.TRUE == collapsed.get(id) ? Boolean.FALSE : Boolean.TRUE);
        collapsed.put(id, b);
        if (forward.endsWith("?")) {
            forward = forward.substring(0, forward.length() - 1);
        }
        return new ActionForward(forward, true);
    }
}