package org.intermine.api.query.codegen;

import java.lang.String;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;
import org.intermine.util.TypeUtil;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * This Class generates Perl source code of web service client for path query and template query.
 *
 * @author Fengyuan Hu
 */
public class WebservicePerlCodeGenerator implements WebserviceCodeGenerator
{
    protected static final String TEST_STRING = "This is a Java test string...";
    protected static final String INVALID_QUERY 
        = "# Invalid query.\n"
        + "# ==============\n"
        + "# The code to run this query could not be generated for the following reasons:\n";
    protected static final String PATH_BAG_CONSTRAINT = "This query contains a list constraint, "
        + "which is currently not supported...";
    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
        + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT = "Loop path constraints are not supported "
        + "in templates";

    protected static final String INDENT = "    ";
    protected static final String SPACE = " ";
    protected static final String ENDL = System.getProperty("line.separator");

    private static final String SUBCLASS_EXPLANATION 
        = "Type constraints must come before all mentions of the paths they constrain";
    private static final String INTERNAL_USE_CONSTRAINT 
        = "This query makes use of a constraint type that can only be used internally";

    private static final String SEPARATOR_CONSTANT
        = "# Set the output field separator as tab" + ENDL
        + "$, = \"\\t\";" + ENDL;

    private static final String NO_WARNINGS_UNDEF
        = "# Silence warnings when printing null fields" + ENDL
        + "no warnings ('uninitialized');" + ENDL;

    private static final String UNICODE_OUTPUT
        = "# Print unicode to standard out" + ENDL
        + "binmode(STDOUT, 'utf8');" + ENDL;

    private static final String SHEBANG = "#!/usr/bin/perl" + ENDL + ENDL;

    protected static final String BOILERPLATE 
        = "use strict;" + ENDL
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

    private String formatProblems(Collection<String> problems) {
        StringBuffer sb = new StringBuffer();
        for (String s: problems) {
            sb.append("#  * " + s + ENDL);
        }
        return sb.toString();
    }

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
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL();
        String projectTitle = wsCodeGenInfo.getProjectTitle();
        String perlWSModuleVer = wsCodeGenInfo.getPerlWSModuleVer();

        // query is null
        if (query == null) {
            return INVALID_QUERY + formatProblems(Arrays.asList("The query is null"));
        }

        if (!query.isValid()) {
            return INVALID_QUERY + formatProblems(query.verifyQuery());
        }

        StringBuffer sb = new StringBuffer(SHEBANG)
                                  .append(INTRO)
                                  .append(BOILERPLATE)
                                  .append(SEPARATOR_CONSTANT)
                                  .append(UNICODE_OUTPUT)
                                  .append(NO_WARNINGS_UNDEF)
                                  .append(ENDL);

        sb.append("# The following import statement sets " + projectTitle + " as your default"
                + ENDL);
        if (wsCodeGenInfo.isPublic()) {
            sb.append("use Webservice::InterMine " 
                    + perlWSModuleVer 
                    + " '" + serviceBaseURL + "';" + ENDL);
        } else {
            sb.append("# You must also supply your login details here to access this query" + ENDL);
            sb.append("use Webservice::InterMine " 
                    + perlWSModuleVer 
                    + " '" + serviceBaseURL + "', "
                    + "'YOUR-API-TOKEN';" + ENDL);
        }
        sb.append(ENDL);

        if (StringUtils.isNotBlank(query.getDescription())) {
            printLine(sb, "# ", "Description: " + query.getDescription());
            sb.append(ENDL);
        }

        try {
            if (query instanceof TemplateQuery) {
                generateTemplateQueryCode((TemplateQuery) query, sb);
            } else {
                generatePathQueryCode(query, sb);
            }
        } catch (InvalidQueryException e) {
            return INVALID_QUERY + formatProblems(e.getProblems());
        }

