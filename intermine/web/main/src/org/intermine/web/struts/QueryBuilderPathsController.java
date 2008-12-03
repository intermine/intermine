package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.query.MainHelper;

/**
 * Controller for the main paths tile.
 * @author Thomas Riley
 */
public class QueryBuilderPathsController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping,
            @SuppressWarnings("unused") ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        populateRequest(request, response);
        return null;
    }

    private static void populateRequest(HttpServletRequest request,
            @SuppressWarnings("unused") HttpServletResponse response) {
        HttpSession session = request.getSession();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        // First merge the query and the view
        PathQuery q = query.clone();
        for (Path p : q.getView()) {
            String path = p.toStringNoConstraints();
            if (!q.getNodes().containsKey(path)) {    
                q.addNode(path);
            }
        }

        Set<String> constrainedPaths = new HashSet<String>();
        for (Map.Entry<String, PathNode> entry : q.getNodes().entrySet()) {
            if (entry.getValue().isAttribute()) {
                PathNode node = entry.getValue();
                if (!node.getConstraints().isEmpty()) {
                    constrainedPaths.add(node.getPrefix());
                }
            }
        }

        Set<String> clickableNodes = new HashSet<String>();
        for (Map.Entry<String, PathNode> entry : q.getNodes().entrySet()) {
            PathNode node = entry.getValue();
            if (!entry.getValue().isAttribute()) {
                if (entry.getKey().indexOf('.') == -1
                        && entry.getKey().indexOf(':') == -1) {
                } else if ((!node.isOuterJoin())
                        && node.isReference()
                        && ((!node.getConstraints().isEmpty()) || constrainedPaths
                                .contains(entry.getKey()))) {
                } else {
                    clickableNodes.add(entry.getKey());
                }
            }
        }
        Map<String, PathNode> qNodes = new TreeMap<String, PathNode>(q.getNodes());

        request.setAttribute("clickableNodes", clickableNodes);
        request.setAttribute("qNodes", qNodes);
        request.setAttribute("constraintDisplayValues", MainHelper.makeConstraintDisplayMap(query));
    }
}
