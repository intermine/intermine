package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.query.QueryMonitorTimeout;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * The portal query action handles links into flymine from external sites.
 * At the moment the action expects 'class' and 'externalid' parameters
 * the it performs some sensible query and redirects the user to the
 * results page or a tailored 'portal' page (at the moment it just goes
 * to the object details page).
 *
 * @author Thomas Riley
 */

public class PortalQueryAction extends InterMineAction
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
        String extId = request.getParameter("externalid");
        String origin = request.getParameter("origin");

        if (origin == null) {
            origin = "";
        } else if (origin.length() > 0) {
            origin = "." + origin;
        }

        if (extId == null) {
            recordError(new ActionMessage("errors.badportalquery"), request);
            return mapping.findForward("failure");
        }

        Properties properties = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String templateName = properties.getProperty("begin.browse.template");
        Integer op = ConstraintOp.EQUALS.getIndex();
        String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
        TemplateQuery template = TemplateHelper.findTemplate(servletContext, session, userName,
                                                             templateName, "global");

        if (template == null) {
            throw new IllegalStateException("Could not find template \"" + templateName + "\"");
        }

        // Populate template form bean
        TemplateForm tf = new TemplateForm();
        tf.setAttributeOps("1", op.toString());
        tf.setAttributeValues("1", extId);
        tf.parseAttributeValues(template, session, new ActionErrors(), false);

        // Convert form to path query
        PathQuery queryCopy = TemplateHelper.templateFormToTemplateQuery(tf, template, 
                                                                         new HashMap());
        // Convert path query to intermine query
        SessionMethods.loadQuery(queryCopy, request.getSession(), response);
        // Add a message to welcome the user
        SessionMethods.recordMessage(properties.getProperty("portal.welcome" + origin), session);

        // Set collapsed/uncollapsed state of object details UI
        Map collapsed = SessionMethods.getCollapsedMap(session);
        collapsed.put("fields", Boolean.TRUE);
        collapsed.put("further", Boolean.FALSE);
        collapsed.put("summary", Boolean.FALSE);

        session.setAttribute(Constants.PORTAL_QUERY_FLAG, Boolean.TRUE);

        QueryMonitorTimeout clientState
                = new QueryMonitorTimeout(Constants.QUERY_TIMEOUT_SECONDS * 1000);
        MessageResources messages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        String qid = SessionMethods.startQuery(clientState, session, messages, false, queryCopy);
        Thread.sleep(200); // slight pause in the hope of avoiding holding page
        return new ForwardParameters(mapping.findForward("waiting"))
                            .addParameter("qid", qid).forward();
    }
}

