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

import java.util.Collection;
import java.util.Map.Entry;

import org.intermine.api.template.TemplateQuery;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintIds;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

/**
 * This Class generates Java source code of web service client for path query and template query.
 *
 * @author Fengyuan Hu
 *
 */
public class WebserviceJavaCodeGenerator implements WebserviceCodeGenerator
{
    protected static final String TEST_STRING = "This is a Java test string...";
    protected static final String INVALID_QUERY = "Invalid query. No fields selected for output.";
    protected static final String NULL_QUERY = "Invalid query. Query can not be null.";
    protected static final String TEMPLATE_BAG_CONSTRAINT = "This template contains a list "
        + "constraint, which is currently not supported.";

    protected static final String INDENT = "    ";
    protected static final String INDENT2 = INDENT + INDENT;
    protected static final String SPACE = " ";
    protected static final String ENDL = System.getProperty("line.separator");

    /**
     * This method will generate web service source code in Java from a path query
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

        // query is null
        if (query == null) {
            return NULL_QUERY;
        }

        String queryClassName = TypeUtil.unqualifiedName(query.getClass().toString());
        StringBuffer pac = new StringBuffer();
        StringBuffer impJava = new StringBuffer();
        StringBuffer impIM = new StringBuffer();
        StringBuffer sb = new StringBuffer();

        if ("PathQuery".equals(queryClassName)) {
            if (query.getView() == null || query.getView().isEmpty()) {
                return INVALID_QUERY;
            } else {
                sb = generatePathQueryCode(wsCodeGenInfo, pac, impJava, impIM, sb);
            }
        } else if ("TemplateQuery".equals(queryClassName)) {
            sb = generateTemplateQueryCode(wsCodeGenInfo, pac, impJava, impIM, sb);
        }

        if (!TEMPLATE_BAG_CONSTRAINT.equals(sb.toString())) {
            return pac.toString() + impJava.toString() + ENDL
                    + impIM.toString() + ENDL + sb.toString();
        } else {
            return sb.toString();
        }
    }

    /**
     * This method will generate Java source code for PathQuery.
     * @param query a PathQuery object
     * @param serviceBaseURL webservice base url, like "http://www.flymine.org"
     * @param projectTitle the name of mine
     * @param pac a StringBuffer to hold the package string
     * @param impJava a StringBuffer to hold all the import strings from standard Java classes
     * @param impIM a StringBuffer to hold all the import strings from InterMine classes
     * @param sb a StringBuffer to hold the rest of the source code strings
     * @return sb
     */
    private StringBuffer generatePathQueryCode(WebserviceCodeGenInfo info,
            StringBuffer pac, StringBuffer impJava, StringBuffer impIM, StringBuffer sb) {
        // Add package and import
        pac.append("package ")
            .append(TypeUtil.javaisePackageName(info.getProjectTitle()))
            .append(";" + ENDL + ENDL);

        impJava.append("import java.io.IOException;" + ENDL)
            .append("import java.util.List;" + ENDL);

        impIM.append("import org.intermine.metadata.Model;" + ENDL)
            .append("import org.intermine.webservice.client.core.ServiceFactory;" + ENDL)
            .append("import org.intermine.webservice.client.services.ModelService;" + ENDL)
            .append("import org.intermine.webservice.client.services.QueryService;" + ENDL)
            .append("import org.intermine.pathquery.PathQuery;" + ENDL);

        // Add class comments
        sb.append("/**" + ENDL)
                .append(SPACE + "*" + SPACE
                        + "This is an automatically generated Java program to run the "
                        + info.getProjectTitle() + " query." + ENDL)
            .append(SPACE + "*" + ENDL)
            .append(SPACE + "*" + SPACE + "@author " + info.getProjectTitle() + ENDL)
            .append(SPACE + "*" + ENDL)
            .append(SPACE + "*/" + ENDL);

        // Add class code
        sb.append("public class QueryClient" + ENDL)
            .append(
                "{" + ENDL
                + INDENT + "private static final String ROOT "
                    + "= \"" + info.getServiceBaseURL() + "/service\";" + ENDL
                + ENDL);
        if (!info.isPublic()) {
            sb.append(
                INDENT + "//Add your login details here by setting your password." + ENDL
              + INDENT + "private static final String USERNAME = \"" + info.getUserName() + "\";" + ENDL
              + INDENT + "private static final String PASSWORD = null;" + ENDL
              + ENDL);
        }

        // Add methods code
        // Add Main method
        sb.append(
              INDENT +         "/**" + ENDL
            + INDENT + SPACE + "*" + SPACE + "Perform the query and print the rows of results." + ENDL
            + INDENT + SPACE + "*" + SPACE + "@param args command line arguments" + ENDL
            + INDENT + SPACE + "*" + SPACE + "@throws IOException" + ENDL
            + INDENT + SPACE + "*/" + ENDL);

        sb.append(
            INDENT + "public static void main(String[] args) {" + ENDL
            + INDENT2 + "ServiceFactory factory = new ServiceFactory(ROOT);" + ENDL
            + INDENT2 + "Model model = factory.getModelService().getModel();" + ENDL
            + INDENT2 + "QueryService service = factory.getQueryService();" + ENDL
            + INDENT2 + "PathQuery query = new PathQuery(model);" + ENDL
            + ENDL);

        // If we need to authenticate, do that now.
        if (!info.isPublic()) {
            sb.append(
                 INDENT2 + "// Log in to access the private lists in this query" + ENDL
               + INDENT2 + "service.setAuthentication(USERNAME, PASSWORD);" + ENDL
               + ENDL);
        }

        PathQuery query = info.getQuery();
        // Add views
        sb.append(INDENT2 + "// Add views" + ENDL);
        if (query.getView().size() > 1) {
            int idx = 1;
            for (String pathString : query.getView()) {
                if (idx == 1) {
                    sb.append(INDENT2 + "query.addViews(\"" + pathString + "\"," + ENDL);
                    idx++;
                    continue;
                }
                if (idx == query.getView().size()) {
                    sb.append(INDENT2 + INDENT2 + "\"" + pathString + "\");" + ENDL);
                    break;
                }
                sb.append(INDENT2 + INDENT2 + "\"" + pathString + "\"," + ENDL);
                idx++;
            }
        } else {
            sb.append(INDENT + INDENT + "query.addView(\""
                    + query.getView().iterator().next() + "\");" + ENDL);
        }

        sb.append(ENDL);

        // Add orderby
        if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
            impIM.append("import org.intermine.pathquery.OrderDirection;" + ENDL);
            sb.append(INDENT + INDENT + "// Add orderby" + ENDL);
            for (OrderElement oe : query.getOrderBy()) {
                sb.append(INDENT + INDENT + "query.addOrderBy(\""
                        + oe.getOrderPath() + "\", OrderDirection." + oe.getDirection() + ");"
                        + ENDL);
            }

            sb.append(ENDL);
        }

