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

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.session.SessionMethods;

/**
 * A RequestProcessor that sends you back to the start if your session isn't valid.
 *
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
    // TODO note that 'experiment' and 'features' are modMine specific.  We should make this
    // configurable by properties
    public static final List<String> START_PATHS =
        Arrays.asList(LOGON_PATH, LOGON_INIT_PATH, "/classChooser", "/bagBuild", "/report",
                "/examples", "/browseAction", "/collectionDetails", "/iqlQuery", "/login",
                "/contact", "/portal", "/templates", "/templateSearch", "/template", "/aspect",
                "/ping", "/standalone", "/quickStart", "/importQuery", "/tree", "/headMenu",
                "/htmlHead", "/dataCategories", "/bagDetails", "/results", "/passwordReset",
                "/experiment", "/features", "/loadQuery", "/loadTemplate", "/customQuery",
                "/importQueries", "/bag", "/keywordSearchResults");

    /**
     * This is called during the processing of every controller.
     *
     * {@inheritDoc}
     */
    protected boolean processPreprocess(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        if (SessionMethods.isErrorOnInitialiser(request.getSession().getServletContext())) {
            return true;
        }
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        try {
            String processPath = processPath(request, response);
            // Avoid creating a session for each page accessed via SSI
            if (processPath.startsWith("/standalone")) {
                return true;
            }

            String userAgent = null;
            if (request.getHeader("user-agent") != null) {
                userAgent = request.getHeader("user-agent").toLowerCase();
            }

            Profile profile = SessionMethods.getProfile(session);
            if (profile == null) {
                session.invalidate();
                profile = SessionMethods.getProfile(session);
            }

            if (userAgent != null && !profile.isLoggedIn()) {
                for (String bot : bots) {
                    if (userAgent.contains(bot)) {
                        session.setMaxInactiveInterval(60);
                        break;
                    }
                }
            }

            ProfileManager pm = im.getProfileManager();

            if (session.getAttribute("ser") != null) {
                session.removeAttribute("ser");
                SessionMethods.initSession(session);

                String user = (String) session.getAttribute("ser-username");
                if (user != null) {
                    // Replace default anon UserProfile
                    Profile p = pm.getProfile(user);
                    if (p != null) {
                        LOG.warn("Could not find profile for user " + user);
                        SessionMethods.setProfile(session, profile);
                    }
                    session.removeAttribute("ser-username");
                }

                String queryXml = (String) session.getAttribute("ser-query");
                if (queryXml != null) {
                    BagManager bagManager = im.getBagManager();
                    Map<String, InterMineBag> allBags = bagManager.getBags(profile);
                    PathQuery pq =
                        PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml),
                                PathQuery.USERPROFILE_VERSION);

                    // check bags used by this query exist
                    Set<String> missingBags = new HashSet<String>();
                    for (String bagName : pq.getBagNames()) {
                        if (!allBags.containsKey(bagName)) {
                            missingBags.add(bagName);
                        }
                    }

                    if (!pq.isValid()) {
                        LOG.warn("PathQuery XML in saved session invalid! " + queryXml);
                    } else if (!missingBags.isEmpty()) {
                        LOG.warn("PathQuery XML in saved session references bags that don't exist: "
                                + missingBags + " query: " + queryXml);
                    } else {
                        SessionMethods.setQuery(session, pq);
                    }

                    session.removeAttribute("ser-query");
                }
            }


            if (!request.isRequestedSessionIdValid()
                && request.getAttribute(Globals.MESSAGE_KEY) == null
                && !START_PATHS.contains(processPath)
                && !processPath.startsWith("/init")) {
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
     * Overridden to copy errors and messages to session before performing a redirecting forward.
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    protected void processForwardConfig(HttpServletRequest request, HttpServletResponse response,
            ForwardConfig forward) throws java.io.IOException, javax.servlet.ServletException {
        ForwardConfig forwardConfig = forward;
        ActionMessages messages = (ActionMessages) request.getAttribute(Globals.MESSAGE_KEY);
        ActionMessages errors = (ActionMessages) request.getAttribute(Globals.ERROR_KEY);

        if (forwardConfig != null && forwardConfig.getRedirect()) {
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
                String path = forwardConfig.getPath();
                if (path.indexOf('?') != -1) {
                    path += "&" + params;
                } else {
                    path += "?" + params;
                }
                forwardConfig = new ForwardConfig("dummy", path, true);
            }
        }

        super.processForwardConfig(request, response, forwardConfig);
    }

    private final Set<String> bots = Collections.unmodifiableSet(new HashSet<String>(
                Arrays.asList("slurp", "bot", "spider", "crawl",
                    "scooter", "ezooms", "archiver", "eventbox", "docomo", "nutch", "grabber")));
}
