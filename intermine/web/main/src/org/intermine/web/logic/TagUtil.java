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

import java.util.Properties;

import org.intermine.util.StringUtil;
import org.intermine.web.logic.profile.Profile;

import java.lang.String;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Utility methods for tags.
 * @author Kim Rutherford
 */
public abstract class TagUtil
{
    /**
     * Look at the current webapp page and subtab and return the help page and tab.
     * @param request the request object
     * @return 
     */
    public static String[] getHelpPage(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        Properties webProps = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String pageName = (String) request.getAttribute("pageName");
        String subTab = profile.getUserOption("subtab" + pageName);

        String prop;
        if (subTab == null) {
            prop = webProps.getProperty("help.page." + pageName);
        } else {
            prop = webProps.getProperty("help.page." + pageName + "." + subTab);
        }
        
        if (prop == null) {
            return new String[0];
        } else {
            return StringUtil.split(prop, ":");
        }
    }
}
