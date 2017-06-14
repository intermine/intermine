package org.intermine.webservice.server.template;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.web.logic.template.TemplateResultInput;
import org.intermine.web.logic.template.Templates;
import org.intermine.web.logic.template.Templates.TemplateValueParseException;
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

    /* ERROR MESSAGES */
    private static final String NOT_LISTABLE =
            "You cannot make lists from objects of type ";
    private static final String INVALID_VIEW =
            "The new view string is not a valid path";
    private static final String NOT_IM_OBJECT =
            "The new view string refers to an attribute which is not the object id";
    private static final String BAD_CONSTRAINT_VALUES =
            "Could not apply template constraint values. ";
    private static final String NEW_VIEW_PARAM = "path";

    private final TemplateManager templateManager;

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
            newViewPath = template.makePath(newViewString);
        } catch (PathException e) {
            throw new BadRequestException(INVALID_VIEW, e);
        }

        if (newViewPath.endIsAttribute()) {
            if (!newViewString.endsWith(".id")) {
                throw new BadRequestException(NOT_IM_OBJECT);
            }
        } else {
            try {
                newViewString = newViewPath.append("id").getNoConstraintsString();
            } catch (PathException e) {
                // FastPath objects don't have IDs, and can't be stored in lists.
                throw new BadRequestException(
                    NOT_LISTABLE + newViewPath.getLastClassDescriptor().getUnqualifiedName());
            }
        }

        Map<String, List<TemplateValue>> templateValues;
        try {
            templateValues = Templates.getValuesFromInput(template, input);
        } catch (TemplateValueParseException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
        TemplateQuery populatedTemplate;
        try {
            populatedTemplate = TemplatePopulator.getPopulatedTemplate(template, templateValues);
        } catch (TemplatePopulatorException e) {
            throw new BadRequestException(BAD_CONSTRAINT_VALUES + e.getMessage(), e);
        }

        PathQuery pq = populatedTemplate.getQueryToExecute();
        pq.clearView();
        pq.addView(newViewString);
        return pq;
    }
}
