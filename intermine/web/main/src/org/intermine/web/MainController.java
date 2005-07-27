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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
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
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = (Model) os.getModel();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext.
                                               getAttribute(Constants.OBJECT_STORE_SUMMARY);

        // set up the metadata
        context.putAttribute("nodes",
                             MainHelper.makeNodes((String) session.getAttribute("path"), model));

        //set up the node on which we are editing constraints
        if (session.getAttribute("editingNode") != null) {
            PathNode node = (PathNode) session.getAttribute("editingNode");
            session.removeAttribute("editingNode");
            request.setAttribute("editingNode", node);
            if (node.getPath().indexOf(".") != -1 && node.isAttribute()) {
                request.setAttribute("displayConstraint", new DisplayConstraint(node, model, oss));
            } else {
                // loop query arguments
                ArrayList paths = new ArrayList();
                Map displayPaths = new HashMap();
                Iterator iter = query.getNodes ().values ().iterator ();
                while (iter.hasNext ()) {
                    PathNode anode = (PathNode) iter.next();
                    if (anode != node && anode.getType().equals (node.getType())) {
                        paths.add(anode.getPath());
                    }
                }

                Map attributeOps = MainHelper.mapOps(ClassConstraint.VALID_OPS);
                request.setAttribute ("loopQueryOps", attributeOps);
                request.setAttribute ("loopQueryPaths", paths);
            }
            if (profile.getSavedBags().size() > 0) {
                request.setAttribute("bagOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
            }
        }

        // constraint display values
        request.setAttribute("constraintDisplayValues", MainHelper.makeConstraintDisplayMap(query));
        request.setAttribute("lockedPaths", listToMap(findLockedPaths(query)));
        request.setAttribute("viewPaths", listToMap(query.getView()));
        request.setAttribute("viewPathOrder", createIndexMap(query.getView()));
        request.setAttribute("viewPathTypes", getPathTypes(query.getView(), query));
        Map prefixes = getViewPathLinkPaths(query);
        request.setAttribute("viewPathLinkPrefixes", prefixes);
        request.setAttribute("viewPathLinkPaths", getPathTypes(prefixes.values(), query));
        
        // set up the navigation links (eg. Department > employees > department)
        String prefix = (String) session.getAttribute("prefix");
        String current = null;
        Map navigation = new LinkedHashMap();
        Map navigationPaths = new LinkedHashMap();
        if (prefix != null && prefix.indexOf(".") != -1) {
            for (StringTokenizer st = new StringTokenizer(prefix, "."); st.hasMoreTokens();) {
                String token = st.nextToken();
                current = (current == null ? token : current + "." + token);
                navigation.put(token, current);
                navigationPaths.put(token,
                        TypeUtil.unqualifiedName(MainHelper.getTypeForPath(current, query)));
            }
        }
        request.setAttribute("navigation", navigation);
        request.setAttribute("navigationPaths", navigationPaths);

        return null;
    }

    /**
     * Given a input List, return a Map from list element value to list index.
     * 
     * @param list a List
     * @return Map from list element values to list index Integer
     */
    protected static Map createIndexMap(List list) {
        HashMap map = new HashMap();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i), new Integer(i));
        }
        return map;
    }

    /**
     * Get a list of paths that should not be removed from the query by the
     * user. This is usually because they are involved in a loop query constraint.
     *
     * @param pathquery  the PathQuery containing the paths
     * @return           list of paths (as Strings) that cannot be removed by the user
     */
    protected static List findLockedPaths(PathQuery pathquery) {
        ArrayList paths = new ArrayList();
        Iterator iter = pathquery.getNodes().values().iterator();
        while (iter.hasNext()) {
            PathNode node = (PathNode) iter.next();
            Iterator citer = node.getConstraints().iterator();
            while (citer.hasNext()) {
                Constraint con = (Constraint) citer.next();
                if (!node.isAttribute() && !BagConstraint.VALID_OPS.contains(con.getOp())) {
                    // loop query constraint
                    // get path and superpaths
                    String path = (String) con.getValue();
                    while (path != null) {
                        paths.add(path);
                        if (path.indexOf('.') != -1) {
                            path = path.substring(0, path.lastIndexOf('.'));
                        } else {
                            path = null;
                        }
                    }
                }
            }
        }
        return paths;
    }

    /**
     * Return a Map from path to unqualified type name.
     * @param paths collection of paths
     * @param pathquery related PathQuery
     * @return Map from path to type
     */
    protected static Map getPathTypes(Collection paths, PathQuery pathquery) {
        Map viewPathTypes = new HashMap();

        Iterator iter = paths.iterator();

        while (iter.hasNext()) {
            String path = (String) iter.next();
            String unqualifiedName =
                TypeUtil.unqualifiedName(MainHelper.getTypeForPath(path, pathquery));
            viewPathTypes.put(path, unqualifiedName);
        }

        return viewPathTypes;
    }
    
    /**
     * Return a Map from path to path/subpath pointing to the nearest not attribute for each
     * path on the select list.
     * practise this results in the same path or the path with an attribute name chopped off
     * the end.
     * @param pathquery the path query
     * @return mapping from select list path to non-attribute path
     */
    protected static Map getViewPathLinkPaths(PathQuery pathquery) {
        Map linkPaths = new HashMap();
        Iterator iter = pathquery.getView().iterator();

        while (iter.hasNext()) {
            String path = (String) iter.next();
            if (MainHelper.isPathAttribute(path, pathquery)) {
                linkPaths.put(path, path.substring(0, path.lastIndexOf(".")));
            } else {
                linkPaths.put(path, path);
            }
        }
        
        return linkPaths;
    }

    /**
     * Returns a map where every item in <code>list</code> maps to Boolean TRUE.
     *
     * @param list  the list of map keys
     * @return      a map that maps every item in list to Boolean.TRUE
     */
    protected static Map listToMap(List list) {
        Map map = new HashMap();
        int n = list.size();
        for (int i = 0; i < n; i++) {
            map.put(list.get(i), Boolean.TRUE);
        }
        return map;
    }
}