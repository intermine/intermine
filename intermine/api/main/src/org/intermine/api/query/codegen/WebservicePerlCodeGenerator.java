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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.intermine.api.template.TemplateQuery;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

/**
 * This Class generates Perl source code of web service client for path query and template query.
 *
 * @author Fengyuan Hu
 */
public class WebservicePerlCodeGenerator implements WebserviceCodeGenerator
{
    protected static final String TEST_STRING = "This is a Java test string...";
    protected static final String INVALID_QUERY = "Invalid query. No fields selected for output...";
    protected static final String NULL_QUERY = "Invalid query. Query can not be null...";
    protected static final String PATH_BAG_CONSTRAINT = "This query contains a list constraint, "
        + "which is currently not supported...";
    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
        + "constraint, which is currently not supported...";
    protected static final String LOOP_CONSTRAINT = "Loop path constraint is not supported "
        + "at the moment...";

    protected static final String INDENT = "    ";
    protected static final String SPACE = " ";
    protected static final String ENDL = System.getProperty("line.separator");

    /**
     * This method will generate web service source code in Perl from a path query
     * or template query.
     *
     * @param wsCodeGenInfo a WebserviceCodeGenInfo object
     * @return web service source code in a string
     */
    public String generate(WebserviceCodeGenInfo wsCodeGenInfo) {

        PathQuery query = wsCodeGenInfo.getQuery();
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL();
        String projectTitle = wsCodeGenInfo.getProjectTitle();
        String perlWSModuleVer = wsCodeGenInfo.getPerlWSModuleVer();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        String queryClassName = TypeUtil.unqualifiedName(query.getClass().toString());

        StringBuffer sb = new StringBuffer();

        if ("PathQuery".equals(queryClassName)) {
            // Add use Webservice::InterMine
            sb.append("use Webservice::InterMine " + perlWSModuleVer + " '"
                            + serviceBaseURL + "/service';" + ENDL + ENDL)
                .append("# This is an automatically generated script to run the " + projectTitle
                        + " query" + ENDL)
                    .append("# You should install the Webservice::InterMine modules to run this "
                            + "example, e.g. sudo cpan Webservice::InterMine"
                            + ENDL + ENDL);

            if (query.getDescription() == null || "".equals(query.getDescription())) {
                sb.append("# query description - no description" + ENDL);
            } else {
                sb.append("# query description - " + query.getDescription() + ENDL + ENDL);
            }

            sb.append("my $query = Webservice::InterMine->new_query;" + ENDL + ENDL);


            // Add a name and a description is purely optional
//            sb.append("## Specifying a name and a description is purely optional" + ENDL)
//                .append("$query->name('<Query Name>');" + ENDL);
//
//            if (query.getDescription() == null) {
//                sb.append("$query->description('<Query Description>');" + ENDL);
//            } else {
//                sb.append(query.getDescription() + ENDL);
//            }
//
//            sb.append(ENDL);

            // Add views
            if (query.getView() == null || query.getView().isEmpty()) {
                return INVALID_QUERY;
            } else {
                sb.append("# The view specifies the output columns" + ENDL)
                    .append("$query->add_view(qw/" + ENDL);
                for (String pathString : query.getView()) {
                    sb.append(INDENT + pathString + ENDL);
                }
                sb.append("/);" + ENDL);
            }

            sb.append(ENDL);

            // Add orderBy
            if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
                sb.append("# Sort by" + ENDL);
                for (OrderElement oe : query.getOrderBy()) {
                    sb.append("$query->set_sort_order('"
                            + oe.getOrderPath() + "' => '" + oe.getDirection() + "');"
                            + ENDL);
                }
                sb.append(ENDL);
            }


            // Add constraints
            if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
                // Add comments for constraints
                sb.append("# You can edit the constraint values below" + ENDL);

                if (query.getConstraints().size() == 1) { // no logic
                    PathConstraint pc = query.getConstraints().entrySet()
                            .iterator().next().getKey();
                    String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                    if ("PathConstraintBag".equals(className)) {
                        return PATH_BAG_CONSTRAINT;
                    }
                    if ("PathConstraintLoop".equals(className)) {
                        return LOOP_CONSTRAINT;
                    }
                    sb.append(pathContraintUtil(pc));
                } else {
                    for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                        PathConstraint pc = entry.getKey();
                        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                        if ("PathConstraintBag".equals(className)) {
                            return PATH_BAG_CONSTRAINT;
                        }
                        if ("PathConstraintLoop".equals(className)) {
                            return LOOP_CONSTRAINT;
                        }
                        // Insert "code => 'A'"
                        StringBuffer constStr = new StringBuffer(pathContraintUtil(pc));
                        int idx = pathContraintUtil(pc).indexOf(");");
                        constStr.insert(idx, INDENT + "code => '" + entry.getValue() + "'," + ENDL);
                        sb.append(constStr.toString() + ENDL);
                    }

                    // Add constraintLogic
                    if (query.getConstraintLogic() != null
                            && !"".equals(query.getConstraintLogic())) {
                        sb.append("# Constraint Logic" + ENDL);
                        sb.append("$query->logic('" + query.getConstraintLogic() + "');"
                                + ENDL);
                    }
                }

