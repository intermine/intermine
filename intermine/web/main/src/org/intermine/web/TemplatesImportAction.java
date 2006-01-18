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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.path.Path;
import org.intermine.web.config.FieldConfig;
import org.intermine.web.config.FieldConfigHelper;
import org.intermine.web.config.WebConfig;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.axis.utils.SessionUtils;
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
            if (((Boolean) session.getAttribute(Constants.IS_SUPERUSER)).booleanValue()) {
                makeAttributeView(template, servletContext);
            }
            profile.saveTemplate(template.getName(), template);
            imported++;
        }

        TemplateRepository tr = TemplateRepository.getTemplateRepository(servletContext);
        tr.globalTemplatesChanged();
        //InitialiserPlugin.loadGlobalTemplateQueries(getServlet().getServletContext());
        
        recordMessage(new ActionMessage("importTemplates.done",
                                        new Integer(deleted), new Integer(imported)), request);
        
        return mapping.findForward("history");
    }

    private void makeAttributeView(TemplateQuery template, ServletContext servletContext) {
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Model model = os.getModel();
        WebConfig wc = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        PathQuery pathQuery = template.getQuery();
        
        List newView = new ArrayList();
            
        Iterator viewIter = pathQuery.getView().iterator();
        while (viewIter.hasNext()) {
            String node = (String) viewIter.next();
            Path path;
            try {
                path = new Path(model, node);
            } catch (RuntimeException e) {
                // the path probably contains a sub-class constraint - just ignore it for now
                continue;
            }
            
            ClassDescriptor cd = path.getEndClassDescriptor();
            
            if (cd == null) {
                if (!newView.contains(path.toString())) {
                    newView.add(path.toString());
                }
            } else {
                List fieldConfigs = FieldConfigHelper.getClassFieldConfigs(wc, cd);
                
                Iterator fcIter = fieldConfigs.iterator();
                while (fcIter.hasNext()) {
                    FieldConfig fc = (FieldConfig) fcIter.next();
                    String expr = fc.getFieldExpr();
                    
                    String newViewPath = path + "." + expr;
                    if (!newView.contains(newViewPath)) {
                        newView.add(newViewPath);
                    }
                }

            }
        }                

        pathQuery.addAlternativeView(ATTRIBUTE_VIEW_NAME, newView);
    }
    
    /**
     * The name to use for the automatically created view for attributes.
     */
    public final static String ATTRIBUTE_VIEW_NAME = "Attributes of default view";
}
