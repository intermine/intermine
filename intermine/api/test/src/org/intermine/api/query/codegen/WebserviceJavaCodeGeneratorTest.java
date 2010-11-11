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

import java.io.StringReader;

import org.intermine.api.InterMineAPI;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

import junit.framework.TestCase;

/**
 * Tests for the WebserviceJavaCodeGenerator class
 *
 * @author Fengyuan Hu
 */
public class WebserviceJavaCodeGeneratorTest extends TestCase {

    private String INDENT = WebserviceJavaCodeGenerator.INDENT;
    private String SPACE = WebserviceJavaCodeGenerator.SPACE;
    private String ENDL = WebserviceJavaCodeGenerator.ENDL;

    private String TEST_STRING = WebserviceJavaCodeGenerator.TEST_STRING;
    private String INVALID_QUERY = WebserviceJavaCodeGenerator.INVALID_QUERY;
    private String NULL_QUERY = WebserviceJavaCodeGenerator.NULL_QUERY;

    private WebserviceJavaCodeGenerator cg;

    public WebserviceJavaCodeGeneratorTest(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    public void setUp() {

    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    public void tearDown() {

    }

    /**
     * This method tests when a path query has no constraints
     */
    public void testPathQueryCodeGenerationWithNoConstraints() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
                "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
                "sortOrder=\"Gene.primaryIdentifier asc\"></query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        String expected = "package your.foo.bar.package.name;" + ENDL + ENDL +
        "import java.io.IOException;" + ENDL +
        "import java.util.List;" + ENDL + ENDL +
        "import org.intermine.metadata.Model;" + ENDL +
        "import org.intermine.webservice.client.core.ServiceFactory;" + ENDL +
        "import org.intermine.webservice.client.services.ModelService;" + ENDL +
        "import org.intermine.webservice.client.services.QueryService;" + ENDL +
        "import org.intermine.pathquery.PathQuery;" + ENDL +
        "import org.intermine.pathquery.OrderDirection;" + ENDL + ENDL +
        "/**" + ENDL +
        SPACE + "* Add some description to the class..." + ENDL +
        SPACE + "*" + ENDL +
        SPACE + "* @auther auther name" + ENDL +
        SPACE + "**/" + ENDL +
        "public class PathQueryClient" + ENDL +
        "{" + ENDL +
        INDENT + "private static String serviceRootUrl = \"http://<your webapp base url>/service\";" + ENDL + ENDL +
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
        INDENT + INDENT + "query.addView(\"Gene.primaryIdentifier\");" + ENDL +
        INDENT + INDENT + "query.addView(\"Gene.secondaryIdentifier\");" + ENDL +
        INDENT + INDENT + "query.addView(\"Gene.symbol\");" + ENDL +
        INDENT + INDENT + "query.addView(\"Gene.name\");" + ENDL +
        INDENT + INDENT + "query.addView(\"Gene.organism.shortName\");" + ENDL + ENDL +
        INDENT + INDENT + "// Add orderby" + ENDL +
        INDENT + INDENT + "query.addOrderBy(\"Gene.primaryIdentifier\", OrderDirection.ASC);" + ENDL + ENDL +
        INDENT + INDENT + "// Number of results are fetched" + ENDL +
        INDENT + INDENT + "int maxCount = 100;" + ENDL +
        INDENT + INDENT + "List<List<String>> result = service.getResult(query, maxCount);" + ENDL +
        INDENT + INDENT + "System.out.println(\"Results: \");" + ENDL +
        INDENT + INDENT + "for (List<String> row : result) {" + ENDL +
        INDENT + INDENT + INDENT + "for (String cell : row) {" + ENDL +
        INDENT + INDENT + INDENT + INDENT + "System.out.print(cell + \" \");" + ENDL +
        INDENT + INDENT + INDENT + "}" + ENDL +
        INDENT + INDENT + INDENT + "System.out.println();" + ENDL +
        INDENT + INDENT + "}" + ENDL +
        INDENT + "}" + ENDL + ENDL +
        INDENT + "private static Model getModel() {" + ENDL +
        INDENT + INDENT + "ModelService service = new ServiceFactory(serviceRootUrl, \"ModelService\").getModelService();" + ENDL +
        INDENT + INDENT + "return service.getModel();" + ENDL +
        INDENT + "}" + ENDL +
        "}" + ENDL;

        assertEquals(expected, cg.generate(pathQuery));
    }

    /**
     * This method tests when a path query is null.
     */
    public void testPathQueryCodeGenerationWithNullQuery() {
        PathQuery pathQuery = null;
        String expected = NULL_QUERY;
        assertEquals(expected, cg.generate(pathQuery));
    }

    /**
     * This method tests when a path query has no views.
     */
    public void testPathQueryCodeGenerationWithInvalidQuery() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"></query>";

        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);
        // Mock up
        pathQuery.clearView();
        String expected = INVALID_QUERY;
        assertEquals(expected, cg.generate(pathQuery));
    }

    public void testTemplateQueryCodeGeneration() {
        String queryXml = "";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        TemplateQueryBinding tqb = new TemplateQueryBinding();
        TemplateQuery tq = null;

        String expected = "";
        assertEquals(expected, cg.generate(tq));
    }

    public void testTemplateQueryCodeGenerationWithNullQuery() {
        TemplateQuery tq = null;
        String expected = NULL_QUERY;
        assertEquals(expected, cg.generate(tq));
    }

    // Other private methods can be added...
}
