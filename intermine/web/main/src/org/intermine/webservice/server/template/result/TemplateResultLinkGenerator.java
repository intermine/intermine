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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.util.StringUtil;
import org.intermine.webservice.server.CodeTranslator;
import org.intermine.webservice.server.LinkGeneratorBase;
import org.intermine.webservice.server.WebServiceConstants;
import org.intermine.webservice.server.WebServiceRequestParser;
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
     * Get a link that takes the user to a results page in the originating mine.
     * @param baseUrl The mine's base URL.
     * @param tq The template.
     * @return A URL.
     */
    public String getMineResultsLink(String baseUrl, TemplateQuery tq) {
        return baseUrl + getMineResultsPath(tq, false);
    }

    /**
     * Get a link that takes the user to a results page in the originating mine.
     * @param tq The template.
     * @param highlighted Whether or not to highlight certain parameters.
     * @return The path section of a URL.
     */
    public String getMineResultsPath(TemplateQuery tq, boolean highlighted) {
        String ret = "/loadTemplate.do?";
        ret += getTemplateParameterQueryString(tq, highlighted);
        ret += "&method=results";
        return ret;
    }

    /**
     * Generates TemplateResultService web service link.
     * @param baseUrl base url that doesn't terminate with '/' ,
     * e.g. http://www.flymine.org/release-12.0
     * @param template template for which the link is generated
     * @return generated link
     */
    public String getHtmlLink(String baseUrl, TemplateQuery template) {
        return getHtmlLinkInternal(baseUrl, template, false);
    }

    private String getHtmlLinkInternal(String baseUrl, TemplateQuery template, boolean hl) {
        String ret = getLink(baseUrl, template, WebServiceRequestParser.FORMAT_PARAMETER_HTML, hl);
        ret += "&size=";
        ret += format("" + DEFAULT_RESULT_SIZE, hl);
        ret += "&" + QueryResultRequestParser.LAYOUT_PARAMETER + "=minelink|paging";
        return ret;
    }

    /**
     * Returns link which gives results as lines, where values are tab separated.
      @see #getLink(String, TemplateQuery)
     * @param baseUrl base url
     * @param template template
     * @return a url
     */
    public String getTabLink(String baseUrl, TemplateQuery template) {
        String ret = getLink(baseUrl, template, false);
        ret += "&size=" + DEFAULT_RESULT_SIZE;
        return ret;
    }

    /**
     * Returns the path section of the link for this template. When passing just the
     * template, the format defaults to tab.
     * @param template The template to get the link for.
     * @return A string such that baseUrl + returnValue = a webservice query for this template
     */
    public String getLinkPath(TemplateQuery template) {
        return getLinkPath(template, WebServiceRequestParser.FORMAT_PARAMETER_TAB);
    }

    /**
     * Returns the path section of the link for this template. The format parameter is set to
     * the value of the format argument.
     * @param template The template to get the link for.
     * @param format The output format to use (xml, jsonobjects, count, etc)
     * @return A string such that baseUrl + returnValue = a webservice query for this template
     */
    public String getLinkPath(TemplateQuery template, String format) {
        return getLinkPath(template, format, false);
    }

    private String getTemplateParameterQueryString(TemplateQuery tq, boolean highlighted) {
        String ret = "name=" + tq.getName();

        // Splits the long result url to 2 parts -> so it is less probable,
        // that the url will overflow the div
        if (highlighted) {
            ret += "<br />";
        }
        int index = 1;
        Map<String, List<PathConstraint>> consMap = getConstraints(tq);
        for (String path : consMap.keySet()) {
            List<PathConstraint> constraints = consMap.get(path);
            for (PathConstraint con : constraints) {
                // We don't want to include optional constraints that are switched off
                if (SwitchOffAbility.OFF.equals(tq.getSwitchOffAbility(con))) {
                    continue;
                }
                ret += "&" + pathToString(path, index);
                // if there are more constraints with the some path, codes must be added
                if (constraints.size() > 1) {
                    String code = tq.getConstraints().get(con);
                    ret += "&" + codeToString(code, index);
                }
                ret += "&" + operationToString(con.getOp(), index , highlighted);

                if (con instanceof PathConstraintMultiValue) {
                    for (String value: ((PathConstraintMultiValue) con).getValues()) {
                        ret += "&" + valueToString(value, index, highlighted);
                    }
                } else {
                    ret += "&" + valueToString(getConstraintValue(con), index, highlighted);
                }
                if (con instanceof PathConstraintLookup) {
                    PathConstraintLookup conLookup = (PathConstraintLookup) con;
                    ret += "&" + extraToString(conLookup.getExtraValue(), index, highlighted);
                }
                if (highlighted) {
                    ret += "<br />";
                }
                index++;
            }
        }
        return ret;
    }

    /**
     * Returns the path section of the link for this template. The format parameter is set to
     * the value of the format argument, and if the url is to be highlighted, then it will have
     * the appropriate formatting.
     * @param template The template to get the link for.
     * @param format The output format to use (xml, jsonobjects, count, etc)
     * @param highlighted Whether or not we are highlighting this url.
     * @return A string such that baseUrl + returnValue = a webservice query for this template
     */
    public String getLinkPath(TemplateQuery template, String format, boolean highlighted) {
        String ret = "/" + WebServiceConstants.MODULE_NAME + "/template/results?";
        ret += getTemplateParameterQueryString(template, highlighted);
        ret += "&format=" + format;
        return ret;
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

    private String getLink(String baseUrl, TemplateQuery template, boolean hl) {
        return getLink(baseUrl, template, WebServiceRequestParser.FORMAT_PARAMETER_TAB, hl);
    }

    private String getLink(String baseUrl, TemplateQuery template, String format,
            boolean highlighted) {
        if (template.getBagNames().size() > 0) {
            error = "This template contains list constraints. The service for this "
                + "special template is not implemented yet. Solution: Don't use list contraint.";
            return null;
        }
        String ret = baseUrl + getLinkPath(template, format, highlighted);
        return ret;
    }

    private String codeToString(String code, int index) {
        return "code" + index + "=" + code;
    }

    private String pathToString(String path, int index) {
        return "constraint" + index + "=" + path;
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
        String value = encode(objectToString(valueObject));
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
        ret += format(encode(objectToString(extraValue)),
                highlighted);
        return ret;
    }

    /**
     * This method is made to be consistent with the way in which TemplateConfigurator
     * parses constraints. So the order of constraints is correct.
     * @param template
     * @return editable constraints
     */
    private Map<String, List<PathConstraint>> getConstraints(TemplateQuery template) {
        Map<String, List<PathConstraint>> ret = new HashMap<String, List<PathConstraint>>();
        for (String editablePath : template.getEditablePaths()) {
            ret.put(editablePath, template.getEditableConstraints(editablePath));
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

    private String getConstraintValue(PathConstraint con) {
        if (con instanceof PathConstraintAttribute) {
            return ((PathConstraintAttribute) con).getValue();
        } else if (con instanceof PathConstraintLookup) {
            return ((PathConstraintLookup) con).getValue();
        } else if (con instanceof PathConstraintBag) {
            return ((PathConstraintBag) con).getBag();
        } else if (con instanceof PathConstraintMultiValue) {
            return StringUtil.join(((PathConstraintMultiValue) con).getValues(), ",");
        } else if (con instanceof PathConstraintNull) {
            return ((PathConstraintNull) con).getOp().toString();
        } else {
            throw new IllegalArgumentException("Constraints of type '" + con.getClass()
                    + "' are not yet supported template queries.");

        }

    }
}
