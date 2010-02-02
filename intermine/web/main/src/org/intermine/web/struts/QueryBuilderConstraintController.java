package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ClassConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.query.DisplayConstraint;
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
            @SuppressWarnings("unused") HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Profile profile = SessionMethods.getProfile(session);
        ServletContext servletContext = session.getServletContext();
        Model model = im.getModel();
        PathQuery query = SessionMethods.getQuery(session);
        query = query.clone();
        for (Path p : query.getView()) {
            String path = p.toStringNoConstraints();
            if (!query.getNodes().containsKey(path)) {
                query.addNode(path);
            }
        }
        ObjectStoreSummary oss = im.getObjectStoreSummary();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();

        PathNode node = (PathNode) session.getAttribute("editingNode");
        //set up the node on which we are editing constraints
        if (node != null) {
            SessionMethods.moveToRequest("editingNode", request);
            SessionMethods.moveToRequest("editingConstraintIndex", request);
            SessionMethods.moveToRequest("editingTemplateConstraint", request);
            SessionMethods.moveToRequest("editingConstraintValue", request);
            SessionMethods.moveToRequest("editingConstraintOperand", request);
            SessionMethods.moveToRequest("editingConstraintExtraValue", request);

            // Set up the Path, used to distinguish between outer-joinable nodes
            Path editingPath = PathQuery.makePath(model, query, node.getPathString());

            request.setAttribute("editingPath", editingPath);
            request.setAttribute("displayConstraint", new DisplayConstraint(node, model, oss,
                        null, classKeys));

            // we can't create loop constraints on outer join paths, nodes on this path may already
            // be constrained to inner joins - get current style for query
            //String correctJoinPath = query.getCorrectJoinStyle(node.getPathString());
//            if (correctJoinPath.indexOf(":") == -1 && !node.isAttribute()) {
                // loop query arguments
            ArrayList paths = new ArrayList();
            String nodeJoinGroup = node.getOuterJoinGroup();
            Iterator iter = query.getNodes().values().iterator();
            while (iter.hasNext()) {
                PathNode anode = (PathNode) iter.next();
                // we can create a loop constraint if:
                // - there is another node of the same type
                // - the other node has no outer joins in its path
                if (anode != node && anode.getType().equals(node.getType())
                        && nodeJoinGroup.equals(anode.getOuterJoinGroup())) {
                    paths.add(anode.getPathString());
                }
            }

            Map attributeOps = MainHelper.mapOps(ClassConstraint.VALID_OPS);
//                request.setAttribute("loopQueryOJ", node.isOuterJoin());
            request.setAttribute ("loopQueryOps", attributeOps);
            request.setAttribute ("loopQueryPaths", paths);
//            }

            // work out the parent class of node if it is a key field or the class
            // of object/reference/collection
            String nodeType;
            boolean useBags;
            if (node.isAttribute() && (node.getParent() != null)) {
                //nodeType = (query.getNodes().get(node.getPrefix())).getType();
                nodeType = TypeUtil.unqualifiedName(PathQuery.makePath(model, query,
                                node.getPathString()).getEndType().getName());
                useBags = ClassKeyHelper.isKeyField(classKeys, nodeType, node
                        .getFieldName());
                //fetch AutoCompleter from servletContext
                AutoCompleter ac = SessionMethods.getAutoCompleter(servletContext);

                if (ac != null && ac.hasAutocompleter(node.getParentType(), node.getFieldName())) {
                    request.setAttribute("useAutoCompleter", ac);

                }
                request.setAttribute("classDescriptor", node.getParentType());
                request.setAttribute("fieldDescriptor", node.getFieldName());

            } else {
                if (node.getParent() != null) {
                    nodeType = TypeUtil.unqualifiedName(PathQuery.makePath(model, query,
                                    node.getPathString()).getEndType().getName());
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

            // Search for elements already in the view list, to see whether an outer join is
            // permissable.
            {
                String ojBase = (node.isAttribute() ? node.getParent() : node).getPathString();
                String ojBaseDot = ojBase + ".";
                String ojBaseColon = ojBase + ":";
                boolean allowOuterJoin = false;
                for (String viewNode : query.getViewStrings()) {
                    if (viewNode.startsWith(ojBaseDot) || viewNode.startsWith(ojBaseColon)) {
                        allowOuterJoin = true;
                        break;
                    }
                }
                request.setAttribute("allowOuterJoin", Boolean.valueOf(allowOuterJoin));
            }

            if (useBags) {
                BagManager bagManager = im.getBagManager();
                Map<String, InterMineBag> bagsOfType =
                    bagManager.getUserOrGlobalBagsOfType(profile, nodeType);
                if (!bagsOfType.isEmpty()) {
                    request.setAttribute("bagOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
                    request.setAttribute("bags", bagsOfType);
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
        } else if (session.getAttribute("joinStylePath") != null) {
            // ONLY EDITING JOIN STYLE
            request.setAttribute("editingPath", PathQuery.makePath(model, query,
                                (String) session.getAttribute("joinStylePath")));
            request.setAttribute("joinStyleOnly", "true");
            session.removeAttribute("joinStylePath");
            request.setAttribute("allowOuterJoin", Boolean.TRUE);
        }

        return null;
    }


}
