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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.util.TypeUtil;
import org.intermine.web.results.DisplayObject;

import org.apache.commons.lang.StringUtils;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Controller for the template list tile.
 * @author Thomas Riley
 */
public class TemplateListController extends TilesAction
{
    /**
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *                          HttpServletResponse) 
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String type = (String) context.getAttribute("type");
        String aspect = (String) context.getAttribute("aspect");
        DisplayObject object = (DisplayObject) context.getAttribute("displayObject");
        Set templates = null;
        
        if (StringUtils.equals("global", type)) {
            if (object == null) {
                templates = TemplateListHelper.getAspectTemplates(aspect, servletContext);
            } else {
                Map fieldExprs = new HashMap();
                templates = TemplateListHelper
                    .getAspectTemplateForClass(aspect, servletContext, object.getObject(),
                            fieldExprs);
                request.setAttribute("fieldExprMap", fieldExprs);
                request.setAttribute("templateCounts", calcTemplateCounts(templates, fieldExprs,
                        object, session));
            }
        } else if (StringUtils.equals("user", type)) {
            //templates = profile.get
        }
        
        request.setAttribute("templates", templates);
        
        return null;
    }
    
    private Map calcTemplateCounts(Set templates, Map fieldExprs, DisplayObject displayObject,
            HttpSession session) {
        Map newTemplateCounts = new TreeMap();
        InterMineObject object = displayObject.getObject();
        
        TEMPLATES:
            for (Iterator iter = templates.iterator(); iter.hasNext(); ) {
            TemplateQuery template = (TemplateQuery) iter.next();
            String templateName = template.getName();
            
            List templateConstraints = template.getAllConstraints();
            List editableConstraints = new ArrayList();
            List exprList = (List) fieldExprs.get(template);
            
            Iterator templateConstraintIter = templateConstraints.iterator();
    
            while (templateConstraintIter.hasNext()) {
                Constraint thisConstraint = (Constraint) templateConstraintIter.next();
    
                if (thisConstraint.isEditableInTemplate()) {
                    editableConstraints.add(thisConstraint);
                }
            }
    
            if (editableConstraints.size() != exprList.size()) {
                continue;
            }
    
            TemplateForm tf = new TemplateForm();
    
            for (int i = 0; i < editableConstraints.size(); i++) {
                Constraint thisConstraint = (Constraint) editableConstraints.get(i);
    
                String constraintIdentifier = thisConstraint.getIdentifier();
    
                int dotIndex = constraintIdentifier.indexOf('.');
    
                if (dotIndex == -1) {
                    throw new RuntimeException("constraint identifier is not in current "
                                               + "format");
                }
    
                String fieldName = constraintIdentifier.substring(dotIndex + 1);
    
                Object fieldValue;
                try {
                    fieldValue = TypeUtil.getFieldValue(object, fieldName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("cannot set field " + fieldName + " of object "
                                               + object.getId(), e);
                }
    
                if (exprList.contains(constraintIdentifier) && fieldValue != null) {
                    tf.setAttributeOps("" + (1 + i), ConstraintOp.EQUALS.getIndex().toString());
                    tf.setAttributeValues("" + (1 + i), fieldValue.toString());
                } else {
                    // unmatched constraints so bail out
                    continue TEMPLATES;
                }
            }
    
            tf.parseAttributeValues(template, session, new ActionErrors(), false);
    
            PathQuery pathQuery = TemplateHelper.templateFormToQuery(tf, template);
            Query query;
            try {
                query = MainHelper.makeQuery(pathQuery, Collections.EMPTY_MAP);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ServletContext servletContext = session.getServletContext();
            ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
            Results results;
            try {
                results = os.execute(query);
            } catch (ObjectStoreException e) {
                throw new RuntimeException("cannot find results of template query " 
                                           + templateName + " for object " + object.getId());
            }
    
            newTemplateCounts.put(templateName, new Integer(results.size()));
        }
        
        return newTemplateCounts;
    }
}