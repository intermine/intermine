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

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateSummariser;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

import java.util.HashMap;
import java.util.Map;

/**
 * A service which returns if a template has been summirised or not.
 * @author Daniela Butano
 *
 */
public class TemplateSummariseStatusService extends JSONService
{
    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public TemplateSummariseStatusService(InterMineAPI im) {
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
        String templateName = getOptionalParameter("name");
        Profile currentProfile = getPermission().getProfile();
        Map<String, ApiTemplate> templates = currentProfile.getSavedTemplates();
        TemplateSummariser summariser = im.getTemplateSummariser();
        Map<String, Boolean> summarisedTemplateMap = new HashMap<>();

        if (!StringUtils.isEmpty(templateName)) {
            ApiTemplate template = templates.get(templateName);
            if (template == null) {
                throw new BadRequestException("The template " + templateName + " doesn't exist");
            } else if (template.isValid()) {
                summarisedTemplateMap.put(template.getName(), summariser.isSummarised(template));
            }
        } else {
            for (ApiTemplate template : templates.values()) {
                if (template.isValid()) {
                    summarisedTemplateMap.put(template.getName(),
                        summariser.isSummarised(template));
                }
            }
        }
        addResultItem(summarisedTemplateMap, false);
    }

    @Override
    public String getResultsKey() {
        return "templates";
    }
}
