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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import java.util.Map;
import java.util.Iterator;

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Action to handle submit from the template page.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class TemplateAction extends Action
{
    /**
     * Build a query based on the template and the input from the user.
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
        Map templateQueries = (Map) servletContext.getAttribute(Constants.TEMPLATE_QUERIES);
        String queryName = (String) session.getAttribute("queryName");

        TemplateQuery template = (TemplateQuery) templateQueries.get(queryName);
        
        for (Iterator i = template.getNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            int j = template.getNodes().indexOf(node);
                String op = (String) ((TemplateForm) form).getAttributeOps("" + (j + 1));
            ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf(op));
            
            Object constraintValue = ((TemplateForm) form).getParsedAttributeValues("" + (j + 1));
            
            node.getConstraints().set(0, new Constraint(constraintOp, constraintValue));
        }
        
        SessionMethods.loadQuery(template.getQuery(), request.getSession());
        
        form.reset (mapping, request);
        
        return handleTemplateQuery(mapping, form, request, response);
    }
    
    /**
     * Called after the form has been read and the query has been loaded into the session. By
     * default, this method returns forward "query". Override to forward to some other
     * destination or do more processing on the template query.
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
    protected ActionForward handleTemplateQuery(ActionMapping mapping,
                                             ActionForm form,
                                             HttpServletRequest request,
                                             HttpServletResponse response)
        throws Exception {
        return mapping.findForward("query");
    }
}
