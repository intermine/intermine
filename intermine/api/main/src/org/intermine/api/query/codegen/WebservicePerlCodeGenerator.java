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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.intermine.metadata.ConstraintOp;
import org.intermine.metadata.TypeUtil;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.TemplateQuery;


/**
 * This Class generates Perl source code of web service client for path query and template query.
 *
 * @author Fengyuan Hu
 */
public class WebservicePerlCodeGenerator implements WebserviceCodeGenerator
{
    protected String endl = System.getProperty("line.separator");

    /**
     * @return error msg
     */
    protected String getInvalidQuery() {
        return "# Invalid query." + endl
                + "# ==============" + endl
                + "# The code to run this query could not be generated for the following reasons:"
                + endl;
    }
    protected static final String PATH_BAG_CONSTRAINT = "This query contains a list constraint, "
        + "which is currently not supported...";
    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
        + "constraint, which is currently not supported.";
    protected static final String LOOP_CONSTRAINT = "Loop path constraints are not supported "
        + "in templates";

    protected static final String INDENT = "    ";
    protected static final String SPACE = " ";

    private static final String SUBCLASS_EXPLANATION
        = "Type constraints must come before all mentions of the paths they constrain";
    private static final String INTERNAL_USE_CONSTRAINT
        = "This query makes use of a constraint type that can only be used internally";

    private String getSetFieldSeparator() {
        return "# Set the output field separator as tab" + endl
                + "$, = \"\\t\";" + endl;
    }

    private String getNoWarningsUndef() {
        return "# Silence warnings when printing null fields" + endl
                + "no warnings ('uninitialized');" + endl;
    }

    private String getUnicodeOutput() {
        return "# Print unicode to standard out" + endl + "binmode(STDOUT, 'utf8');" + endl;
    }

    private String getSheBang() {
        return "#!/usr/bin/perl" + endl + endl;
    }

    private String getBoilerPlate() {
        return "use strict;" + endl + "use warnings;" + endl + endl;
    }

    private String getIntro() {
        StringBuffer intro = new StringBuffer()
            .append("######################################################################")
            .append(endl)
            .append("# This is an automatically generated script to run your query.")
            .append(endl)
            .append("# To use it you will require the InterMine Perl client libraries.")
            .append(endl)
            .append("# These can be installed from CPAN, using your preferred client, eg:")
            .append(endl)
            .append("#").append(endl)
            .append("#").append(INDENT).append("sudo cpan Webservice::InterMine").append(endl)
            .append("#").append(endl)
            .append("# For help using these modules, please see these resources:").append(endl)
            .append("#").append(endl)
            .append("#  * https://metacpan.org/pod/Webservice::InterMine").append(endl)
            .append("#       - API reference").append(endl)
            .append("#  * https://metacpan.org/pod/Webservice::InterMine::Cookbook").append(endl)
            .append("#       - A How-To manual").append(endl)
            .append("#  * http://intermine.readthedocs.org/en/latest/web-services").append(endl)
            .append("#       - General Usage").append(endl)
            .append("#  * http://iodoc.labs.intermine.org").append(endl)
            .append("#       - Reference documentation for the underlying REST API").append(endl)
            .append("#").append(endl)
            .append("######################################################################")
            .append(endl + endl);
        return intro.toString();
    }

