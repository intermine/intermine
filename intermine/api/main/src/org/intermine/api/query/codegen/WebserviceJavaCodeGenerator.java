package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;

/**
 *
 * @author Fengyuan Hu
 *
 */
public class WebserviceJavaCodeGenerator implements WebserviceCodeGenerator
{
    protected static final String TEST_STRING = "This is a Java test string...";
    protected static final String INVALID_QUERY = "Invalid query. No fields selected for output...";

    protected static final String INDENT = "    ";
    protected static final String SPACE = " ";
    protected static final String ENDL = System.getProperty("line.separator");

    /**
     * This method will generate web service source code in Java from a path query.
     *
     * @param query a PathQuery
     * @return web service source code in a string
     */
    public String generate(PathQuery query) {
        StringBuffer pac = new StringBuffer();
        StringBuffer impJava = new StringBuffer();
        StringBuffer impIM = new StringBuffer();
        StringBuffer sb = new StringBuffer();

        // Add package and import
        pac.append("package ")
            .append("your.foo.bar.package.name")
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
            .append(SPACE + "*" + SPACE + "Add some description to the class..." + ENDL)
            .append(SPACE + "*" + ENDL)
            .append(SPACE + "*" + SPACE + "@auther auther name" + ENDL)
            .append(SPACE + "**/" + ENDL);

        // Add class code
        sb.append("public class PathQueryClient" + ENDL)
            .append("{" + ENDL + INDENT)
            .append("private static String serviceRootUrl "
                    + "= \"http://<your webapp base url>/service\";" + ENDL + ENDL);

        // Add methods code
        // Add Main method
        sb.append(INDENT + "/**" + ENDL)
            .append(INDENT + SPACE + "*" + SPACE + "@param args command line arguments" + ENDL)
            .append(INDENT + SPACE + "*" + SPACE + "@throws IOException" + ENDL)
            .append(INDENT + SPACE + "*/" + ENDL);

        sb.append(INDENT + "public static void main(String[] args) {" + ENDL)
            .append(INDENT + INDENT + "QueryService service =" + ENDL)
                .append(INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl,"
                        + " \"QueryService\").getQueryService();" + ENDL)
            .append(INDENT + INDENT + "Model model = getModel();" + ENDL)
            .append(INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL);

        // Add views
        if (query.getView() == null || query.getView().isEmpty()) {
            return INVALID_QUERY;
        } else {
            sb.append(INDENT + INDENT + "// Add views" + ENDL);
            for (String pathString : query.getView()) {
                sb.append(INDENT + INDENT + "query.addView(\"" + pathString + "\");" + ENDL);
            }
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
            sb.append(INDENT + INDENT + "// Add constraints" + ENDL);
            if (query.getConstraints().size() == 1) {
                PathConstraint pc = query.getConstraints().entrySet().iterator().next().getKey();
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
                if ("PathConstraintBag".equals(className)) {
                    sb.append(INDENT + INDENT
                            + "// Only public lists are supported"
                            + ENDL);
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
                            + pathContraintUtil(pc) + ", \"" + entry.getValue() + "\");"
                            + ENDL);
                    sb.append(ENDL);
                }

                // Add constraintLogic
                if (query.getConstraintLogic() != null && !query.getConstraintLogic().isEmpty()) {
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
            sb.append(INDENT + INDENT + "// Add join status" + ENDL);
            for (Entry<String, OuterJoinStatus> entry : query.getOuterJoinStatus().entrySet()) {
                sb.append(INDENT + INDENT + "query.setOuterJoinStatus(\""
                        + entry.getKey() + "\", OuterJoinStatus." + entry.getValue() + ");"
                        + ENDL);
            }

            sb.append(ENDL);
        }

        // Add description?

        // Add display results code
        sb.append(INDENT + INDENT + "// Number of results are fetched" + ENDL)
            .append(INDENT + INDENT + "int maxCount = 100;" + ENDL)
                .append(INDENT + INDENT
                        + "List<List<String>> result = service.getResult(query, maxCount);"
                        + ENDL)
            .append(INDENT + INDENT + "System.out.println(\"Results: \");" + ENDL)
            .append(INDENT + INDENT + "for (List<String> row : result) {" + ENDL)
            .append(INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL)
            .append(INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL)
            .append(INDENT + INDENT + INDENT + "}" + ENDL)
            .append(INDENT + INDENT + INDENT + "System.out.println();" + ENDL)
            .append(INDENT + INDENT  + "}" + ENDL)
            .append(INDENT + "}" + ENDL + ENDL);

        // Add getModel method
        sb.append(INDENT + "private static Model getModel() {" + ENDL)
            .append(INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl,"
                    + " \"ModelService\").getModelService();" + ENDL)
            .append(INDENT + INDENT + "return service.getModel();" + ENDL)
            .append(INDENT + "}" + ENDL);

        sb.append("}" + ENDL);

        if (!sb.toString().isEmpty()) {
            return pac.toString() + impJava.toString() + ENDL
                    + impIM.toString() + ENDL + sb.toString();
        } else {
            return TEST_STRING;
        }
    }

    /**
     * This method will generate web service source code in Java from a template query.
     *
     * @param query a TemplateQuery
     * @return web service source code in a string
     */
    public String generate(TemplateQuery query) {
        StringBuffer pac = new StringBuffer();
        StringBuffer impJava = new StringBuffer();
        StringBuffer impIM = new StringBuffer();
        StringBuffer sb = new StringBuffer();

        // Add package and import
        pac.append("package ")
            .append("your.foo.bar.package.name")
            .append(";" + ENDL + ENDL);

        impJava.append("import java.util.ArrayList;" + ENDL)
            .append("import java.util.List;" + ENDL);

        impIM.append("import org.intermine.webservice.client.core.ServiceFactory;" + ENDL)
            .append("import org.intermine.webservice.client.services.TemplateService;" + ENDL)
            .append("import org.intermine.webservice.client.template.TemplateParameter;" + ENDL);

        // Add class comments
        sb.append("/**" + ENDL)
            .append(SPACE + "*" + SPACE + "Add some description to the class..." + ENDL)
            .append(SPACE + "*" + ENDL)
            .append(SPACE + "*" + SPACE + "@auther auther name" + ENDL)
            .append(SPACE + "**/" + ENDL);

        // Add class code
        sb.append("public class TemplateQueryClient" + ENDL)
            .append("{" + ENDL + INDENT)
            .append("private static String serviceRootUrl "
                    + "= \"http://<your webapp base url>/service\";" + ENDL + ENDL);

        // Add methods code
        // Add Main method
        sb.append(INDENT + "/**" + ENDL)
            .append(INDENT + SPACE + "*" + SPACE + "@param args command line arguments" + ENDL)
            .append(INDENT + SPACE + "*/" + ENDL);

        sb.append(INDENT + "public static void main(String[] args) {" + ENDL + ENDL)
            .append(INDENT + INDENT
                    + "TemplateService service =" + "new ServiceFactory(serviceRootUrl,"
                        + " \"TemplateService\").getTemplateService();" + ENDL + ENDL)
            .append(INDENT + INDENT
                    + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();"
                    + ENDL + ENDL);

        for (PathConstraint pc : query.getConstraints().keySet()) {
            String className = TypeUtil.unqualifiedName(pc.getClass().toString());
            if ("PathConstraintBag".equals(className)) {
                return "This template contains a list constraint, which is currently not supported."
                    + " Remove the list constraint and try again...";
            } else {
                sb.append(INDENT + INDENT + templateConstraintUtil(pc) + ENDL);
            }
        }

        sb.append(ENDL);

        String templateName = query.getName();

        // Add display results code
        sb.append(INDENT + INDENT
                        + "// Name of a public template, "
                        + "private templates are not supported at the moment"
                        + ENDL)
            .append(INDENT + INDENT + "String templateName = \"" + templateName + "\";" + ENDL)
            .append(ENDL + INDENT + INDENT + "// Number of results are fetched" + ENDL)
            .append(INDENT + INDENT + "int maxCount = 100;" + ENDL)
            .append(INDENT + INDENT
                    + "List<List<String>> result = "
                    + "service.getResult(templateName, parameters, maxCount);"
                    + ENDL)
            .append(INDENT + INDENT + "System.out.println(\"Results: \");" + ENDL)
            .append(INDENT + INDENT + "for (List<String> row : result) {" + ENDL)
            .append(INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL)
            .append(INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL)
            .append(INDENT + INDENT + INDENT + "}" + ENDL)
            .append(INDENT + INDENT + INDENT + "System.out.println();" + ENDL)
            .append(INDENT + INDENT  + "}" + ENDL);

        sb.append(INDENT + "}" + ENDL)
            .append("}" + ENDL);

        if (!sb.toString().isEmpty()) {
            return pac.toString() + impJava.toString() + ENDL
                + impIM.toString() + ENDL + sb.toString();
        } else {
            return TEST_STRING;
        }
    }

    /**
     * Say something???
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
        return null;
    }

    /**
     *
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

        if ("PathConstraintBag".equals(className)) {
            // not supported
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
        return null;
    }
}
