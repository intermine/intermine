package org.flymine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;

import org.intermine.web.TemplateAction;
import org.intermine.web.PathQuery;
import org.intermine.web.PathNode;
import org.intermine.web.Constraint;
import org.intermine.web.Constants;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.SessionMethods;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.TypeUtil;

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
        String className = request.getParameter("class");
        
        if (extId == null || className == null) {
            ActionErrors errors = new ActionErrors();
            errors.add(ActionErrors.GLOBAL_MESSAGE, new ActionError("errors.badportalquery"));
            saveErrors(request, errors);
            return mapping.findForward("failure");
        }
        
        PathQuery query = new PathQuery(os.getModel());
        PathNode node = query.addNode("Synonym");
        node = query.addNode("Synonym.value");
        node.setConstraints(Collections.singletonList(new Constraint(ConstraintOp.EQUALS, extId)));
        query.setView(Collections.singletonList("Synonym.subject"));
        
        // Stop handleTemplateQuery trying to forward to query builder
        SessionMethods.loadQuery(query, request.getSession());
        return handleTemplateQuery(mapping, request, response, true);
    }
}

