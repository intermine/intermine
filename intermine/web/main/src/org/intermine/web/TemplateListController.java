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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.intermine.web.results.DisplayObject;
import org.intermine.web.results.InlineTemplateTable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
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
        ServletContext servletContext = session.getServletContext();
        Map newTemplateCounts = new TreeMap();
        String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();        
        Integer objectId = displayObject.getObject().getId();
                
        for (Iterator iter = templates.iterator(); iter.hasNext(); ) {
            TemplateQuery template = (TemplateQuery) iter.next();
            String templateName = template.getName();

            InlineTemplateTable itt =
                TemplateHelper.getInlineTemplateTable(servletContext, templateName, objectId, 
                                                      userName);
            
            if (itt == null) {
                // template has unconstrained fields so we can't inline it
                newTemplateCounts.put(templateName, null);
            } else {
                newTemplateCounts.put(templateName, new Integer(itt.getResultsSize()));
            }
        }
        
        return newTemplateCounts;
    }
}