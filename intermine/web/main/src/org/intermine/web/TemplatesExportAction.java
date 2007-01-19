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

import java.io.PrintStream;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.intermine.util.XmlUtil;

import org.apache.commons.lang.StringUtils;

/**
 * Exports templates to XML.
 *
 * @author Thomas Riley
 */
public class TemplatesExportAction extends InterMineAction
{
    /**
     * @see InterMineAction#execute(ActionMapping, ActionForm, HttpServletRequest,
     *  HttpServletResponse)
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String type = request.getParameter("type");
        Map templates = null;
        
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition ", "inline; filename=template-queries.xml");
        
        if (type == null || type.equals("user")) {
            templates = profile.getSavedTemplates();
        } else if (type.equals("global")) {
            templates = SessionMethods.getSuperUserProfile(servletContext).getSavedTemplates();
        } else {
            return null;
        }
        
        String name = request.getParameter("name");
        
        PrintStream out = new PrintStream(response.getOutputStream());
        String xml = null;
        
        if (StringUtils.isNotEmpty(name)) {
            xml = ((TemplateQuery) templates.get(name)).toXml();
        } else {
            xml = TemplateHelper.templateMapToXml(templates);
        }
        xml = XmlUtil.indentXmlSimple(xml);
        out.print(xml);
        out.flush();
        return null;
    }
}
