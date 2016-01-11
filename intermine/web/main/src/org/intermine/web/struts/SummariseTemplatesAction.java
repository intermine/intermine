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

import java.util.Map;

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
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.api.template.TemplateManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to summarise all templates.
 *
 * @author Matthew Wakeling
 */
public class SummariseTemplatesAction extends InterMineAction
{
    protected static final Logger LOG = Logger.getLogger(CreateTemplateAction.class);

    /**
     * Summarises every public template, and then forwards to the mymine template page.
     * This comes from a link on the my mine page for the super user only.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Profile profile = SessionMethods.getProfile(session);
        final TemplateSummariser summariser = im.getTemplateSummariser();

        TemplateManager templateManager = new TemplateManager(profile,
                im.getObjectStore().getModel());
        Map<String, ApiTemplate> templates = templateManager.getGlobalTemplates();

        for (Map.Entry<String, ApiTemplate> entry : templates.entrySet()) {
            //String templateName = entry.getKey();
            ApiTemplate template = entry.getValue();
            try {
                summariser.summarise(template);
            } catch (ObjectStoreException e) {
                recordError(new ActionMessage("errors.query.objectstoreerror"), request, e, LOG);
            }
        }

        return new ForwardParameters(mapping.findForward("mymine"))
            .addParameter("subtab", "templates").forward();
    }
}
