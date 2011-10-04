package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.api.template.TemplateQuery;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

/**
 * A Class for generating JavaScript that would run a given query.
 * @author Alexis Kalderimis
 *
 */
public class WebserviceJavaScriptCodeGenerator implements WebserviceCodeGenerator
{

    protected static final String INVALID_QUERY = "Invalid query. No fields selected for output.";
    protected static final String NULL_QUERY    = "Invalid query. Query can not be null.";

    protected static final String INDENT        = "    ";
    protected static final String INDENT2       = INDENT + INDENT;
    protected static final String SPACE         = " ";
    protected static final String ENDL          = System.getProperty("line.separator");

    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
                                                + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT         = "Loop path constraint is not supported "
                                                              + "at the moment...";
    protected static final String SCRIPT_IMPORTS          =
          "<!-- You need to import the IMBedding client library - this is hosted at intermine.org for your convenience: -->" + ENDL
          + "<script src=\"http://www.intermine.org/lib/imbedding/0.2/imbedding.js\" type=\"text/javascript\"></script>" + ENDL + ENDL
          + "<!-- We also need to import a stylesheet - you can choose from light, dark or bold-->" + ENDL
          + "<link rel=\"stylesheet\" type=\"text/css\" title=\"light\" href=\"http://intermine.org/lib/imbedding/0.2/style/light.css\">" + ENDL
          + ENDL;

    protected static final String PRELUDE =
        "<!-- This is an automatically generated code snippet to run your query" + ENDL
        + " using the intermine JavaScript client library. It is assumed that you" + ENDL
        + " will be wanting to run this query from a webpage, and so the code is" + ENDL
        + " formatted such that you can just cut and paste it into any webpage -->" + ENDL + ENDL;

    protected static final String PLACEHOLDER =
        "<!-- You need to set a place holder element in your page to hold the resultant table - this can also hold apology text/content -->" + ENDL
        + "<div id=\"queryplaceholder\"><p class=\"apology\">Please be patient while the results of your query are retrieved.</p></div>" + ENDL + ENDL;

    protected static final String BOILERPLATE = "<script type=\"text/javascript\">" + ENDL;
    protected static final String QUERY_METHOD = "IMBedding.loadQuery(query, ";
    protected static final String TEMPLATE_METHOD = "IMBedding.loadTemplate(";

    /**
     * This method will generate code that will run using the python webservice
     * client library.
     *
     * @param wsCodeGenInfo a WebserviceCodeGenInfo object
     * @return the code as a string
     */
    @Override
    public String generate(WebserviceCodeGenInfo wsCodeGenInfo) {

        PathQuery query = wsCodeGenInfo.getQuery();
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }
        if (query.getView().isEmpty()) {
            return INVALID_QUERY;
        }

        String queryClassName = TypeUtil.unqualifiedName(query.getClass().toString());

        StringBuffer sb = new StringBuffer().append(PRELUDE)
                                            .append(SCRIPT_IMPORTS)
                                            .append(PLACEHOLDER)
                                            .append(BOILERPLATE);

