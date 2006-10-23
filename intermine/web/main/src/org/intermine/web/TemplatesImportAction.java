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

/**
 * Imports templates in XML format.
 *
 * @author Thomas Riley
 */
public class TemplatesImportAction extends InterMineAction
{
    /**
     * @see InterMineAction#execute(ActionMapping, ActionForm, HttpServletRequest,
     *               HttpServletResponse)
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
        int deleted = 0, imported = 0;
        
        templates = TemplateHelper.xmlToTemplateMap(tif.getXml());
        
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
            profile.saveTemplate(template.getName(), template);
            imported++;
        }

        TemplateRepository tr = TemplateRepository.getTemplateRepository(servletContext);
        tr.globalTemplatesChanged();
        //InitialiserPlugin.loadGlobalTemplateQueries(getServlet().getServletContext());
        
        recordMessage(new ActionMessage("importTemplates.done",
                                        new Integer(deleted), new Integer(imported)), request);
        
        return mapping.findForward("mymine");
    }
}
