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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.util.TypeUtil;

/**
 * Controller for the main tile
 * @author Mark Woodbridge
 */
public class MainController extends TilesAction
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
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);

        //set up the path-based query
        Map qNodes = (Map) session.getAttribute(Constants.QUERY);
        if (qNodes == null) {
            String className = (String) request.getAttribute("class");
            if (className == null) {
                //return mapping.findForward("begin");
                className = "Company";
            }
            className = TypeUtil.unqualifiedName(className);
            RightNode node = new RightNode(className);
            qNodes = new TreeMap();
            qNodes.put(className, node);
            session.setAttribute(Constants.QUERY, qNodes);
        }
        List view = (List) session.getAttribute(Constants.VIEW);
        if (view == null) {
            session.setAttribute(Constants.VIEW, new ArrayList());
        }

        //set up the metadata
        String path = (String) session.getAttribute("path");
        if (path == null) {
            path = (String) qNodes.keySet().iterator().next();
            session.setAttribute("path", path);
        }
        context.putAttribute("nodes", MainHelper.makeNodes(path, model));

        //set up the node on which we are editing constraints
        if (request.getAttribute("editingNode") != null) {
            RightNode node = (RightNode) request.getAttribute("editingNode");
            if (node.getPath().indexOf(".") != -1
                && MainHelper.getFieldDescriptor(node.getPath(), model).isAttribute()) {
                AttributeDescriptor attr = (AttributeDescriptor) MainHelper
                    .getFieldDescriptor(node.getPath(), model);
                Map validOps = MainHelper.
                    mapOps(SimpleConstraint.validOps(TypeUtil.instantiate(attr.getType())));
                request.setAttribute("validOps", validOps);
            } else {
                ClassDescriptor cld = MainHelper
                    .getClassDescriptor(MainHelper.getType(node.getPath(), model), model);
                request.setAttribute("subclasses", getChildren(cld));
                if (session.getAttribute(Constants.SAVED_BAGS) != null) {
                    ((Map) session.getAttribute(Constants.SAVED_BAGS)).put("", null);
                    request.setAttribute("validOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
                }
            }
        }

        return null;
    }

    /**
     * Get the names of the type of this ClassDescriptor and all its descendents
     * @param cld the ClassDescriptor
     * @return a Set of class names
     */
    protected static Set getChildren(ClassDescriptor cld) {
        Set children = new HashSet();
        children.add(TypeUtil.unqualifiedName(cld.getName()));
        for (Iterator i = cld.getSubDescriptors().iterator(); i.hasNext();) {
            ClassDescriptor child = (ClassDescriptor) i.next();
            children.addAll(getChildren(child));
        }
        return children;
    }
}