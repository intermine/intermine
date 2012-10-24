package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.objectstore.query.ConstraintOp;
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
import org.intermine.util.TypeUtil;

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
    protected static final String ENDL          = System.getProperty("line.separator");

    protected static final String INVALID_QUERY
        = "# Invalid query.\n# =============\n"
        + "# This query cannot be run because of the following problems:" + ENDL;
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

    private static final String BOILERPLATE =
            "#!/usr/bin/env python" + ENDL + ENDL
            + "# This is an automatically generated script to run your query" + ENDL
            + "# to use it you will require the intermine python client." + ENDL
            + "# To install the client, run the following command from a terminal:" + ENDL
            + "#" + ENDL
            + "#     sudo easy_install intermine" + ENDL
            + "#" + ENDL
            + "# For further documentation you can visit:" + ENDL
            + "#     http://www.intermine.org/wiki/PythonClient" + ENDL + ENDL
            + "# The following two lines will be needed in every python script:" + ENDL
            + "from intermine.webservice import Service" + ENDL;

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

        PathQuery query = info.getQuery();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        List<String> problems = query.verifyQuery();
        if (!problems.isEmpty()) {
            return INVALID_QUERY + formatProblems(problems);
        }

        StringBuffer sb = new StringBuffer(BOILERPLATE);

        if (info.isPublic()) {
            sb.append("service = Service(\"" + info.getServiceBaseURL() + "/service\")"
                    + ENDL + ENDL);
        } else {
            sb.append("service = Service(\"" + info.getServiceBaseURL() + "\", \"YOUR-API-KEY\")"
                    +  ENDL + ENDL);
        }

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
                sb.append(currentLine.toString() + "\\" + ENDL);
                currentLine = new StringBuffer(INDENT + INDENT);
            }
            currentLine.append(toPrint);
        }
        sb.append(currentLine.toString() + ENDL);

        return sb.toString();
    }

    private String decapitate(String longPath) {
        return longPath.substring(longPath.indexOf(".") + 1);
    }

    private String handlePathQuery(StringBuffer sb, PathQuery query, List<String> rootLessViews) {

        if (StringUtils.isNotBlank(query.getDescription())) {
            sb.append("# query description - " + query.getDescription() + ENDL + ENDL);
        }

        sb.append("# Get a new query on the class (table) you will be querying:"  + ENDL);

        try {
            sb.append("query = service.new_query(\"" + query.getRootClass() + "\")" + ENDL);
        } catch (PathException e) {
            // Should have been caught above...
            return INVALID_QUERY + formatProblems(query.verifyQuery());
        }

        sb.append(ENDL);

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
            return INVALID_QUERY + formatProblems(constraintProblems);
        }

        if (!uncodedQueryTexts.isEmpty()) {
            // Subclass constraints must come first or the query will break
            sb.append("# Type constraints should come early - before all mentions ");
            sb.append("of the paths they constrain" + ENDL);
            for (String text: uncodedQueryTexts) {
                sb.append(text);
            }
            sb.append(ENDL);
        }

        sb.append("# The view specifies the output columns" + ENDL);
        sb.append("query.add_view(");
        StringBuffer viewLine = new StringBuffer();

        listFormatUtil(viewLine, rootLessViews);
        if (viewLine.toString().length() <= 74) {
            sb.append(viewLine.toString());
        } else {
            sb.append(ENDL);
            printLine(sb, INDENT, viewLine.toString());
        }
        sb.append(")" + ENDL + ENDL);

        // Add orderBy
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) { // no sort order
            if (// The default
                query.getOrderBy().size() == 1
                    && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0))
                    && query.getOrderBy().get(0).getDirection() == OrderDirection.ASC) {
                sb.append("# " + DEFAULT_SO_MSG + ENDL + "# ");
            } else {
                sb.append("# " + CUSTOM_SO_MSG + ENDL);
            }
            for (OrderElement oe : query.getOrderBy()) {
                sb.append("query.add_sort_order(");
                sb.append("\"" + oe.getOrderPath() + "\", \"" + oe.getDirection() + "\"");
                sb.append(")" + ENDL);
            }
            sb.append(ENDL);
        }

        // Add constraints
        if (!codedQueryTexts.isEmpty()) {
            // Add comments for constraints
            sb.append("# You can edit the constraint values below" + ENDL);

            for (String text: codedQueryTexts) {
                sb.append(text);
            }
            sb.append(ENDL);

            // Add constraintLogic
            if (query.getConstraintLogic() != null
                && !"".equals(query.getConstraintLogic())) {
                String logic = query.getConstraintLogic();
                if (codedQueries <= 1 || logic.indexOf("or") == -1) {
                    sb.append("# " + DEFAULT_LOGIC_MSG + ENDL + "# ");
                } else {
                    sb.append("# " + CUSTOM_LOGIC_MSG + ENDL);
                }
                sb.append("query.set_logic(\"" + logic + "\")" + ENDL + ENDL);
            }
        }

        if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
            List<String> outerjoinSection = generateOuterJoinSection(query.getOuterJoinStatus());
            if (!outerjoinSection.isEmpty()) {
                sb.append("# " + OUTER_JOINS_TITLE + ENDL);
                for (String line : OUTER_JOINS_EXPLANATION) {
                    sb.append("# " + line + ENDL);
                }
                for (String line : outerjoinSection) {
                    sb.append(line + ENDL);
                }
                sb.append(ENDL);
            }
        }
        sb.append("for row in query.rows():" + ENDL);

        return null;
    }


    private List<String> generateOuterJoinSection(
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
            return INVALID_QUERY + formatProblems(Arrays.asList(
                    "This template has no editable constraints."));
        }
        StringBuffer constraints = new StringBuffer();
        StringBuffer constraintComments = new StringBuffer();

        constraintComments.append("# You can edit the constraint values below" + ENDL);

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
                constraintComments.append(ENDL);
            } else {
                constraintComments.append(INDENT + constraintDes + ENDL);
            }
            try {
                constraints.append(templateConstraintUtil(pc, opCode));
            } catch (UnhandledFeatureException e) {
                constraintProblems.add(e.getMessage());
            }
            if (conIter.hasNext()) {
                constraints.append(",");
            }
            constraints.append(ENDL);

        }
        if (!constraintProblems.isEmpty()) {
            return INVALID_QUERY + formatProblems(constraintProblems);
        }

        if (description != null && !"".equals(description)) {
            printLine(sb, "# ", description);
            sb.append(ENDL);
        }
        sb.append("template = service.get_template('" + templateName + "')" + ENDL + ENDL);
        sb.append(constraintComments.toString() + ENDL);
        sb.append("rows = template.rows(" + ENDL);
        sb.append(constraints.toString() + ")" + ENDL);
        sb.append("for row in rows:" + ENDL);

        return null;
    }

    private void listFormatUtil(StringBuffer sb, Collection<String> coll) {
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
            buf.append(ENDL);
        }
        return buf.toString();
    }

    /*
     * Nicely format long lines
     */
    private void printLine(StringBuffer sb, String prefix, String line) {
        String lineToPrint;
        if (prefix != null) {
            lineToPrint = prefix + line;
        } else {
            lineToPrint = line;
        }
        if (lineToPrint.length() > 80 && lineToPrint.lastIndexOf(' ', 80) != -1) {
            int lastCutPoint = lineToPrint.lastIndexOf(' ', 80);
            String frontPart = lineToPrint.substring(0, lastCutPoint);
            sb.append(frontPart + ENDL);
            String nextLine = lineToPrint.substring(lastCutPoint + 1);
            printLine(sb, prefix, nextLine);
        } else {
            sb.append(lineToPrint + ENDL);
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
            sb.append("\"" + op.toString() + "\"");
            sb.append(", \"" + value + "\", \"" + extraValue + "\"");
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
            sb.append(", \"" + code + "\"");
        }
        sb.append(")" + ENDL);
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
    private String templateConstraintUtil(PathConstraint pc, String opCode)
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
            return start + "{\"op\": \"LOOKUP\", \"value\": \"" + value
                + "\", \"extra_value\": \"" + extraValue + "\"}";
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
