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

import java.util.ArrayList;
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

/**
 * This Class generates Perl source code of web service client for path query and template query.
 *
 * @author Fengyuan Hu
 */
public class WebservicePerlCodeGenerator implements WebserviceCodeGenerator
{
    protected static final String TEST_STRING = "This is a Java test string...";
    protected static final String INVALID_QUERY = "Invalid query. No fields selected for output.";
    protected static final String NULL_QUERY = "Invalid query. Query can not be null.";
    protected static final String PATH_BAG_CONSTRAINT = "This query contains a list constraint, "
        + "which is currently not supported...";
    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
        + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT = "Loop path constraints are not supported "
        + "in templates";

    protected static final String INDENT = "    ";
    protected static final String SPACE = " ";
    protected static final String ENDL = System.getProperty("line.separator");

    protected static final String BOILERPLATE =
        "#!/usr/bin/perl" + ENDL
        + ENDL
        + "use strict;" + ENDL
        + "use warnings;" + ENDL + ENDL;

    protected static final String INTRO =
          "######################################################################" + ENDL
        + "# This is an automatically generated script to run your query." + ENDL
        + "# To use it you will require the InterMine Perl client libraries." + ENDL
        + "# These can be installed from CPAN, using your preferred client, eg:" + ENDL
        + "#" + ENDL
        + "#" + INDENT + "sudo cpan Webservice::InterMine" + ENDL
        + "#" + ENDL
        + "# For help using these modules, please see these resources:" + ENDL
        + "#" + ENDL
        + "#  * http://search.cpan.org/perldoc?Webservice::InterMine" + ENDL
        + "#       - API reference" + ENDL
        + "#  * http://search.cpan.org/perldoc?Webservice::InterMine::Cookbook" + ENDL
        + "#       - A How-To manual" + ENDL
        + "#  * http://www.intermine.org/wiki/PerlWebServiceAPI" + ENDL
        + "#       - General Usage" + ENDL
        + "#  * http://www.intermine.org/wiki/WebService" + ENDL
        + "#       - Reference documentation for the underlying REST API" + ENDL
        + "#" + ENDL
        + "######################################################################" + ENDL
        + ENDL;

