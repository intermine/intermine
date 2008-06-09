package org.intermine.webservice.core;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;


/**
 * Public templates manager used by web service.
 * @author Jakub Kulaviak
 **/
public class TemplateManager
{
    
    private HttpServletRequest request;
    
    /**
     * TemplateManager constructor.
     * @param request request 
     */
    public TemplateManager(HttpServletRequest request) {
        this.request = request;
    }
    
    /**
     * Returns public template of specified name.  
     * @param name template name
     * @return TemplateQuery or null if template wasn't found 
     */
    public TemplateQuery getGlobalTemplate(String name) {
        String userName = ((Profile) request.getSession().
                getAttribute(Constants.PROFILE)).getUsername();
        return TemplateHelper.findTemplate(request.getSession().getServletContext(),
                request.getSession(), userName, name, TemplateHelper.GLOBAL_TEMPLATE);
    }
}
