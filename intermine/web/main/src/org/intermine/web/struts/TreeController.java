package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.TreeNode;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Perform initialisation steps for displaying a tree
 * @author Mark Woodbridge
 * @author Kim Rutherford
 */
public class TreeController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        
        Set<String> openClasses = (Set<String>) session.getAttribute("openClasses");
        if (openClasses == null) {
            openClasses = new HashSet<String>();
            openClasses.add("org.intermine.model.InterMineObject");
            session.setAttribute("openClasses", openClasses);
        }

        ServletContext servletContext = session.getServletContext();
        Model model = im.getModel();

        String rootClass = (String) request.getAttribute("rootClass");
        List<ClassDescriptor> rootClasses = new ArrayList<ClassDescriptor>();
        if (rootClass != null) {
            rootClasses.add(model.getClassDescriptorByName(rootClass));
        } else {
            rootClass = "org.intermine.model.InterMineObject";
            rootClasses.add(model.getClassDescriptorByName(rootClass));
            for (ClassDescriptor cld : model.getClassDescriptors()) {
                if (cld.getSuperDescriptors().isEmpty() && (!"org.intermine.model.InterMineObject"
                            .equals(cld.getName()))) {
                    rootClasses.add(cld);
                }
            }
        }
        Map classCounts = (Map) servletContext.getAttribute("classCounts");

        List nodes = new ArrayList();
        for (ClassDescriptor cld : rootClasses) {
            nodes.addAll(makeNodes(cld, openClasses, 0, classCounts));
        }

        context.putAttribute("nodes", nodes);

        return null;
    }

    /**
     * Produce a list of tree nodes for a class and its children, marking as 'open' and recursing
     * on any that appear on the openClasses list.
     * @param parent the root class
     * @param openClasses the Set of open classes
     * @param depth the current depth from the root
     * @param classCounts the classCounts attribute from the ServletContext
     * @return a List of nodes
     */
    protected List<TreeNode> makeNodes(ClassDescriptor parent, Set openClasses, int depth,
                             Map classCounts) {
        List<TreeNode> nodes = new ArrayList<TreeNode>();
        nodes.add(new TreeNode(parent, classCounts.get(parent.getName()).toString(),
                               depth, false,
                               parent.getSubDescriptors().size() == 0,
                               openClasses.contains(parent.getName())));
        if (openClasses.contains(parent.getName())) {
            Set<ClassDescriptor> sortedClds = new TreeSet<ClassDescriptor>(
                    new Comparator<ClassDescriptor>() {
                        public int compare(ClassDescriptor c1, ClassDescriptor c2) {
                            return c1.getName().compareTo(c2.getName());
                        }
                    });
            sortedClds.addAll(parent.getSubDescriptors());
            for (ClassDescriptor cld : sortedClds) {
                nodes.addAll(makeNodes(cld, openClasses, depth + 1, classCounts));
            }
        }
        return nodes;
    }
}

