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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

/**
 * Tests for the WebserviceJavaCodeGenerator class.
 *
 * @author Fengyuan Hu
 * @author Alexis Kalderimis
 */
public class WebserviceJavaCodeGeneratorTest extends TestCase {

    private final String TEMPLATE_BAG_CONSTRAINT = WebserviceJavaCodeGenerator.TEMPLATE_BAG_CONSTRAINT;

    private final String serviceRootURL = "TEST_SERVICE_ROOT";
    private final String projectTitle = "TEST_PROJECT_TITLE";
    private final String perlWSVersion = "TEST_WS_VERSION";

    protected String lang;

    protected WebserviceCodeGenerator cg;

    private final Properties testProps = new Properties();

    public WebserviceJavaCodeGeneratorTest() {
        super();
        init();
    }

    public WebserviceJavaCodeGeneratorTest(String testName) {
        super(testName);
        init();
    }

    private void init() {
        try {
            testProps.load(getClass().getResourceAsStream("WebserviceJavaCodeGeneratorTest.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not read test properties", e);
        }
    }

    private String readExpected(String name) {
        String filename = name + "." + this.lang + ".expected";
        InputStream is = getClass().getResourceAsStream(filename);
        StringWriter sw = new StringWriter();
        try {
            IOUtils.copy(is, sw);
        } catch (Exception e) {
            throw new RuntimeException("Could not read resource "+ filename);
        }
        return sw.toString();
    }

    private void doComparison(String xml, String resource) {
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(xml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery);

        String expected = readExpected(resource);
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }


    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    @Override
    public void setUp() {
        lang = "java";
        cg = new WebserviceJavaCodeGenerator();
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    @Override
    public void tearDown() {

    }

    private WebserviceCodeGenInfo getGenInfo(PathQuery pq) {
        return new WebserviceCodeGenInfo(pq, serviceRootURL, projectTitle, perlWSVersion,
                true, null);
    }

    //****************************** Test PathQuery *********************************
    /**
     * This method tests when a path query is null.
     */
    public void testPathQueryCodeGenerationWithNullQuery() {
        PathQuery pathQuery = null;


        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery); getGenInfo(pathQuery);
        String expected = testProps.getProperty("null.query");
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

        // Parse XML to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml),
                PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery);

        // Mock up
        pathQuery.clearView();
        String expected = testProps.getProperty("invalid.query");
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a path query has one view.
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier"
     *   sortOrder="Gene.primaryIdentifier asc">
     * </query>
     * @throws IOException
     *
     */
    public void testPathQueryCodeGenerationWithOneView() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"></query>";
        // Parse xml to PathQuery - PathQueryBinding
        doComparison(queryXml, "one-view");
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
        doComparison(queryXml, "no-constraints");
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
        doComparison(queryXml, "join-status");
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
        doComparison(queryXml, "without-sort-order");
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
        doComparison(queryXml, "eq-constraint");
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
        doComparison(queryXml, "neq-constraint");
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
        doComparison(queryXml, "like-constraint");
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
        doComparison(queryXml, "notlike-constraint");
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
        doComparison(queryXml, "gt-constraint");
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
        doComparison(queryXml, "ge-constraint");
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
        doComparison(queryXml, "lt-constraint");
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
        doComparison(queryXml, "le-constraint");
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
        doComparison(queryXml, "lookup-constraint");
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
        doComparison(queryXml, "in-constraint");
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
        doComparison(queryXml, "notin-constraint");
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintMultiValue
     * ConstraintOp.ONE_OF
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.organism.commonName" op="ONE OF">
     *       <value>fruit fly
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
        doComparison(queryXml, "oneof-constraint");
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintMultiValue
     * ConstraintOp.NONE_OF
     *
     * Test PathQuery:
     * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc">
     *   <constraint path="Gene.organism.commonName" op="NONE OF">
     *       <value>fruit fly
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
        doComparison(queryXml, "noneof-constraint");
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
        doComparison(queryXml, "notnull-constraint");
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
        doComparison(queryXml, "isnull-constraint");
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
        doComparison(queryXml, "loopeq-constraint");
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
        doComparison(queryXml, "loopne-constraint");
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
        doComparison(queryXml, "multiple-constraints");

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
     */
    public void testPathQueryCodeGenerationWithConstraintType() {
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name " +
        "Employee.age\">" +
        "<constraint path=\"Employee\" type=\"Manager\"/>" +
        "</query>";
        doComparison(queryXml, "subclass-constraint");
    }


    //****************************** Test TemplateQuery *********************************
    /**
    * This method tests when a template query is null.
    */
    public void testTemplateQueryCodeGenerationWithNullQuery() {
        TemplateQuery templateQuery = null;

        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(templateQuery);


        String expected = testProps.getProperty("null.query");

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    private void doTemplateComparison(String xml, String resource) {
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(xml), null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.values().toArray()[0];

        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(templateQuery);
        String expected = readExpected(resource);
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintEq() {
        String xml = "<template name=\"im_available_organisms\" title=\"All genes --&gt; TaxonId\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" comment=\"used for displaying links to other intermines\">" +
            "<query name=\"im_available_organisms\" model=\"genomic\" view=\"Gene.organism.shortName\" longDescription=\"For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines.\" sortOrder=\"Gene.organism.shortName asc\">" +
            "<constraint path=\"Gene.primaryIdentifier\" editable=\"true\" description=\"\" op=\"=\" value=\"zen\"/>" +
            "</query>" +
            "</template>";
        doTemplateComparison(xml, "eq-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.NOT_EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintNeq() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.age\" editable=\"true\" op=\"!=\" value=\"10\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "ne-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.LESS_THAN
     */
    public void testTemplateQueryCodeGenerationWithConstraintLessThan() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.age\" editable=\"true\" op=\"&lt;\" value=\"10\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "lt-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.LESS_THAN_EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintLessThanEqualTo() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.age\" editable=\"true\" op=\"&lt;=\" value=\"10\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "le-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.GREATER_THAN
     */
    public void testTemplateQueryCodeGenerationWithConstraintGreaterThan() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.age\" editable=\"true\" op=\"&gt;\" value=\"10\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "gt-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.GREATER_THAN_EQUALS
     */
    public void testTemplateQueryCodeGenerationWithConstraintGreaterThanEqualTo() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.age\" editable=\"true\" op=\"&gt;=\" value=\"10\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "ge-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.MATCHES
     */
    public void testTemplateQueryCodeGenerationWithConstraintLike() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.name\" editable=\"true\" op=\"LIKE\" value=\"Emp*\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "like-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintAttribute
     * ConstraintOp.DOES_NOT_MATCH
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotLike() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.name\" editable=\"true\" op=\"NOT LIKE\" value=\"Emp*\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "notlike-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintLookup
     * ConstraintOp.LOOKUP
     */
    public void testTemplateQueryCodeGenerationWithConstraintLookup() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee\" editable=\"true\" op=\"LOOKUP\" value=\"EmployeeA1\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "lookup-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintBag
     * ConstraintOp.IN
     */
    public void testTemplateQueryCodeGenerationWithConstraintIn() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee\" editable=\"true\" op=\"IN\" value=\"aList\"/>" +
            "</query></template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(xml),
                null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.values().toArray()[0];
        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(templateQuery);
        String expected = TEMPLATE_BAG_CONSTRAINT;
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintBag
     * ConstraintOp.NOT_IN
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotIn() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee\" editable=\"true\" op=\"NOT IN\" value=\"aList\"/>" +
            "</query></template>";
        // Parse xml to TemplateQuery - TemplateQueryBinding
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshal(new StringReader(xml),
                null, PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.values().toArray()[0];
        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(templateQuery);
        String expected = TEMPLATE_BAG_CONSTRAINT;
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintMultiValue
     * ConstraintOp.ONE_OF
     */
    public void testTemplateQueryCodeGenerationWithConstraintOneOfValues() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee\" editable=\"true\" op=\"ONE OF\">" +
            "<value>Employee A1</value><value>EmployeeA2</value></constraint>" +
            "</query></template>";
        doTemplateComparison(xml, "oneof-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintMultiValue
     * ConstraintOp.NONE_OF
     */
    public void testTemplateQueryCodeGenerationWithConstraintNoneOfValues() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee\" editable=\"true\" op=\"NONE OF\">" +
            "<value>Employee A1</value><value>EmployeeA2</value></constraint>" +
            "</query></template>";
        doTemplateComparison(xml, "noneof-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintNull
     * ConstraintOp.IS_NOT_NULL
     */
    public void testTemplateQueryCodeGenerationWithConstraintIsNotNull() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.department\" editable=\"true\" op=\"IS NOT NULL\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "notnull-template");
    }

    /**
     * This method tests when a path template has one constraint - PathConstraintNull
     * ConstraintOp.IS_NULL
     */
    public void testTemplateQueryCodeGenerationWithConstraintIsNull() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.end\" editable=\"true\" op=\"IS NULL\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "isnull-template");
    }

    /**
     * This method tests when a template query has two and more constraints, only some of which
     * are editable.
     */
    public void testTemplateQueryCodeGenerationWithTwoAndMoreConstraints() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.end\" editable=\"true\" op=\"IS NULL\"/>" +
            "<constraint path=\"Employee.name\" editable=\"true\" op=\"=\" value=\"Foo\"/>" +
            "<constraint path=\"Employee.age\" editable=\"false\" op=\"&gt;\" value=\"10\"/>" +
            "<constraint path=\"Employee\" editable=\"true\" op=\"LOOKUP\" value=\"EmployeeA1\" extraValue=\"DepartmentA\"/>" +
            "</query></template>";
        doTemplateComparison(xml, "multi-constraint-template");
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
