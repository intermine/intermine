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

import java.util.List;
import java.util.Arrays;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.tiles.TilesRequestProcessor;
import org.apache.struts.Globals;

/**
 * A RequestProcessor that sends you back to the start if your session isn't valid
 * @author Mark Woodbridge
 */
public class InterMineRequestProcessor extends TilesRequestProcessor
{
    private static final String LOGON_PATH = "/begin";
    private static final String LOGON_INIT_PATH = "/initBegin";

    /**
     * Paths that can be used as initial pages ie. when there is no session.
     */
    public static final List START_PATHS =
        Arrays.asList(new String[] {
                          LOGON_PATH, LOGON_INIT_PATH, "/classChooser", "/bagBuild",
                          "/objectDetails", "/initObjectDetails", "/examples",
                          "/collectionDetails", "/iqlQuery", "/login", "/feedback"
                      });
    
    /**
     * @see TilesRequestProcessor#processPreprocess
     */
    protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!request.isRequestedSessionIdValid()
                && request.getAttribute(Globals.MESSAGE_KEY) == null
                && !START_PATHS.contains(processPath(request, response))) {
                ActionMessages messages = new ActionMessages();
                messages.add(ActionMessages.GLOBAL_MESSAGE,
                             new ActionMessage("errors.session.nosession"));
                request.setAttribute(Globals.ERROR_KEY, messages);
                processForwardConfig(request, response, new ActionForward(LOGON_PATH + ".do"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return true;
    }
}
