package org.intermine.webservice.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.core.TemplateManager;
import org.intermine.webservice.query.result.QueryResultService;

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
        if (!validate(input)) {
            return;
        }
        
        TemplateQuery template = new TemplateManager(request).getQuery(input.getName());
        if (template == null) {
            output.addError("public template with name '" + input.getName() + "' doesn't exist.");
            return;
        }
        template = new TemplateConfigurator().getConfiguredTemplate(template, 
                input.getConstraints());
        runPathQuery(template, input.getStart() - 1, input.getMaxCount(), 
                input.isComputeTotalCount(), template.getTitle(), 
                template.getDescription(), input);
    }

    private TemplateResultInput getInput() {
        return new TemplateResultRequestParser(request).getInput();
    }
}