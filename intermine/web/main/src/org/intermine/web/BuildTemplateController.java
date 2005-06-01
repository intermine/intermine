package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the template building page.
 *
 * @author Thomas Riley
 */

public class BuildTemplateController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(BeginController.class);
    
    /**
     * Adds the required <code>constraintDisplayValues</code> request attribute because the
     * form displays constraint values. Locates the query via TEMPLATE_PATHQUERY session
     * attribute.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        BuildTemplateForm tf = (BuildTemplateForm) form;
        PathQuery query = (PathQuery) session.getAttribute(Constants.TEMPLATE_PATHQUERY);
        TemplateQuery template = (TemplateQuery) session.getAttribute(Constants.EDITING_TEMPLATE);
        
        // avoid initialising the form bean when the preview button is pressed
        if (template != null && request.getParameter("preview") == null) {
            tf.initFromTemplate(template);
        }
        
        request.setAttribute("constraintDisplayValues", MainHelper.makeConstraintDisplayMap(query));
        return null;
    }
}
