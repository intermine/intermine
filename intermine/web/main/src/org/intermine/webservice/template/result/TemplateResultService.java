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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.WebServiceException;
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
        try {
            template = new TemplateConfigurator().getConfiguredTemplate(template, 
                    input.getConstraints(), getLocale(request));
        } catch (WebServiceException e) {
            output.addError(e.getMessage());
            return;
        }
        runPathQuery(template, input.getStart() - 1, input.getMaxCount(), 
                input.isComputeTotalCount(), template.getTitle(), 
                template.getDescription(), input);
    }

    private Locale getLocale(HttpServletRequest request) {
        return (Locale) request.getSession().getAttribute(Globals.LOCALE_KEY);
    }
    
    private TemplateResultInput getInput() {
        return new TemplateResultRequestParser(request).getInput();
    }
}