        if ("PathQuery".equals(queryClassName)) {

            sb.append("/* Your query can be defined as XML */" + ENDL);
            sb.append("var query = '" + query.toXml() + "';" + ENDL + ENDL);
            sb.append("/* It can now be loaded into a table with the following command */" + ENDL);
            sb.append(QUERY_METHOD);
            sb.append("{baseUrl: '" + serviceBaseURL + "'}, '#queryplaceholder');" + ENDL);


        } else if ("TemplateQuery".equals(queryClassName)) {

            TemplateQuery template = (TemplateQuery) query;

            String templateName = template.getName();
            String description = template.getDescription();
            Map<PathConstraint, String> allConstraints = query.getConstraints();
            List<PathConstraint> editableConstraints = template.getEditableConstraints();

            sb.append(TEMPLATE_METHOD + ENDL);
            sb.append(INDENT + "{" + ENDL);
            if (description != null && !"".equals(description)) {
                printLine(sb, INDENT2 + "// ", description);
            }
            sb.append(INDENT2 + getFormattedObjKey("name:") + "\"" + templateName + "\"," + ENDL);

            Iterator<PathConstraint> conIter = editableConstraints.iterator();
            int constraintNo = 1;
            while (conIter.hasNext()) {
                PathConstraint pc = conIter.next();
                // Add comments for constraints
                String constraintDes = template.getConstraintDescription(pc);
                sb.append(ENDL);
                if (constraintDes != null && !"".equals(constraintDes)) {
                    sb.append(INDENT + INDENT + "// " + constraintDes + ENDL);
                }

                String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                if ("PathConstraintBag".equals(className)) {
                    return TEMPLATE_BAG_CONSTRAINT;
                }
                if ("PathConstraintLoop".equals(className)) {
                    return LOOP_CONSTRAINT;
                }

                String code = allConstraints.get(pc);
                Map<String, String> templateParams = templateConstraintUtil(pc, code, constraintNo);

                Iterator<Entry<String, String>> entryIter = templateParams.entrySet().iterator();
                while (entryIter.hasNext()) {
                    Entry<String, String> pair = entryIter.next();
                    sb.append(INDENT + INDENT + getFormattedObjKey(pair.getKey() + ":"));
                    sb.append(quote(pair.getValue()));
                    if (entryIter.hasNext() || conIter.hasNext()) {
                        sb.append(",");
                    }
                    sb.append(ENDL);
                }

                constraintNo++;
            }

            sb.append(INDENT + "}," + ENDL);
            sb.append(INDENT + "\'#queryplaceholder'," + ENDL);
            sb.append(INDENT + "{baseUrl: '" + serviceBaseURL + "'}" + ENDL);
            sb.append(");" + ENDL);
        }

        sb.append("</script>" + ENDL);
        return sb.toString();
    }

    private String quote(String str) {
        if (str == null) {
            return "";
        } else if (str.startsWith("{") && str.endsWith("}")) {
            return str;
        } else if (str.startsWith("[") && str.endsWith("]")) {
            return str;
        } else {
            return "\"" + escapeJavaScript(str) + "\"";
        }
    }


    private static String getFormattedObjKey(String key) {
        StringBuffer sb = new StringBuffer(key);
        while (sb.length() < 15) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Format a list of strings as a nice javascript array.
     */
    private static void listFormatUtil(StringBuffer sb, Collection<String> coll) {
        Iterator<String> it = coll.iterator();
        sb.append("[ ");
        while (it.hasNext()) {
            sb.append("\"" + escapeJavaScript(it.next()) + "\"");
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(" ]");
    }

    /*
     * Nicely format long lines
     */
    private static void printLine(StringBuffer sb, String prefix, String line) {
        String lineToPrint;
        if (prefix != null) {
            lineToPrint = prefix + line;
        } else {
            lineToPrint = line;
        }
        if (lineToPrint.length() > 80 && lineToPrint.lastIndexOf(' ', 80) != -1) {
            int lastCutPoint = lineToPrint.lastIndexOf(' ', 80);
            sb.append(lineToPrint.substring(0, lastCutPoint) + ENDL);
            String nextLine = lineToPrint.substring(lastCutPoint + 1);
            printLine(sb, prefix, nextLine);
        } else {
            sb.append(lineToPrint + ENDL);
        }
    }

    /**
     * This method helps to generate Template Parameters (predefined
     * constraints) source code for TemplateQuery
     *
     * @param pc
     *            PathConstraint object
     * @param opCode
     *            operation code
     * @return A map that contains the parameters for this constraint
     */
    private static Map<String, String> templateConstraintUtil(PathConstraint pc,
            String opCode, int constraintNo) {
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        String op = pc.getOp().toString();

        Map<String, String> ret = new LinkedHashMap<String, String>();

        ret.put("constraint" + constraintNo, path);
        ret.put("op" + constraintNo, op);

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            ret.put("value" + constraintNo, value);
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            ret.put("value" + constraintNo, value);
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();
            if (extraValue != null) {
                ret.put("extra" + constraintNo, extraValue);
            }
        }

        if ("PathConstraintBag".equals(className)) {
            // not supported
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
        }

        if ("PathConstraintMultiValue".equals(className)) {
            StringBuffer sb = new StringBuffer();
            Collection<String> values = ((PathConstraintMultiValue) pc).getValues();
            listFormatUtil(sb, values);
            ret.put("value" + constraintNo, sb.toString());
        }

        if ("PathConstraintSubclass".equals(className)) {
            // not handled
        }

        if ("PathConstraintLoop".equals(className)) {
            // not supported
        }

        ret.put("code" + constraintNo, opCode);
        return ret;
    }
}
