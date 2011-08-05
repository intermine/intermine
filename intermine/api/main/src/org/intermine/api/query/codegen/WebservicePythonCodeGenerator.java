package org.intermine.api.query.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.api.template.TemplateQuery;
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
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

public class WebservicePythonCodeGenerator implements WebserviceCodeGenerator
{

    protected static final String INVALID_QUERY           = "Invalid query. No fields selected for output.";
    protected static final String NULL_QUERY              = "Invalid query. Query can not be null.";

    protected static final String INDENT                  = "    ";
    protected static final String SPACE                   = " ";
    protected static final String ENDL                    = System.getProperty("line.separator");

    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
                                                              + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT         = "Loop path constraint is not supported "
                                                              + "at the moment...";

    /**
     * This method will generate code that will run using the python webservice
     * client library.
     *
     * @param wsCodeGeninfo
     *            a WebserviceCodeGenInfo object
     * @return the code as a string
     */
    @Override
    public String generate(WebserviceCodeGenInfo info) {

        PathQuery query = info.getQuery();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        String queryClassName = TypeUtil.unqualifiedName(query.getClass().toString());

        StringBuffer sb = new StringBuffer();
        sb.append("#!/usr/bin/env python" + ENDL + ENDL);
        sb.append("# This is an automatically generated script to run your query" + ENDL);
        sb.append("# to use it you will require the intermine python client." + ENDL);
        sb.append("# To install the client, run the following command from a terminal:" + ENDL);
        sb.append("#" + ENDL);
        sb.append("#     sudo easy_install intermine" + ENDL);
        sb.append("#" + ENDL);
        sb.append("# For further documentation you can visit:" + ENDL);
        sb.append("#     http://www.intermine.org/wiki/PythonClient" + ENDL + ENDL);
        sb.append("# The following two lines will be needed in every python script:" + ENDL);
        sb.append("from intermine.webservice import Service" + ENDL);
        if (info.isPublic()) {
            sb.append("service = Service(\"" + info.getServiceBaseURL() + "/service\")" + ENDL + ENDL);
        } else {
            sb.append("service = Service(\"" + info.getServiceBaseURL() + "\", \"YOUR-API-KEY\")" +  ENDL + ENDL);
        }


        if ("PathQuery".equals(queryClassName)) {

            if (query.getDescription() != null && !"".equals(query.getDescription())) {
                sb.append("# query description - " + query.getDescription() + ENDL + ENDL);
            }

            sb.append("# Get a new query from the service you will be querying:"  + ENDL);
            sb.append("query = service.new_query()" + ENDL + ENDL);

            if (query.getView() == null || query.getView().isEmpty()) {
                return INVALID_QUERY;
            } else {
                sb.append("# The view specifies the output columns" + ENDL);
                sb.append("query.add_view(");
                StringBuffer viewLine = new StringBuffer();
                listFormatUtil(viewLine, query.getView());
                if (viewLine.toString().length() <= 74) {
                    sb.append(viewLine.toString());
                } else {
                    sb.append(ENDL);
                    printLine(sb, INDENT, viewLine.toString());
                }
                sb.append(")" + ENDL + ENDL);
            }

            // Add orderBy
            if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) { // no sort order
                if ( // The default
                    query.getOrderBy().size() == 1
                    && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0))
                    && query.getOrderBy().get(0).getDirection() == OrderDirection.ASC) {
                    sb.append("# Uncomment and edit the line below (the default) to select a custom sort order:" + ENDL);
                    sb.append("# ");
                } else {
                    sb.append("# Your custom sort order is specified with the following code:" + ENDL);
                }
                for (OrderElement oe : query.getOrderBy()) {
                    sb.append("query.add_sort_order(");
                    sb.append("\"" + oe.getOrderPath() + "\", \"" + oe.getDirection() + "\"");
                    sb.append(")" + ENDL);
                }
                sb.append(ENDL);
            }

            // Add constraints
            if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
                // Add comments for constraints
                sb.append("# You can edit the constraint values below" + ENDL);

                int coded_queries = 0;
                List<String> uncoded_query_texts = new ArrayList<String>();
                List<String> coded_query_texts = new ArrayList<String>();

                for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                    PathConstraint pc = entry.getKey();
                    if (entry.getValue() != null) {
                        coded_queries++;
                        coded_query_texts.add(pathContraintUtil(pc, entry.getValue()));
                    } else {
                        uncoded_query_texts.add(pathContraintUtil(pc, entry.getValue()));
                    }
                }
                // Subclass constraints must come first or the query will break
                for (String text: uncoded_query_texts) {
                    sb.append(text);
                }
                for (String text: coded_query_texts) {
                    sb.append(text);
                }
                sb.append(ENDL);

                // Add constraintLogic
                if (query.getConstraintLogic() != null
                    && !"".equals(query.getConstraintLogic())) {
                    String logic = query.getConstraintLogic();
                    if (coded_queries <= 1 || logic.indexOf("or") == -1) {
                        sb.append("# Uncomment and edit the code below to specify your own custom logic:" + ENDL + "# ");
                    } else {
                        sb.append("# Your custom constraint logic is specified with the code below:" + ENDL);
                    }
                    sb.append("query.set_logic(\"" + logic + "\")" + ENDL + ENDL);
                }
            }

            if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
                sb.append("# Join status" + ENDL);
                for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                    sb.append("query.add_join(\"" + entry.getKey() + "\", \"" + entry.getValue()
                        + "\")" + ENDL);
                }
            }
            sb.append("for row in query.rows():" + ENDL);
            sb.append(INDENT + "print row" + ENDL);

        } else if ("TemplateQuery".equals(queryClassName)) {

            String templateName = ((TemplateQuery) query).getName();
            String description = ((TemplateQuery) query).getDescription();
            Map<PathConstraint, String> allConstraints = query.getConstraints();
            List<PathConstraint> editableConstraints = ((TemplateQuery) query)
                .getEditableConstraints();
            StringBuffer constraints = new StringBuffer();
            StringBuffer constraintComments = new StringBuffer();

            constraintComments.append("# You can edit the constraint values below" + ENDL);

            Iterator<PathConstraint> conIter = editableConstraints.iterator();
            while (conIter.hasNext()) {
                PathConstraint pc = conIter.next();
                // Add comments for constraints
                String path = pc.getPath();

                String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                if ("PathConstraintBag".equals(className)) {
                    return TEMPLATE_BAG_CONSTRAINT;
                }
                if ("PathConstraintLoop".equals(className)) {
                    return LOOP_CONSTRAINT;
                }
                String opCode = allConstraints.get(pc);
                constraintComments.append("# " + opCode + INDENT + path);
                String constraintDes = ((TemplateQuery) query).getConstraintDescription(pc);
                if (constraintDes == null || "".equals(constraintDes)) {
                    constraintComments.append(ENDL);
                } else {
                    constraintComments.append(INDENT + constraintDes + ENDL);
                }
                constraints.append(templateConstraintUtil(pc, opCode));
                if (conIter.hasNext()) {
                    constraints.append(",");
                }
                constraints.append(ENDL);

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
            sb.append(INDENT + "print row" + ENDL);
        }

        return sb.toString();
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
     * @param pc
     *            PathConstraint object
     * @return a string for constraints source code
     */
    private String pathContraintUtil(PathConstraint pc, String code) {
        // Ref to Constraints
        StringBuffer sb = new StringBuffer("query.add_constraint(");

        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        ConstraintOp op = pc.getOp();

        sb.append("\"" + path + "\", ");

        if ("PathConstraintSubclass".equals(className)) {
            // can not test from webapp
            String type = ((PathConstraintSubclass) pc).getType();
            sb.append("\"" + type + "\"");
        } else if ("PathConstraintLoop".equals(className)) {
            if (op.equals(ConstraintOp.EQUALS)) {
                sb.append("\"IS\"");
            } else {
                sb.append("\"IS NOT\"");
            }
        } else {
            sb.append("\"" + op.toString() + "\"");
        }

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            sb.append(", \"" + value + "\"");
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();

            sb.append(", \"" + value + "\", \"" + extraValue + "\"");
        }

        if ("PathConstraintBag".equals(className)) {
            String list = ((PathConstraintBag) pc).getBag();
            sb.append(", \"" + list + "\"");
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
        }

        if ("PathConstraintMultiValue".equals(className)) {
            sb.append(", [");
            Collection<String> values = ((PathConstraintMultiValue) pc).getValues();
            listFormatUtil(sb, values);
            sb.append("]");
        }

        if ("PathConstraintLoop".equals(className)) {
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            sb.append(", \"" + loopPath + "\"");
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
    private String templateConstraintUtil(PathConstraint pc, String opCode) {
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
            // not supported
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
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
            // not handled
        }

        if ("PathConstraintLoop".equals(className)) {
            // not supported
        }
        return null;
    }
}
