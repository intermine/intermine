package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateResultInput;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.query.QueryToListService;
import org.intermine.webservice.server.template.result.TemplateResultRequestParser;

/**
 * A service for turning the results of a template query into a list.
 * @author Alexis Kalderimis
 *
 */
public class TemplateToListService extends QueryToListService
{

    private static final String NEW_VIEW_PARAM = "path";

    private final TemplateManager templateManager;

    private static final Logger LOG = Logger.getLogger(TemplateToListService.class);

    /**
     * Constructor
     * @param im API settings bundle
     */
    public TemplateToListService(InterMineAPI im) {
        super(im);
        templateManager = im.getTemplateManager();
    }

    @Override
    protected PathQuery getQuery(HttpServletRequest request) {

        String newViewString = request.getParameter(NEW_VIEW_PARAM);
        if (StringUtils.isEmpty(newViewString)) {
            throw new BadRequestException("new view string is blank");
        }

        Profile profile = getPermission().getProfile();

        TemplateResultInput input = new TemplateResultRequestParser(request).getInput();
        TemplateQuery template = templateManager.getUserOrGlobalTemplate(profile, input.getName());

        Path newViewPath;
        try {
            newViewPath = new Path(template.getModel(), newViewString);
        } catch (PathException e) {
            throw new BadRequestException("The new view string is not a valid path", e);
        }

        if (newViewPath.endIsAttribute()) {
            if (!newViewString.endsWith(".id")) {
                throw new BadRequestException(
                        "The new view string refers to an attribute which is not the object id");
            }
        } else {
            newViewString += ".id";
        }


        Map<String, List<TemplateValue>> templateValues = TemplateHelper.getValuesFromInput(
                template, input);
        TemplateQuery populatedTemplate;
        try {
            populatedTemplate =
                TemplatePopulator.getPopulatedTemplate(template, templateValues);
        } catch (TemplatePopulatorException e) {
            e.printStackTrace();
            LOG.error("Error populating template: " + template.getName() + ". " + e);
            throw new BadRequestException("Error in applying constraint values to template: "
                    + template.getName(), e);
        }

        PathQuery pq = populatedTemplate.getQueryToExecute();

        pq.clearView();
        pq.addView(newViewString);

        return pq;
    }

}
