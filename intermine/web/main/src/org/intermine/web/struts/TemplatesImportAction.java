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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.logic.template.TemplateRepository;

/**
 * Imports templates in XML format.
 *
 * @author Thomas Riley
 */
public class TemplatesImportAction extends InterMineAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        TemplatesImportForm tif = (TemplatesImportForm) form;

        Map templates = null;
        int deleted = 0, imported = 0, renamed = 0;
        
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        templates = TemplateHelper.xmlToTemplateMap(tif.getXml(), profile.getSavedBags(),
                                                    classKeys);
        
        if (tif.isOverwriting()
            && profile.getSavedTemplates().size() > 0) {
            Iterator iter = new HashSet(profile.getSavedTemplates().keySet()).iterator();
            while (iter.hasNext()) {
                profile.deleteTemplate((String) iter.next());
                deleted++;
            }
        }
       
        Iterator iter = templates.values().iterator();
        while (iter.hasNext()) {
            TemplateQuery template = (TemplateQuery) iter.next();
            
            String templateName = template.getName();
             if (!WebUtil.isValidName(templateName)) {  
                templateName = WebUtil.replaceSpecialChars(templateName);
                renamed++;
            }
            templateName = validateQueryName(templateName, profile);
            template = renameTemplate(templateName, template);
            profile.saveTemplate(templateName, template);
            imported++;
        }

        TemplateRepository tr = TemplateRepository.getTemplateRepository(servletContext);
        tr.globalTemplatesChanged();
        //InitialiserPlugin.loadGlobalTemplateQueries(getServlet().getServletContext());
        
        recordMessage(new ActionMessage("importTemplates.done",
                                        new Integer(deleted), 
                                        new Integer(imported), 
                                        new Integer(renamed)), 
                                        request);
        
        return mapping.findForward("mymine");
    }
    
    // rebuild the template, but with the new special-character-free name
    private TemplateQuery renameTemplate(String newName, TemplateQuery template) {
     
        TemplateQuery newTemplate = new TemplateQuery(newName, template.getTitle(), 
                                                      template.getDescription(), 
                                                      template.getComment(), 
                                                      template.getPathQuery(),
                                                      template.getKeywords());
        
        return newTemplate;
    }
    
    /**
     * Checks that the query name doesn't already exist and returns a numbered
     * name if it does.  
     * @param queryName the query name
     * @param profile the user profile
     * @return a validated name for the query
     */
    private String validateQueryName(String queryName, Profile profile) {
        String newQueryName = queryName;

        if (!WebUtil.isValidName(queryName)) {   
            newQueryName = WebUtil.replaceSpecialChars(newQueryName);
        }
        
        if (profile.getSavedTemplates().containsKey(newQueryName)) {
            int i = 1;
            while (true) {
                String testName = newQueryName + "_" + i;
                if (!profile.getSavedTemplates().containsKey(testName)) {
                    return testName;
                }
                i++;
            }
        } else {
            return newQueryName;
        }
    }
    
}
