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

import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionErrors;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.ConstraintOp;

/**
 * The portal query action handles links into flymine from external sites.
 * At the moment the action expects 'class' and 'externalid' parameters
 * the it performs some sensible query and redirects the user to the
 * results page or a tailored 'portal' page (at the moment it just goes
 * to the object details page).
 *
 * @author Thomas Riley
 */

public class PortalQuery extends TemplateAction
{
    /**
     * Link-ins from other sites end up here (after some redirection).
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
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        
        String extId = request.getParameter("externalid");
        String origin = request.getParameter("origin");
        
        if (origin == null) {
            origin = "";
        } else if (origin.length() > 0) {
            origin = "." + origin;
        }
        
        if (extId == null) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionMessage("errors.badportalquery"));
            saveErrors(request, errors);
            return mapping.findForward("failure");
        }
        
        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String templateName = properties.getProperty("begin.browse.template");
        Integer op = ConstraintOp.EQUALS.getIndex();
        TemplateQuery template = TemplateHelper.findTemplate(request, templateName, "global");
        
        // Populate template form bean
        TemplateForm tf = new TemplateForm();
        tf.setAttributeOps("1", op.toString());
        tf.setAttributeValues("1", extId);
        tf.parseAttributeValues(template, session, new ActionErrors());
        
        // Convert form to path query
        PathQuery queryCopy = TemplateHelper.templateFormToQuery(tf, template);
        // Convert path query to intermine query
        SessionMethods.loadQuery(queryCopy, request.getSession());
        // Add a message to welcome the user
        recordMessage(new ActionMessage("portal.welcome" + origin), request);
        
        // Set collapsed/uncollapsed state of object details UI
        Map collapsed = (Map) session.getAttribute("COLLAPSED");
        collapsed.put("fields", Boolean.TRUE);
        collapsed.put("further", Boolean.TRUE);
        collapsed.put("other", Boolean.TRUE);
        collapsed.put("summary", Boolean.FALSE);
        
        return handleTemplateQuery(mapping, request, response, true, false);
    }
}

