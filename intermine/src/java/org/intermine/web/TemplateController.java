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
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.objectstore.query.SimpleConstraint;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Controller for the template page
 * @author Mark Woodbridge
 */
public class TemplateController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Map templateQueries = (Map) servletContext.getAttribute(Constants.TEMPLATE_QUERIES);

        String queryName = request.getParameter("name");
        TemplateQuery template = (TemplateQuery) templateQueries.get(queryName);

        List nodes = new ArrayList();
        Map ops = new HashMap();
        Map names = new HashMap();

        //build list of nodes with editable constraints, the valid ops for those constraints,
        //and the human-readable "name" for each node (Department.company.name -> "Company name")
        for (int i = 0; i < template.getPaths().size(); i++) {
            String path = (String) template.getPaths().get(i);
            PathNode node = (PathNode) template.getQuery().getNodes().get(path);
            nodes.add(node);
            ops.put(node, MainHelper.mapOps(SimpleConstraint.validOps(MainHelper.
                                                                      getClass(node.getType()))));
            Constraint c = (Constraint) node.getConstraints().get(0);
            ((TemplateForm) form).setAttributeValues("" + (i + 1), "" + c.getValue());
            ((TemplateForm) form).setAttributeOps("" + (i + 1), "" + c.getOp().getIndex());

            PathNode parent = (PathNode) template.getQuery().getNodes().
                get(path.substring(0, path.lastIndexOf(".")));
            names.put(node, parent.getType() + " " + path.substring(path.lastIndexOf(".") + 1));
        }

        request.setAttribute("queryName", queryName);
        request.setAttribute("templateQuery", template);
        request.setAttribute("nodes", nodes);
        request.setAttribute("ops", ops);
        request.setAttribute("names", names);

        return null;
    }
}
