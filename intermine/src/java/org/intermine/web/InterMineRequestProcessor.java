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
import org.apache.struts.tiles.TilesRequestProcessor;

/**
 * A RequestProcessor that sends you back to start if your session isn't valid
 * @author Mark Woodbridge
 */
public class InterMineRequestProcessor extends TilesRequestProcessor
{
    /**
     * @see TilesRequestProcessor#processPreprocess
     */
    protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
        if (!request.isRequestedSessionIdValid()) {
            try {
                processActionForward(request, response, new ActionForward("/sessionTimeout.jsp"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }
}
