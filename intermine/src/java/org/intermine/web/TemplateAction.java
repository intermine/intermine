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

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.util.TypeUtil;

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

        TemplateQuery template = (TemplateQuery) templateQueries.get(((TemplateForm)
                                                                      form).getQueryName());
        
        for (int i = 0; i < template.getPaths().size(); i++) {
            String path = (String) template.getPaths().get(i);
            String op = (String) ((TemplateForm) form).getAttributeOps("" + (i + 1));
            String value = (String) ((TemplateForm) form).getAttributeValues("" + (i + 1));

            PathNode node = (PathNode) template.getQuery().getNodes().get(path);
            ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf(op));
            Class fieldClass = MainHelper.getClass(node.getType());
            Object constraintValue = TypeUtil.stringToObject(fieldClass, value);

            node.getConstraints().set(0, new Constraint(constraintOp, constraintValue));
        }
        
        LoadQueryAction.loadQuery(template.getQuery(), request.getSession());

        return mapping.findForward("query");
    }
}
