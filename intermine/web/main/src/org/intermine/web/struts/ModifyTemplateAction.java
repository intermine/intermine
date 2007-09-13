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

import java.util.Map;

import org.intermine.util.XmlUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateQuery;

import java.io.PrintStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Action that results from a button press on the user profile page.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ModifyTemplateAction extends InterMineAction
{
    /**
     * Forward to the correct method based on the button pressed.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        
        
        ModifyTemplateForm mtf = (ModifyTemplateForm) form;
        ActionErrors errors = mtf.validate(mapping, request);
        if (errors == null || errors.isEmpty()) {  
            if (request.getParameter("delete") != null) {
                errors = delete(mapping, form, request, response);
            } else if (request.getParameter("export") != null || mtf.getTemplateButton() != null) {
                export(mapping, form, request, response);
            } 
        }
        saveErrors(request, (ActionMessages) errors);
        return getReturn(mtf.getPageName(), mapping);
    }

    /**
     * Delete some templates.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return errors The errors, if any, encountered whilst attempting to delete templates
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionErrors delete(@SuppressWarnings("unused") ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyTemplateForm mqf = (ModifyTemplateForm) form;
        ActionErrors errors = new ActionErrors();
        try {
            profile.disableSaving();
            for (int i = 0; i < mqf.getSelected().length; i++) {
                String template = mqf.getSelected()[i];
                // if this template is not one of theirs
                if (profile.getTemplate(template) == null) {                    
                    errors.add(ActionMessages.GLOBAL_MESSAGE,
                               new ActionMessage("errors.modifyTemplate.delete"));
                }
                profile.deleteTemplate(template);
            }
        } finally {
            profile.enableSaving();
        }
        
        if (profile.getUsername() != null
            && profile.getUsername()
            .equals(servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT))) {
            SearchRepository tr = SearchRepository.getGlobalSearchRepository(servletContext);
            tr.globalChange(TagTypes.TEMPLATE);
        }
        return errors;
    }

    /**
     * Export the selected templates.
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public void export(@SuppressWarnings("unused") ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyTemplateForm mqf = (ModifyTemplateForm) form;
        ServletContext servletContext = session.getServletContext();
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=template-queries.xml");

        PrintStream out = new PrintStream(response.getOutputStream());
        out.println("<template-list>");
        Map myTemplates = profile.getSavedTemplates();
        Map publicTemplates
                = SessionMethods.getSuperUserProfile(servletContext).getSavedTemplates();
        for (int i = 0; i < mqf.getSelected().length; i++) {
            String name = mqf.getSelected()[i];
            String xml = null;
            
            if (publicTemplates.get(name) != null) {
                xml = ((TemplateQuery) publicTemplates.get(name)).toXml();
            } else if (myTemplates.get(name) != null) {
                xml = ((TemplateQuery) myTemplates.get(name)).toXml();
            }
            if (xml != null) { 
                xml = XmlUtil.indentXmlSimple(xml);
                out.println(xml);
            }
        }
        out.println("</template-list>");
        out.flush();
    }

   
    private ActionForward getReturn(String pageName, ActionMapping mapping) {
        if (pageName != null && pageName.equals("MyMine")) {
            return mapping.findForward("mymine");
        } else {
            return mapping.findForward("templates");
        }
    }
}