    private String formatProblems(Collection<String> problems) {
        StringBuffer sb = new StringBuffer();
        for (String s: problems) {
            sb.append("#  * " + s + endl);
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

        endl = wsCodeGenInfo.getLineBreak();

        PathQuery query = wsCodeGenInfo.getQuery();
        String serviceBaseURL = wsCodeGenInfo.getServiceBaseURL();
        String projectTitle = wsCodeGenInfo.getProjectTitle();
        String perlWSModuleVer = wsCodeGenInfo.getPerlWSModuleVer();

        // query is null
        if (query == null) {
            return getInvalidQuery() + formatProblems(Arrays.asList("The query is null"));
        }

        if (!query.isValid()) {
            return getInvalidQuery() + formatProblems(query.verifyQuery());
        }

        StringBuffer sb = new StringBuffer(getSheBang())
                                  .append(getIntro())
                                  .append(getBoilerPlate())
                                  .append(getSetFieldSeparator())
                                  .append(getUnicodeOutput())
                                  .append(getNoWarningsUndef())
                                  .append(endl);

        sb.append("# This code makes use of the Webservice::InterMine library."
                + endl);
        sb.append("# The following import statement sets " + projectTitle + " as your default"
                + endl);
        if (wsCodeGenInfo.isPublic()) {
            sb.append("use Webservice::InterMine"
                    + (perlWSModuleVer == null ? "" : " " + perlWSModuleVer)
                    + " '" + serviceBaseURL + "';" + endl);
        } else {
            sb.append("# You must also supply your login details here to access this query" + endl);
            sb.append("use Webservice::InterMine"
                    + (perlWSModuleVer == null ? "" : " " + perlWSModuleVer)
                    + " '" + serviceBaseURL + "', "
                    + "'" + wsCodeGenInfo.getUserToken() + "';" + endl);
        }
        sb.append(endl);

        if (StringUtils.isNotBlank(query.getDescription())) {
            printLine(sb, "# ", "Description: " + query.getDescription());
            sb.append(endl);
        }

        try {
            if (query instanceof TemplateQuery) {
                generateTemplateQueryCode((TemplateQuery) query, sb);
            } else {
                generatePathQueryCode(query, sb);
            }
        } catch (InvalidQueryException e) {
            return getInvalidQuery() + formatProblems(e.getProblems());
        }

        return sb.toString();
    }

    private static String q(String input) {
        if (input == null) {
            return "";
        } else {
            return "'" + input + "'";
        }
    }

    private static String qq(String input) {
        if (input == null) {
            return "";
        } else {
            return "\"" + input + "\"";
        }
    }

    private static String decapitate(String input) {
        if (input == null) {
            return "";
        } else {
            return input.substring(input.indexOf(".") + 1);
        }
    }

    private void generateTemplateQueryCode(
            TemplateQuery template,
            StringBuffer sb) throws InvalidQueryException {
        String name = template.getName();
        Map<PathConstraint, String> allConstraints = template.getConstraints();
        List<PathConstraint> editableConstraints = template.getEditableConstraints();

        if (editableConstraints.isEmpty()) {
            throw new InvalidQueryException("This template has no editable constraints");
        }

        sb.append("my $template = Webservice::InterMine->template(" + q(name) + ")" + endl)
            .append(INDENT + "or die 'Could not find a template called " + name + "';" + endl)
            .append(endl)
            .append("# Use an iterator to avoid having all rows in memory at once." + endl)
            .append("my $it = $template->results_iterator_with(" + endl);

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

        sb.append(");" + endl);
        sb.append(endl);
        printResults(template, sb);
    }

    private void generatePathQueryCode(
            PathQuery query,
            StringBuffer sb) throws InvalidQueryException {

        String rootClass = null;
        try {
            rootClass = query.getRootClass();
        } catch (PathException e) {
            throw new InvalidQueryException(e.getMessage());
        }

        sb.append("my $query = new_query(class => " + q(rootClass) + ");" + endl + endl);

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
            sb.append("# " + SUBCLASS_EXPLANATION + endl);
            // Subclass constraints must come first or the query will break
            for (String text: uncodedConstraints) {
                sb.append(text);
            }
        }

        sb.append("# The view specifies the output columns" + endl)
            .append("$query->add_view(qw/" + endl);
        for (String pathString : query.getView()) {
            sb.append(INDENT + decapitate(pathString) + endl);
        }
        sb.append("/);" + endl);
        sb.append(endl);

        // Add orderBy
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) { // no sort order
            if (query.getOrderBy().size() == 1
                    && query.getOrderBy().get(0).getOrderPath().equals(query.getView().get(0))
                    && query.getOrderBy().get(0).getDirection() == OrderDirection.ASC) {
                sb.append("# edit the line below to change the sort order:" + endl);
                sb.append("# ");
            } else {
                sb.append("# Your custom sort order is specified with the following code:"
                        + endl);
            }
            for (OrderElement oe : query.getOrderBy()) {
                sb.append("$query->add_sort_order(");
                sb.append(q(decapitate(oe.getOrderPath())) + ", "
                        + q(oe.getDirection().toString()));
                sb.append(");" + endl);
            }
            sb.append(endl);
        }

