package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.search.Scope;
import org.intermine.api.template.SwitchOffAbility;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplatePopulator;
import org.intermine.api.template.TemplatePopulatorException;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.template.TemplateValue;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.PortalHelper;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.TemplateAction;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.WebService;
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
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) {
        TemplateManager templateManager = this.im.getTemplateManager();
        TemplateResultInput input = getInput();
        TemplateQuery template;
        if (isAuthenticated()) {
            Profile profile = SessionMethods.getProfile(request.getSession());
            template = templateManager.getUserOrGlobalTemplate(profile, input.getName());
        } else {
            template = templateManager.getGlobalTemplate(input.getName());
        }
        if (template == null) {
            throw new ResourceNotFoundException("public template with name '" + input.getName()
                    + "' doesn't exist.");
        }

        Map<String, List<TemplateValue>> templateValues = getValuesFromInput(template, input);
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
        if (getFormat() == WebService.JSON_OBJ_FORMAT) {
            List<String> newView = PathQueryBuilderForJSONObj.getAlteredViews(populatedTemplate);
            populatedTemplate.clearView();
            populatedTemplate.addViews(newView);
        }
        setHeaderAttributes(populatedTemplate, input.getStart(), input.getMaxCount(),
                    input.getName());
        if (populatedTemplate.isValid()) {
            runPathQuery(populatedTemplate, input.getStart(), input.getMaxCount(),
                    populatedTemplate.getTitle(), populatedTemplate.getDescription(), input,
                    getMineLinkURL(request, populatedTemplate, input), input.getLayout());
        } else {
            String msg = "Required data source (template) is outdated and is in conflict "
                + "with model: " + populatedTemplate.verifyQuery();
            throw new BadRequestException(msg);
        }
    }

    private Map<String, List<TemplateValue>> getValuesFromInput(TemplateQuery template,
            TemplateResultInput input) {
        Map<String, List<TemplateValue>> values = new HashMap<String, List<TemplateValue>>();
        for (String path : template.getEditablePaths()) {
            List<PathConstraint> constraintsForPath = template.getEditableConstraints(path);
            List<ConstraintInput> inputsForPath = new ArrayList<ConstraintInput>();
            if (input.getConstraints().get(path) != null) {
                inputsForPath.addAll(input.getConstraints().get(path));
            }

            // too many inputs for path
            if (constraintsForPath.size() < inputsForPath.size()) {
                throw new BadRequestException("There were more constraints specified "
                        + " in the request than there are editable constraints for path "
                        + path + ".");
            }

            if (constraintsForPath.size() == 1) {
                // one constraint and at most one input
                PathConstraint con = constraintsForPath.get(0);
                ConstraintInput conInput = null;
                if (!inputsForPath.isEmpty()) {
                    conInput = inputsForPath.get(0);
                }
                checkAndAddValue(values, template, con, conInput, null);
            } else {
                // more than one constraint so we need to look at codes
                for (PathConstraint con : constraintsForPath) {
                    ConstraintInput foundConInput = null;
                    String code = template.getConstraints().get(con);
                    for (ConstraintInput conInput : inputsForPath) {
                        if (StringUtils.isBlank(conInput.getCode())) {
                            throw new BadRequestException("There are multiple editable constraints"
                                    + "for path " + path + " but codes weren't set.  If there is"
                                    + " more than one constraint on a path you need to specify the"
                                    + " corresponding constraint codes.");
                        }
                        if (conInput.getCode().equals(code)) {
                            if (foundConInput != null) {
                                throw new BadRequestException("There was more than one constraint"
                                        + " specified with code: " + conInput.getCode()
                                        + " in the request for path: " + path + "  You should only"
                                        + " provide one value per code.");
                            }
                            foundConInput = conInput;
                        }
                    }
                    // foundConInput may be null but that's ok if the constraint is optional
                    checkAndAddValue(values, template, con, foundConInput, code);
                }
            }
        }
        return values;
    }

    private void checkAndAddValue(Map<String, List<TemplateValue>> values, TemplateQuery template,
            PathConstraint con, ConstraintInput conInput, String code) {
        if (conInput != null) {
            if (template.isRequired(con)) {
                addToValuesMap(values, createTemplateValue(con, conInput, SwitchOffAbility.LOCKED));
            } else {
                addToValuesMap(values, createTemplateValue(con, conInput, SwitchOffAbility.ON));
            }
        } else if (template.isRequired(con)) {
            throw new BadRequestException("There isn't a specified constraint value "
                    + "and operation for path " + con.getPath()
                    + ((code != null) ? " and code " + code : "")
                    + " in the request; this constraint is required.");
        } else {
            // no value was provided but the constraint was optional so we can do nothing
        }
    }

    private void addToValuesMap(Map<String, List<TemplateValue>> valMap, TemplateValue newValue) {
        String path = newValue.getConstraint().getPath();
        List<TemplateValue> values = valMap.get(path);
        if (values == null) {
            values = new ArrayList<TemplateValue>();
            valMap.put(path, values);
        }
        values.add(newValue);
    }

    private TemplateValue createTemplateValue(PathConstraint con, ConstraintInput input,
            SwitchOffAbility switchOffAbility) {
        TemplateValue value;
        if (con instanceof PathConstraintLookup) {
            value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                    TemplateValue.ValueType.SIMPLE_VALUE, input.getExtraValue(), switchOffAbility);
        } else if (con instanceof PathConstraintBag) {
            value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                    TemplateValue.ValueType.BAG_VALUE, switchOffAbility);
        } else {
            value = new TemplateValue(con, input.getConstraintOp(), input.getValue(),
                    TemplateValue.ValueType.SIMPLE_VALUE, switchOffAbility);
        }
        return value;
    }

//    private TemplateValue createDummyTemplateValue(PathConstraint con) {
//        TemplateValue value;
//        value = new TemplateValue(con, con.getOp(), "dummy",
//                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.OFF);
//        return value;
//    }

    private TemplateResultInput getInput() {
        return new TemplateResultRequestParser(request).getInput();
    }

    private String getMineLinkURL(HttpServletRequest request, TemplateQuery template,
            TemplateResultInput input) {
        String ret = new URLGenerator(request).getBaseURL();
        ret += "/" + TemplateAction.TEMPLATE_ACTION_PATH;
        ret += "?" + getQueryString(request, template, input);
        ret += "&" + TemplateAction.SKIP_BUILDER_PARAMETER + "&"
            + TemplateResultService.TYPE_PARAMETER + "=" + Scope.ALL;
        return ret;
    }

    private String getQueryString(HttpServletRequest request,
            TemplateQuery template, TemplateResultInput input) {
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
    protected String getExportLink(PathQuery pq, String format) {
        if (!(pq instanceof TemplateQuery)) {
            throw new IllegalArgumentException(
                    "The template results service only handles "
                    + "TemplateQuerys, I got: " + pq.getClass());
        }
        TemplateQuery template = (TemplateQuery) pq;
        String baseUrl = PortalHelper.getBaseUrl(request);
        TemplateResultLinkGenerator linkGen = new TemplateResultLinkGenerator();
        String xml = pq.toXml(PathQuery.USERPROFILE_VERSION);
        return linkGen.getTabLink(baseUrl, template);
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
