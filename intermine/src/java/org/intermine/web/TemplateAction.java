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
 * Action to handle submit from the template page
 * @author Mark Woodbridge
 */
public class TemplateAction extends Action
{
    /** 
     * @see Action#execute
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
        
        LoadQueryAction.loadQuery(template.getQuery(), request.getSession());

        if (request.getParameter("skipBuilder") != null) {
            // If the form wants to skip the query builder we need to execute the query
            if (!SessionMethods.runQuery (session, request)) {
                return mapping.findForward("failure");
            }
            return mapping.findForward("results");
        }
        
        return mapping.findForward("query");
    }
}
