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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;


/**
 * Controller for templates.page
 *
 * @author Thomas Riley
 */
public class TemplatesController extends Action
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map templateQueries = (Map) servletContext.getAttribute(Constants.GLOBAL_TEMPLATE_QUERIES);
        String category = request.getParameter("category");
        
        List templates = new ArrayList();
        Iterator iter = templateQueries.values().iterator();
        
        while (iter.hasNext()) {
            TemplateQuery template = (TemplateQuery) iter.next();
            if (category == null || category.equals(template.getCategory ())) {
                templates.add(template);
            }
        }
        
        request.setAttribute("templates", templates);

        return null;
    }
}