        return sb.toString();
    }

    private String q(String input) {
        if (input == null) {
            return "";
        } else {
            return "'" + input + "'";
        }
    }

    private String qq(String input) {
        if (input == null) {
            return "";
        } else {
            return "\"" + input + "\"";
        }
    }

    private String decapitate(String input) {
        if (input == null) {
            return "";
        } else {
            return input.substring(input.indexOf(".") + 1);
        }
    }

    private void generateTemplateQueryCode(TemplateQuery template, StringBuffer sb) throws InvalidQueryException {
        String name = template.getName();
        Map<PathConstraint, String> allConstraints = template.getConstraints();
        List<PathConstraint> editableConstraints = template.getEditableConstraints();

        if (editableConstraints.isEmpty()) {
            throw new InvalidQueryException("This template has no editable constraints");
        }

        sb.append("my $template = Webservice::InterMine->template(" + q(name) + ")" + ENDL)
            .append(INDENT + "or die 'Could not find a template called " + name + "';" + ENDL)
            .append(ENDL)
            .append("# Use an iterator to avoid having all rows in memory at once." + ENDL)
            .append("my $it = $template->results_iterator_with(" + ENDL);

        List<String> constraintProblems = new ArrayList<String>();
        for (PathConstraint pc : editableConstraints) {
            // Add comments for constraints
            String path = pc.getPath();

            String opCode = allConstraints.get(pc);
            String constraintInfo = opCode + ":  " + path;
            String constraintDes = template.getConstraintDescription(pc);
            if (StringUtils.isNotBlank(constraintDes)) {
                constraintInfo += " - " + constraintDes;
            }
            printLine(sb, INDENT + "# ", constraintInfo);

            try {
                sb.append(templateConstraintUtil(pc, opCode));
            } catch (UnhandledFeatureException e) {
                constraintProblems.add(e.getMessage());
            }
        }
        if (!constraintProblems.isEmpty()) {
            throw new InvalidQueryException(constraintProblems);
        }

        sb.append(");" + ENDL);
        sb.append(ENDL);
        printResults(template, sb);
    }

    private void generatePathQueryCode(PathQuery query, StringBuffer sb) throws InvalidQueryException {

        String rootClass = null;
        try {
            rootClass = query.getRootClass();
        } catch (PathException e) {
            throw new InvalidQueryException(e.getMessage());
        }

        sb.append("my $query = new_query(class => " + q(rootClass) + ");" + ENDL + ENDL);

        List<String> uncodedConstraints = new ArrayList<String>();
        List<String> codedConstraints = new ArrayList<String>();
        List<String> constraintProblems = new ArrayList<String>();
        if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
            for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                PathConstraint pc = entry.getKey();
                try {
                    if (entry.getValue() != null) {
                        codedConstraints.add(pathContraintUtil(pc, entry.getValue()));
                    } else {
                        uncodedConstraints.add(pathContraintUtil(pc, entry.getValue()));
                    }
                } catch (UnhandledFeatureException e) {
                    constraintProblems.add(e.getMessage());
                }
            }
        }
        if (!constraintProblems.isEmpty()) {
            throw new InvalidQueryException(constraintProblems);
        }

        if (!uncodedConstraints.isEmpty()) {
            sb.append("# " + SUBCLASS_EXPLANATION + ENDL);
            // Subclass constraints must come first or the query will break
            for (String text: uncodedConstraints) {
                sb.append(text);
            }
        }

        sb.append("# The view specifies the output columns" + ENDL)
            .append("$query->add_view(qw/" + ENDL);
        for (String pathString : query.getView()) {
            sb.append(INDENT + decapitate(pathString) + ENDL);
        }
        sb.append("/);" + ENDL);
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
                sb.append(q(decapitate(oe.getOrderPath())) + ", " + q(oe.getDirection().toString()));
                sb.append(");" + ENDL);
            }
            sb.append(ENDL);
        }

        // Add join status
        if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
            sb.append("# Outer Joins" + ENDL);
            sb.append("# (Show attributes of these relations if they exist, but do not require them to exist.)" + ENDL);
            for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                // Only outer joins need to be declared.
                if (entry.getValue() == OuterJoinStatus.OUTER) {
                    sb.append("$query->add_outer_join(" + q(decapitate(entry.getKey())) + ");" + ENDL);
                }
            }
            sb.append(ENDL);
        }

        for (String text: codedConstraints) {
            sb.append(text);
        }
        sb.append(ENDL);

        // Add constraintLogic
        if (codedConstraints.size() > 1 && StringUtils.isNotBlank(query.getConstraintLogic())) {
            String logic = query.getConstraintLogic();
            if (logic.indexOf("or") == -1) {
                sb.append("# Edit the code below to specify your own custom logic:" + ENDL
                        + "# ");
            } else {
                sb.append("# Your custom logic is specified with the code below:" + ENDL);
            }
            sb.append("$query->set_logic(" + q(logic) + ");" + ENDL + ENDL);
        }
        sb.append("# Use an iterator to avoid having all rows in memory at once." + ENDL);
        sb.append("my $it = $query->iterator();" + ENDL);
        printResults(query, sb);
    }

    private void printResults(PathQuery pq, StringBuffer sb) {
        sb.append("while (my $row = <$it>) {" + ENDL);
        StringBuffer currentLine = new StringBuffer(INDENT + "print");
        Iterator<String> it = pq.getView().iterator();
        while (it.hasNext()) {
            String toPrint = "$row->{" + q(decapitate(it.next())) + "}";
            if (it.hasNext()) {
                toPrint += ",";
            }
            if (currentLine.length() + toPrint.length() > 100) {
                sb.append(currentLine.toString() + ENDL);
                currentLine = new StringBuffer(INDENT + INDENT);
            }
            if (StringUtils.isNotBlank(currentLine.toString())) {
                currentLine.append(" ");
            }
            currentLine.append(toPrint);
        }
        sb.append(currentLine + ", " + qq("\\n") + ";" + ENDL);
        sb.append("}").append(ENDL);
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
    private String pathContraintUtil(PathConstraint pc, String code) throws UnhandledFeatureException {
        // Ref to Constraints
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        ConstraintOp op = pc.getOp();
        String value = PathConstraint.getValue(pc);
        String extraValue = PathConstraint.getExtraValue(pc);

        if ("PathConstraintAttribute".equals(className)
                || "PathConstraintBag".equals(className)) {
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path  => '" + path + "'," + ENDL
                + INDENT + "op    => '" + op.toString() + "'," + ENDL
                + INDENT + "value => '" + value + "'," + ENDL
                + INDENT + "code  => '" + code + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintLookup".equals(className)) {
            return
                "$query->add_constraint(" + ENDL
                + INDENT + "path        => '" + path + "'," + ENDL
                + INDENT + "op          => 'LOOKUP'," + ENDL
                + INDENT + "value       => '" + value + "'," + ENDL
                + INDENT + "extra_value => '" + extraValue + "'," + ENDL
                + INDENT + "code        => '" + code + "'," + ENDL
                + ");" + ENDL;
        }

        if ("PathConstraintIds".equals(className)) {
            throw new UnhandledFeatureException(INTERNAL_USE_CONSTRAINT + " (" + className + ")");
        }

        if ("PathConstraintMultiValue".equals(className)) {
            StringBuffer values = new StringBuffer();
            for (String aValue : ((PathConstraintMultiValue) pc).getValues()) {
                values.append(INDENT + INDENT + q(aValue) + "," + ENDL);
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
                + INDENT + "loop_path => '" + value + "'," + ENDL
                + INDENT + "code      => '" + code + "'," + ENDL
                + ");" + ENDL;
        }

        throw new UnhandledFeatureException("Unknown constraint type (" + className + ")");
    }

    /**
     * This method helps to generate Template Parameters (predefined constraints) source code for
     * TemplateQuery
     * @param pc PathConstraint object
     * @param opCode operation code
     * @return a line of source code
     */
    private String templateConstraintUtil(PathConstraint pc, String opCode) throws UnhandledFeatureException {
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String op = pc.getOp().toString();
        String value = PathConstraint.getValue(pc);
        String extraValue = PathConstraint.getExtraValue(pc);

        if ("PathConstraintAttribute".equals(className)
                || "PathConstraintBag".equals(className)) {
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + ENDL
                + INDENT + "value" + opCode + " => '" + value + "'," + ENDL;
        }

        if ("PathConstraintLookup".equals(className)) {
            String ret =
                INDENT + "op" + opCode + "    => 'LOOKUP'," + ENDL
                + INDENT + "value" + opCode + " => '" + value + "'," + ENDL;
            if (extraValue != null && !"".equals(extraValue)) {
                ret += INDENT + "extra_value" + opCode + " => '" + extraValue + "'," + ENDL;
            }
            return ret;
        }

        if ("PathConstraintIds".equals(className)
            || ("PathConstraintLoop".equals(className))) {
            throw new UnhandledFeatureException(INTERNAL_USE_CONSTRAINT + " (" + className + ")");
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
            throw new UnhandledFeatureException("Type constraints should not be editable");
        }

        throw new UnhandledFeatureException("Unknown constraint type (" + className + ")");
    }
}

