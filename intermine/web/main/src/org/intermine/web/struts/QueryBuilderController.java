package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
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

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.MetadataNode;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.session.SessionMethods;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the main query builder tile. Generally, request attributes that are required by
 * multiple tiles on the query builder are synthesized here.
 * 
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @see org.intermine.web.struts.QueryBuilderConstraintController
 * @see org.intermine.web.struts.QueryBuilderPathsController
 */
public class QueryBuilderController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        populateRequest(request, response);
        return null;
    }

    /**
     * Populate the request with the necessary attributes to render the query builder page. This
     * method is static so that it can be called from the AJAX actions in MainChange.java
     * 
     * @param request
     *            the current request
     * @param response
     *            the current response
     * @see QueryBuilderChange
     */
    public static void populateRequest(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);

        // constraint display values
        request.setAttribute("lockedPaths", listToMap(findLockedPaths(query)));
        List view = SessionMethods.getEditingView(session);

        request.setAttribute("viewPaths", listToMap(view));
        request.setAttribute("viewPathOrder", createIndexMap(view));
        request.setAttribute("viewPathTypes", getPathTypes(view, query));

        // set up the metadata
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        boolean isSuperUser;
        Boolean superUserAttribute = (Boolean) session.getAttribute(Constants.IS_SUPERUSER);
        if (superUserAttribute != null && superUserAttribute.equals(Boolean.TRUE)) {
            isSuperUser = true;
        } else {
            isSuperUser = false;
        }
        
        String prefix = (String) session.getAttribute("prefix");
        Collection nodes = 
            MainHelper.makeNodes((String) session.getAttribute("path"), model, isSuperUser);
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            MetadataNode node = (MetadataNode) iter.next();
            // Update view nodes
            String pathName = node.getPath();
            int firstDot = pathName.indexOf('.');
            String fullPath;
            if (firstDot == -1) {
                fullPath = prefix;
            } else {
                String pathNameWithoutClass = pathName.substring(firstDot + 1);
                fullPath = prefix + "." + pathNameWithoutClass;
            }
            if (view.contains(fullPath)) {
                node.setSelected(true);
            } else {
                Path path = new Path(model, pathName);
                // If an object has been selected, select its fields instead
                if (path.getEndFieldDescriptor() == null || path.endIsReference()
                    || path.endIsCollection()) {
                    if (view.contains(path)) {
                        ClassDescriptor cld = path.getEndClassDescriptor();
                        List cldFieldConfigs = 
                            FieldConfigHelper.getClassFieldConfigs(webConfig, cld);
                        Iterator cldFieldConfigIter = cldFieldConfigs.iterator();
                        while (cldFieldConfigIter.hasNext()) {
                            FieldConfig fc = (FieldConfig) cldFieldConfigIter.next();
                            String pathFromField = pathName + "." + fc.getFieldExpr();
                            if (view.contains(pathFromField)) {
                                node.setSelected(true);
                            } else {
                                node.setSelected(false);
                            }
                        }
                    }
                }
            }
        }
        request.setAttribute("nodes", nodes);

        Map prefixes = getViewPathLinkPaths(query);
        request.setAttribute("viewPathLinkPrefixes", prefixes);
        request.setAttribute("viewPathLinkPaths", getPathTypes(prefixes.values(), query));

        // set up the navigation links (eg. Department > employees > department)
        String current = null;
        Map navigation = new LinkedHashMap();
        Map navigationPaths = new LinkedHashMap();
        if (prefix != null && prefix.indexOf(".") != -1) {
            for (StringTokenizer st = new StringTokenizer(prefix, "."); st.hasMoreTokens();) {
                String token = st.nextToken();
                current = (current == null ? token : current + "." + token);
                navigation.put(token, current);
                navigationPaths.put(token, TypeUtil.unqualifiedName(MainHelper
                    .getTypeForPath(current, query)));
            }
        }
        request.setAttribute("navigation", navigation);
        request.setAttribute("navigationPaths", navigationPaths);
    }

    /**
     * Given a input List, return a Map from list element value to list index.
     * 
     * @param list
     *            a List
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
     * Get a list of paths that should not be removed from the query by the user. This is usually
     * because they are involved in a loop query constraint.
     * 
     * @param pathquery
     *            the PathQuery containing the paths
     * @return list of paths (as Strings) that cannot be removed by the user
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
     * 
     * @param paths
     *            collection of paths
     * @param pathquery
     *            related PathQuery
     * @return Map from path to type
     */
    protected static Map getPathTypes(Collection paths, PathQuery pathquery) {
        Map viewPathTypes = new HashMap();

        Iterator iter = paths.iterator();

        while (iter.hasNext()) {
            String path = (String) iter.next();
            String unqualifiedName = TypeUtil.unqualifiedName(MainHelper.getTypeForPath(path,
                                                                                        pathquery));
            viewPathTypes.put(path, unqualifiedName);
        }

        return viewPathTypes;
    }

    /**
     * Return a Map from path to path/subpath pointing to the nearest not attribute for each path on
     * the select list. practise this results in the same path or the path with an attribute name
     * chopped off the end.
     * 
     * @param pathquery
     *            the path query
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
     * @param list
     *            the list of map keys
     * @return a map that maps every item in list to Boolean.TRUE
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
