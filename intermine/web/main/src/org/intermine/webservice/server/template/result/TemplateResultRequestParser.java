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

import javax.servlet.http.HttpServletRequest;

import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateResultInput;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.query.result.QueryResultRequestParser;

/**
 * Processes service request. Evaluates parameters and validates them and check if
 * its combination is valid.
 *
 * @author Jakub Kulaviak
 **/
public class TemplateResultRequestParser extends WebServiceRequestParser
{
    private static final String NAME_PARAMETER = "name";
    private HttpServletRequest request;

    /**
     * TemplateResultRequestProcessor constructor.
     * @param request request
     */
    public TemplateResultRequestParser(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns parsed parameters in parameter object - so this
     * values can be easily obtained from this object.
     * @return web service input
     */
    public TemplateResultInput getInput() {
        TemplateResultInput input = new TemplateResultInput();
        parseRequest(input);
        return input;
    }

    private void parseRequest(TemplateResultInput input) {
        super.parseRequest(request, input);
        input.setName(getRequiredStringParameter(NAME_PARAMETER));
        input.setConstraints(TemplateHelper.parseConstraints(request));
        input.setLayout(request.getParameter(QueryResultRequestParser.LAYOUT_PARAMETER));
    }

    private String getRequiredStringParameter(String name) {
        String param = request.getParameter(name);
        if (param == null || "".equals(param)) {
            throw new IllegalArgumentException("Missing required parameter: " + name);
        } else {
            return param;
        }
    }
}
