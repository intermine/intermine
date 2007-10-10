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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;

import org.intermine.metadata.AttributeDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.DynamicUtil;
import org.intermine.util.StringUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.DisplayConstraint;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.template.TemplateBuildState;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

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
    private static final Logger LOG = Logger.getLogger(TemplateController.class);
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
                                 @SuppressWarnings("unused") ActionMapping mapping, 
                                 ActionForm form, 
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) 
    throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext
                .getAttribute(Constants.OBJECT_STORE_SUMMARY);
        BagQueryConfig bagQueryConfig =
            (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
        String extraClassName = bagQueryConfig.getExtraConstraintClassName();
        TemplateForm tf = (TemplateForm) form;
        TemplateQuery template = null;
        String queryName = request.getParameter("name");
        String scope = request.getParameter("scope");
        String loadModifiedTemplate = request.getParameter("loadModifiedTemplate");
        String bagName = request.getParameter("bagName");
        
        String idForLookup = request.getParameter("idForLookup");
        InterMineObject imObj = null;
        if (idForLookup != null && idForLookup.length() != 0) {
            imObj = os.getObjectById(new Integer(idForLookup));
        }
        
        if (queryName == null) {
            queryName = request.getParameter("templateName");
        }
       
        // look for request attribute "previewTemplate" which is set while building a template
        template = (TemplateQuery) request.getAttribute("previewTemplate");

        // load the temporary template from the query history
        
        // If the template has been modified and uses an object bag constraint
        // it will be missing one of its original constraints (which will have been
        // replaced by constraining the id to be in the bag.  
        TemplateQuery modifiedTemplate = null;
        if (loadModifiedTemplate != null) {
            String userName = ((Profile) session
                    .getAttribute(Constants.PROFILE)).getUsername();
            modifiedTemplate = TemplateHelper.findTemplate(servletContext, session,
                    userName, queryName, TemplateHelper.TEMP_TEMPLATE);
            queryName = modifiedTemplate.getName();
        }

        if (context.getAttribute("builder") != null) {
            PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
            TemplateBuildState tbs = (TemplateBuildState) session
                    .getAttribute(Constants.TEMPLATE_BUILD_STATE);
            template = TemplateHelper.buildTemplateQuery(tbs, query);
            request.setAttribute("previewTemplate", template);
        }

        if (queryName == null && template != null) {
            queryName = template.getName();
        } else {
            if (scope == null) {
                scope = TemplateHelper.ALL_TEMPLATE;
            }
            String userName = ((Profile) session
                    .getAttribute(Constants.PROFILE)).getUsername();
            template = TemplateHelper.findTemplate(servletContext, session,
                    userName, queryName, scope);
        }

        if (template == null) {
            return null;
        }

        Map displayConstraints = new HashMap();
        Map names = new HashMap();
        Map constraints = new HashMap();
        Map bags = new HashMap();
        Map constraintBagTypes = new HashMap();
        Map selectedBagNames = new HashMap();
        Map keyFields = new HashMap();
        Map haveExtraConstraint = new HashMap();
        
        servletContext = session.getServletContext();
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);

        // For each node with an editable constraint, create a DisplayConstraint bean
        // and the human-readable "name" for each node (Department.company.name -> "Company name")

        TemplateQuery displayTemplate = (TemplateQuery) template.clone();
        
        Map<String, InterMineBag> searchBags =
            WebUtil.getAllBags(profile.getSavedBags(), servletContext);

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
            int j = 1;
            for (Iterator ci = displayTemplate.getEditableConstraints(node).iterator(); ci
                    .hasNext();) {
                Constraint c = (Constraint) ci.next();
                if (modifiedTemplate != null) {
                    Constraint modC = modifiedTemplate.getConstraintByCode(c.getCode());
                    Object value;
                    String selectedBagName = null;
                    if (!modC.getOp().equals(ConstraintOp.IN)) {
                        value = modC.getValue(); 
                    } else {
                        // modified constraint set to a bag
                        value = c.getValue();
                        selectedBagName = (String) modC.getValue();
                    }
                    Constraint newC = new Constraint(c.getOp(), value, true, c.getDescription(),
                            c.getCode(), c.getIdentifier(), c.getExtraValue());
                    displayNode.getConstraints().set(node.getConstraints().indexOf(c), newC);
                    c = newC;
                    if (selectedBagName != null) {
                        selectedBagNames.put(c, modC.getValue());
                    }
                }
                displayConstraints.put(c, new DisplayConstraint(displayNode, os
                        .getModel(), oss, template.getPossibleValues(node), classKeys));
                
                // create display name
                PathNode parent;
                if (displayNode.getPathString().indexOf('.') >= 0) {
                    parent = displayTemplate.getNodes().get(
                            displayNode.getPathString().substring(0,
                            displayNode.getPathString().lastIndexOf(".")));
                } else {
                    parent = displayNode;
                }
                String displayName = parent.getType();
                if (displayNode.getPathString().indexOf('.') >= 0) {
                    displayName += " " + displayNode.getPathString()
                    .substring(displayNode.getPathString().lastIndexOf(".") + 1);
                }
                names.put(c, displayName);
                        
                // check if this constraint can be used with bags and if any available
                if (ClassKeyHelper.isKeyField(classKeys, parent.getType(), displayNode
                        .getFieldName())) {
                    constraintBagTypes.put(c, parent.getType());
                    Map constraintBags = 
                        WebUtil.getBagsOfType(searchBags, parent.getType(),
                                              os.getModel());
                    if (constraintBags != null && constraintBags.size() != 0) {
                        bags.put(c, constraintBags);
                        if (bagName != null && constraintBags.containsKey(bagName)) {
                            tf.setUseBagConstraint(j + "", true);
                            selectedBagNames.put(c, bagName);
                        }
                    }
                }
                if (!node.isAttribute()) {
                    constraintBagTypes.put(c, node.getType());
                    Map constraintBags =
                        WebUtil.getBagsOfType(searchBags, node.getType(),
                                              os.getModel());
                    if (constraintBags != null && constraintBags.size() != 0) {
                        bags.put(c, constraintBags);
                        if (bagName != null && constraintBags.containsKey(bagName)) {
                            tf.setUseBagConstraint(j + "", true);
                            selectedBagNames.put(c, bagName);
                        }
                    }
                    // this might be a lookup constraint, find the key fields for a help message
                    if (c.getOp().equals(ConstraintOp.LOOKUP)) {
                        Collection<String> keyFieldCol = ClassKeyHelper.getKeyFieldNames(classKeys,
                                node.getType());
                        String keyFieldStr = StringUtil.prettyList(keyFieldCol, true);
                        keyFields.put(c, keyFieldStr);
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
                            LOG.info("Checking extra constraint. editableNodesMap: " + editableNodesMap + ", extraPath: " + extraPath);
                            boolean alreadyConstrained = constrainedNodesMap.containsKey(extraPath);
                            PathNode extraNode = template.getNode(extraPath);
                            if (extraNode != null) {
                                for (String classKey : ClassKeyHelper.getKeyFieldNames(classKeys,
                                            extraNode.getType())) {
                                    LOG.info("Checking path " + extraPath + "." + classKey);
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
                }
                j++;
            }
            constraints.put(displayNode, displayTemplate.getEditableConstraints(displayNode));
        }
        
        populateTemplateForm(displayTemplate, tf, request, servletContext, imObj);

        tf.setTemplateName(queryName);
        tf.setTemplateType(scope);
        // The template query
        request.setAttribute("templateQuery", displayTemplate);
        // A Map from Constraint to a String that should be displayed as the constraint name
        request.setAttribute("names", names);
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
        
        if (searchBags.size() > 0) {
            request.setAttribute("bagOps", MainHelper
                    .mapOps(BagConstraint.VALID_OPS));
        }

        return null;
    }

    /**
     *  Populate parts of the template form that are used in javscript methods in template.jsp
     */
    private static void populateTemplateForm(TemplateQuery template,
            TemplateForm tf, HttpServletRequest request, ServletContext servletContext, 
            InterMineObject imObject) {
        int j = 0;
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();

            for (Iterator ci = template.getEditableConstraints(node).iterator(); ci
                    .hasNext();) {
                Constraint c = (Constraint) ci.next();
                String attributeKey = "" + (j + 1);
                tf.setAttributeValues(attributeKey, "" + c.getDisplayValue());
                tf.setAttributeOps(attributeKey, "" + c.getOp().getIndex());
                tf.setExtraValues(attributeKey, "" + c.getExtraValue());
                if (imObject != null) {
                    Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
                    Collection keyFields = (Collection) classKeys.get(DynamicUtil
                            .getFriendlyName(imObject.getClass()));
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
                j++;
            }
        }
    }
}
