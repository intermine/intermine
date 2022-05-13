package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.template.ApiTemplate;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.template.TemplateQuery;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

import java.util.HashMap;
import java.util.Map;

/**
 * A service to enable templates to be precomputed.
 * @author Daniela Butano
 *
 */
public class TemplatePrecomputeService extends JSONService
{
    private static final Logger LOG = Logger.getLogger(TemplatePrecomputeService.class);
    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public TemplatePrecomputeService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void validateState() {
        if (!isAuthenticated() || !getPermission().getProfile().isSuperuser()) {
            throw new ServiceForbiddenException("This request is not authenticated.");
        }
    }

    @Override
    protected boolean canServe(Format format) {
        switch (format) {
            case TEXT:
            case JSON:
            case HTML:
            case XML:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void execute() throws Exception {
        String templateName = getRequiredParameter("name");
        Profile currentProfile = getPermission().getProfile();
        Map<String, ApiTemplate> templates = currentProfile.getSavedTemplates();
        TemplateQuery template = templates.get(templateName);
        Map<String, Boolean> precomputedTemplateMap = new HashMap<>();
        if (template == null) {
            throw new BadRequestException("The template " + templateName + " doesn't exist");
        }
        if (!template.isValid()) {
            throw new BadRequestException("The template " + templateName + " is not valid");
        }

        WebResultsExecutor executor = im.getWebResultsExecutor(currentProfile);
        try {
            executor.precomputeTemplate(template);
            precomputedTemplateMap.put(templateName, true);
        } catch (ObjectStoreException e) {
            LOG.error("Error while precomputing", e);
            precomputedTemplateMap.put(templateName, false);
        }
        addResultItem(precomputedTemplateMap, false);
    }

    @Override
    public String getResultsKey() {
        return "templates";
    }
}