        // Add constraints
        if (query.getConstraints() != null && !query.getConstraints().isEmpty()) {
            impIM.append("import org.intermine.pathquery.Constraints;" + ENDL);
            sb.append(INDENT + INDENT
                    + "// Add constraints and you can edit the constraint values below" + ENDL);
            if (query.getConstraints().size() == 1) {
                PathConstraint pc = query.getConstraints().entrySet()
                        .iterator().next().getKey();
                String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                if ("PathConstraintMultiValue".equals(className)) {
                    impJava.append("import java.util.ArrayList;" + ENDL);
                    sb.append(INDENT + INDENT
                            + "List<String> values = new ArrayList<String>();"
                            + ENDL);
                    for (String value : ((PathConstraintMultiValue) pc).getValues()) {
                        sb.append(INDENT + INDENT + "values.add(\"" + value + "\");" + ENDL);
                    }
                }
                if ("PathConstraintIds".equals(className)) {
                    impJava.append("import java.util.ArrayList;" + ENDL);
                    sb.append(INDENT + INDENT
                            + "List<String> values = new ArrayList<String>();"
                            + ENDL);
                    for (Integer id : ((PathConstraintIds) pc).getIds()) {
                        sb.append(INDENT + INDENT + "ids.add(" + id + ");" + ENDL);
                    }
                }
                sb.append(INDENT + INDENT + "query.addConstraint("
                        + pathContraintUtil(pc) + ");"
                        + ENDL);
            } else {
                for (Entry<PathConstraint, String> entry : query.getConstraints().entrySet()) {
                    PathConstraint pc = entry.getKey();
                    String className = TypeUtil.unqualifiedName(pc.getClass().toString());
                    if ("PathConstraintMultiValue".equals(className)) {
                        impJava.append("import java.util.ArrayList;" + ENDL);
                        sb.append(INDENT + INDENT
                                + "List<String> values = new ArrayList<String>();"
                                + ENDL);
                        for (String value : ((PathConstraintMultiValue) pc).getValues()) {
                            sb.append(INDENT + INDENT + "values.add(\""
                                    + value + "\");" + ENDL);
                        }
                    }
                    if ("PathConstraintIds".equals(className)) {
                        impJava.append("import java.util.ArrayList;" + ENDL);
                        sb.append(INDENT + INDENT
                                + "List<String> values = new ArrayList<String>();"
                                + ENDL);
                        for (Integer id : ((PathConstraintIds) pc).getIds()) {
                            sb.append(INDENT + INDENT + "ids.add(" + id + ");" + ENDL);
                        }
                    }
                    sb.append(INDENT + INDENT + "query.addConstraint("
                            + pathContraintUtil(pc) + ", \"" + entry.getValue() + "\");"
                            + ENDL);
                    sb.append(ENDL);
                }

                // Add constraintLogic
                if (query.getConstraintLogic() != null
                        && !"".equals(query.getConstraintLogic())) {
                    sb.append(INDENT + INDENT + "// Add constraintLogic" + ENDL);
                    sb.append(INDENT + INDENT + "query.setConstraintLogic(\""
                            + query.getConstraintLogic() + "\");"
                            + ENDL);
                }
            }

            sb.append(ENDL);
        }

