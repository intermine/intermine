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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.objectstore.query.SimpleConstraint;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Controller for the template page
 * @author Mark Woodbridge
 */
public class TemplateController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map templateQueries = (Map) servletContext.getAttribute(Constants.TEMPLATE_QUERIES);

        String queryName = request.getParameter("name");
        if (queryName == null) {
            //validation failed last time
            queryName = (String) session.getAttribute("queryName");
        }

        //this to work around struts crappiness - the controller should be configured to expect
        //a templateForm, but when it is the controller isn't called on page redisplay if validation
        //fails. so we retrieve it manually, creating it if necessary.
        boolean populate = false;
        TemplateForm templateForm = (TemplateForm) session.getAttribute("templateForm");
        if (templateForm == null) {
            templateForm = new TemplateForm();
            session.setAttribute("templateForm", templateForm);
            populate = true;
        }

        TemplateQuery template = (TemplateQuery) templateQueries.get(queryName);

        Map ops = new HashMap();
        Map names = new HashMap();

        //for each node with an editable constraint, store the valid ops for those constraints
        //and the human-readable "name" for each node (Department.company.name -> "Company namae")
        for (Iterator i = template.getNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();

            ops.put(node, MainHelper.mapOps(SimpleConstraint.validOps(MainHelper.
                                                                      getClass(node.getType()))));

            PathNode parent = (PathNode) template.getQuery().getNodes().
                get(node.getPath().substring(0, node.getPath().lastIndexOf(".")));
            names.put(node, parent.getType() + " "
                      + node.getPath().substring(node.getPath().lastIndexOf(".") + 1));
            
            if (populate) {
                Constraint c = (Constraint) node.getConstraints().get(0);
                int j = template.getNodes().indexOf(node);
                templateForm.setAttributeValues("" + (j + 1), "" + c.getValue());
                templateForm.setAttributeOps("" + (j + 1), "" + c.getOp().getIndex());
            }
        }

        session.setAttribute("queryName", queryName);
        request.setAttribute("templateQuery", template);
        request.setAttribute("ops", ops);
        request.setAttribute("names", names);

        return null;
    }
}
