package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQueryUtil;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.web.struts.TemplateAction;
import org.intermine.web.struts.TemplateForm;
import org.intermine.web.util.URLGenerator;
import org.intermine.webservice.server.core.TemplateManager;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;
import org.intermine.webservice.server.query.result.QueryResultService;

/**
 * Web service that returns results of public template constrained with values in request.
 * All constraints operations and values that are in template must be specified in request.
 * @author Jakub Kulaviak
 */
public class TemplateResultService extends QueryResultService
{

    /**
     * {@inheritDoc}}
     */
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) {

        TemplateResultInput input = getInput();
        TemplateQuery template = new TemplateManager(request).getGlobalTemplate(input.getName());
        if (template == null) {
            throw new ResourceNotFoundException("public template with name '" + input.getName()
                    + "' doesn't exist.");
        }
        template = new TemplateConfigurator().getConfiguredTemplate(template,
                                                                    input.getConstraints());
        if (template.getPathQuery().isValid()) {
            runPathQuery(template, input.getStart(), input.getMaxCount(), template.getTitle(),
                    template.getDescription(), input, getMineLinkURL(request, template, input),
                    input.getLayout());
        } else {
            String msg = "Required data source (template) is outdated and is in conflict "
                + "with model: "
                + PathQueryUtil.getProblemsSummary(template.getPathQuery().getProblems());
            throw new BadRequestException(msg);
        }
    }

    private TemplateResultInput getInput() {
        return new TemplateResultRequestParser(request).getInput();
    }

    private String getMineLinkURL(HttpServletRequest request, TemplateQuery template, 
            TemplateResultInput input) {
        String ret = new URLGenerator(request).getBaseURL();
        ret += "/" + TemplateAction.TEMPLATE_ACTION_PATH;
        ret += "?" + getQueryString(request, template, input);
        ret += "&" + TemplateAction.SKIP_BUILDER_PARAMETER + "&" + TemplateForm.TYPE_PARAMETER
            + "=" + TemplateHelper.ALL_TEMPLATE;
        return ret;
    }

    private String getQueryString(HttpServletRequest request, TemplateQuery template,
            TemplateResultInput input) {
        String ret = "";
        ret += TemplateForm.NAME_PARAMETER + "=" + en(input.getName()) + "&";
        int i = 1;
        for (PathNode node : template.getEditableNodes()) {
            for (Constraint cons : template.getEditableConstraints(node)) {
                ConstraintLoad load = getCorrespondingLoad(node.getPathString(), cons, input);
                ret += constraintToString(load, i);
                i++;
            }
        }
        return ret;
    }

    private ConstraintLoad getCorrespondingLoad(String path, Constraint cons,
            TemplateResultInput input) {
        List<ConstraintLoad> loads = input.getConstraints().get(path);
        if (loads.size() == 1) {
            return loads.get(0);
        } else {
            for (ConstraintLoad load : loads) {
                if (load.getCode().equals(cons.getCode())) {
                    return load;
                }
            }
        }
        throw new BadRequestException("Parameters for constraint with path " + path 
                + " and code '" + cons.getCode() + "' were not probably provided.");
    }

    private String constraintToString(ConstraintLoad load, int index) {
        String ret = "";
        
        ret += en("attributeOps(" + index + ")") + "=";
        ret += en(load.getConstraintOp().getIndex().toString()) + "&";
        
        ret += en("attributeValues(" + index + ")") + "=";
        ret += en(load.getValue()) + "&";
        
        if (load.getExtraValue() != null) {
            ret += en("extraValues(" + index + ")") + "=" 
                + en(load.getExtraValue()) + "&";
        }
        return ret;
    }

    private String en(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}