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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import org.intermine.objectstore.ObjectStore;

/**
 * Action to create a new TemplateQuery from current query.
 *
 * @author Thomas Riley
 */
public class BuildTemplateAction extends InterMineAction
{
    /**
     * Handle submission of the build template form. Build a UserTemplateQuery.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        PathQuery query = (PathQuery) session.getAttribute(Constants.TEMPLATE_PATHQUERY);
        BuildTemplateForm tf = (BuildTemplateForm) form;
        TemplateQuery editing = (TemplateQuery) session.getAttribute(Constants.EDITING_TEMPLATE);
        
        TemplateQuery template = TemplateHelper.buildTemplateQuery(tf, query);
        
        if (request.getParameter("preview") != null) {
            request.setAttribute("previewTemplate", template);
            request.setAttribute("showPreview", Boolean.TRUE);
            return mapping.findForward("templateBuilder");
        } else {
            // Create new user template
            session.removeAttribute(Constants.TEMPLATE_PATHQUERY);
            session.removeAttribute(Constants.EDITING_TEMPLATE);
            String key = (editing == null) ? "templateBuilder.templateCreated"
                                           : "templateBuilder.templateUpdated";
            recordMessage(new ActionMessage(key, template.getName()), request);
            tf.reset();
            // Replace template if needed
            if (editing != null) {
                profile.deleteTemplate(editing.getName());
            }
            profile.saveTemplate(template.getName(), template);
            // If superuser then rebuild shared templates
            if (profile.getUsername() != null
                && profile.getUsername().equals
                    (servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT))) {
                TemplateRepository tr = TemplateRepository.getTemplateRepository(servletContext);
                if (editing != null) {
                    tr.globalTemplateUpdated(template);
                } else {
                    tr.globalTemplateAdded(template);
                }
                //InitialiserPlugin.loadGlobalTemplateQueries(servletContext);
            }
            return mapping.findForward("finish");
        }
    }
}
