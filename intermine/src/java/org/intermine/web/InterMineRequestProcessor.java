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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionError;
import org.apache.struts.tiles.TilesRequestProcessor;
import org.apache.struts.Globals;

/**
 * A RequestProcessor that sends you back to start if your session isn't valid
 * @author Mark Woodbridge
 */
public class InterMineRequestProcessor extends TilesRequestProcessor
{
    private static final String LOGON_PATH = "/begin";

    /**
     * @see TilesRequestProcessor#processPreprocess
     */
    protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
        if (!request.isRequestedSessionIdValid()) {
            if (request.getAttribute(Globals.MESSAGE_KEY) == null) {
                ActionErrors messages = new ActionErrors();
                ActionError error = new ActionError("errors.session.nosession");
                messages.add(ActionErrors.GLOBAL_ERROR, error);
                request.setAttribute(Globals.ERROR_KEY, messages);
                try {
                    if (!processPath(request, response).equals(LOGON_PATH)) {
                        processActionForward(request, response, new ActionForward("/begin.do"));
                    } else {
                        // don't go into a loop if something very bad happens
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }
}
