package org.intermine.api.query.codegen;

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
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

public class WebservicePythonCodeGenerator implements WebserviceCodeGenerator
{

    protected static final String INVALID_QUERY           = "Invalid query. No fields selected for output...";
    protected static final String NULL_QUERY              = "Invalid query. Query can not be null...";

    protected static final String INDENT                  = "    ";
    protected static final String SPACE                   = " ";
    protected static final String ENDL                    = System.getProperty("line.separator");

    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
                                                              + "constraint, which is currently not supported...";
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
    public String generate(WebserviceCodeGenInfo wsCodeGenInfo) {

        PathQuery query = wsCodeGenInfo.getQuery();
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL();
        String projectTitle = wsCodeGenInfo.getProjectTitle();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        String queryClassName = TypeUtil.unqualifiedName(query.getClass().toString());

        StringBuffer sb = new StringBuffer();
        sb.append("#!/usr/bin/env python" + ENDL);
        sb.append("from intermine.service import Service");
        sb.append(ENDL + ENDL);
        sb.append("# This is an automatically generated script to run your query" + ENDL);
        sb.append("# to use it you will require the intermine python client." + ENDL);
        sb.append("# To install the client, run the following command from a terminal:" + ENDL);
        sb.append("#" + ENDL);
        sb.append("#     easyinstall intermine-webservice" + ENDL);
        sb.append("#" + ENDL);
        sb.append("# For further documentation you can visit:" + ENDL);
        sb.append("#     http://www.intermine.org/PythonClient" + ENDL + ENDL);
        sb.append("service = Service(\"" + serviceBaseURL + "\")" + ENDL);

        if ("PathQuery".equals(queryClassName)) {

            if (query.getDescription() == null || "".equals(query.getDescription())) {
                sb.append("# query description - no description" + ENDL + ENDL);
            } else {
                sb.append("# query description - " + query.getDescription() + ENDL + ENDL);
            }

            sb.append("# Queries are associated with the service they query:");
            sb.append("query = service.new_query()" + ENDL + ENDL);

            if (query.getView() == null || query.getView().isEmpty()) {
                return INVALID_QUERY;
            } else {
                sb.append("# The view specifies the output columns" + ENDL);
                sb.append("query.add_view(");
                listFormatUtil(sb, query.getView());
                sb.append(")" + ENDL);
            }

            // Add orderBy
            if (query.getOrderBy() != null // unset
                && !query.getOrderBy().isEmpty() // no sort order
                && !( // The default
                query.getOrderBy().size() == 1
                    && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0)) && query
                    .getOrderBy().get(0).getDirection() != OrderDirection.ASC)) {
                sb.append("# Determine the sort order for the results" + ENDL);
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

                if (query.getConstraints().size() == 1) { // no logic
                    PathConstraint pc = query.getConstraints().entrySet().iterator().next()
                        .getKey();
                    String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                    sb.append(pathContraintUtil(pc, null));
                } else {
                    for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                        PathConstraint pc = entry.getKey();
                        String className = TypeUtil.unqualifiedName(pc.getClass().toString());

                        // Insert "code => 'A'"
                        sb.append(pathContraintUtil(pc, entry.getValue()));
                    }

                    // Add constraintLogic
                    if (query.getConstraintLogic() != null
                        && !"".equals(query.getConstraintLogic())) {
                        sb.append("# Constraint Logic" + ENDL);
                        sb.append("query.set_logic(\"" + query.getConstraintLogic() + "\")" + ENDL);
                    }
                }