        // Add join status
        if (query.getOuterJoinStatus() != null && !query.getOuterJoinStatus().isEmpty()) {
            impIM.append("import org.intermine.pathquery.OuterJoinStatus;" + ENDL);
            sb.append(INDENT2 + "// Add join status" + ENDL);
            for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                sb.append(INDENT2 + "query.setOuterJoinStatus(\""
                        + entry.getKey() + "\", OuterJoinStatus." + entry.getValue() + ");"
                        + ENDL);
            }

            sb.append(ENDL);
        }

        // Add description?

        // Add display results code
        sb.append(INDENT2 + "List<List<String>> result = service.getAllResults(query);" + ENDL
                + INDENT2 + "List<String> view = query.getView();" + ENDL
                + INDENT2 + "System.out.println(\"Results:\");" + ENDL
                + INDENT2 + "for (List<String> row : result) {" + ENDL
                + INDENT2 + INDENT + "for (String cell : row) {" + ENDL
                + INDENT2 + INDENT2 + "System.out.print(cell + \" \");" + ENDL
                + INDENT2 + INDENT + "}" + ENDL
                + INDENT2 + INDENT + "System.out.print(\"\\n\");" + ENDL
                + INDENT2 + "}" + ENDL
                + INDENT + "}" + ENDL
                + ENDL);

        sb.append("}" + ENDL);

        return sb;
    }

    /**
     * This method will generate Java source code for TemplateQuery.
     * @param query a PathQuery object, must cast to TemplateQuery
     * @param serviceBaseURL webservice base url, like "http://www.flymine.org"
     * @param projectTitle the name of mine
     * @param pac a StringBuffer to hold the package string
     * @param impJava a StringBuffer to hold all the import strings from standard Java classes
     * @param impIM a StringBuffer to hold all the import strings from InterMine classes
     * @param sb a StringBuffer to hold the rest of the source code strings
     * @return sb
    */
    private StringBuffer generateTemplateQueryCode(WebserviceCodeGenInfo info,
            StringBuffer pac, StringBuffer impJava, StringBuffer impIM, StringBuffer sb) {

        TemplateQuery template = (TemplateQuery) info.getQuery();
        if (template.getBagNames().size() > 0) {
            return new StringBuffer(TEMPLATE_BAG_CONSTRAINT);
        }
        String srcClassName = TypeUtil.javaiseClassName(template.getName());

        // Add package and import
        pac.append("package " + TypeUtil.javaisePackageName(info.getProjectTitle()) + ';'
                + ENDL + ENDL);

        impJava.append(
                "import java.util.ArrayList;" + ENDL
             +  "import java.util.List;" + ENDL);

        impIM.append(
              "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL
            + "import org.intermine.webservice.client.services.TemplateService;" + ENDL
            + "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL);

        // Add class comments
        sb.append("/**" + ENDL)
            .append(SPACE + "* This is an automatically generated Java program to run the "
                + info.getProjectTitle() + " template, " + template.getName() +  ENDL);
        if (!(template.getDescription() == null || "".equals(template.getDescription()))) {
            sb.append(SPACE + "* Description:" + SPACE + template.getDescription() + ENDL);
        }
        sb.append(SPACE + "*" + ENDL)
            .append(SPACE + "*" + SPACE + "@author " + info.getProjectTitle() + ENDL)
            .append(SPACE + "*" + ENDL)
            .append(SPACE + "*/" + ENDL);

        // Add class code
        sb.append("public class Template" + srcClassName + ENDL
            + "{" + ENDL
            + INDENT + "private static final String ROOT = \"" + info.getServiceBaseURL()
                     + "/service\";" + ENDL
            + ENDL);

        if (!info.isPublic()) {
               sb.append(
                   INDENT + "//Add your login details here by setting your password." + ENDL
                + INDENT + "private static final String USERNAME = \"" + info.getUserName() + "\";" + ENDL
                + INDENT + "private static final String PASSWORD = null;" + ENDL
                + ENDL);
        }

        // Add methods code
        // Add Main method
        sb.append(INDENT + "/**" + ENDL)
            .append(INDENT + SPACE + "*" + SPACE + "@param args command line arguments" + ENDL)
            .append(INDENT + SPACE + "*/" + ENDL);

        sb.append(
              INDENT + "public static void main(String[] args) {" + ENDL
            + ENDL+ INDENT2
            + "TemplateService service = new ServiceFactory(ROOT).getTemplateService();" + ENDL
            + ENDL);

        if (!info.isPublic()) {
            sb.append(
                    INDENT2 + "// Log in to access this template" + ENDL
                 + INDENT2 + "service.setAuthentication(USERNAME, PASSWORD);" + ENDL
                 + ENDL);
        }

        sb.append(
            INDENT2 + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL
            + INDENT2 + "// You can edit the constraint values below" + ENDL);

        // Only editable constraints will be generated
        for (PathConstraint pc : template.getEditableConstraints()) {
            String constraintDes = template.getConstraintDescription(pc);
            if (constraintDes == null || "".equals(constraintDes)) {
            } else {
                sb.append(INDENT + INDENT + "// Constraint description - "
                        + constraintDes + ENDL);
            }
            sb.append(INDENT + INDENT + templateConstraintUtil(pc) + ENDL);
        }

        sb.append(ENDL);

        // Add display results code
        sb.append(
              INDENT2 + "// Name of template" + ENDL
            + INDENT2 + "String name = \"" + template.getName() + "\";" + ENDL
            + ENDL
            + INDENT2 + "List<List<String>> rows = " + "service.getAllResults(name, parameters);" + ENDL
            + INDENT2 + "System.out.println(\"Results:\");" + ENDL
            + INDENT2 + "for (List<String> row : rows) {" + ENDL
            + INDENT2 + INDENT + "for (String cell : row) {" + ENDL
            + INDENT2 + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL
            + INDENT2 + INDENT + "}" + ENDL
            + INDENT2 + INDENT + "System.out.print(\"\\n\");" + ENDL
            + INDENT2 + "}" + ENDL
            + INDENT + "}" + ENDL
            + "}" + ENDL);

        return sb;
    }

    /**
     * This method helps to generate constraint source code for PathQuery
     * @param pc PathConstraint object
     * @return a string like "Constraints.lessThan(\"Gene.length\", \"1000\")"
     */
    private String pathContraintUtil(PathConstraint pc) {
        // Generate a string like "Constraints.lessThan(\"Gene.length\", \"1000\")"
        // Ref to Constraints
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        ConstraintOp op = pc.getOp();

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            if (op.equals(ConstraintOp.EQUALS)) {
                return "Constraints.eq(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.NOT_EQUALS)) {
                return "Constraints.neq(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.MATCHES)) {
                return "Constraints.like(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.DOES_NOT_MATCH)) {
                return "Constraints.notLike(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.LESS_THAN)) {
                return "Constraints.lessThan(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.LESS_THAN_EQUALS)) {
                return "Constraints.lessThanEqualTo(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.GREATER_THAN)) {
                return "Constraints.greaterThan(\"" + path + "\", \"" + value + "\")";
            }

            if (op.equals(ConstraintOp.GREATER_THAN_EQUALS)) {
                return "Constraints.greaterThanEqualTo(\"" + path + "\", \"" + value + "\")";
            }
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();

            return "Constraints.lookup(\"" + path + "\", \"" + value + "\", \""
                    + extraValue + "\")";
        }

        if ("PathConstraintBag".equals(className)) {
            String bag = ((PathConstraintBag) pc).getBag();
            if (op.equals(ConstraintOp.IN)) {
                return "Constraints.in(\"" + path + "\", \"" + bag + "\")";
            }

            if (op.equals(ConstraintOp.NOT_IN)) {
                return "Constraints.notIn(\"" + path + "\", \"" + bag + "\")";
            }
        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
            if (op.equals(ConstraintOp.IN)) {
                return "Constraints.inIds(\"" + path + "\", ids)";
            }

            if (op.equals(ConstraintOp.NOT_IN)) {
                return "Constraints.notInIds(\"" + path + "\", ids)";
            }
        }

        if ("PathConstraintMultiValue".equals(className)) {
            if (op.equals(ConstraintOp.ONE_OF)) {
                return "Constraints.oneOfValues(\"" + path + "\", values)";
            }

            if (op.equals(ConstraintOp.NONE_OF)) {
                return "Constraints.noneOfValues(\"" + path + "\", values)";
            }
        }

        if ("PathConstraintNull".equals(className)) {
            if (op.equals(ConstraintOp.IS_NULL)) {
                return "Constraints.isNull(\"" + path + "\")";
            }

            if (op.equals(ConstraintOp.IS_NOT_NULL)) {
                return "Constraints.isNotNull(\"" + path + "\")";
            }
        }

        if ("PathConstraintSubclass".equals(className)) {
            // can not test from webapp
            String type = ((PathConstraintSubclass) pc).getType();
            return "Constraints.type(\"" + path + "\", \"" + type + "\")";
        }

        if ("PathConstraintLoop".equals(className)) {
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            if (op.equals(ConstraintOp.EQUALS)) {
                return "Constraints.equalToLoop(\"" + path + "\", \"" + loopPath + "\")";
            }

            if (op.equals(ConstraintOp.NOT_EQUALS)) {
                return "Constraints.notEqualToLoop(\"" + path + "\", \"" + loopPath + "\")";
            }
        }

        return null;
    }

    /**
     * This method helps to generate Template Parameters (predefined constraints) source code for
     * TemplateQuery
     * @param pc PathConstraint object
     * @return a line of source code
     */
    private String templateConstraintUtil(PathConstraint pc) {
        String className = TypeUtil.unqualifiedName(pc.getClass().toString());
        String path = pc.getPath();
        String op = pc.getOp().toString();

        if ("PathConstraintAttribute".equals(className)) {
            String value = ((PathConstraintAttribute) pc).getValue();
            if ("=".equals(op)) { op = "eq"; }
            if ("!=".equals(op)) { op = "ne"; }
            if ("<".equals(op)) { op = "lt"; }
            if ("<=".equals(op)) { op = "le"; }
            if (">".equals(op)) { op = "gt"; }
            if (">=".equals(op)) { op = "ge"; }
            return "parameters.add(new TemplateParameter(\"" + path + "\", \""
                    + op + "\", \"" + value + "\"));";
        }

        if ("PathConstraintLookup".equals(className)) {
            String value = ((PathConstraintLookup) pc).getValue();
            String extraValue = ((PathConstraintLookup) pc).getExtraValue();
            return "parameters.add(new TemplateParameter(\"" + path + "\", \""
                + op + "\", \"" + value + "\", \"" + extraValue + "\"));";
        }

        // Bag constraint is not supported
        if ("PathConstraintBag".equals(className)) {

        }

        if ("PathConstraintIds".equals(className)) {
            // can not test from webapp
            Collection<Integer> ids = ((PathConstraintIds) pc).getIds();
            StringBuilder idSB = new StringBuilder();
            for (Integer id : ids) {
                idSB.append(id);
                idSB.append(",");
            }
            idSB.deleteCharAt(idSB.lastIndexOf(","));
            return "parameters.add(new TemplateParameter(\"" + path + "\", \""
                + op + "\", \"" + idSB.toString() + "\"));";
        }

        if ("PathConstraintMultiValue".equals(className)) {
            Collection<String> values = ((PathConstraintMultiValue) pc).getValues();
            StringBuilder multiSB = new StringBuilder();
            for (String value : values) {
                multiSB.append(value);
                multiSB.append(",");
            }
            multiSB.deleteCharAt(multiSB.lastIndexOf(","));

            return "parameters.add(new TemplateParameter(\"" + path + "\", \""
                + op + "\", \"" + multiSB.toString() + "\"));";

        }

        if ("PathConstraintNull".equals(className)) {
            return "parameters.add(new TemplateParameter(\"" + path + "\", \""
                + op + "\", \"" + op + "\"));";
        }

        if ("PathConstraintSubclass".equals(className)) {
            // not handled
        }

        // Loop constraint is uneditable
        if ("PathConstraintLoop".equals(className)) {
            String loopPath = ((PathConstraintLoop) pc).getLoopPath();
            return "parameters.add(new TemplateParameter(\"" + path + "\", \""
                + op + "\", \"" + loopPath + "\"));";
        }

        return null;
    }
}
