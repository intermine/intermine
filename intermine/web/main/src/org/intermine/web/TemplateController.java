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
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext.
                                               getAttribute(Constants.OBJECT_STORE_SUMMARY);
        TemplateForm tf = (TemplateForm) form;
        TemplateQuery template = null;
        String queryName = request.getParameter("name");
        String type = request.getParameter("type");
        String loadModifiedTemplate = request.getParameter("loadModifiedTemplate");
        boolean populate = true;
        
        if (queryName == null) {
            queryName = request.getParameter("templateName");
        }
        
        // look for request attribute "previewTemplate" which is set while building a template
        template = (TemplateQuery) request.getAttribute("previewTemplate");
        
        // load the temporary template from the query history
        if (loadModifiedTemplate != null) {
            SavedQuery savedQuery = (SavedQuery) profile.getHistory().get(queryName);
            template = (TemplateQuery) savedQuery.getPathQuery();
            type = TemplateHelper.TEMP_TEMPLATE;
        }
        
        if (context.getAttribute("builder") != null) {
            PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
            TemplateBuildState tbs = (TemplateBuildState)
                    session.getAttribute(Constants.TEMPLATE_BUILD_STATE);
            template = TemplateHelper.buildTemplateQuery(tbs, query);
            request.setAttribute("previewTemplate", template);
        }
        
        if (queryName == null && template != null) {
            queryName = template.getName();
        } else {
            if (type == null) {
                type = TemplateHelper.GLOBAL_TEMPLATE;
            }
            String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
            template = TemplateHelper.findTemplate(servletContext, userName,
                                                   queryName, type);
        }
        
        if (template == null) {
            return null;
        }
        
        Map displayConstraints = new HashMap();
        Map names = new HashMap();
        Map constraints = new HashMap();
        
        // For each node with an editable constraint, create a DisplayConstraint bean
        // and the human-readable "name" for each node (Department.company.name -> "Company namae")
        
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            
            for (Iterator ci = template.getConstraints(node).iterator(); ci.hasNext();) {
                Constraint c = (Constraint) ci.next();
                displayConstraints.put(c, new DisplayConstraint(node, os.getModel(), oss));
                
                PathNode parent = (PathNode) template.getNodes().
                    get(node.getPath().substring(0, node.getPath().lastIndexOf(".")));
                names.put(c, parent.getType() + " "
                          + node.getPath().substring(node.getPath().lastIndexOf(".") + 1));
            }
            
            constraints.put(node, template.getConstraints(node));
        }
        
        populateTemplateForm(template, tf, request);
        
        tf.setTemplateName(queryName);
        tf.setTemplateType(type);
        request.setAttribute("templateQuery", template);
        request.setAttribute("names", names);
        request.setAttribute("constraints", constraints);
        request.setAttribute("displayConstraints", displayConstraints);

        if (profile.getSavedBags().size() > 0) {
            request.setAttribute("bagOps", MainHelper.mapOps(BagConstraint.VALID_OPS));
        }
        
        return null;
    }
    
    private static void populateTemplateForm(TemplateQuery template, TemplateForm tf,
            HttpServletRequest request) {
        int j = 0;
        for (Iterator i = template.getEditableNodes().iterator(); i.hasNext();) {
            PathNode node = (PathNode) i.next();
            
            for (Iterator ci = template.getConstraints(node).iterator(); ci.hasNext();) {
                Constraint c = (Constraint) ci.next();
                String attributeKey = "" + (j + 1);
                tf.setAttributeValues (attributeKey, "" + c.getDisplayValue());
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
