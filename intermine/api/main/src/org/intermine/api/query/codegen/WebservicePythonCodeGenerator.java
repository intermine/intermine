package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.metadata.TypeUtil;

/**
 * Class for generating Python code to run a query, using the intermine python library.
 * @author Alex Kalderimis
 *
 */
public class WebservicePythonCodeGenerator implements WebserviceCodeGenerator
{
    protected static final String NULL_QUERY    = "Invalid query. Query can not be null.";
    protected static final String INDENT        = "    ";
    protected static final String SPACE         = " ";

    private String endl          = System.getProperty("line.separator");

    /**
     * @return error message
     */
    protected String getInvalidQuery() {
        StringBuffer message = new StringBuffer()
            .append("# Invalid query.").append(endl)
            .append("# =============").append(endl)
            .append("# This query cannot be run because of the following problems:").append(endl);
        return message.toString();
    }

    private static final String DEFAULT_SO_MSG
        = "Uncomment and edit the line below (the default) to select a custom sort order:";
    private static final String CUSTOM_SO_MSG
        = "This query's custom sort order is specified below:";
    private static final String DEFAULT_LOGIC_MSG
        = "Uncomment and edit the code below to specify your own custom logic:";
    private static final String CUSTOM_LOGIC_MSG
        = "Your custom constraint logic is specified with the code below:";

    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
                                                 + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT         = "Loop path constraint is not supported "
                                                              + "at the moment...";

    private String getBoilerPlate() {
        String boilerplate = "#!/usr/bin/env python" + endl + endl
            + "# This is an automatically generated script to run your query" + endl
            + "# to use it you will require the intermine python client." + endl
            + "# To install the client, run the following command from a terminal:" + endl
            + "#" + endl
            + "#     sudo easy_install intermine" + endl
            + "#" + endl
            + "# For further documentation you can visit:" + endl
            + "#     http://intermine.readthedocs.org/en/latest/web-services/" + endl + endl
            + "# The following two lines will be needed in every python script:" + endl
            + "from intermine.webservice import Service" + endl;
        return boilerplate;
    }

    private static final String OUTER_JOINS_TITLE = "Outer Joins";
    private static final String[] OUTER_JOINS_EXPLANATION = new String[] {
        "(display properties of these relations if they exist,",
        "but also show objects without these relationships)"};
    private static final String UNHANDLED_CONSTRAINT =
            "It contains a constraint type that can only be used internally";

    /**
     * This method will generate code that will run using the python webservice
     * client library.
     *
     * @param info a WebserviceCodeGenInfo object
     * @return the code as a string
     */
    @Override
    public String generate(WebserviceCodeGenInfo info) {

        endl = info.getLineBreak();

        PathQuery query = info.getQuery();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        List<String> problems = query.verifyQuery();
        if (!problems.isEmpty()) {
            return getInvalidQuery() + formatProblems(problems);
        }

        StringBuffer sb = new StringBuffer(getBoilerPlate());

        sb.append("service = Service(\"").append(info.getServiceBaseURL()).append("/service\"");

        if (!info.isPublic()) {
            sb.append(", token = \"YOUR-API-KEY\"");
        }
        sb.append(")" + endl + endl);

        List<String> rootLessViews = new ArrayList<String>();
        List<String> rowKeyAccesses = new ArrayList<String>();
        for (String v: query.getView()) {
            rootLessViews.add(decapitate(v));
        }
        for (String key : rootLessViews) {
            rowKeyAccesses.add("row[\"" + key + "\"]");
        }

        String error = null;
        if (query instanceof TemplateQuery) {
            error = handleTemplate(sb, query);
        } else {
            error = handlePathQuery(sb, query, rootLessViews);
        }
        if (error != null) {
            return error;
        }

        StringBuffer currentLine = new StringBuffer(INDENT + "print");
        Iterator<String> rowKeyIt = rowKeyAccesses.iterator();
        while (rowKeyIt.hasNext()) {
            String toPrint = rowKeyIt.next();
            if (StringUtils.isNotBlank(currentLine.toString())) {
                toPrint = " " + toPrint;
            }
            if (rowKeyIt.hasNext()) {
                toPrint += ",";
            }
            if (currentLine.length() + toPrint.length() > 100) {
                sb.append(currentLine.toString() + " \\" + endl);
                currentLine = new StringBuffer(INDENT + INDENT);
                toPrint = toPrint.substring(1);
            }
            currentLine.append(toPrint);
        }
        sb.append(currentLine.toString() + endl);

        return sb.toString();
    }