        // Add join status
        if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
            sb.append("# Outer Joins" + endl);
            sb.append("# (Show attributes of these relations if they exist, "
                    + "but do not require them to exist.)" + endl);
            for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                // Only outer joins need to be declared.
                if (entry.getValue() == OuterJoinStatus.OUTER) {
                    sb.append("$query->add_outer_join(" + q(decapitate(entry.getKey()))
                            + ");" + endl);
                }
            }
            sb.append(endl);
        }

        for (String text: codedConstraints) {
            sb.append(text);
        }
        sb.append(endl);

        // Add constraintLogic
        if (codedConstraints.size() > 1 && StringUtils.isNotBlank(query.getConstraintLogic())) {
            String logic = query.getConstraintLogic();
            if (logic.indexOf("or") == -1) {
                sb.append("# Edit the code below to specify your own custom logic:" + endl
                        + "# ");
            } else {
                sb.append("# Your custom logic is specified with the code below:" + endl);
            }
            sb.append("$query->set_logic(" + q(logic) + ");" + endl + endl);
        }
        sb.append("# Use an iterator to avoid having all rows in memory at once." + endl);
        sb.append("my $it = $query->iterator();" + endl);
        printResults(query, sb);
    }

    private void printResults(PathQuery pq, StringBuffer sb) {
        sb.append("while (my $row = <$it>) {" + endl);
        StringBuffer currentLine = new StringBuffer(INDENT + "print");
        Iterator<String> it = pq.getView().iterator();
        while (it.hasNext()) {
            String toPrint = "$row->{" + q(decapitate(it.next())) + "}";
            if (it.hasNext()) {
                toPrint += ",";
            }
            if (currentLine.length() + toPrint.length() > 100) {
                sb.append(currentLine.toString() + endl);
                currentLine = new StringBuffer(INDENT + INDENT);
            }
            if (StringUtils.isNotBlank(currentLine.toString())) {
                currentLine.append(" ");
            }
            currentLine.append(toPrint);
        }
        sb.append(currentLine + ", " + qq("\\n") + ";" + endl);
        sb.append("}").append(endl);
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
            sb.append(lineToPrint.substring(0, lastCutPoint) + endl);
            String nextLine = lineToPrint.substring(lastCutPoint + 1);
            printLine(sb, prefix, nextLine);
        } else {
            sb.append(lineToPrint + endl);
        }
    }

    /**
     * This method helps to generate constraint source code for PathQuery
     * @param pc PathConstraint object
     * @return a string for constraints source code
     */
    private String pathContraintUtil(
            PathConstraint pc,
            String code) throws UnhandledFeatureException {
        // Ref to Constraints
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        ConstraintOp op = pc.getOp();
        String value = PathConstraint.getValue(pc);
        String extraValue = PathConstraint.getExtraValue(pc);

        if ("PathConstraintAttribute".equals(className)
                || "PathConstraintBag".equals(className)) {
            return
                "$query->add_constraint(" + endl
                + INDENT + "path  => '" + path + "'," + endl
                + INDENT + "op    => '" + op.toString() + "'," + endl
                + INDENT + "value => '" + value + "'," + endl
                + INDENT + "code  => '" + code + "'," + endl
                + ");" + endl;
        }

        if ("PathConstraintLookup".equals(className)) {
            String evLine = "";
            if (extraValue != null) {
                evLine = INDENT + "extra_value => '" + extraValue + "'," + endl;
            }
            return
                "$query->add_constraint(" + endl
                + INDENT + "path        => '" + path + "'," + endl
                + INDENT + "op          => 'LOOKUP'," + endl
                + INDENT + "value       => '" + value + "'," + endl
                + evLine
                + INDENT + "code        => '" + code + "'," + endl
                + ");" + endl;
        }

        if ("PathConstraintIds".equals(className)) {
            throw new UnhandledFeatureException(INTERNAL_USE_CONSTRAINT + " (" + className + ")");
        }

        if ("PathConstraintMultiValue".equals(className)) {
            StringBuffer values = new StringBuffer();
            for (String aValue : ((PathConstraintMultiValue) pc).getValues()) {
                values.append(INDENT + INDENT + q(aValue) + "," + endl);
            }
            return
                "$query->add_constraint(" + endl
                + INDENT + "path   => '" + path + "'," + endl
                + INDENT + "op     => '" + op.toString() + "'," + endl
                + INDENT + "values => [" + endl
                + values.toString()
                + INDENT + "]," + endl
                + INDENT + "code  => '" + code + "'," + endl
                + ");" + endl;
        }

        if ("PathConstraintNull".equals(className)) {
            return
                "$query->add_constraint(" + endl
                + INDENT + "path => '" + path + "'," + endl
                + INDENT + "op   => '" + op.toString() + "'," + endl
                + INDENT + "code => '" + code + "'," + endl
                + ");" + endl;
        }

        if ("PathConstraintSubclass".equals(className)) {
            String type = ((PathConstraintSubclass) pc).getType();
            return
                "$query->add_constraint(" + endl
                + INDENT + "path => '" + path + "'," + endl
                + INDENT + "type => '" + type + "'," + endl
                + ");" + endl;
        }

        if ("PathConstraintLoop".equals(className)) {
            String opStr = op.toString();
            if (ConstraintOp.EQUALS.equals(op)) {
                opStr = "IS";
            } else if (ConstraintOp.NOT_EQUALS.equals(op)) {
                opStr = "IS NOT";
            }
            return
                "$query->add_constraint(" + endl
                + INDENT + "path      => '" + path + "'," + endl
                + INDENT + "op        => '" + opStr + "'," + endl
                + INDENT + "loop_path => '" + value + "'," + endl
                + INDENT + "code      => '" + code + "'," + endl
                + ");" + endl;
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
    private String templateConstraintUtil(PathConstraint pc, String opCode)
        throws UnhandledFeatureException {
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String op = pc.getOp().toString();
        String value = PathConstraint.getValue(pc);
        String extraValue = PathConstraint.getExtraValue(pc);

        if ("PathConstraintAttribute".equals(className)
                || "PathConstraintBag".equals(className)) {
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + endl
                + INDENT + "value" + opCode + " => '" + value + "'," + endl;
        }

        if ("PathConstraintLookup".equals(className)) {
            String ret =
                INDENT + "op" + opCode + "    => 'LOOKUP'," + endl
                + INDENT + "value" + opCode + " => '" + value + "'," + endl;
            if (extraValue != null && !"".equals(extraValue)) {
                ret += INDENT + "extra_value" + opCode + " => '" + extraValue + "'," + endl;
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
                values.append(INDENT + INDENT + "'" + aValue + "'," + endl);
            }
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + endl
                + INDENT + "value" + opCode + " => [" + endl
                + values.toString()
                + INDENT + "]," + endl;
        }

        if ("PathConstraintNull".equals(className)) {
            return
                INDENT + "op" + opCode + "    => '" + op + "'," + endl;
        }

        if ("PathConstraintSubclass".equals(className)) {
            throw new UnhandledFeatureException("Type constraints should not be editable");
        }

        throw new UnhandledFeatureException("Unknown constraint type (" + className + ")");
    }
}

