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

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.ObjectStore;
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
                Class type = MainHelper.getClass(node.getType());
                Map attributeOps = MainHelper.mapOps(SimpleConstraint.validOps(type));
                request.setAttribute("attributeOps", attributeOps);
            } else {
                Map classCounts = (Map) servletContext.getAttribute("classCounts");
                ClassDescriptor cld = MainHelper.getClassDescriptor(node.getType(), model);
                ArrayList subclasses = new ArrayList();
                Iterator iter = new TreeSet(getChildren(cld)).iterator();
                while (iter.hasNext()) {
                    String thisClassName = (String) iter.next();
                    if (((Integer) classCounts.get(thisClassName)).intValue() > 0) {
                        subclasses.add(TypeUtil.unqualifiedName(thisClassName));
                    }
                }
                request.setAttribute("subclasses", subclasses);
                
                // loop query arguments
                ArrayList paths = new ArrayList();
                Map displayPaths = new HashMap();
                iter = query.getNodes ().values ().iterator ();
                while (iter.hasNext ()) {
                    PathNode anode = (PathNode) iter.next();
                    if (anode != node && anode.getType ().equals (node.getType())) {
                        paths.add(anode.getPath());
                        displayPaths.put(anode.getPath(),
                               MainForm.dotPathToNicePath(anode.getPath()));
                    }
                }
                
                Map attributeOps = MainHelper.mapOps(ClassConstraint.VALID_OPS);
                request.setAttribute ("loopQueryOps", attributeOps);
                request.setAttribute ("loopQueryPaths", paths);
                request.setAttribute ("loopQueryPathsDisplay", displayPaths);
            }
            if (profile.getSavedBags().size() > 0) {
                request.setAttribute("bagOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
            }
            
            String parentType = node.getParentType();
            if (parentType != null) {
                String parentClassName = MainHelper.getClass(parentType, os.getModel()).getName();
                List fieldNames = oss.getFieldValues(parentClassName, node.getFieldName());
                if (fieldNames != null && node.getType() != null) {
                    request.setAttribute("attributeOptions", fieldNames);
                    Class parentClass = MainHelper.getClass(node.getType());
                    List fixedOps = SimpleConstraint.fixedEnumOps(parentClass);
                    List fixedOpsCodes = new ArrayList();
                    Iterator iter = fixedOps.iterator();
                    while (iter.hasNext()) {
                        fixedOpsCodes.add(((ConstraintOp) iter.next()).getIndex());
                    }
                    request.setAttribute("fixedOptionsOps", fixedOpsCodes);
                }
            }
        }

        // constraint display values
        request.setAttribute("constraintDisplayValues", MainHelper.makeConstraintDisplayMap(query));
        request.setAttribute("lockedPaths", listToMap(findLockedPaths(query)));
        request.setAttribute("viewPaths", listToMap(query.getView()));
        
        // set up the navigation links (eg. Department > employees > department)
        String prefix = (String) session.getAttribute("prefix");
        String current = null;
        Map navigation = new LinkedHashMap();
        if (prefix != null && prefix.indexOf(".") != -1) {
            for (StringTokenizer st = new StringTokenizer(prefix, "."); st.hasMoreTokens();) {
                String token = st.nextToken();
                current = (current == null ? token : current + "." + token);
                navigation.put(token, current);
            }
        }
        request.setAttribute("navigation", navigation);

        return null;
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
    
    /**
     * Get the names of the type of this ClassDescriptor and all its descendents
     * @param cld the ClassDescriptor
     * @return a Set of class names
     */
    protected static Set getChildren(ClassDescriptor cld) {
        Set children = new HashSet();
        getChildren(cld, children);
        return children;
    }
    
    /**
     * Add the names of the descendents of a ClassDescriptor to a Set
     * @param cld the ClassDescriptor
     * @param children the Set of child names
     */
    protected static void getChildren(ClassDescriptor cld, Set children) {
        for (Iterator i = cld.getSubDescriptors().iterator(); i.hasNext();) {
            ClassDescriptor child = (ClassDescriptor) i.next();
            children.add(child.getName());
            getChildren(child, children);
        }
    }
}