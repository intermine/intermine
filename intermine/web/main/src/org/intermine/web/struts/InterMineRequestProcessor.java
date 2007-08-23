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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.tiles.TilesRequestProcessor;
import org.apache.struts.util.MessageResources;

/**
 * A RequestProcessor that sends you back to the start if your session isn't valid
 * @author Mark Woodbridge
 */
public class InterMineRequestProcessor extends TilesRequestProcessor
{
    private static final Logger LOG = Logger.getLogger(InterMineRequestProcessor.class);
    
    private static final String LOGON_PATH = "/begin";
    private static final String LOGON_INIT_PATH = "/initBegin";

    /**
     * Paths that can be used as initial pages ie. when there is no session.
     */
    public static final List START_PATHS =
        Arrays.asList(new String[] {
                          LOGON_PATH, LOGON_INIT_PATH, "/classChooser", "/bagBuild",
                          "/objectDetails", "/examples", "/browseAction",
                          "/collectionDetails", "/iqlQuery", "/login", "/feedback", "/portal",
                          "/templates", "/templateSearch", "/template", "/aspect",
                          "/ping", "/standalone", "/quickStart", "/importQuery", "/tree",
                          "/headMenu", "/htmlHead"
                      });
    
    /**
     * {@inheritDoc}
     */
    protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
        
        try {
            // Avoid creating a session for each page accessed via SSI
            if (processPath(request, response).startsWith("/standalone")) {
                return true;
            }
            
            HttpSession session = request.getSession();
            ServletContext sc = session.getServletContext();
            ProfileManager pm = (ProfileManager) sc.getAttribute(Constants.PROFILE_MANAGER);

            if (session.getAttribute("ser") != null) {
                session.removeAttribute("ser");
                SessionMethods.initSession(session);
                
                String user = (String) session.getAttribute("ser-username");
                if (user != null) {
                    // Replace default anon UserProfile
                    Profile p = pm.getProfile(user);
                    if (p != null) {
                        LOG.warn("Could not find profile for user " + user);
                        session.setAttribute(Constants.PROFILE, pm.getProfile(user));
                    }
                    session.removeAttribute("ser-username");
                }
                Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
                String queryXml = (String) session.getAttribute("ser-query");
                if (queryXml != null) {
                    Map<String, InterMineBag> allBags =
                        WebUtil.getAllBags(profile.getSavedBags(), sc);
                    PathQuery pq = PathQuery.fromXml(queryXml, allBags, sc);
                    if (pq.isValid()) {
                        session.setAttribute(Constants.QUERY, 
                                             PathQuery.fromXml(queryXml, profile.getSavedBags(),
                                                               sc));
                    } else {
                        LOG.warn("PathQuery XML in saved session invalid! " + queryXml);
                    }
                    session.removeAttribute("ser-query");
                }
            }
            
            if (request.getSession().getAttribute(Constants.PROFILE) == null) {
                request.getSession().invalidate();
            }
            
            if (!request.isRequestedSessionIdValid()
                && request.getAttribute(Globals.MESSAGE_KEY) == null
                && !START_PATHS.contains(processPath(request, response))
                && !processPath(request, response).startsWith("/init")) {
                ActionMessages messages = new ActionMessages();
                messages.add(ActionMessages.GLOBAL_MESSAGE,
                             new ActionMessage("errors.session.nosession"));
                request.setAttribute(Globals.ERROR_KEY, messages);
                processForwardConfig(request, response,
                                     new ActionForward(LOGON_PATH + ".do", true));
            }
        } catch (Exception e) {
            request.getSession().invalidate(); // safer?
            throw new RuntimeException(e);
        }
        
        return true;
    }

    /**
     * Overriden to copy errors and messages to session before performing
     * a redirecting forward.
     *
     * {@inheritDoc}
     */
    protected void processForwardConfig(HttpServletRequest request,
                                        HttpServletResponse response,
                                        ForwardConfig forward)
                                        throws java.io.IOException, javax.servlet.ServletException {
        ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
        ActionMessages errors = (ActionMessages) request.getAttribute(Globals.ERROR_KEY);
        
        if (forward != null && forward.getRedirect()) {
            MessageResources resources
                = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
            if (errors != null && !errors.isEmpty()) {
                Iterator iter = errors.get();
                while (iter.hasNext()) {
                    ActionMessage message = (ActionMessage) iter.next();
                    String msg = resources.getMessage(message.getKey(), message.getValues());
                    SessionMethods.recordError(msg, request.getSession());
                }
            }
            if (messages != null && !messages.isEmpty()) {
                Iterator iter = messages.get();
                while (iter.hasNext()) {
                    ActionMessage message = (ActionMessage) iter.next();
                    String msg = resources.getMessage(message.getKey(), message.getValues());
                    SessionMethods.recordMessage(msg, request.getSession());
                }
            }
            String params = request.getParameter("__intermine_forward_params__");
            if (params != null) {
                String path = forward.getPath();
                if (path.indexOf('?') != -1) {
                    path += "&" + params;
                } else {
                    path += "?" + params;
                }
                forward = new ForwardConfig("dummy", path, true);
            }
        }
        
        super.processForwardConfig(request, response, forward);
    }
}