                sb.append(ENDL);
            }

            // Add join status
            if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
                sb.append("# Join status" + ENDL);
                for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                    sb.append("$query->add_join(" + ENDL
                            + INDENT + "path => '" + entry.getKey() + "'," + ENDL
                            + INDENT + "style => '" + entry.getValue() + "'," + ENDL
                            + ");" + ENDL);
                }

                sb.append(ENDL);
            }

            // Add print results
            sb.append("print $query->results(as => 'string').\"\\n\";");
            sb.append(ENDL);

        } else if ("TemplateQuery".equals(queryClassName)) {

            String templateName = ((TemplateQuery) query).getName();
            String description = ((TemplateQuery) query).getDescription();
            Map<PathConstraint, String> allConstraints = query.getConstraints();
            List<PathConstraint> editableConstraints = ((TemplateQuery) query)
                    .getEditableConstraints();
            StringBuffer constraints = new StringBuffer();
            StringBuffer constraintComments = new StringBuffer();

            constraintComments.append("# You can edit the constraint values below" + ENDL);

            for (PathConstraint pc : editableConstraints) {
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
            }

            sb.append("use Webservice::InterMine " + perlWSModuleVer + " '"
                    + serviceBaseURL + "/service';" + ENDL + ENDL)
                .append("# This is an automatically generated script to run the " + projectTitle
                    + " template" + ENDL)
                .append("# You should install the Webservice::InterMine modules to run this "
                        + "example, e.g. sudo cpan Webservice::InterMine"
                        + ENDL + ENDL)
                .append("# template name - " + templateName + ENDL);
            if (description == null || "".equals(description)) {
                sb.append("# template description - no description" + ENDL + ENDL);
            } else {
                sb.append("# template description - " + description + ENDL + ENDL);
            }
            sb.append("my $template = Webservice::InterMine->template('"
                            + templateName + "')" + ENDL)
                .append(INDENT + "or die 'Could not find template';" + ENDL)
                .append(ENDL)
                .append(constraintComments.toString() + ENDL)
                .append("my $results = $template->results_with(" + ENDL)
                .append(INDENT + "as     => 'string'," + ENDL)
                .append(constraints.toString())
                .append(");" + ENDL)
                .append(ENDL)
                .append("print $results.\"\\n\";" + ENDL);
        }

        return sb.toString();
    }

    /**
     * This method helps to generate constraint source code for PathQuery
     * @param pc PathConstraint object
     * @return a string for constraints source code
     */
    private String pathContraintUtil(PathConstraint pc) {
        // Ref to Constraints
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        ConstraintOp op = pc.getOp();

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            if (op.equals(ConstraintOp.EQUALS)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '='," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.NOT_EQUALS)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '!='," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.MATCHES)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => 'LIKE'," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.DOES_NOT_MATCH)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => 'NOT LIKE'," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.LESS_THAN)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '<'," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.LESS_THAN_EQUALS)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '<='," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.GREATER_THAN)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '>'," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.GREATER_THAN_EQUALS)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '>='," + ENDL
                    + INDENT + "value => '" + value + "'," + ENDL
                    + ");" + ENDL;
            }
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();

            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path  => '" + path + "'," + ENDL
                + INDENT + "op    => 'LOOKUP'," + ENDL
                + INDENT + "value => '" + value + "'," + ENDL
                + INDENT + "extra_value => '" + extraValue + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintBag".equals(className)) {
            // Not supported
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
        }

        if ("PathConstraintMultiValue".equals(className)) {
            if (op.equals(ConstraintOp.ONE_OF)) {
                StringBuffer values = new StringBuffer();
                for (String aValue : ((PathConstraintMultiValue) pc).getValues()) {
                    values.append(INDENT + INDENT + "'" + aValue + "'," + ENDL);
                }
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => 'ONE OF'," + ENDL
                    + INDENT + "value => [" + ENDL
                    + values.toString()
                    + INDENT + "]," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.NONE_OF)) {
                StringBuffer values = new StringBuffer();
                for (String aValue : ((PathConstraintMultiValue) pc).getValues()) {
                    values.append(INDENT + INDENT + "'" + aValue + "'," + ENDL);
                }
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => 'NONE OF'," + ENDL
                    + INDENT + "value => [" + ENDL
                    + values.toString()
                    + INDENT + "]," + ENDL
                    + ");" + ENDL;
            }
        }

        if ("PathConstraintNull".equals(className)) {
            if (op.equals(ConstraintOp.IS_NULL)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => 'IS NULL'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.IS_NOT_NULL)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => 'IS NOT NULL'," + ENDL
                    + ");" + ENDL;
            }
        }

        if ("PathConstraintSubclass".equals(className)) {
            // can not test from webapp
            String type = ((PathConstraintSubclass) pc).getType();
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path  => '" + path + "'," + ENDL
                + INDENT + "type => '" + type + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintLoop".equals(className)) {
            // not supported
            /*
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            if (op.equals(ConstraintOp.EQUALS)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '='," + ENDL
                    + INDENT + "loop_path => '" + loopPath + "'," + ENDL
                    + ");" + ENDL;
            }

            if (op.equals(ConstraintOp.NOT_EQUALS)) {
                return
                    "$query->add_constraint(" + ENDL
                    + INDENT + "path  => '" + path + "'," + ENDL
                    + INDENT + "op    => '!='," + ENDL
                    + INDENT + "loop_path => '" + loopPath + "'," + ENDL
                    + ");" + ENDL;
            }
            */
        }
        return null;
    }

    /**
     * This method helps to generate Template Parameters (predefined constraints) source code for
     * TemplateQuery
     * @param pc PathConstraint object
     * @param opCode operation code
     * @return a line of source code
     */
    private String templateConstraintUtil(PathConstraint pc, String opCode) {
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String op = pc.getOp().toString();

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + ENDL
                + INDENT + "value" + opCode + " => '" + value + "'," + ENDL;
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();
            return
                INDENT + "op" + opCode + "    => 'LOOKUP'," + ENDL
                + INDENT + "value" + opCode + " => '" + value + "'," + ENDL
                + INDENT + "extra_value" + opCode + " => '" + extraValue + "'," + ENDL;
        }

        if ("PathConstraintBag".equals(className)) {
            // not supported
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
        }

        if ("PathConstraintMultiValue".equals(className)) {
            StringBuffer values = new StringBuffer();
            for (String aValue : ((PathConstraintMultiValue) pc).getValues()) {
                values.append(INDENT + INDENT + "'" + aValue + "'," + ENDL);
            }
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + ENDL
                + INDENT + "value" + opCode + " => [" + ENDL
                + values.toString()
                + INDENT + "]," + ENDL;
        }

        if ("PathConstraintNull".equals(className)) {
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + ENDL;
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

