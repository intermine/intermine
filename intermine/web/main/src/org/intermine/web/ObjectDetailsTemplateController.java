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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.results.DisplayObject;
import org.intermine.web.results.InlineTemplateTable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for an inline table created by running a template on an object details page.
 * @author Kim Rutherford
 */
public class ObjectDetailsTemplateController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ObjectDetailsTemplateController.class);

    /**
     * @see TilesAction#execute(ComponentContext context, ActionMapping mapping, ActionForm form,
     *                          HttpServletRequest request, HttpServletResponse response) 
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);

        TemplateQuery  templateQuery = (TemplateQuery) context.getAttribute("templateQuery");
        DisplayObject displayObject = (DisplayObject) context.getAttribute("displayObject");
        String templateName = templateQuery.getName();
        
        TemplateQuery template =
            TemplateHelper.findTemplate(session, templateName, TemplateHelper.ALL_TEMPLATE);

        if (template == null) {
            throw new IllegalStateException("Could not find template \"" 
                                            + templateName + "\"");
        }

        TemplateForm templateForm = new TemplateForm();
        int unconstrainedCount = 
            fillTemplateForm(template, displayObject.getObject(), templateForm, os.getModel());
        if (unconstrainedCount == 0) {
            templateForm.parseAttributeValues(template, session, new ActionErrors(), false);
            PathQuery pathQuery = TemplateHelper.templateFormToQuery(templateForm, template);
            try {
                Query query = MainHelper.makeQuery(pathQuery, Collections.EMPTY_MAP);
                Results results = os.execute(query);        

                List columnNames = new ArrayList(pathQuery.getView());
                InlineTemplateTable itt =
                    new InlineTemplateTable(results, columnNames, webProperties);
                List viewNodes = pathQuery.getView();

                Boolean viewNodesAreAttributes = Boolean.TRUE;
                Iterator viewIter = viewNodes.iterator();
                while (viewIter.hasNext()) {
                    String path = (String) viewIter.next();
                    String className = MainHelper.getTypeForPath(path, pathQuery);
                    if (className.indexOf(".") == -1) {
                        // a primative like "int"
                    } else {
                        Class nodeClass = Class.forName(className);

                        if (InterMineObject.class.isAssignableFrom(nodeClass)) {
                            viewNodesAreAttributes = Boolean.FALSE;
                            break;
                        }
                    }
                }
                context.putAttribute("table", itt);
                context.putAttribute("viewNodesAreAttributes", viewNodesAreAttributes);
            } catch (IllegalArgumentException e) {
                // probably a template is out of date
                LOG.error("error while getting inline template information", e);
            }
        }


        context.putAttribute("unconstrainedCount", new Integer(unconstrainedCount));

        return null;
    }

    private int fillTemplateForm(TemplateQuery template, InterMineObject object,
                                 TemplateForm templateForm, Model model) {
        List constraints = template.getAllConstraints();
        int unmatchedConstraintCount = constraints.size();
        String equalsString = ConstraintOp.EQUALS.getIndex().toString();

        for (int constraintIndex = 0; constraintIndex < constraints.size(); constraintIndex++) {
            Constraint c = (Constraint) constraints.get(constraintIndex);

            String constraintIdentifier = c.getIdentifier();
            String[] bits = constraintIdentifier.split("\\.");
            
            if (bits.length == 2) {
                String className = model.getPackageName() + "." + bits[0];
                String fieldName = bits[1];
                
                try {
                    Class testClass = Class.forName(className);

                    if (testClass.isInstance(object)) {
                        ClassDescriptor cd = model.getClassDescriptorByName(className);
                        if (cd.getFieldDescriptorByName(fieldName) != null) {
                            Object fieldValue = TypeUtil.getFieldValue(object, fieldName);
                            
                            if (fieldValue == null) {
                                // this field is not a good constraint value
                                continue;
                            }
                            
                            unmatchedConstraintCount--;

                            templateForm.setAttributeOps("" + (constraintIndex + 1), equalsString);
                            templateForm.setAttributeValues("" + (constraintIndex + 1), fieldValue);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error(e);
                } catch (IllegalAccessException e) {
                    LOG.error(e);
                }                
            }
        }

        return unmatchedConstraintCount;
    }
}