    private static String decapitate(String longPath) {
        return longPath.substring(longPath.indexOf(".") + 1);
    }

    private String handlePathQuery(StringBuffer sb, PathQuery query, List<String> rootLessViews) {

        if (StringUtils.isNotBlank(query.getDescription())) {
            sb.append("# query description - " + query.getDescription() + endl + endl);
        }

        sb.append("# Get a new query on the class (table) you will be querying:"  + endl);

        try {
            sb.append("query = service.new_query(\"" + query.getRootClass() + "\")" + endl);
        } catch (PathException e) {
            // Should have been caught above...
            return getInvalidQuery() + formatProblems(query.verifyQuery());
        }

        sb.append(endl);

        int codedQueries = 0;
        List<String> uncodedQueryTexts = new ArrayList<String>();
        List<String> codedQueryTexts = new ArrayList<String>();

        List<String> constraintProblems = new LinkedList<String>();
        if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
            for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                PathConstraint pc = entry.getKey();
                String constraint = "";
                try {
                    constraint = pathContraintUtil(pc, entry.getValue());
                } catch (UnhandledFeatureException e) {
                    constraintProblems.add(e.getMessage());
                }
                if (entry.getValue() != null) {
                    codedQueries++;
                    codedQueryTexts.add(constraint);
                } else {
                    uncodedQueryTexts.add(constraint);
                }
            }
        }
        if (!constraintProblems.isEmpty()) {
            return getInvalidQuery() + formatProblems(constraintProblems);
        }

        if (!uncodedQueryTexts.isEmpty()) {
            // Subclass constraints must come first or the query will break
            sb.append("# Type constraints should come early - before all mentions ");
            sb.append("of the paths they constrain" + endl);
            for (String text: uncodedQueryTexts) {
                sb.append(text);
            }
            sb.append(endl);
        }

        sb.append("# The view specifies the output columns" + endl);
        sb.append("query.add_view(");
        StringBuffer viewLine = new StringBuffer();

        listFormatUtil(viewLine, rootLessViews);
        if (viewLine.toString().length() <= 74) {
            sb.append(viewLine.toString());
        } else {
            sb.append(endl);
            printLine(sb, INDENT, viewLine.toString());
        }
        sb.append(")" + endl + endl);

        // Add orderBy
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) { // no sort order
            if (// The default
                query.getOrderBy().size() == 1
                    && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0))
                    && query.getOrderBy().get(0).getDirection() == OrderDirection.ASC) {
                sb.append("# " + DEFAULT_SO_MSG + endl + "# ");
            } else {
                sb.append("# " + CUSTOM_SO_MSG + endl);
            }
            for (OrderElement oe : query.getOrderBy()) {
                sb.append("query.add_sort_order(");
                sb.append("\"" + oe.getOrderPath() + "\", \"" + oe.getDirection() + "\"");
                sb.append(")" + endl);
            }
            sb.append(endl);
        }

        // Add constraints
        if (!codedQueryTexts.isEmpty()) {
            // Add comments for constraints
            sb.append("# You can edit the constraint values below" + endl);

            for (String text: codedQueryTexts) {
                sb.append(text);
            }
            sb.append(endl);

            // Add constraintLogic
            if (query.getConstraintLogic() != null
                && !"".equals(query.getConstraintLogic())) {
                String logic = query.getConstraintLogic();
                if (codedQueries <= 1 || logic.indexOf("or") == -1) {
                    sb.append("# " + DEFAULT_LOGIC_MSG + endl + "# ");
                } else {
                    sb.append("# " + CUSTOM_LOGIC_MSG + endl);
                }
                sb.append("query.set_logic(\"" + logic + "\")" + endl + endl);
            }
        }

        if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
            List<String> outerjoinSection = generateOuterJoinSection(query.getOuterJoinStatus());
            if (!outerjoinSection.isEmpty()) {
                sb.append("# " + OUTER_JOINS_TITLE + endl);
                for (String line : OUTER_JOINS_EXPLANATION) {
                    sb.append("# " + line + endl);
                }
                for (String line : outerjoinSection) {
                    sb.append(line + endl);
                }
                sb.append(endl);
            }
        }
        sb.append("for row in query.rows():" + endl);

        return null;
    }


    private static List<String> generateOuterJoinSection(
            Map<String, OuterJoinStatus> outerJoinStatus) {
        List<String> lines = new LinkedList<String>();
        for (Entry<String, OuterJoinStatus> entry : outerJoinStatus.entrySet()) {
            if (entry.getValue() == OuterJoinStatus.OUTER) {
                lines.add("query.outerjoin(\"" + decapitate(entry.getKey()) + "\")");
            }
        }
        return lines;
    }

    private String handleTemplate(StringBuffer sb, PathQuery query) {

        String templateName = ((TemplateQuery) query).getName();
        String description = ((TemplateQuery) query).getDescription();

        Map<PathConstraint, String> allConstraints = query.getConstraints();
        List<PathConstraint> editableConstraints = ((TemplateQuery) query)
            .getEditableConstraints();
        if (editableConstraints == null || editableConstraints.isEmpty()) {
            return getInvalidQuery() + formatProblems(Arrays.asList(
                    "This template has no editable constraints."));
        }
        StringBuffer constraints = new StringBuffer();
        StringBuffer constraintComments = new StringBuffer();

        constraintComments.append("# You can edit the constraint values below" + endl);

        Iterator<PathConstraint> conIter = editableConstraints.iterator();
        List<String> constraintProblems = new LinkedList<String>();
        while (conIter.hasNext()) {
            PathConstraint pc = conIter.next();
            // Add comments for constraints
            String path = pc.getPath();

            String opCode = allConstraints.get(pc);
            constraintComments.append("# " + opCode + INDENT + path);
            String constraintDes = ((TemplateQuery) query).getConstraintDescription(pc);
            if (constraintDes == null || "".equals(constraintDes)) {
                constraintComments.append(endl);
            } else {
                constraintComments.append(INDENT + constraintDes + endl);
            }
            try {
                constraints.append(templateConstraintUtil(pc, opCode));
            } catch (UnhandledFeatureException e) {
                constraintProblems.add(e.getMessage());
            }
            if (conIter.hasNext()) {
                constraints.append(",");
            }
            constraints.append(endl);

        }
        if (!constraintProblems.isEmpty()) {
            return getInvalidQuery() + formatProblems(constraintProblems);
        }

        if (description != null && !"".equals(description)) {
            printLine(sb, "# ", description);
            sb.append(endl);
        }
        sb.append("template = service.get_template('" + templateName + "')" + endl + endl);
        sb.append(constraintComments.toString() + endl);
        sb.append("rows = template.rows(" + endl);
        sb.append(constraints.toString() + ")" + endl);
        sb.append("for row in rows:" + endl);

        return null;
    }

    private static void listFormatUtil(StringBuffer sb, Collection<String> coll) {
        Iterator<String> it = coll.iterator();
        while (it.hasNext()) {
            sb.append("\"" + it.next() + "\"");
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
    }

    private String formatProblems(List<String> problems) {
        StringBuffer buf = new StringBuffer();
        for (String issue: problems) {
            buf.append("#  * ");
            buf.append(issue);
            buf.append(endl);
        }
        return buf.toString();
    }

    /*
     * Nicely format long lines
     *
     * @param sb Existing stringbuffer on which to append line. Cannot be null.
     * @param prefix Prefix for text on line (e.g. python indent or comment prefix). Cannot be null.
     * @param text Text of line. Will be trimmed. Cannot be null.
     */
    private void printLine(StringBuffer sb, String prefix, String text) {
        text = text.trim();
        int desiredMaxLineLength = 80;
        String line = prefix + text;

        // If our prefix is greater than desiredMaxTextLength then don't attempt to wrap the lines
        // at all.  This should never happen but lets produce something unwrapped rather than fail
        // in an unexpected way.
        if (line.length() <= desiredMaxLineLength || prefix.length() >= desiredMaxLineLength) {
            sb.append(line + endl);
            return;
        }

        // We need to handle the case where a long path may generate a line without spaces past the
        // 80 column mark except for the prefix.  Otherwise we get infinite recursion
        int cutPoint = text.length();
        int prevCutPoint = cutPoint;

        int desiredMaxTextLength = desiredMaxLineLength - prefix.length();

        while (cutPoint > desiredMaxTextLength) {
            prevCutPoint = cutPoint;
            cutPoint = text.lastIndexOf(' ', cutPoint - 1);
        }

        // Handle the case where our non-space text is greater than the desired max text length.
        // This can be happen with long paths.
        if (cutPoint == -1) {
            cutPoint = prevCutPoint;
        }

        String thisLine = prefix + text.substring(0, cutPoint);
        sb.append(thisLine + endl);

        if (thisLine.length() < line.length()) {
            String nextLine = text.substring(cutPoint + 1);
            printLine(sb, prefix, nextLine);
        }
    }

    /**
     * This method helps to generate constraint source code for PathQuery
     *
     * @param pc PathConstraint object
     * @return a string for constraints source code
     * @throws UnhandledFeatureException If the constraint type cannot be represented in python.
     */
    private String pathContraintUtil(PathConstraint pc, String code)
        throws UnhandledFeatureException {
        // Ref to Constraints
        StringBuffer sb = new StringBuffer("query.add_constraint(");

        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath().substring(pc.getPath().indexOf(".") + 1);
        ConstraintOp op = pc.getOp();

        sb.append("\"" + path + "\", ");

        if ("PathConstraintSubclass".equals(className)) {
            String type = ((PathConstraintSubclass) pc).getType();
            sb.append("\"" + type + "\"");
        } else if ("PathConstraintNull".equals(className)) {
            sb.append("\"" + op.toString() + "\"");
        } else if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            sb.append("\"" + op.toString() + "\"");
            sb.append(", \"" + value + "\"");
        } else if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();
            sb.append('"').append(op.toString()).append('"');
            sb.append(", \"").append(value).append('"');
            if (extraValue != null) {
                sb.append(", \"").append(extraValue).append('"');
            }
        } else if ("PathConstraintBag".equals(className)) {
            String list = ((PathConstraintBag) pc).getBag();
            sb.append("\"" + op.toString() + "\"");
            sb.append(", \"" + list + "\"");
        } else if ("PathConstraintMultiValue".equals(className)) {
            sb.append("\"" + op.toString() + "\"");
            sb.append(", [");
            Collection<String> values = ((PathConstraintMultiValue) pc).getValues();
            listFormatUtil(sb, values);
            sb.append("]");
        } else if ("PathConstraintLoop".equals(className)) {
            if (op.equals(ConstraintOp.EQUALS)) {
                sb.append("\"IS\"");
            } else {
                sb.append("\"IS NOT\"");
            }
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            sb.append(", \"" + decapitate(loopPath) + "\"");
        } else {
            throw new UnhandledFeatureException(UNHANDLED_CONSTRAINT + " (" + className + ")");
        }

        if (code != null) {
            sb.append(", code = \"" + code + "\""); // kwargs
        }
        sb.append(")" + endl);
        return sb.toString();
    }

    /**
     * This method helps to generate Template Parameters (predefined
     * constraints) source code for TemplateQuery
     *
     * @param pc
     *            PathConstraint object
     * @param opCode
     *            operation code
     * @return a line of source code
     */
    private static String templateConstraintUtil(PathConstraint pc, String opCode)
        throws UnhandledFeatureException {
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String op = pc.getOp().toString();
        String start = INDENT + opCode + " = ";

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            return start + "{\"op\": \"" + op + "\", \"value\": \"" + value + "\"}";
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();
            StringBuilder sb = new StringBuilder(start)
                                    .append("{\"op\": \"LOOKUP\", \"value\": \"")
                                    .append(value)
                                    .append('"');
            if (extraValue != null) {
                sb.append(", \"extra_value\": \"").append(extraValue).append('"');
            }
            sb.append('}');
            return sb.toString();
        }

        if ("PathConstraintBag".equals(className)) {
            String value = ((PathConstraintBag) pc).getBag();
            return start + "{\"op\": \"" + op + "\", \"value\": \"" + value + "\"}";
        }

        if ("PathConstraintIds".equals(className)) {
            throw new UnhandledFeatureException(UNHANDLED_CONSTRAINT + " (" + className + ")");
        }

        if ("PathConstraintMultiValue".equals(className)) {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            Collection<String> values = ((PathConstraintMultiValue) pc).getValues();
            listFormatUtil(sb, values);
            sb.append("]");
            return start + "{\"op\": \"" + op + "\", \"values\": \"" + sb.toString()
                + "\"}";
        }

        if ("PathConstraintNull".equals(className)) {
            return start + "{\"op\": \"" + op + "\"}";
        }

        if ("PathConstraintSubclass".equals(className)) {
            throw new UnhandledFeatureException(UNHANDLED_CONSTRAINT + " (" + className + ")");
        }

        if ("PathConstraintLoop".equals(className)) {
            throw new UnhandledFeatureException(UNHANDLED_CONSTRAINT + " (" + className + ")");
        }
        return null;
    }
}
