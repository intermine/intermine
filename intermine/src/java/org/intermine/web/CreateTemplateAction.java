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

import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Action to create a new TemplateQuery from current query.
 *
 * @author Thomas Riley
 */
public class CreateTemplateAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(CreateTemplateAction.class);

    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
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
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);

        boolean seenProblem = false;

        // Make sure this attribute is cleared
        session.setAttribute(Constants.EDITING_TEMPLATE, null);
        
        // Check whether query has at least one constraint and at least one output
        if (query.getView().size() == 0) {
            recordError(new ActionMessage("errors.createtemplate.nooutputs"), request);
            seenProblem = true;
        }
        Iterator iter = query.getNodes().values().iterator();
        boolean foundConstraint = false;
        while (iter.hasNext()) {
            PathNode node = (PathNode) iter.next();
            if (node.isAttribute()) {
                if (node.getConstraints().size() > 0) {
                    foundConstraint = true;
                    break;
                }
            }
        }
        if (!foundConstraint) {
            recordError(new ActionMessage("errors.createtemplate.noconstraints"), request);
            seenProblem = true;
        }

        // Ensure that we can actually execute the query
        try {
            if (query.getInfo() == null) {
                query.setInfo(os.estimate(MainHelper.makeQuery(query, profile.getSavedBags())));
            }
        } catch (ObjectStoreException e) {
            recordError(new ActionMessage("errors.query.objectstoreerror"), request, e, LOG);
            seenProblem = true;
        }

        if (seenProblem) {
            return mapping.findForward("query");
        }

        PathQuery queryClone = (PathQuery) query.clone();
        session.setAttribute(Constants.TEMPLATE_PATHQUERY, queryClone);
        return mapping.findForward("templateBuilder");
    }
}
