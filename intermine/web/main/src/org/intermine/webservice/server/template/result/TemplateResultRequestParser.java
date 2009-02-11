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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.webservice.server.CodeTranslator;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.query.result.QueryResultRequestParser;
import org.intermine.webservice.server.query.result.WebServiceRequestParser;

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
    
    private static Logger logger = Logger.getLogger(TemplateResultRequestParser.class);
    
    private static final String OPERATION_PARAMETER = "op";
    
    private static final String EXTRA_PARAMETER = "extra";
    
    private static final String VALUE_PARAMETER = "value";

    private static final String ID_PARAMETER = "cons";

    private static final String CODE_PARAMETER = "code";
    
    
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
        input.setConstraints(parseConstraints(request));
        input.setLayout(request.getParameter(QueryResultRequestParser.LAYOUT_PARAMETER));
    }

    private Map<String, List<ConstraintLoad>> parseConstraints(HttpServletRequest request) {
        // Maximum of constraints is 50, it should be enough  
        logger.debug("request: " + request.getQueryString());
        Map<String, List<ConstraintLoad>> ret = new HashMap<String, List<ConstraintLoad>>();
        for (int i = 0; i < 50; i++) {
            
            String idParameter = ID_PARAMETER + i;
            String id = request.getParameter(idParameter);
            
            String opParameter = OPERATION_PARAMETER + i;
            String opString = request.getParameter(opParameter);
            ConstraintOp op = getConstraintOp(opParameter, opString);
            
            String valueParameter = VALUE_PARAMETER + i;
            String value = request.getParameter(valueParameter);            
            
            String extraParameter = EXTRA_PARAMETER + i;
            String extraValue = request.getParameter(extraParameter);

            String codeParameter = CODE_PARAMETER + i;
            String code = request.getParameter(codeParameter);
            
            if (opString != null && opString.length() > 0 && op == null) {
                throw new BadRequestException("invalid parameter: " + opParameter + " with value " 
                        + opString + " It must be valid operation code. Special characters "
                        + "must be encoded in request. See help for 'url encoding'.");
            }

            if (isPresent(op) || isPresent(value) || isPresent(id) || isPresent(extraValue)
                    || isPresent(code)) {
                if (!isPresent(id)) {
                    throw new BadRequestException("There is no path provided for constraint " + i  
                            + ".Missing parameter " + idParameter + ".");
                }
                if (!isPresent(op)) {
                    throw new BadRequestException("There is no operation provided for constraint " 
                            + i  + ".Missing parameter " + opParameter + ".");
                }
                if (!isPresent(value)) {
                    throw new BadRequestException("There is no value provided for constraint " + i  
                            + ".Missing parameter " + valueParameter + ".");
                }
                ConstraintLoad load = new ConstraintLoad(idParameter, id, code, op, value, 
                        extraValue);
                if (ret.get(id) == null) {
                    ret.put(id, new ArrayList<ConstraintLoad>());
                } 
                ret.get(id).add(load);
            }
        }
        return ret;
    }

    private ConstraintOp getConstraintOp(String parName, String parValue) {
        ConstraintOp ret = ConstraintOp.getConstraintOp(CodeTranslator.getCode(parValue));
        if (parValue != null && ret == null) {
            throw new BadRequestException("Invalid value of parameter: " + parName 
                    + "It must specify operation.");
        }
        return ret;
    }

    private boolean isPresent(String value) {
        return (value != null && value.length() > 0);
    }

    private boolean isPresent(ConstraintOp op) {
        return (op != null);
    }
    
    private String getRequiredStringParameter(String name) {
        String param = request.getParameter(name);
        if (param == null || param.equals("")) {
            throw new BadRequestException("Missing required parameter: " + name);
        } else {
            return param;
        }
    }    
}
