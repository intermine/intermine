package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplateHelper;
import org.intermine.api.template.TemplateManager;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.util.XmlUtil;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Exports templates to XML.
 *
 * @author Thomas Riley
 */
public class TemplatesExportAction extends TemplateAction
{
    protected static final Logger LOG = Logger.getLogger(TemplatesExportAction.class);

    /**
     * {@inheritDoc}
     * @param mapping not used
     * @param form not used
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Profile profile = SessionMethods.getProfile(session);
        String name = request.getParameter("name");
        String scope = request.getParameter("scope");
        String originalTemplate = request.getParameter("originalTemplate");

        String xml = null;

        TemplateManager templateManager = im.getTemplateManager();
        if (name == null) {
            if (scope == null || scope.equals(Scope.USER)) {
                xml = TemplateHelper.apiTemplateMapToXml(profile.getSavedTemplates(),
                        PathQuery.USERPROFILE_VERSION);
            } else if (scope.equals(Scope.GLOBAL)) {
                xml = TemplateHelper.apiTemplateMapToXml(templateManager.getGlobalTemplates(),
                        PathQuery.USERPROFILE_VERSION);
            } else {
                throw new IllegalArgumentException("Cannot export all templates for scope "
                                                   + scope);
            }
        } else {
            TemplateQuery template = (originalTemplate != null)
                                     ? templateManager.getTemplate(profile, name, scope)
                                     : (TemplateQuery) SessionMethods.getQuery(session);
            if (template != null) {
                xml = template.toXml(PathQuery.USERPROFILE_VERSION);
            } else {
//                throw new IllegalArgumentException("Cannot find template " + name + " in context "
//                        + scope);
                recordError(new ActionMessage("errors.template.missing", name), request);
                return mapping.findForward("mymine");
            }
        }
        xml = XmlUtil.indentXmlSimple(xml);

        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().write(xml);

        return null;
    }
}
