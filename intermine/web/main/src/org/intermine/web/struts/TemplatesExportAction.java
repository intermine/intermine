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

import java.io.PrintStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * Exports templates to XML.
 *
 * @author Thomas Riley
 */
public class TemplatesExportAction extends InterMineAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        
        String xml = null;

        if (name == null) {
            if (scope == null || scope.equals("user")) {
                xml = TemplateHelper.templateMapToXml(profile.getSavedTemplates());
            } else if (scope.equals("global")) {
                xml = TemplateHelper.templateMapToXml(SessionMethods
                        .getSuperUserProfile(servletContext).getSavedTemplates());
            } else {
                throw new IllegalArgumentException("Cannot export all templates for scope " 
                                                   + scope);
            }
        } else {
            TemplateQuery t = TemplateHelper.findTemplate(servletContext, session,
                    profile.getUsername(), name, scope);
            if (t != null) {
                xml = t.toXml();
            } else {
                throw new IllegalArgumentException("Cannot find template " + name + " in context "
                        + scope);
            }
        }
        xml = XmlUtil.indentXmlSimple(xml);
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=template-queries.xml");
        
        PrintStream out = new PrintStream(response.getOutputStream());
        out.print(xml);
        out.flush();
        return null;
    }
}
