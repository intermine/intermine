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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;

/**
 * Perform initialisation steps for displaying a tree
 * @author Mark Woodbridge
 * @author Kim Rutherford
 */
public class TreeController extends TilesAction
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

        Set openClasses = (Set) session.getAttribute("openClasses");
        if (openClasses == null) {
            openClasses = new HashSet();
            openClasses.add("org.intermine.model.InterMineObject");
            session.setAttribute("openClasses", openClasses);
        }

        String rootClass = (String) request.getAttribute("rootClass");
        if (rootClass == null) {
            rootClass = "org.intermine.model.InterMineObject";
        }
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);
        ClassDescriptor root =
            model.getClassDescriptorByName(rootClass);
        context.putAttribute("nodes", makeNodes(root, openClasses, 0));

        return null;
    }

    /**
     * Produce a list of tree nodes for a class and its children, marking as 'open' and recursing
     * on any that appear on the openClasses list.
     * @param parent the root class
     * @param openClasses the Set of open classes
     * @param depth the current depth from the root
     * @return a List of nodes
     */
    protected List makeNodes(ClassDescriptor parent, Set openClasses, int depth) {
        List nodes = new ArrayList();
        nodes.add(new TreeNode(parent.getName(), depth, false,
                               parent.getSubDescriptors().size() == 0,
                               openClasses.contains(parent.getName())));
        if (openClasses.contains(parent.getName())) {
            for (Iterator i = parent.getSubDescriptors().iterator(); i.hasNext();) {
                nodes.addAll(makeNodes((ClassDescriptor) i.next(), openClasses, depth + 1));
            }
        }
        return nodes;
    }
}

