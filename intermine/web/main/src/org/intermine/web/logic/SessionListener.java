package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

/**
 * Perform InterMine-specific actions when a Session is constructed or destroyed
 * @author Mark Woodbridge
 */
public class SessionListener implements HttpSessionListener
{
    /**
     * @see HttpSessionListener#sessionCreated
     */
    public void sessionCreated(HttpSessionEvent se) {
        //System. out.println("sessionCreated");
        HttpSession session = se.getSession();
        SessionMethods.initSession(session);
        session.setAttribute("activation-listener", new SessionActivationListener());
    }

    /**
     * @see HttpSessionListener#sessionDestroyed
     */
    public void sessionDestroyed(HttpSessionEvent se) {
        //System. out.println("sessionDestroyed");
        se.getSession().removeAttribute(Constants.TABLE_MAP);
    }
}