                sb.append(ENDL);
            }

            if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
                sb.append("# Join status" + ENDL);
                for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                    sb.append("query.add_join(\"" + entry.getKey() + "\", \"" + entry.getValue()
                        + "\")" + ENDL);
                }
            }
            sb.append(ENDL);
            sb.append("print query.results()" + ENDL);

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
                    constraintComments.append(INDENT + "no constraint description" + ENDL);
                } else {
                    constraintComments.append(INDENT + constraintDes + ENDL);
                }
                constraints.append(templateConstraintUtil(pc, opCode));
                if (conIter.hasNext()) {
                    constraints.append(",");
                }
                constraints.append(ENDL);

            }

            if (description == null || "".equals(description)) {
                sb.append("# template description - no description" + ENDL + ENDL);
            } else {
                sb.append("# template description - " + description + ENDL + ENDL);
            }
            sb.append("template = service.get_template('" + templateName + "')" + ENDL + ENDL);
            sb.append(constraintComments.toString() + ENDL);
            sb.append("results = template.results(" + ENDL);
            sb.append(constraints.toString() + ")" + ENDL);
            sb.append("print results" + ENDL);
        }

        return sb.toString();
    }

    private void listFormatUtil(StringBuffer sb, Collection<String> coll) {
        Iterator<String> it = coll.iterator();
        while (it.hasNext()) {
            sb.append("\"" + it.next() + "\"");
            if (it.hasNext()) {
                sb.append(",");
            }
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

        if ("PathConstraintAttribute".equals(className)) {

            if (op.equals(ConstraintOp.EQUALS)) {
                sb.append("\"=\", ");
            } else if (op.equals(ConstraintOp.NOT_EQUALS)) {
                sb.append("\"!=\", ");
            } else if (op.equals(ConstraintOp.MATCHES)) {
                sb.append("\"=\", ");
            } else if (op.equals(ConstraintOp.DOES_NOT_MATCH)) {
                sb.append("\"!=\", ");
            } else if (op.equals(ConstraintOp.LESS_THAN)) {
                sb.append("\"<\", ");
            } else if (op.equals(ConstraintOp.LESS_THAN_EQUALS)) {
                sb.append("\"<=\", ");
            } else if (op.equals(ConstraintOp.GREATER_THAN)) {
                sb.append("\">=\", ");
            } else if (op.equals(ConstraintOp.GREATER_THAN_EQUALS)) {
                sb.append("\">=\", ");
            }

            String value = ((PathConstraintAttribute) pc).getValue();

            sb.append("\"" + value + "\"");
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();

            sb.append("\"LOOKUP\", \"" + value + "\", \"" + extraValue + "\"");
        }

        if ("PathConstraintBag".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();

            if (op.equals(ConstraintOp.IN)) {
                sb.append("\"IN\", ");
            } else if (op.equals(ConstraintOp.NOT_IN)) {
                sb.append("\"NOT IN\", ");
            }

            sb.append("\"" + value + "\"");
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
        }

        if ("PathConstraintMultiValue".equals(className)) {
            if (op.equals(ConstraintOp.ONE_OF)) {
                sb.append("\"ONE OF\", ");
            } else if (op.equals(ConstraintOp.NONE_OF)) {
                sb.append("\"NONE OF\", ");
            }
            sb.append("[");
            Collection<String> values = ((PathConstraintMultiValue) pc).getValues();
            listFormatUtil(sb, values);
            sb.append("]");
        }

        if ("PathConstraintNull".equals(className)) {
            if (op.equals(ConstraintOp.IS_NULL)) {
                sb.append("\"IS NULL\"");
            } else if (op.equals(ConstraintOp.IS_NOT_NULL)) {
                sb.append("\"IS NOT NULL\"");
            }
        }

        if ("PathConstraintSubclass".equals(className)) {
            // can not test from webapp
            String type = ((PathConstraintSubclass) pc).getType();
            sb.append("\"" + type + "\"");
        }

        if ("PathConstraintLoop".equals(className)) {
            if (op.equals(ConstraintOp.EQUALS)) {
                sb.append("\"=\", ");
            } else if (op.equals(ConstraintOp.NOT_EQUALS)) {
                sb.append("\"!=\", ");
            }
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            sb.append("\"" + loopPath + "\"");
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

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            return INDENT + opCode + "{\"op\": \"" + op + "\", \"value\": \"" + value + "\"}";
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();
            return INDENT + opCode + "{\"op\": \"LOOKUP\", \"value\": \"" + value
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
            return INDENT + opCode + "{\"op\": \"" + op + "\", \"values\": \"" + sb.toString()
                + "\"}";
        }

        if ("PathConstraintNull".equals(className)) {
            return INDENT + opCode + "{\"op\": \"" + op + "\"}";
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