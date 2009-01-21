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
import java.util.List;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.util.Util;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.server.CodeTranslator;
import org.intermine.webservice.server.LinkGeneratorBase;
import org.intermine.webservice.server.WebServiceConstants;
import org.intermine.webservice.server.query.result.QueryResultRequestParser;


/**
 * Class that implements generating links of TemplateResultService web service.
 * @author Jakub Kulaviak
 **/
public class TemplateResultLinkGenerator extends LinkGeneratorBase
{

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
     * @param highlighted 
     * @return generated link
     */
    public String getHtmlLink(String baseUrl, TemplateQuery template) {
        return getHtmlLinkInternal(baseUrl, template, false);
    }
    
    private String getHtmlLinkInternal(String baseUrl, TemplateQuery template, 
            boolean highlighted) {
        String ret = getLink(baseUrl, template, highlighted);
        ret += "&size=";
        ret += format("" + DEFAULT_RESULT_SIZE, highlighted);
        ret += "&" + QueryResultRequestParser.LAYOUT_PARAMETER + "=minelink|paging";
        return ret;        
    }

    /**
     * Returns link which gives results as lines, where values are tab separated.
      @see #getLink(String, TemplateQuery) 
     * @param baseUrl base url 
     * @param template template
     * @return highlighted link
     */
    public String getTabLink(String baseUrl, TemplateQuery template) {
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
        return getHtmlLinkInternal(baseUrl, template, true);
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
            if (i != 0) {
                ret += "&";
            }
            int index = i + 1;
            ret += operationToString(cs.getOp(), index , highlighted);
            ret += "&" + valueToString(cs.getValue(), index, highlighted);
            if (cs.getOp().equals(ConstraintOp.LOOKUP)) {
                ret += "&" + extraToString(cs.getExtraValue(), index, highlighted);
            }
        }
        return ret;
    }

    private String operationToString(ConstraintOp op, int index, boolean highlighted) {
        String ret = "";
        ret += "op" + index + "=";
        ret += format(encode(CodeTranslator.getAbbreviation(
                op.toString())), highlighted);
        return ret;
    }

    private String valueToString(Object valueObject, int index, boolean highlighted) {
        String ret = "";
        ret += "value" + index + "=";
        // value could be  treated to be sql valid before, 
        // so we have to find original untreated string
        String value = encode(Util.wildcardSqlToUser(objectToString(valueObject)));
        ret += format(value, highlighted);
        return ret;
    }

    private String objectToString(Object o) {
        if (o != null) {
            return o.toString();
        } else {
            return "";
        }
    }

    private String extraToString(Object extraValue, int index, boolean highlighted) {
        String ret = "";
        ret += "extra" + index + "=";  
        ret += format(encode(Util.wildcardSqlToUser(objectToString(extraValue))), 
                highlighted);
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
     * 
     * @return error if some happened
     */
    public String getError() {
        return error;
    }
    
}
