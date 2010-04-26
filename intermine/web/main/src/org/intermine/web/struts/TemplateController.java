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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
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
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.query.MainHelper;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateBuildState;
import org.intermine.web.logic.template.TemplateHelper;

/**
 * Controller for the template tile. This tile can be used for real template
 * input or to preview a TemplateQuery that is in construction. The preview
 * case involves the page controller or action putting the TemplateQuery in
 * the request attribute "previewTemplate". The jsp can also render differently
 * based on the presence of the "previewTemplate" attribute.<p>
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 * @author Richard Smith
 */
public class TemplateController extends TilesAction
{
    /**
     * Finds the correct template to display in the following ways:
     * <ol>
     *    <li>First, looks for a "name" request parameter and uses that for a the lookup.
     *    <li>If "name" parameter doesn't exist, looks for TemplateQuery in request attribute
     *        "previewTemplate".
     * </ol>
     * In all cases where a template name is provided (in other words, the template query
     * was not provided in the "previewTemplate" request attribute), the template is then
     * looked up based on the value of the "scope" request parameter.
     *
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
            @SuppressWarnings("unused") ActionMapping mapping, ActionForm form,
            HttpServletRequest request, @SuppressWarnings("unused") HttpServletResponse response)
    throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ObjectStore os = im.getObjectStore();
        Model model = os.getModel();
        ObjectStoreSummary oss = im.getObjectStoreSummary();
        TemplateSummariser summariser = im.getTemplateSummariser();
        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();
        AutoCompleter ac = SessionMethods.getAutoCompleter(session.getServletContext());
        TemplateManager templateManager = im.getTemplateManager();
        BagManager bagManager = im.getBagManager();
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();

        TemplateForm tf = (TemplateForm) form;

        String templateName = request.getParameter("name");
        String scope = request.getParameter("scope");
        String loadModifiedTemplate = request.getParameter("loadModifiedTemplate");
        String preSelectedBagName = request.getParameter("bagName");
        String idForLookup = request.getParameter("idForLookup");
        InterMineObject imObj = null;
        if (!StringUtils.isEmpty(idForLookup)) {
            imObj = os.getObjectById(new Integer(idForLookup));
        }
        // FIXME see #2239
        // If the template has been modified and uses an object bag constraint
        // it will be missing one of its original constraints (which will have been
        // replaced by constraining the id to be in the bag.
        TemplateQuery modifiedTemplate = null;
        if (loadModifiedTemplate != null) {
            modifiedTemplate = getTemporaryTemplate(session, templateName);
            templateName = modifiedTemplate.getName();
        }
        TemplateQuery template = null;
        // we're in querybuilder with templatePreview.jsp
        if (context.getAttribute("builder") != null) {
            PathQuery query = SessionMethods.getQuery(session);
            TemplateBuildState tbs = SessionMethods.getTemplateBuildState(session);
            template = TemplateHelper.buildTemplateQuery(tbs, query);
        }
        if (templateName == null && template != null) {
            templateName = template.getName();
        } else {
            if (scope == null) {
                scope = Scope.ALL;
            }
            template = templateManager.getTemplate(profile, templateName, scope);
        }
        // something's gone horribly wrong
        if (template == null) {
            return null;
        }
        Map<Constraint, DisplayConstraint> displayConstraints = new HashMap();
        Map<PathNode, List<Constraint>> constraints = new HashMap();
        Map<Constraint, Map<String, InterMineBag>> bags = new HashMap();
        Map<Constraint, String> constraintBagTypes = new HashMap();
        Map<Constraint, Object> selectedBagNames = new HashMap();
        Map<Constraint, String> keyFields = new HashMap();
        Map<Constraint, Boolean> haveExtraConstraint = new HashMap();
        Map<String, List<FieldDescriptor>> classKeys = im.getClassKeys();
        // for the autocompleter
        Map<String, String> classDesc = new HashMap();
        Map<String, String> fieldDesc = new HashMap();
        // For each node with an editable constraint, create a DisplayConstraint bean
        // and the human-readable "name" for each node (Department.company.name -> "Company name")
        TemplateQuery displayTemplate = (TemplateQuery) template.clone();
        Map<String, PathNode> editableNodesMap = new HashMap();

        for (PathNode node : template.getEditableNodes()) {
            editableNodesMap.put(node.getPathString(), node);
        }

        Map<String, PathNode> constrainedNodesMap = new HashMap();
        for (Map.Entry<String, PathNode> nodeEntry : template.getNodes().entrySet()) {
            PathNode node = nodeEntry.getValue();
            if (node.getConstraints().size() > 0) {
                constrainedNodesMap.put(node.getPathString(), node);
            }
        }

        for (PathNode node : template.getEditableNodes()) {
            PathNode displayNode = displayTemplate.getNodes().get(node.getPathString());
            constructAutocompleteIndex(displayTemplate, ac, model, node, classDesc, fieldDesc);
            int j = 1;
            for (Constraint c : displayTemplate.getEditableConstraints(node)) {

                if (modifiedTemplate != null) {
                    // FIXME See #2239
                    c = updateForModifiedTemplate(modifiedTemplate, node, c, displayNode,
                            selectedBagNames);
                }
                displayConstraints.put(c, new DisplayConstraint(displayNode, model, oss,
                        summariser.getPossibleValues(template, node), classKeys));
                PathNode parent = (PathNode) displayNode.getParent();
                if (parent == null) {
                    parent = displayNode;
                }
                // check if this constraint can be used with bags and if any available
                boolean isKeyField = ClassKeyHelper.isKeyField(classKeys, parent.getType(),
                        displayNode.getFieldName());
                if (isKeyField || !node.isAttribute()) {
                    String nodeType = (isKeyField ? parent.getType() : node.getType());
                    constraintBagTypes.put(c, nodeType);
                    Map<String, InterMineBag> constraintBags =
                        bagManager.getUserOrGlobalBagsOfType(profile, nodeType);
                    if (constraintBags != null && constraintBags.size() != 0) {
                        bags.put(c, constraintBags);
                        if (preSelectedBagName != null
                                && constraintBags.containsKey(preSelectedBagName)) {
                            tf.setUseBagConstraint(j + "", true);
                            selectedBagNames.put(c, preSelectedBagName);
                        }
                    }
                }
                if (!node.isAttribute() && c.getOp().equals(ConstraintOp.LOOKUP)) {

                    //find the key fields for a help message
                    Collection<String> keyFieldCol = ClassKeyHelper.getKeyFieldNames(classKeys,
                            node.getType());
                    String keyFieldStr = StringUtil.prettyList(keyFieldCol, true);
                    keyFields.put(c, keyFieldStr);

                    // check for extra constraint
                    String connectFieldName = bagQueryConfig.getConnectField();
                    Class nodeType;
                    try {
                        nodeType = Class.forName(model.getPackageName() + "." + node.getType());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Can't find class for: " + node.getType());
                    }
                    FieldDescriptor fd = model.getFieldDescriptorsForClass(nodeType)
                    .get(connectFieldName);
                    if ((fd != null) && (fd instanceof ReferenceDescriptor)) {
                        // An extra constraint is possible, now check if it has already been
                        // constrained elsewhere in the query:
                        String extraPath = node.getPathString() + "." + connectFieldName;
                        boolean alreadyConstrained = constrainedNodesMap.containsKey(extraPath);
                        PathNode extraNode = template.getNode(extraPath);
                        if (extraNode != null) {
                            for (String classKey : ClassKeyHelper.getKeyFieldNames(classKeys,
                                    extraNode.getType())) {
                                alreadyConstrained = alreadyConstrained || constrainedNodesMap
                                .containsKey(extraPath + "." + classKey);
                            }
                        }
                        if (alreadyConstrained) {
                            haveExtraConstraint.put(c, Boolean.FALSE);
                        } else {
                            haveExtraConstraint.put(c, Boolean.TRUE);
                        }
                    } else {
                        haveExtraConstraint.put(c, Boolean.FALSE);
                    }

                }
                j++;
            }
            constraints.put(displayNode, displayTemplate.getEditableConstraints(displayNode));
        }
        populateTemplateForm(displayTemplate, tf, request, classKeys, ac, imObj);
        tf.setName(templateName);
        tf.setType(scope);
        // A Map which have as key the pathstring and as value the name of the last class
        request.setAttribute("classDesc", classDesc);
        // A Map which containts as key the pathstring and as value the field name
        request.setAttribute("fieldDesc", fieldDesc);
        // The template query
        request.setAttribute("templateQuery", displayTemplate);
        // A Map from displayNode to a collection of editable constraints
        request.setAttribute("constraints", constraints);
        // A Map from Constraint to a DisplayConstraint which provides access to summary data
        request.setAttribute("displayConstraints", displayConstraints);
        // A Map from Constraint to a Map from bag name to InterMineBag
        request.setAttribute("constraintBags", bags);
        // A Map from Constraint to the Class of the parent node
        request.setAttribute("constraintBagTypes", constraintBagTypes);
        // A Map from Constraint to a bag name, for if a particular bag should be pre-selected
        request.setAttribute("selectedBagNames", selectedBagNames);
        // A Map from Constraint to a String containing help for a LOOKUP constraint
        request.setAttribute("keyFields", keyFields);
        // A Map from Constraint to a Boolean describing whether a LOOKUP constraint on that
        //    Constraint would have an extra constraint
        request.setAttribute("haveExtraConstraint", haveExtraConstraint);
        // The type of the extra constraint class
        request.setAttribute("extraBagQueryClass", TypeUtil.unqualifiedName(extraClassName));
        // A List of values for the extra constraint
        request.setAttribute("extraClassFieldValues", oss.getFieldValues(extraClassName,
                bagQueryConfig.getConstrainField()));
        if (bagManager.getUserAndGlobalBags(profile).size() > 0) {
            request.setAttribute("bagOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
        }
        return null;
    }

    private Constraint updateForModifiedTemplate(TemplateQuery modifiedTemplate, PathNode node,
            Constraint con, PathNode displayNode, Map<Constraint, Object> selectedBagNames) {
        Constraint c = con;
        Constraint modC = modifiedTemplate.getConstraintByCode(c.getCode());
        Object value;
        String selectedBagName = null;
        ConstraintOp newOp = modC.getOp();
        if (!newOp.equals(ConstraintOp.IN) && !newOp.equals(ConstraintOp.NOT_IN)) {
            value = modC.getValue();
        } else {
            // modified constraint set to a bag
            value = c.getValue();
            selectedBagName = (String) modC.getValue();
        }
        Constraint newC = new Constraint(newOp, value, true, c.getDescription(),
                c.getCode(), c.getIdentifier(), modC.getExtraValue());
        displayNode.getConstraints().set(node.getConstraints().indexOf(c), newC);
        c = newC;
        if (selectedBagName != null) {
            selectedBagNames.put(c, modC.getValue());
        }
        return c;
    }

    private TemplateQuery getTemporaryTemplate(HttpSession session, String templateName) {
        Profile profile = SessionMethods.getProfile(session);
        SavedQuery savedQuery = profile.getHistory().get(templateName);
        PathQuery currentQuery = SessionMethods.getQuery(session);
        TemplateQuery template = null;
        if (savedQuery.getPathQuery() instanceof TemplateQuery) {
            template = (TemplateQuery) savedQuery.getPathQuery();
        } else if (currentQuery instanceof TemplateQuery) {
            // see #1435
            template = (TemplateQuery) currentQuery;
        }
        return template;
    }

    private void constructAutocompleteIndex(PathQuery query, AutoCompleter ac,
            Model model, PathNode node,
            Map<String, String> classDesc,
            Map<String, String> fieldDesc) {
        if (ac != null && ac.hasAutocompleter(node.getParentType(), node.getFieldName())) {
            Path path;
            try {
                path = PathQuery.makePath(model, query, node.getPathString());
            } catch (PathException e) {
                // Should not happen, as the path was taken from a node
                throw new Error("There must be a bug", e);
            }
            if (path.getEndFieldDescriptor() != null) {
                fieldDesc.put(node.getPathString(), path.getEndFieldDescriptor().getName());
                String[] tmp = path.getLastClassDescriptor().getName().split("\\.");
                classDesc.put(node.getPathString(), tmp[tmp.length - 1]);
            }
        }
    }

    /**
     *  Populate parts of the template form that are used in javascript methods in template.jsp
     */
    private static void populateTemplateForm(TemplateQuery template,
            TemplateForm tf, HttpServletRequest request,
            Map<String, List<FieldDescriptor>> classKeys, AutoCompleter ac,
            InterMineObject imObject) {
        int j = 0;

        Map<String, String> autoMap = new HashMap<String, String>();

        for (PathNode node : template.getEditableNodes()) {
            for (Constraint c : template.getEditableConstraints(node)) {
                String attributeKey = "" + (j + 1);
                tf.setAttributeValues(attributeKey, "" + c.getDisplayValue());
                tf.setAttributeOps(attributeKey, "" + c.getOp().getIndex());
                tf.setExtraValues(attributeKey, "" + c.getExtraValue());
                if (imObject != null) {
                    List<FieldDescriptor> keyFields =
                        classKeys.get(DynamicUtil.getFriendlyName(imObject.getClass()));
                    AttributeDescriptor classKey = (AttributeDescriptor) keyFields.iterator()
                    .next();
                    String value = null;
                    try {
                        value = (String) TypeUtil.getFieldValue(imObject, classKey.getName());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Error while filling in Lookup template values:"
                                + e.getMessage());
                    }
                    tf.setAttributeValues(attributeKey, value);
                }
                if (ac != null
                        && ac.hasAutocompleter(node.getParentType(), node.getFieldName())) {
                    autoMap.put(node.getParentType() + "." + node.getFieldName(),
                    "useAutoCompleter");
                }
                j++;
            }
        }
        request.setAttribute("autoCompleterMap", autoMap);
    }
}
