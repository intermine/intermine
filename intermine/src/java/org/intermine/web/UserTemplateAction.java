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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.actions.DispatchAction;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Make some change to a user template.
 *
 * @author Thomas Riley
 */
public class UserTemplateAction extends DispatchAction
{
    /**
     * Delete a template query.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String templateName = request.getParameter("name");
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        
        TemplateQuery template = (TemplateQuery) profile.getSavedTemplates().get(templateName);
        if (template != null) {
            String msg = getResources(request).getMessage("templateList.deleted", templateName);
            session.setAttribute(Constants.MESSAGE, msg);
            profile.deleteTemplate(templateName);
            // If superuser then rebuild shared templates
            if (profile.getUsername() != null
                && profile.getUsername().equals
                    (servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT))) {
                InitialiserPlugin.loadGlobalTemplateQueries(servletContext);
            }
        } else {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE,
                        new ActionError("errors.template.nosuchtemplate"));
            saveErrors(request, errors);
        }
        
        return mapping.findForward("begin");
    }
}
