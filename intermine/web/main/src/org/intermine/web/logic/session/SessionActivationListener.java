package org.intermine.web.logic.session;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;

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
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        if (profile != null && profile.getUsername() != null) {
            session.setAttribute("ser-username", profile.getUsername());
        }
        // Save current query as string
        if (session.getAttribute(Constants.QUERY) != null) {
            String queryXml = ((PathQuery) session.getAttribute(Constants.QUERY)).toXml();
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
