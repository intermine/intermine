package org.intermine.webservice.server.template.result;

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

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.web.logic.template.TemplateResultInput;
import org.intermine.web.logic.template.Templates;
import org.intermine.web.logic.template.Templates.TemplateValueParseException;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.query.result.PathQueryBuilderForJSONObj;
import org.intermine.webservice.server.query.result.QueryResultService;

/**
 * Web service that returns results of public template constrained with values in request.
 * All constraints operations and values that are in template must be specified in request.
 * @author Jakub Kulaviak
 */
public class TemplateResultService extends QueryResultService
{

    /** Name of type parameter **/
    public static final String TYPE_PARAMETER = "type";
    /** Name of name parameter **/
    public static final String NAME_PARAMETER = "name";

    private static final Logger LOG = Logger.getLogger(TemplateResultService.class);

    /**
     * Construct with an InterMineAPI.
     * @param im the InterMine API
     */
    public TemplateResultService(InterMineAPI im) {
        super(im);
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    protected void execute() {
        TemplateManager templateManager = this.im.getTemplateManager();
        TemplateResultInput input = getInput();
        TemplateQuery template;
        Profile profile = getPermission().getProfile();
        template = templateManager.getUserOrGlobalTemplate(profile, input.getName());
        if (template == null) {
            throw new ResourceNotFoundException(
                "You do not have access to a template called '"
                        + input.getName() + "' in this mine.");
        }

        Map<String, List<TemplateValue>> templateValues;
        try {
            templateValues = Templates.getValuesFromInput(
                    template, input);
        } catch (TemplateValueParseException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
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
        if (formatIsJsonObj()) {
            List<String> newView = PathQueryBuilderForJSONObj.getAlteredViews(populatedTemplate);
            populatedTemplate.clearView();
            populatedTemplate.addViews(newView);
        }
        setHeaderAttributes(populatedTemplate, input.getStart(), input.getLimit());
        if (populatedTemplate.isValid()) {
            runPathQuery(populatedTemplate, input.getStart(), input.getLimit());
        } else {
            String msg = "Required data source (template) is outdated and is in conflict "
                + "with model: " + populatedTemplate.verifyQuery();
            throw new BadRequestException(msg);
        }
    }

    private TemplateResultInput getInput() {
        return new TemplateResultRequestParser(request).getInput();
    }

}
