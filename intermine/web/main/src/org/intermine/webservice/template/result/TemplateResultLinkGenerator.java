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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.web.logic.query.Constraint;
import org.intermine.web.logic.query.PathNode;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.CodeTranslator;
import org.intermine.webservice.WebServiceConstants;


/**
 * Class that implements generating links of TemplateResultService web service.
 * @author Jakub Kulaviak
 **/
public class TemplateResultLinkGenerator
{

    private static final int LINE_LENGTH = 70;

    /**
     * Default value of size parameter 
     */
    public static final int DEFAULT_RESULT_SIZE = 10;
    
    private String error;

    /**
     * Generates TemplateResultService web service link.
     * @param baseUrl base url that doesn't terminate with '/' , 
     * e.g. http://www.flymine.org/release-12.0
     * @param template template for which the link generate
     * @return generated link
     */
    public String getLink(String baseUrl, TemplateQuery template) {
        return getLink(baseUrl, template, false);
    }
    
    /**
     * Returns html formatted link in which are highlighted parameters that 
     * are to be replaced. * @see #getLink(String, TemplateQuery) 
     * @param baseUrl base url 
     * @param template template
     * @return highlighted link
     */
    public String getHighlightedLink(String baseUrl, TemplateQuery template) {
        return getLink(baseUrl, template, true);
    }
    
    private String getLink(String baseUrl, TemplateQuery template, 
            boolean highlighted) {
        if (template.getBagNames().size() > 0) {
            error = "This template contains list(s) constraint. The service for this "
                + "special template is not implemented yet. Solution: Don't use list contraint.";
            return null;
        }
        String ret = baseUrl;
        ret += "/" + WebServiceConstants.MODULE_NAME + "/template/results?name=" 
            + template.getName() + "&";
        // Splits the long result url to 2 parts -> so it is less probable, 
        // that the url will overflow the div
        if (highlighted) {
            ret += "<br />";
        }
        List<Constraint> constraints = getConstraints(template);
        for (int i = 0; i < constraints.size(); i++) {
            Constraint cs = constraints.get(i);
            int code = i + 1;
            String opString;
            if (i != 0) {
                opString = "&op";
            } else {
                opString = "op";
            }
            ret += opString + code + "=";
            ret += format(TemplateResultLinkGenerator.encode(
                    CodeTranslator.getAbbreviation(cs.getOp().toString())), highlighted);
            ret += "&value" + code + "=";
            ret += format(TemplateResultLinkGenerator.encode(cs.getValue()), highlighted);
            if (cs.getOp().equals(ConstraintOp.LOOKUP)) {
                ret += "&extraValue" + code + "="  
                + TemplateResultLinkGenerator.encode(cs.getExtraValue());                
            }
        }
        ret += "&size=";
        ret += format("" + DEFAULT_RESULT_SIZE, highlighted);
        return ret;
    }

    /**
     * This method is made to be consistent with the way in which TemplateConfigurator
     * parses constraints. So the order of constraints is correct.
     * @param template
     * @return editable constraints 
     */
    private List<Constraint> getConstraints(TemplateQuery template) {
        List<Constraint> ret = new ArrayList<Constraint>();
        for (PathNode node : template.getEditableNodes()) {
            ret.addAll(template.getEditableConstraints(node));
        }
        return ret;
    }

    private String format(String text, boolean highlight) {
        if (highlight) {
            return "<span class=\"highlighted\">" + text + "</span>"; 
        } else {
            return text;
        }
    }
    
    /**
     *  Encodes object string value to be able to be part of url.
     * @param o encoded object
     * @return encoded string
     */
    private static String encode(Object o) {
        if (o == null) {
            return "";
        } else {
            try {
                return URLEncoder.encode(o.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding string failed", e);
            }            
        }
    }

    /**
     * 
     * @return error if some happened
     */
    public String getError() {
        return error;
    }
    
}
