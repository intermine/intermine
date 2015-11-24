package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action that results from a button press on the user profile page.
 *
 * @author Mark Woodbridge
 * @author Thomas Riley
 */
public class ModifyTemplateAction extends InterMineAction
{
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(ModifyTemplateAction.class);

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
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
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
        if (request.getParameter("export") != null || mtf.getTemplateButton() != null) {
            return null;
        } else {
            return getReturn(mtf.getPageName(), mapping);
        }
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
    public ActionErrors delete(ActionMapping mapping,
            ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        InterMineAPI im = SessionMethods.getInterMineAPI(session);
//        ServletContext servletContext = session.getServletContext();
        Profile profile = SessionMethods.getProfile(session);
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

                profile.deleteTemplate(template, im.getTrackerDelegate(), true);
            }
        } finally {
            profile.enableSaving();
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
    public void export(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        ModifyTemplateForm mqf = (ModifyTemplateForm) form;
//        ServletContext servletContext = session.getServletContext();

        response.setContentType("text/plain; charset=utf-8");

        PrintStream out = new PrintStream(response.getOutputStream());

        out.println("<template-queries>");
        Map<?, ?> myTemplates = profile.getSavedTemplates();
        Map<?, ?> publicTemplates =
                im.getProfileManager().getSuperuserProfile().getSavedTemplates();
        for (int i = 0; i < mqf.getSelected().length; i++) {
            String name = mqf.getSelected()[i];
            String xml = null;

            if (publicTemplates.get(name) != null) {
                xml = ((TemplateQuery) publicTemplates.get(name)).toXml(PathQuery
                        .USERPROFILE_VERSION);
            } else if (myTemplates.get(name) != null) {
                xml = ((TemplateQuery) myTemplates.get(name)).toXml(PathQuery.USERPROFILE_VERSION);
            }
            if (xml != null) {
                xml = XmlUtil.indentXmlSimple(xml).trim();
                out.println(xml);
            }
        }
        out.println("</template-queries>");
        out.flush();
    }

    private ActionForward getReturn(String pageName, ActionMapping mapping) {
        if (pageName != null && "MyMine".equals(pageName)) {
            return mapping.findForward("mymine");
        }
        return mapping.findForward("templates");
    }
}
