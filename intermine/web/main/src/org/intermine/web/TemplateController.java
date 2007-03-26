package org.intermine.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.query.BagConstraint;
import org.intermine.objectstore.query.ConstraintOp;

/**
 * Controller for the template tile. This tile can be used for real template
 * input or to preview a TemplateQuery that is in construction. The preview
 * case involves the page controller or action putting the TemplateQuery in
 * the request attribute "previewTemplate". The jsp can also render differently
 * based on the presence of the "previewTemplate" attribute.<p>
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
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
     * looked up based on the value of the "type" request parameter.
     * <ol>
     *    <li>no type parameter means the template is loaded from built-in templates
     *    <li>if type == user then it's one of the users saved templates
     *    <li>shared? public?
     * </ol>
     *
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
            ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStore os = (ObjectStore) servletContext
                .getAttribute(Constants.OBJECTSTORE);
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext
                .getAttribute(Constants.OBJECT_STORE_SUMMARY);
        TemplateForm tf = (TemplateForm) form;
        TemplateQuery template = null;
        String queryName = request.getParameter("name");
        String type = request.getParameter("type");
        String loadModifiedTemplate = request
                .getParameter("loadModifiedTemplate");
        String bagName = request.getParameter("bagName");

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
            if (type == null) {
                type = TemplateHelper.GLOBAL_TEMPLATE;
            }
            String userName = ((Profile) session
                    .getAttribute(Constants.PROFILE)).getUsername();
            template = TemplateHelper.findTemplate(servletContext, session,
                    userName, queryName, type);
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
        
        servletContext = session.getServletContext();
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);

        // For each node with an editable constraint, create a DisplayConstraint bean
        // and the human-readable "name" for each node (Department.company.name -> "Company name")

        TemplateQuery displayTemplate = (TemplateQuery) template.clone();
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            PathNode displayNode = (PathNode) displayTemplate.getNodes().get(node.getPath());
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
                    Constraint newC = new Constraint(c.getOp(), value, true,
                            c.getDescription(), c.getCode(), c.getIdentifier());
                    displayNode.getConstraints().set(node.getConstraints().indexOf(c), newC);
                    c = newC;
                    if (selectedBagName != null) {
                        selectedBagNames.put(c, modC.getValue());
                    }
                }
                displayConstraints.put(c, new DisplayConstraint(displayNode, os
                        .getModel(), oss, template.getPossibleValues(node)));
                
                // create display name
                PathNode parent;
                if (displayNode.getPath().indexOf('.') >= 0) {
                    parent = (PathNode) displayTemplate.getNodes().get(
                            displayNode.getPath().substring(0,
                            displayNode.getPath().lastIndexOf(".")));
                } else {
                    parent = displayNode;
                }
                names.put(c, parent.getType()
                        + " "
                        + displayNode.getPath().substring(
                                displayNode.getPath().lastIndexOf(".") + 1));

                // check if this constraint can be used with bags and if any available
                if (ClassKeyHelper.isKeyField(classKeys, parent.getType(), displayNode
                        .getFieldName())) {
                    constraintBagTypes.put(c, parent.getType());
                    Map constraintBags = profile.getBagsOfType(parent.getType(), os.getModel());
                    if (constraintBags != null && constraintBags.size() != 0) {
                        bags.put(c, constraintBags);
                        if (bagName != null && constraintBags.containsKey(bagName)) {
                            tf.setUseBagConstraint(j + "", true);
                            selectedBagNames.put(c, bagName);
                        }
                    }
                }
                j++;
            }
            constraints.put(displayNode, displayTemplate.getEditableConstraints(displayNode));
        }
        
        populateTemplateForm(displayTemplate, tf, request);

        tf.setTemplateName(queryName);
        tf.setTemplateType(type);
        request.setAttribute("templateQuery", displayTemplate);
        request.setAttribute("names", names);
        request.setAttribute("constraints", constraints);
        request.setAttribute("displayConstraints", displayConstraints);
        request.setAttribute("constraintBags", bags);
        request.setAttribute("constraintBagTypes", constraintBagTypes);
        request.setAttribute("selectedBagNames", selectedBagNames);
        
        if (profile.getSavedBags().size() > 0) {
            request.setAttribute("bagOps", MainHelper
                    .mapOps(BagConstraint.VALID_OPS));
        }

        return null;
    }

    /**
     *  Populate parts of the template form that are used in javscript methods in template.jsp
     */
    private static void populateTemplateForm(TemplateQuery template,
            TemplateForm tf, HttpServletRequest request) {
        int j = 0;
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();

            for (Iterator ci = template.getEditableConstraints(node).iterator(); ci
                    .hasNext();) {
                Constraint c = (Constraint) ci.next();
                String attributeKey = "" + (j + 1);
                tf.setAttributeValues(attributeKey, "" + c.getDisplayValue());
                tf.setAttributeOps(attributeKey, "" + c.getOp().getIndex());
                if (c.getIdentifier() != null) {
                    // If special request parameter key is present then we initialise
                    // the form bean with the parameter value
                    String paramName = c.getIdentifier() + "_value";
                    String constraintValue = request.getParameter(paramName);
                    if (constraintValue != null) {
                        tf.setAttributeValues(attributeKey, constraintValue);
                    }
                }
                j++;
            }
        }
    }
}
