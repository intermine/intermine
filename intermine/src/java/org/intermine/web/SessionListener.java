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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionEvent;

import java.util.HashMap;

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
        HttpSession session = se.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        session.setAttribute(Constants.PROFILE,
                             new Profile(pm, null, new HashMap(), new HashMap(), new HashMap()));
    }

    /**
     * @see HttpSessionListener#sessionDestroyed
     */
    public void sessionDestroyed(HttpSessionEvent se) {
    }
}