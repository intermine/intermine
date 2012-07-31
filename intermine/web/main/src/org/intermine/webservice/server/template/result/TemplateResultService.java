package org.intermine.webservice.server.template.result;

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

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.search.Scope;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.web.logic.template.ConstraintInput;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateResultInput;
import org.intermine.web.struts.TemplateAction;
import org.intermine.web.util.URLGenerator;
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
                    "There is no public template called '" + input.getName() + "' in this mine.");
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
        if (formatIsJsonObj()) {
            List<String> newView = PathQueryBuilderForJSONObj.getAlteredViews(populatedTemplate);
            populatedTemplate.clearView();
            populatedTemplate.addViews(newView);
        }
        setHeaderAttributes(populatedTemplate, input.getStart(), input.getMaxCount());
        if (populatedTemplate.isValid()) {
            runPathQuery(populatedTemplate, input.getStart().intValue(),
                    input.getMaxCount().intValue(),  populatedTemplate.getTitle(),
                    populatedTemplate.getDescription(), input,
                    getMineLinkURL(populatedTemplate, input), input.getLayout());
        } else {
            String msg = "Required data source (template) is outdated and is in conflict "
                + "with model: " + populatedTemplate.verifyQuery();
            throw new BadRequestException(msg);
        }
    }

    private TemplateResultInput getInput() {
        return new TemplateResultRequestParser(request).getInput();
    }

    private String getMineLinkURL(TemplateQuery template, TemplateResultInput input) {
        String ret = new URLGenerator(request).getBaseURL();
        ret += "/" + TemplateAction.TEMPLATE_ACTION_PATH;
        ret += "?" + getQueryString(template, input);
        ret += "&" + TemplateAction.SKIP_BUILDER_PARAMETER + "&"
            + TemplateResultService.TYPE_PARAMETER + "=" + Scope.ALL;
        return ret;
    }

    private String getQueryString(TemplateQuery template, TemplateResultInput input) {
        String ret = "";
        ret += TemplateResultService.NAME_PARAMETER + "=" + encode(input.getName()) + "&";
        int i = 1;
        for (PathConstraint con : template.getEditableConstraints()) {
            ret += constraintToString(getCorrespondingInput(template, con, input), i);
            i++;
        }
        return ret;
    }

    @Override
    protected String getLinkPath(PathQuery pq, String format) {
        if (!(pq instanceof TemplateQuery)) {
            throw new IllegalArgumentException(
                    "The template results service only handles "
                    + "TemplateQuerys, I got: " + pq.getClass());
        }
        TemplateQuery template = (TemplateQuery) pq;
        TemplateResultLinkGenerator linkGen = new TemplateResultLinkGenerator();
        return linkGen.getLinkPath(template, format);
    }

    @Override
    protected String getMineResultsLinkPath(PathQuery pq) {
        if (!(pq instanceof TemplateQuery)) {
            throw new IllegalArgumentException(
                    "The template results service only handles "
                    + "TemplateQuerys, I got: " + pq.getClass());
        }
        TemplateQuery template = (TemplateQuery) pq;
        TemplateResultLinkGenerator linkGen = new TemplateResultLinkGenerator();
        return linkGen.getMineResultsPath(template, false);
    }

    private ConstraintInput getCorrespondingInput(TemplateQuery template, PathConstraint con,
            TemplateResultInput input) {
        List<ConstraintInput> conInputs = input.getConstraints().get(con.getPath());
        String code = template.getConstraints().get(con);
        if (conInputs != null) {
            if (conInputs.size() == 1) {
                return conInputs.get(0);
            } else {
                for (ConstraintInput conInput : conInputs) {
                    if (conInput.getCode().equals(code)) {
                        return conInput;
                    }
                }
            }
        }
        return null;
    }

    private String constraintToString(ConstraintInput input, int index) {
        String ret = "";

        if (input != null) {
            ret += encode("attributeOps(" + index + ")") + "=";
            ret += encode(input.getConstraintOp().getIndex().toString()) + "&";

            ret += encode("attributeValues(" + index + ")") + "=";
            ret += encode(input.getValue()) + "&";

            if (input.getExtraValue() != null) {
                ret += encode("extraValues(" + index + ")") + "="
                    + encode(input.getExtraValue()) + "&";
            }
        }
        return ret;
    }

}
