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

import java.io.StringReader;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * Tests for the WebserviceJavaCodeGenerator class.
 *
 * @author Fengyuan Hu
 */
public class WebserviceJavaCodeGeneratorTest extends TestCase {

    private String INDENT = WebserviceJavaCodeGenerator.INDENT;
    private String SPACE = WebserviceJavaCodeGenerator.SPACE;
    private String ENDL = WebserviceJavaCodeGenerator.ENDL;

    private String INVALID_QUERY = WebserviceJavaCodeGenerator.INVALID_QUERY;
    private String NULL_QUERY = WebserviceJavaCodeGenerator.NULL_QUERY;
    private String TEMPLATE_BAG_CONSTRAINT = WebserviceJavaCodeGenerator.TEMPLATE_BAG_CONSTRAINT;

    private String serviceRootURL = "http://newt.flymine.org:8080/modminepreview";
    private String projectTitle = "modMine_Test-2.M";

    private WebserviceJavaCodeGenerator cg;

    public WebserviceJavaCodeGeneratorTest(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    public void setUp() {
        cg = new WebserviceJavaCodeGenerator();
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    public void tearDown() {

    }

    //****************************** Test PathQuery *********************************
    /**
     * This method tests when a path query is null.
     */
    public void testPathQueryCodeGenerationWithNullQuery() {
        PathQuery pathQuery = null;

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = NULL_QUERY;
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has no views.
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier
     *   Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     * </query>
     *
     * Views will be removed from the PathQuery object.
     *
     */
    public void testPathQueryCodeGenerationWithInvalidQuery() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"></query>";

        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        // Mock up
        pathQuery.clearView();
        String expected = INVALID_QUERY;
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one view.
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier"
     *   sortOrder="Gene.primaryIdentifier asc">
     * </query>
     *
     */
    public void testPathQueryCodeGenerationWithOneView() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"></query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addView(\"Gene.primaryIdentifier\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has no constraints and more than one view.
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier
     *   Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     * </query>
     *
     */
    public void testPathQueryCodeGenerationWithNoConstraints() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
                "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
                "sortOrder=\"Gene.primaryIdentifier asc\"></query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has no constraints and more than one view.
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier
     *   Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <join path="Gene.organism" style="OUTER"/>
     * </query>
     *
     */
    public void testPathQueryCodeGenerationWithJoinStatus() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
                "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
                "sortOrder=\"Gene.primaryIdentifier asc\">" +
                "<join path=\"Gene.organism\" style=\"OUTER\"/>" +
                "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.OuterJoinStatus;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add join status" + ENDL +
        INDENT + INDENT + "query.setOuterJoinStatus(\"Gene.organism\", OuterJoinStatus.OUTER);" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has no sortOrder.
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier
     *   Gene.symbol Gene.name Gene.organism.shortName">
     * </query>
     *
     */
    public void testPathQueryCodeGenerationWithoutSortOrder() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\"></query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.EQUALS
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.secondaryIdentifier" op="=" value="zen"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintEq() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.secondaryIdentifier\" op=\"=\" value=\"zen\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.eq(\"Gene.secondaryIdentifier\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.NOT_EQUALS
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.secondaryIdentifier" op="!=" value="zen"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintNeq() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.secondaryIdentifier\" op=\"!=\" value=\"zen\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.neq(\"Gene.secondaryIdentifier\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.MATCHES
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.organism.commonName" op="LIKE" value="D.*"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintLike() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.organism.commonName\" op=\"LIKE\" value=\"D.*\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.like(\"Gene.organism.commonName\", \"D.*\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.DOES_NOT_MATCH
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.organism.commonName" op="NOT LIKE" value="D.*"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintNotLike() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.organism.commonName\" op=\"NOT LIKE\" value=\"D.*\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.notLike(\"Gene.organism.commonName\", \"D.*\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.GREATER_THAN
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.length" op="&gt;" value="1024"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintGreaterThan() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.length\" op=\"&gt;\" value=\"1024\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.greaterThan(\"Gene.length\", \"1024\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.GREATER_THAN_EQUALS
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.length" op="&gt;=" value="1024"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintGreaterThanEqualTo() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.length\" op=\"&gt;=\" value=\"1024\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.greaterThanEqualTo(\"Gene.length\", \"1024\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.LESS_THAN
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.length" op="&lt;" value="1024"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintLessThan() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.length\" op=\"&lt;\" value=\"1024\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.lessThan(\"Gene.length\", \"1024\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintAttribute
     * ConstraintOp.LESS_THAN_EQUALS
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.length" op="&lt;=" value="1024"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintLessThanEqualTo() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.length\" op=\"&lt;=\" value=\"1024\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.lessThanEqualTo(\"Gene.length\", \"1024\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintLookup
     * ConstraintOp.LOOKUP
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene" op="LOOKUP" value="zen" extraValue="C. elegans"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintLookup() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene\" op=\"LOOKUP\" value=\"zen\" extraValue=\"C. elegans\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.lookup(\"Gene\", \"zen\", \"C. elegans\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintBag
     * ConstraintOp.IN
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene" op="IN" value="aList"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintIn() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene\" op=\"IN\" value=\"aList\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Only public lists are supported" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.in(\"Gene\", \"aList\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintBag
     * ConstraintOp.NOT_IN
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene" op="NOT IN" value="aList"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintNotIn() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene\" op=\"NOT IN\" value=\"aList\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Only public lists are supported" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.notIn(\"Gene\", \"aList\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintMultiValue
     * ConstraintOp.ONE_OF
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.organism.commonName" op="ONE OF">
     *	   <value>fruit fly
     *     </value>
     *     <value>honey bee
     *     </value>
     *   </constraint>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintOneOfValues() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\">" +
        "<constraint path=\"Gene.organism.commonName\" op=\"ONE OF\"><value>fruit fly</value><value>honey bee</value></constraint>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL +
        "import java.util.ArrayList;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "List<String> values = new ArrayList<String>();" + ENDL +
        INDENT + INDENT + "values.add(\"fruit fly\");" + ENDL +
        INDENT + INDENT + "values.add(\"honey bee\");" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.oneOfValues(\"Gene.organism.commonName\", values));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintMultiValue
     * ConstraintOp.NONE_OF
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.organism.commonName" op="NONE OF">
     *	   <value>fruit fly
     *     </value>
     *     <value>honey bee
     *     </value>
     *   </constraint>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintNoneOfValues() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.organism.commonName\" op=\"NONE OF\"><value>fruit fly</value><value>honey bee</value></constraint>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL +
        "import java.util.ArrayList;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "List<String> values = new ArrayList<String>();" + ENDL +
        INDENT + INDENT + "values.add(\"fruit fly\");" + ENDL +
        INDENT + INDENT + "values.add(\"honey bee\");" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.noneOfValues(\"Gene.organism.commonName\", values));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintNull
     * ConstraintOp.IS_NOT_NULL
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.primaryIdentifier" op="IS NOT NULL"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintIsNotNull() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.primaryIdentifier\" op=\"IS NOT NULL\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.isNotNull(\"Gene.primaryIdentifier\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintNull
     * ConstraintOp.IS_NULL
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.primaryIdentifier" op="IS NULL"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintIsNull() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.primaryIdentifier\" op=\"IS NULL\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.isNull(\"Gene.primaryIdentifier\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintLoop
     * ConstraintOp.EQUALS
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.proteins.genes" op="=" loopPath="Gene"/>
     * </query>
     *
     * loopPath equals to "InterMineObject", otherwise thrown java.lang.RuntimeException: Can't find class for class descriptor, Caused by: java.lang.ClassNotFoundException: org.intermine.model.bio.Gene.
     *
     */
    public void testPathQueryCodeGenerationWithConstraintEqualToLoop() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\">" +
        "<constraint path=\"Gene.proteins.genes\" op=\"=\" loopPath=\"InterMineObject\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

//        pathQuery.addConstraint(Constraints.equalToLoop("Gene.proteins.genes", "Gene"));

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.equalToLoop(\"Gene.proteins.genes\", \"InterMineObject\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintLoop
     * ConstraintOp.NOT_EQUALS
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.proteins.genes" op="!=" loopPath="Gene"/>
     * </query>
     */
    public void testPathQueryCodeGenerationWithConstraintNotEqualToLoop() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\">" +
        "<constraint path=\"Gene.proteins.genes\" op=\"!=\" loopPath=\"InterMineObject\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

//        pathQuery.addConstraint(Constraints.notEqualToLoop("Gene.proteins.genes", "Gene"));

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.notEqualToLoop(\"Gene.proteins.genes\", \"InterMineObject\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
    * This method tests when a path query has two or more constraints.
    *
    * Test PathQuery:
    * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc" constraintLogic="(A or B) and C">
    *   <constraint path="Gene.proteins.genes" code="A" op="!=" loopPath="Gene"/>
    *   <constraint path="Gene" code="B" op="LOOKUP" value="zen" extraValue=""/>
    *   <constraint path="Gene.organism.commonName" code="C" op="ONE OF">
    *     <value>fruit fly
    *     </value>
    *     <value>honey bee
    *     </value>
    *   </constraint>
    * </query>
    */
    public void testPathQueryCodeGenerationWithTwoOrMoreConstraints() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\" constraintLogic=\"(A or B) and C\">" +
        "<constraint path=\"Gene.proteins.genes\" code=\"A\" op=\"!=\" loopPath=\"InterMineObject\"/>" +
        "<constraint path=\"Gene\" code=\"B\" op=\"LOOKUP\" value=\"zen\" extraValue=\"\"/>" +
        "<constraint path=\"Gene.organism.commonName\" code=\"C\" op=\"ONE OF\"><value>fruit fly</value><value>honey bee</value></constraint>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

//        pathQuery.addConstraint(Constraints.notEqualToLoop("Gene.proteins.genes", "Gene"), "A");
//        pathQuery.setConstraintLogic("(A or B) and C");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL +
        "import java.util.ArrayList;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL +
        "import org.intermine.pathquery.Constraints;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M query." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class QueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "* @throws IOException" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL +
        INDENT + INDENT + "QueryService service =" + ENDL +
        INDENT + INDENT + INDENT + "new ServiceFactory(serviceRootUrl, \"QueryService\").getQueryService();" + ENDL +
        INDENT + INDENT + "Model model = getModel();" + ENDL +
        INDENT + INDENT + "PathQuery query = new PathQuery(model);" + ENDL + ENDL +
        INDENT + INDENT + "// Add views" + ENDL +
        INDENT + INDENT + "query.addViews(\"Gene.primaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.secondaryIdentifier\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.symbol\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.name\"," + ENDL +
        INDENT + INDENT + INDENT + INDENT + "\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraints and you can edit the constraint values below" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.notEqualToLoop(\"Gene.proteins.genes\", \"InterMineObject\"), \"A\");" + ENDL + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.lookup(\"Gene\", \"zen\", \"\"), \"B\");" + ENDL + ENDL +
        INDENT + INDENT + "List<String> values = new ArrayList<String>();" + ENDL +
        INDENT + INDENT + "values.add(\"fruit fly\");" + ENDL +
        INDENT + INDENT + "values.add(\"honey bee\");" + ENDL +
        INDENT + INDENT + "query.addConstraint(Constraints.oneOfValues(\"Gene.organism.commonName\", values), \"C\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add constraintLogic" + ENDL +
        INDENT + INDENT + "query.setConstraintLogic(\"(A or B) and C\");" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintIds
     * ConstraintOp.IN
     *
     * Can not be tested
     *
     * Test PathQuery:
     */
    public void testPathQueryCodeGenerationWithConstraintInIds() {

    }

    /**
     * This method tests when a path query has one constraint - PathConstraintIds
     * ConstraintOp.NOT_IN
     *
     * Can not be tested
     *
     * Test PathQuery:
     */
    public void testPathQueryCodeGenerationWithConstraintNotInIds() {

    }

    /**
     * This method tests when a path query has one constraint - PathConstraintSubclass
     *
     * Can not be tested
     *
     * Test PathQuery:
     */
    public void testPathQueryCodeGenerationWithConstraintType() {

    }


    //****************************** Test TemplateQuery *********************************
    /**
    * This method tests when a template query is null.
    */
    public void testTemplateQueryCodeGenerationWithNullQuery() {
        TemplateQuery templateQuery = null;

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = NULL_QUERY;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintEq() {
        String queryXml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"=\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("im_available_organisms");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - im_available_organisms" + ENDL +
        SPACE + "* template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateImAvailableOrganisms" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"eq\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"im_available_organisms\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.NOT_EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintNeq() {
        String queryXml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"!=\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("im_available_organisms");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - im_available_organisms" + ENDL +
        SPACE + "* template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateImAvailableOrganisms" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"ne\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"im_available_organisms\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.LESS_THAN
     */
    public void testTemplateQueryCodeGenerationWithConstraintLessThan() {
        String queryXml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"&lt;\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("im_available_organisms");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - im_available_organisms" + ENDL +
        SPACE + "* template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateImAvailableOrganisms" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"lt\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"im_available_organisms\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.LESS_THAN_EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintLessThanEqualTo() {
        String queryXml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"&lt;=\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("im_available_organisms");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - im_available_organisms" + ENDL +
        SPACE + "* template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateImAvailableOrganisms" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"le\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"im_available_organisms\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.GREATER_THAN
     */
    public void testTemplateQueryCodeGenerationWithConstraintGreaterThan() {
        String queryXml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"&gt;\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("im_available_organisms");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - im_available_organisms" + ENDL +
        SPACE + "* template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateImAvailableOrganisms" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"gt\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"im_available_organisms\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.GREATER_THAN_EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintGreaterThanEqualTo() {
        String queryXml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"&gt;=\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("im_available_organisms");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - im_available_organisms" + ENDL +
        SPACE + "* template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateImAvailableOrganisms" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"ge\", \"zen\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"im_available_organisms\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.MATCHES
     */
    public void testTemplateQueryCodeGenerationWithConstraintLike() {
        String queryXml = "<template name=\"Organism_Gene\" title=\"Organism --&gt; All genes.\" longDescription=\"Show all the genes for a particular organism.\" comment=\"\">" +
            "<query name=\"Organism_Gene\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.primaryIdentifier\" longDescription=\"Show all the genes for a particular organism.\" sortOrder=\"Gene.secondaryIdentifier asc\">" +
            "<constraint path=\"Gene.organism.name\" editable=\"true\" description=\"Show all the genes from organism:\" op=\"LIKE\" value=\"Drosophila melanogas*\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Organism_Gene");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - Organism_Gene" + ENDL +
        SPACE + "* template description - Show all the genes for a particular organism." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateOrganismGene" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Constraint description - Show all the genes from organism:" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.organism.name\", \"LIKE\", \"Drosophila melanogas*\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"Organism_Gene\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.DOES_NOT_MATCH
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotLike() {
        String queryXml = "<template name=\"Organism_Gene\" title=\"Organism --&gt; All genes.\" longDescription=\"Show all the genes for a particular organism.\" comment=\"\">" +
            "<query name=\"Organism_Gene\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.primaryIdentifier\" longDescription=\"Show all the genes for a particular organism.\" sortOrder=\"Gene.secondaryIdentifier asc\">" +
            "<constraint path=\"Gene.organism.name\" editable=\"true\" description=\"Show all the genes from organism:\" op=\"NOT LIKE\" value=\"Drosophila melanogas*\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Organism_Gene");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - Organism_Gene" + ENDL +
        SPACE + "* template description - Show all the genes for a particular organism." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateOrganismGene" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Constraint description - Show all the genes from organism:" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.organism.name\", \"NOT LIKE\", \"Drosophila melanogas*\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"Organism_Gene\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintLookup
     * ConstraintOp.LOOKUP
     */
    public void testTemplateQueryCodeGenerationWithConstraintLookup() {
        String queryXml = "<template name=\"Clone_gene\" title=\"Clone --&gt; Gene\" longDescription=\"For a cDNA clone or list of clones give the corresponding gene identifiers.\" comment=\"\">" +
            "<query name=\"Clone_gene\" model=\"genomic\" view=\"CDNAClone.primaryIdentifier CDNAClone.gene.primaryIdentifier CDNAClone.gene.secondaryIdentifier CDNAClone.gene.symbol\" longDescription=\"For a cDNA clone or list of clones give the corresponding gene identifiers.\" sortOrder=\"CDNAClone.primaryIdentifier asc\">" +
            "<join path=\"CDNAClone.gene\" style=\"OUTER\"/>" +
            "<constraint path=\"CDNAClone\" editable=\"true\" description=\"Show corresponding genes for clone(s):\" op=\"LOOKUP\" value=\"LD14383\" extraValue=\"H. sapiens\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Clone_gene");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - Clone_gene" + ENDL +
        SPACE + "* template description - For a cDNA clone or list of clones give the corresponding gene identifiers." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateCloneGene" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Constraint description - Show corresponding genes for clone(s):" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"CDNAClone\", \"LOOKUP\", \"LD14383\", \"H. sapiens\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"Clone_gene\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintBag
     * ConstraintOp.IN
     */
    public void testTemplateQueryCodeGenerationWithConstraintIn() {
        String queryXml = "<template name=\"Gene_ExonLocation2\" title=\"Gene --&gt; Exons.\" longDescription=\"For a specific gene, show its exons with their chromosomal locations and lengths.\" comment=\"07.02.07:re-written to work from gene identifier -Rachel. 06.06.07 updated to work from gene class - Philip\">" +
            "<query name=\"Gene_ExonLocation2\" model=\"genomic\" view=\"Gene.primaryIdentifier Gene.symbol Gene.exons.primaryIdentifier Gene.exons.length Gene.exons.chromosome.primaryIdentifier Gene.exons.chromosomeLocation.start Gene.exons.chromosomeLocation.end Gene.exons.chromosomeLocation.strand\" longDescription=\"For a specific gene, show its exons with their chromosomal locations and lengths.\" sortOrder=\"Gene.primaryIdentifier asc\">" +
            "<pathDescription pathString=\"Gene.exons\" description=\"Exon\"/>" +
            "<pathDescription pathString=\"Gene.exons.chromosome\" description=\"Chromosome\"/>" +
            "<pathDescription pathString=\"Gene.exons.chromosomeLocation\" description=\"Exon &gt; chromosome location\"/>" +
            "<constraint path=\"Gene\" editable=\"true\" description=\"Show the chromosome location of the exons of gene:\" op=\"IN\" value=\"aList\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Gene_ExonLocation2");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = TEMPLATE_BAG_CONSTRAINT;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintBag
     * ConstraintOp.NOT_IN
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotIn() {
        String queryXml = "<template name=\"Gene_ExonLocation2\" title=\"Gene --&gt; Exons.\" longDescription=\"For a specific gene, show its exons with their chromosomal locations and lengths.\" comment=\"07.02.07:re-written to work from gene identifier -Rachel. 06.06.07 updated to work from gene class - Philip\">" +
            "<query name=\"Gene_ExonLocation2\" model=\"genomic\" view=\"Gene.primaryIdentifier Gene.symbol Gene.exons.primaryIdentifier Gene.exons.length Gene.exons.chromosome.primaryIdentifier Gene.exons.chromosomeLocation.start Gene.exons.chromosomeLocation.end Gene.exons.chromosomeLocation.strand\" longDescription=\"For a specific gene, show its exons with their chromosomal locations and lengths.\" sortOrder=\"Gene.primaryIdentifier asc\">" +
            "<pathDescription pathString=\"Gene.exons\" description=\"Exon\"/>" +
            "<pathDescription pathString=\"Gene.exons.chromosome\" description=\"Chromosome\"/>" +
            "<pathDescription pathString=\"Gene.exons.chromosomeLocation\" description=\"Exon &gt; chromosome location\"/>" +
            "<constraint path=\"Gene\" editable=\"true\" description=\"Show the chromosome location of the exons of gene:\" op=\"NOT IN\" value=\"aList\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Gene_ExonLocation2");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = TEMPLATE_BAG_CONSTRAINT;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintMultiValue
     * ConstraintOp.ONE_OF
     */
    public void testTemplateQueryCodeGenerationWithConstraintOneOfValues() {
        String queryXml = "<template name=\"Organism_Gene\" title=\"Organism --&gt; All genes.\" longDescription=\"Show all the genes for a particular organism.\" comment=\"\">" +
            "<query name=\"Organism_Gene\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.primaryIdentifier\" longDescription=\"Show all the genes for a particular organism.\" sortOrder=\"Gene.secondaryIdentifier asc\">" +
            "<constraint path=\"Gene.organism.name\" editable=\"true\" description=\"Show all the genes from organism:\" op=\"ONE OF\">" +
            "<value>Caenorhabditis elegans</value>" +
            "<value>Drosophila melanogaster</value>" +
            "</constraint>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Organism_Gene");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - Organism_Gene" + ENDL +
        SPACE + "* template description - Show all the genes for a particular organism." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateOrganismGene" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Constraint description - Show all the genes from organism:" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.organism.name\", \"ONE OF\", \"Caenorhabditis elegans,Drosophila melanogaster\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"Organism_Gene\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintMultiValue
     * ConstraintOp.NONE_OF
     */
    public void testTemplateQueryCodeGenerationWithConstraintNoneOfValues() {
        String queryXml = "<template name=\"Organism_Gene\" title=\"Organism --&gt; All genes.\" longDescription=\"Show all the genes for a particular organism.\" comment=\"\">" +
            "<query name=\"Organism_Gene\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.primaryIdentifier\" longDescription=\"Show all the genes for a particular organism.\" sortOrder=\"Gene.secondaryIdentifier asc\">" +
            "<constraint path=\"Gene.organism.name\" editable=\"true\" description=\"Show all the genes from organism:\" op=\"NONE OF\">" +
            "<value>Caenorhabditis elegans</value>" +
            "<value>Drosophila melanogaster</value>" +
            "</constraint>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Organism_Gene");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - Organism_Gene" + ENDL +
        SPACE + "* template description - Show all the genes for a particular organism." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateOrganismGene" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "// Constraint description - Show all the genes from organism:" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.organism.name\", \"NONE OF\", \"Caenorhabditis elegans,Drosophila melanogaster\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"Organism_Gene\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintNull
     * ConstraintOp.IS_NOT_NULL
     */
    public void testTemplateQueryCodeGenerationWithConstraintIsNotNull() {
        String queryXml = "<template name=\"AAANotNull\" title=\"AAANotNull\" longDescription=\"\" comment=\"\">" +
            "<query name=\"AAANotNull\" model=\"genomic\" view=\"Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" longDescription=\"\" sortOrder=\"Gene.primaryIdentifier asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" op=\"IS NOT NULL\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("AAANotNull");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - AAANotNull" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateAAANotNull" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"IS NOT NULL\", \"IS NOT NULL\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"AAANotNull\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path template has one constraint - PathConstraintNull
     * ConstraintOp.IS_NULL
     */
    public void testTemplateQueryCodeGenerationWithConstraintIsNull() {
        String queryXml = "<template name=\"AAANotNull\" title=\"AAANotNull\" longDescription=\"\" comment=\"\">" +
            "<query name=\"AAANotNull\" model=\"genomic\" view=\"Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" longDescription=\"\" sortOrder=\"Gene.primaryIdentifier asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" op=\"IS NULL\"/>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("AAANotNull");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - AAANotNull" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateAAANotNull" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.primaryIdentifier\", \"IS NULL\", \"IS NULL\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"AAANotNull\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has two and more constraints.
     */
    public void testTemplateQueryCodeGenerationWithTwoAndMoreConstraints() {
        String queryXml = "<template name=\"Gene_OrthologueOrganism_new\" title=\"Gene --&gt; Orthologues in one specific organism.\" longDescription=\"For a particular gene, show predicted orthologues in one particular organism.  \" comment=\"Orthologues-&gt;subject-&gt;identifier removed for workshop (as dpse identifiers missing so made it confusing) (Rachel);  07.02.07: updated to run from gene identifier - Rachel. 13/03/07 added orthologue organismDbId. Philip 070607 updated to work from gene class - Philip\">" +
            "<query name=\"Gene_OrthologueOrganism_new\" model=\"genomic\" view=\"Gene.secondaryIdentifier Gene.symbol Gene.homologues.homologue.secondaryIdentifier Gene.homologues.homologue.symbol Gene.homologues.type\" longDescription=\"For a particular gene, show predicted orthologues in one particular organism.  \" sortOrder=\"Gene.secondaryIdentifier asc\" constraintLogic=\"D and A\">" +
            "<pathDescription pathString=\"Gene.homologues\" description=\"Homologue\"/>" +
            "<pathDescription pathString=\"Gene.homologues.homologue\" description=\"Homologue\"/>" +
            "<constraint path=\"Gene\" code=\"D\" editable=\"true\" description=\"\" op=\"LOOKUP\" value=\"lin-28\" extraValue=\"D. melanogaster\"/>" +
            "<constraint path=\"Gene.homologues.homologue.organism.name\" code=\"A\" editable=\"true\" description=\"In organism:\" op=\"ONE OF\">" +
            "<value>Homo sapiens</value>" +
            "<value>Mus musculus</value>" +
            "</constraint>" +
            "</query>" +
            "</template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(queryXml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.get("Gene_OrthologueOrganism_new");

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, null);

        String expected = "package modminetest2m;" + ENDL + ENDL +
        "import java.util.ArrayList;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.TemplateService;" + ENDL +
        "import org.intermine.webservice.client.template.TemplateParameter;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* This is an automatically generated Java program to run the modMine_Test-2.M template." + ENDL +
        SPACE + "* template name - Gene_OrthologueOrganism_new" + ENDL +
        SPACE + "* template description - For a particular gene, show predicted orthologues in one particular organism.  " + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @author modMine_Test-2.M" + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "*/" + ENDL +
        "public class TemplateGeneOrthologueOrganismNew" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://newt.flymine.org:8080/modminepreview/service\";" + ENDL + ENDL +
        INDENT + "/**" + ENDL +
        INDENT + SPACE + "* @param args command line arguments" + ENDL +
        INDENT + SPACE + "*/" + ENDL +
        INDENT + "public static void main(String[] args) {" + ENDL + ENDL +
        INDENT + INDENT + "TemplateService service = new ServiceFactory(serviceRootUrl, \"TemplateService\").getTemplateService();" + ENDL + ENDL +
        INDENT + INDENT + "List<TemplateParameter> parameters = new ArrayList<TemplateParameter>();" + ENDL + ENDL +
        INDENT + INDENT + "// You can edit the constraint values below" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene\", \"LOOKUP\", \"lin-28\", \"D. melanogaster\"));" + ENDL +
        INDENT + INDENT + "// Constraint description - In organism:" + ENDL +
        INDENT + INDENT + "parameters.add(new TemplateParameter(\"Gene.homologues.homologue.organism.name\", \"ONE OF\", \"Homo sapiens,Mus musculus\"));" + ENDL + ENDL +
        INDENT + INDENT + "// Name of a public template, private templates are not supported at the moment" + ENDL +
        INDENT + INDENT + "String templateName = \"Gene_OrthologueOrganism_new\";" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 10000;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(templateName, parameters, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.print(\"Results: \\n\");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.print(\"\\n\");" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintIds
     * ConstraintOp.IN
     *
     * Can not be tested
     */
    public void testTemplateQueryCodeGenerationWithConstraintInIds() {

    }

    /**
     * This method tests when a template query has one constraint - PathConstraintIds
     * ConstraintOp.NOT_IN
     *
     * Can not be tested
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotInIds() {

    }

    /**
     * This method tests when a template query has one constraint - PathConstraintSubclass
     *
     * Can not be tested
     */
    public void testTemplateQueryCodeGenerationWithConstraintType() {

    }

    /**
     * This method tests when a template query has one constraint - PathConstraintLoop
     * ConstraintOp.EQUALS
     *
     * Uneditable
     */
    public void testTemplateQueryCodeGenerationWithConstraintEqualToLoop() {

    }

    /**
     * This method tests when a template query has one constraint - PathConstraintLoop
     * ConstraintOp.NOT_EQUALS
     *
     * Uneditable
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotEqualToLoop() {

    }

    // Other private methods can be added...
}
