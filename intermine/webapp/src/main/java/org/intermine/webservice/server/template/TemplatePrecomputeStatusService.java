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
import org.intermine.api.template.TemplatePrecomputeHelper;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.template.TemplateQuery;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.core.JSONService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ServiceForbiddenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A service which returns if a template has been precomputed or not
 * @author Daniela Butano
 *
 */
public class TemplatePrecomputeStatusService extends JSONService
{
    /**
     * Constructor.
     * @param im A reference to the API configuration and settings bundle.
     */
    public TemplatePrecomputeStatusService(InterMineAPI im) {
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
        Profile currentProfile = getPermission().getProfile();
        Map<String, ApiTemplate> templates = currentProfile.getSavedTemplates();
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) im.getObjectStore();
        Map<String, Boolean> precomputedTemplateMap = new HashMap<>();
        String templateName = getOptionalParameter("name");

        if (!StringUtils.isEmpty(templateName)) {
            TemplateQuery template = templates.get(templateName);
            if (template == null) {
                throw new BadRequestException("The template " + templateName + " doesn't exist");
            } else if (template.isValid()) {
                Query query = TemplatePrecomputeHelper
                        .getPrecomputeQuery(template, new ArrayList<QuerySelectable>(), null);
                precomputedTemplateMap.put(template.getName(),
                        os.isPrecomputed(query, "template"));
            }
        } else {
            for (ApiTemplate template : templates.values()) {
                if (template.isValid()) {
                    Query query = TemplatePrecomputeHelper
                            .getPrecomputeQuery(template, new ArrayList<QuerySelectable>(), null);
                    precomputedTemplateMap.put(template.getName(), os
                            .isPrecomputed(query, "template"));
                }
            }
        }
        addResultItem(precomputedTemplateMap, false);
    }

    @Override
    public String getResultsKey() {
        return "templates";
    }
}
