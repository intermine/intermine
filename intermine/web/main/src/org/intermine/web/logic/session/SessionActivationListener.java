package org.intermine.web.logic.session;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionEvent;

import org.intermine.api.profile.Profile;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.web.logic.Constants;

/**
 * Added to each session to monitor activation events.
 * @author Thomas Riley
 * @see SessionListener
 */
public class SessionActivationListener implements HttpSessionActivationListener, Serializable
{
    /**
     * Called just before serialising to disk. Remove things that will fail to
     * serialise and replace some things with a serialisable form.
     * @param event the session event
     */
    public void sessionWillPassivate(HttpSessionEvent event) {
        //System. out.println("sessionWillPassivate");
        HttpSession session = event.getSession();
        // Get rid of the tables
        session.removeAttribute(Constants.TABLE_MAP);
        // Save the username if one exists
        Profile profile = SessionMethods.getProfile(session);
        if (profile != null && profile.getUsername() != null) {
            session.setAttribute("ser-username", profile.getUsername());
        }
        // Save current query as string
        PathQuery query = SessionMethods.getQuery(session);
        if (query != null) {
            String queryXml = PathQueryBinding.marshal(query, "", query.getModel().getName(),
                    PathQuery.USERPROFILE_VERSION);
            session.setAttribute("ser-query", queryXml);
        }
        // Rehydrate serialised stuff on first request
        session.setAttribute("ser", Boolean.TRUE);
    }

    /**
     * Called after a session is unserialised.
     * @param event session event
     */
    public void sessionDidActivate(HttpSessionEvent event) {
        //System. out.println("sessionDidActivate");
    }
}
