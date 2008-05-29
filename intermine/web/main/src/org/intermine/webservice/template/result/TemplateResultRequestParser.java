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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.webservice.CodeTranslator;
import org.intermine.webservice.query.result.QueryResultRequestParser;
import org.intermine.webservice.query.result.WebServiceRequestParser;

/**
 * Processes service request. Evaluates parameters and validates them and check if 
 * its combination is valid. 
 * @author Jakub Kulaviak
 **/
public class TemplateResultRequestParser extends WebServiceRequestParser  
{
    private static final String NAME_PARAMETER = "name";
    
    private List<String> errors = new ArrayList<String>();
    
    private HttpServletRequest request;
    
    private static Logger logger = Logger.getLogger(TemplateResultRequestParser.class);
    
    private static final String OPERATION_PARAMETER = "op";
    
    private static final String EXTRA_PARAMETER = "extra";
    
    private static final String VALUE_PARAMETER = "value";
    
    
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
        input.setComputeTotalCount(parseComputeTotalCountParameter(request));
        input.setName(getRequiredStringParameter(NAME_PARAMETER));
        input.setConstraints(parseConstraints(request));
        input.setErrors(getErrors());
    }

    private boolean parseComputeTotalCountParameter(HttpServletRequest request) {
        String totalCount = request.getParameter(QueryResultRequestParser.
                COMPUTE_TOTAL_COUNT_PARAMETER);
        return totalCount != null;
    }

    private List<ConstraintLoad> parseConstraints(HttpServletRequest request) {
        // Maximum of constraints is 50, it should be enough  
        logger.debug("request: " + request.getQueryString());
        List<ConstraintLoad> ret = new ArrayList<ConstraintLoad>();
        for (int i = 0; i < 50; i++) {
            
            String opParameter = OPERATION_PARAMETER + i;
            String opString = request.getParameter(opParameter);
            ConstraintOp op = ConstraintOp.getConstraintOp(CodeTranslator.getCode(opString));
            
            String valueParameter = VALUE_PARAMETER + i;
            String value = request.getParameter(valueParameter);            
            
            String extraParameter = EXTRA_PARAMETER + i;
            String extraValue = request.getParameter(extraParameter);
            
            if (opString != null && opString.length() > 0 && op == null) {
                addError("invalid parameter: " + opParameter + " with value " 
                        + opString + " It must be valid operation code. Special characters "
                        + "must be encoded in request. See help for 'url encoding'.");
            }

            if (isPresent(op) && !isPresent(value)) {
                addError("invalid parameter: " + valueParameter 
                        + " Operation was specified, but not value.");
            }
            
            if (isPresent(value) && !isPresent(op)) {
                addError("invalid parameter: " + opParameter 
                    + " Value was specified, but not operation.");
            }
            
            if (isPresent(extraValue) && (!isPresent(op) || !isPresent(value))) {
                addError("invalid parameter: " + extraParameter 
                        + " Extra value was specified, but not operation or value.");
            }
            
            if (op != null && value != null && value.length() != 0) {
                ret.add(new ConstraintLoad(op, value, extraValue));
            }            
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
            addError("invalid required parameter: " + name);
            return null;
        } else {
            return param;
        }
    }
    
    private void addError(String error) {
        errors.add(error);
    }

    /**
     * Returns errors occurred  during request parsing.  
     * @return errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Sets errors occurred during request parsing.
     * @param errors errors
     */
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
