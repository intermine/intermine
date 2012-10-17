package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.BadTemplateException;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.util.NameUtil;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.webservice.server.exceptions.BadRequestException;

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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);
        TemplatesImportForm tif = (TemplatesImportForm) form;
        int deleted = 0, imported = 0, renamed = 0;
        BagManager bagManager = im.getBagManager();
        Map<String, InterMineBag> allBags = bagManager.getBags(profile);

        Map<String, TemplateQuery> templates = TemplateHelper.xmlToTemplateMap(tif.getXml(),
                allBags, PathQuery.USERPROFILE_VERSION);

        try {
            profile.disableSaving();

            Set<String> templateNames = new HashSet<String>(profile.getSavedTemplates().keySet());
            if (tif.isOverwriting() && templateNames.size() > 0) {
                for (String templateName : templateNames) {
                    profile.deleteTemplate(templateName, im.getTrackerDelegate(),
                            tif.isDeleteTracks());
                    deleted++;
                }
            }
            boolean validConstraints = true;
            boolean validTemplate = true;
            for (TemplateQuery template : templates.values()) {
                ApiTemplate apiTemplate = new ApiTemplate(template);
                String templateName = apiTemplate.getName();

                String updatedName = NameUtil.validateName(profile.getSavedTemplates().keySet(),
                        templateName);
                if (!templateName.equals(updatedName)) {
                    apiTemplate = renameTemplate(updatedName, apiTemplate);
                }
                if (template.validateLookupConstraints() &&
                    !template.getEditableConstraints().isEmpty()) {
                    try {
                        profile.saveTemplate(apiTemplate.getName(), apiTemplate);
                    } catch (BadTemplateException bte) {
                       validTemplate = false;
                       continue;
                    }
                    imported++;
                } else {
                    validConstraints = false;
                }
            }

            recordMessage(new ActionMessage("importTemplates.done", new Integer(deleted),
                        new Integer(imported), new Integer(renamed)), request);
            if (!validConstraints) {
                recordError(new ActionMessage("importTemplates.error.noneditablelookup"), request);
            }
            if (!validTemplate) {
                recordError(new ActionMessage("importTemplates.error.invalidname"), request);
            }

            return new ForwardParameters(mapping.findForward("mymine"))
                .addParameter("subtab", "templates").forward();
        } finally {
            profile.enableSaving();
        }
    }

    // clone the template and set the new special-character-free name
    private ApiTemplate renameTemplate(String newName, ApiTemplate template) {
        ApiTemplate newTemplate = template.clone();
        newTemplate.setName(newName);
        return newTemplate;
    }
}