    protected static final String RESULTS_PRINTING =
           "while (<$results>) {" + ENDL
         + INDENT + "print $_, \"\\n\";" + ENDL
         + "}" + ENDL
         + ENDL;
    /**
     * This method will generate web service source code in Perl from a path query
     * or template query.
     *
     * @param wsCodeGenInfo a WebserviceCodeGenInfo object
     * @return web service source code in a string
     */
    @Override
    public String generate(WebserviceCodeGenInfo wsCodeGenInfo) {

        PathQuery query = wsCodeGenInfo.getQuery();
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL() + "/service";
        String projectTitle = wsCodeGenInfo.getProjectTitle();
        String perlWSModuleVer = wsCodeGenInfo.getPerlWSModuleVer();

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        String queryClassName = TypeUtil.unqualifiedName(query.getClass().toString());

        StringBuffer sb = new StringBuffer(BOILERPLATE).append(INTRO);
        sb.append("# The following import statement sets " + projectTitle + " as your default"
                + ENDL);
        if (wsCodeGenInfo.isPublic()) {
            sb.append("use Webservice::InterMine " + perlWSModuleVer + " '" + serviceBaseURL + "';"
                    + ENDL);
        } else {
            sb.append("# You must also supply your login details here to access this query" + ENDL);
            sb.append("use Webservice::InterMine " + perlWSModuleVer + " '" + serviceBaseURL + "', "
                    + "'" + wsCodeGenInfo.getUserName() + "', YOUR-PASSWORD;" + ENDL);
        }
        sb.append(ENDL);

        if ("PathQuery".equals(queryClassName)) {
            // Import the client library

            if (query.getDescription() != null && !"".equals(query.getDescription())) {
                printLine(sb, "# ", "Description: " + query.getDescription());
                sb.append(ENDL);
            }

            sb.append("my $query = Webservice::InterMine->new_query;" + ENDL + ENDL);

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
            if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) { // no sort order
                if ( // The default
                    query.getOrderBy().size() == 1
                        && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0))
                        && query.getOrderBy().get(0).getDirection() == OrderDirection.ASC) {
                    sb.append("# edit the line below to change the sort order:" + ENDL);
                    sb.append("# ");
                } else {
                    sb.append("# Your custom sort order is specified with the following code:"
                            + ENDL);
                }
                for (OrderElement oe : query.getOrderBy()) {
                    sb.append("$query->add_sort_order(");
                    sb.append("'" + oe.getOrderPath() + "', '" + oe.getDirection() + "'");
                    sb.append(");" + ENDL);
                }
                sb.append(ENDL);
            }

            // Add constraints
            if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
                // Add comments for constraints
                sb.append("# You can edit the constraint values below" + ENDL);

                int codedQueries = 0;
                List<String> uncodedQueryTexts = new ArrayList<String>();
                List<String> codedQueryTexts = new ArrayList<String>();

                for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                    PathConstraint pc = entry.getKey();
                    if (entry.getValue() != null) {
                        codedQueries++;
                        codedQueryTexts.add(pathContraintUtil(pc, entry.getValue()));
                    } else {
                        uncodedQueryTexts.add(pathContraintUtil(pc, entry.getValue()));
                    }
                }
                // Subclass constraints must come first or the query will break
                for (String text: uncodedQueryTexts) {
                    sb.append(text);
                }
                for (String text: codedQueryTexts) {
                    sb.append(text);
                }
                sb.append(ENDL);

                // Add constraintLogic
                if (query.getConstraintLogic() != null
                    && !"".equals(query.getConstraintLogic())) {
                    String logic = query.getConstraintLogic();
                    if (codedQueries <= 1 || logic.indexOf("or") == -1) {
                        sb.append("# Edit the code below to specify your own custom logic:" + ENDL
                                + "# ");
                    } else {
                        sb.append("# Your custom logic is specified with the code below:" + ENDL);
                    }
                    sb.append("$query->set_logic(\"" + logic + "\");" + ENDL + ENDL);
                }
            }

            // Add join status
            if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
                sb.append("# Join status" + ENDL);
                for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                    if (entry.getValue() == OuterJoinStatus.OUTER) {
                        sb.append("$query->add_outer_join('" + entry.getKey() + "');" + ENDL);
                    }
                }

                sb.append(ENDL);
            }

            // Add print results
            sb.append("$query->show;" + ENDL);


        } else if ("TemplateQuery".equals(queryClassName)) {

            TemplateQuery template = (TemplateQuery) query;
            String templateName = template.getName();
            String description = template.getDescription();
            Map<PathConstraint, String> allConstraints = template.getConstraints();
            List<PathConstraint> editableConstraints = template.getEditableConstraints();

            if (description != null && !"".equals(description)) {
                printLine(sb, "# ", "Description: " + description);
                sb.append(ENDL);
            }

            sb.append("my $template = Webservice::InterMine->template('"
                            + templateName + "')" + ENDL)
                .append(INDENT + "or die 'Could not find template';" + ENDL)
                .append(ENDL)
                .append("$template->show_with(" + ENDL);

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
                String constraintInfo = opCode + ":  " + path;
                String constraintDes = ((TemplateQuery) query).getConstraintDescription(pc);
                if (constraintDes != null && !"".equals(constraintDes)) {
                    constraintInfo += " - " + constraintDes;
                }
                printLine(sb, INDENT + "# ", constraintInfo);

                sb.append(templateConstraintUtil(pc, opCode));
            }

            sb.append(");" + ENDL);
        }

        return sb.toString();
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
     * This method helps to generate constraint source code for PathQuery
     * @param pc PathConstraint object
     * @return a string for constraints source code
     */
    private String pathContraintUtil(PathConstraint pc, String code) {
        // Ref to Constraints
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        ConstraintOp op = pc.getOp();

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path  => '" + path + "'," + ENDL
                + INDENT + "op    => '" + op.toString() + "'," + ENDL
                + INDENT + "value => '" + value + "'," + ENDL
                + INDENT + "code  => '" + code + "'," + ENDL
                + ");" + ENDL;

        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();

            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path        => '" + path + "'," + ENDL
                + INDENT + "op          => 'LOOKUP'," + ENDL
                + INDENT + "value       => '" + value + "'," + ENDL
                + INDENT + "extra_value => '" + extraValue + "'," + ENDL
                + INDENT + "code        => '" + code + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintBag".equals(className)) {
            String list = ((PathConstraintBag) pc).getBag();
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path  => '" + path + "'," + ENDL
                + INDENT + "op    => '" + op.toString() + "'," + ENDL
                + INDENT + "value => '" + list + "'," + ENDL
                + INDENT + "code  => '" + code + "'," + ENDL
                + ");" + ENDL;
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
                "$query->add_constraint(" + ENDL
                + INDENT + "path   => '" + path + "'," + ENDL
                + INDENT + "op     => '" + op.toString() + "'," + ENDL
                + INDENT + "values => [" + ENDL
                + values.toString()
                + INDENT + "]," + ENDL
                + INDENT + "code  => '" + code + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintNull".equals(className)) {
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path => '" + path + "'," + ENDL
                + INDENT + "op   => '" + op.toString() + "'," + ENDL
                + INDENT + "code => '" + code + "'," + ENDL
                + ");" + ENDL;

        }

        if ("PathConstraintSubclass".equals(className)) {
            String type = ((PathConstraintSubclass) pc).getType();
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path => '" + path + "'," + ENDL
                + INDENT + "type => '" + type + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintLoop".equals(className)) {
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            String opStr = op.toString();
            if (ConstraintOp.EQUALS.equals(op)) {
                opStr = "IS";
            } else if (ConstraintOp.NOT_EQUALS.equals(op)) {
                opStr = "IS NOT";
            }
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path      => '" + path + "'," + ENDL
                + INDENT + "op        => '" + opStr + "'," + ENDL
                + INDENT + "loop_path => '" + loopPath + "'," + ENDL
                + INDENT + "code      => '" + code + "'," + ENDL
                + ");" + ENDL;
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
            String ret =
                INDENT + "op" + opCode + "    => 'LOOKUP'," + ENDL
                + INDENT + "value" + opCode + " => '" + value + "'," + ENDL;
            if (extraValue != null && !"".equals(extraValue)) {
                ret += INDENT + "extra_value" + opCode + " => '" + extraValue + "'," + ENDL;
            }
            return ret;
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

