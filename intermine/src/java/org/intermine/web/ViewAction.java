package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.web.results.ChangeResultsSizeForm;

/**
 * Action to handle buttons on view tile.
 * 
 * @author Mark Woodbridge
 * @author Tom Riley
 */
public class ViewAction extends InterMineAction
{
    /**
     * Run the query and forward to the results page.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();

        ChangeResultsSizeForm resultsForm =
            (ChangeResultsSizeForm) session.getAttribute("changeResultsForm");
        if (resultsForm != null) {
            resultsForm.reset(mapping, request);
        }

        final OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
        String msg = getResources(request).getMessage("query.runningquery");
        writer.write(msg);
        writer.flush();

        RunQueryMonitorDots monitor = new RunQueryMonitorDots(writer);
        
        String destUrl = null;
        
        if (SessionMethods.runQuery(this, session, request, true, monitor)) {
            destUrl = mapping.findForward("results").getPath();
        } else {
            destUrl = mapping.findForward("query").getPath();
        }
        
        try {
            monitor.forwardClient(request.getContextPath() + destUrl);
        } catch (IOException _) {
            // user cancelled - swallow
        }
        return null;
    }
}