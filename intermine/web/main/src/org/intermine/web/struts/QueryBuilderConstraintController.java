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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Controller for the main constraint editing tile
 * @author Thomas Riley
 */
public class QueryBuilderConstraintController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 @SuppressWarnings("unused") HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext.
                                               getAttribute(Constants.OBJECT_STORE_SUMMARY);
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        BagQueryConfig bagQueryConfig =
            (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();

        //set up the node on which we are editing constraints
        if (session.getAttribute("editingNode") != null) {

            SessionMethods.moveToRequest("editingNode", request);
            PathNode node = (PathNode) request.getAttribute("editingNode");
            SessionMethods.moveToRequest("editingConstraintIndex", request);
            SessionMethods.moveToRequest("editingTemplateConstraint", request);
            SessionMethods.moveToRequest("editingConstraintValue", request);
            SessionMethods.moveToRequest("editingConstraintOperand", request);
            SessionMethods.moveToRequest("editingConstraintExtraValue", request);

            request.setAttribute("displayConstraint", new DisplayConstraint(node, model, oss,
                        null, classKeys));
            // we can't create loop constraints on outer join paths, nodes on this path may already
            // be constrained to inner joins - get current style for query
            String correctJoinPath = query.getCorrectJoinStyle(node.getPathString());
            if (correctJoinPath.indexOf(":") == -1 && !node.isAttribute()) {
                // loop query arguments
                ArrayList paths = new ArrayList();
                Iterator iter = query.getNodes().values().iterator();
                while (iter.hasNext()) {
                    PathNode anode = (PathNode) iter.next();
                    // we can create a loop constraint if:
                    // - there is another node of the same type
                    // - the other node has no outer joins in its path
                    if (anode != node && anode.getType().equals(node.getType()) 
                            && (anode.getPathString().indexOf(":") == -1)) {
                        paths.add(anode.getPathString());
                    }
                }

                Map attributeOps = MainHelper.mapOps(ClassConstraint.VALID_OPS);
                request.setAttribute("loopQueryOJ", node.isOuterJoin());
                request.setAttribute ("loopQueryOps", attributeOps);
                request.setAttribute ("loopQueryPaths", paths);
            }

            // work out the parent class of node if it is a key field or the class
            // of object/reference/collection
            String nodeType;
            boolean useBags;
            if (node.isAttribute() && (node.getPathString().indexOf('.')) >= 0) {
                nodeType = (query.getNodes().get(
                        node.getPathString().substring(0,
                        node.getPathString().lastIndexOf(".")))).getType();
                useBags = ClassKeyHelper.isKeyField(classKeys, nodeType, node
                        .getFieldName());
                //fetch AutoCompleter from servletContext
                AutoCompleter ac = (AutoCompleter)
                                        servletContext.getAttribute(Constants.AUTO_COMPLETER);

                if (ac != null && ac.hasAutocompleter(node.getParentType(), node.getFieldName())) {
                    request.setAttribute("useAutoCompleter", ac);

                }
                request.setAttribute("classDescriptor", node.getParentType());
                request.setAttribute("fieldDescriptor", node.getFieldName());

            } else {
                if ((node.getPathString().indexOf('.')) >= 0) {
                    nodeType = TypeUtil.unqualifiedName(MainHelper.getTypeForPath(
                                node.getPathString(), query));
                } else {
                    nodeType = node.getType();
                }
                String connectFieldName = bagQueryConfig.getConnectField();
                Class nodeClass;
                Boolean haveExtraConstraint;
                try {
                    nodeClass = Class.forName(model.getPackageName() + "." + node.getType());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Can't find class for: " + node.getType());
                }
                FieldDescriptor fd = model.getFieldDescriptorsForClass(nodeClass)
                    .get(connectFieldName);
                if ((fd != null) && (fd instanceof ReferenceDescriptor)) {
                    haveExtraConstraint = Boolean.TRUE;
                } else {
                    haveExtraConstraint = Boolean.FALSE;
                }
                // A Boolean describing whether a LOOKUP constraint on that
                //    Constraint would have an extra constraint
                request.setAttribute("haveExtraConstraint", haveExtraConstraint);
                // The type of the extra constraint class
                request.setAttribute("extraBagQueryClass",
                        TypeUtil.unqualifiedName(extraClassName));
                // A List of values for the extra constraint
                request.setAttribute("extraClassFieldValues", oss.getFieldValues(extraClassName,
                            bagQueryConfig.getConstrainField()));
                useBags = ClassKeyHelper.hasKeyFields(classKeys, nodeType);
                Collection<String> keyFields = ClassKeyHelper.getKeyFieldNames(classKeys, nodeType);
                String keyFieldStr = StringUtil.prettyList(keyFields, true);
                request.setAttribute("keyFields", keyFieldStr);
            }

            if (useBags) {
                Map<String, InterMineBag> allBags =
                    WebUtil.getAllBags(profile.getSavedBags(), SessionMethods.getSearchRepository
                            (servletContext));
                Map bags = WebUtil.getBagsOfType(allBags, nodeType, os.getModel());
                if (!bags.isEmpty()) {
                        request.setAttribute("bagOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
                        request.setAttribute("bags", bags);
                }
            }
            Integer index = (Integer) request.getAttribute("editingConstraintOperand");
            if (index != null) {
                ConstraintOp op = ConstraintOp.getOpForIndex(index);
                List ops = BagConstraint.VALID_OPS;
                if (op != null && ops.contains(op)) {
                    request.setAttribute("constrainOnBag", "true");
                }
            }
        }
        return null;
    }
}
