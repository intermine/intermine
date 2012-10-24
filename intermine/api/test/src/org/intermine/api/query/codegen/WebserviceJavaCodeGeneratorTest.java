package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;

/**
 * Tests for the WebserviceJavaCodeGenerator class.
 *
 * @author Fengyuan Hu
 * @author Alexis Kalderimis
 */
public class WebserviceJavaCodeGeneratorTest extends TestCase
{
    private final String serviceRootURL = "TEST_SERVICE_ROOT";
    private final String projectTitle = "TEST_PROJECT_TITLE";
    private final String perlWSVersion = "TEST_WS_VERSION";

    private static final String DATE_PATTERN = "(Mon|Tue|Wed|Thu|Fri|Sat|Sun) \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\w+ 20\\d{2}";
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
        String className = getClass().getSimpleName();
        try {
            testProps.load(getClass().getResourceAsStream(className + ".properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not read test properties for " + className, e);
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
        doComparison(pathQuery, resource);
    }

    private void doComparison(PathQuery pathQuery, String resource) {
        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery, false);
        String expected = readExpected(resource).replaceAll(DATE_PATTERN, "__SOME-DATE__");

        assertEquals(expected, cg.generate(wsCodeGenInfo).replaceAll(DATE_PATTERN, "__SOME-DATE__"));
    }

    private void doPrivateComparison(String xml, String resource) {
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(xml), PathQuery.USERPROFILE_VERSION);
        doPrivateComparison(pathQuery, resource);
    }

    private void doPrivateComparison(PathQuery pathQuery, String resource) {
        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery, true);
        String expected = readExpected(resource).replaceAll(DATE_PATTERN, "__SOME-DATE__");

        assertEquals(expected, cg.generate(wsCodeGenInfo).replaceAll(DATE_PATTERN, "__SOME-DATE__"));
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
        return getGenInfo(pq, false);
    }
    private WebserviceCodeGenInfo getGenInfo(PathQuery pq, boolean isPrivate) {
        return new WebserviceCodeGenInfo(pq, serviceRootURL, projectTitle, perlWSVersion,
                !isPrivate, null);
    }

    //****************************** Test PathQuery *********************************
    /**
     * This method tests when a path query is null.
     */
    public void testPathQueryCodeGenerationWithNullQuery() {
        PathQuery pathQuery = null;
        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery);
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
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\"></query>";

        // Parse XML to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml),
                PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery);

        // Mock up
        pathQuery.clearView();
        String expected = testProps.getProperty("invalid.query");
        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    // Should get information about all the query's problems in a comment block
    public void testPathQueryCodeGenerationWithMultipleProblems() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Employee.name "
                + "Employee.age\" sortOrder=\"Employee.department.name asc\">"
                + "<constraint path=\"Foo\" op=\"=\" value=\"bar\"/>"
                + "</query>";

        // Parse XML to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml),
                PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo = getGenInfo(pathQuery);

        // Mock up
        pathQuery.clearView();
        String expected = testProps.getProperty("very.invalid.query");
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

    public void testPathQueryCodeGenerationWithConstraintInPrivate() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene\" op=\"IN\" value=\"aList\"/>" +
        "</query>";
        doPrivateComparison(queryXml, "private-in-constraint");
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
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name " +
        "Employee.department.name\">" +
        "<constraint path=\"Employee.department.manager\" op=\"=\" loopPath=\"Employee.department.company.CEO\"/>" +
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
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name " +
                "Employee.department.name\">" +
                "<constraint path=\"Employee.department.manager\" op=\"!=\" loopPath=\"Employee.department.company.CEO\"/>" +
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
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.department.name\" " +
        "constraintLogic=\"(A or C) and B\">" +
        "<constraint path=\"Employee.department.manager\" code=\"A\" op=\"!=\" loopPath=\"Employee.department.company.CEO\"/>" +
        "<constraint path=\"Employee\" code=\"B\" op=\"LOOKUP\" value=\"M*\" extraValue=\"\"/>" +
        "<constraint path=\"Employee.department.name\" code=\"C\" op=\"ONE OF\"><value>Sales</value><value>Warehouse</value></constraint>" +
        "</query>";
        doComparison(queryXml, "multiple-constraints");
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintIds
     * ConstraintOp.IN
     *
     * Test PathQuery:
     */
    public void testPathQueryCodeGenerationWithConstraintInIds() {
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        pq.addConstraint(Constraints.inIds("Employee", Arrays.asList(1, 2, 3, 4, 5)));

        doComparison(pq, "inids-query");
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintIds
     * ConstraintOp.NOT_IN
     *
     * Test PathQuery:
     */
    public void testPathQueryCodeGenerationWithConstraintNotInIds() {
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        pq.addConstraint(Constraints.notInIds("Employee", Arrays.asList(1, 2, 3, 4, 5)));

        doComparison(pq, "inids-query");
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintSubclass
     *
     */
    public void testPathQueryCodeGenerationWithSubClassConstraint() {
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name " +
        "Employee.age\">" +
        "<constraint path=\"Employee.department.manager\" type=\"CEO\"/>" +
        "</query>";
        doComparison(queryXml, "subclass-constraint");
    }

    /**
     * This method tests when a path query has one constraint - PathConstraintSubclass
     *
     */
    public void testPathQueryCodeGenerationWithNecessarySubClassConstraint() {
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name " +
        "Employee.age Employee.department.manager.salary\">" + // accessing CEO.salary from CEO
        "<constraint path=\"Employee.department.manager\" type=\"CEO\"/>" +
        "</query>";
        doComparison(queryXml, "necessary-subclass-constraint");
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
        Map<String, TemplateQuery> tqs = TemplateQueryBinding.unmarshalTemplates(new StringReader(xml), PathQuery.USERPROFILE_VERSION);
        TemplateQuery templateQuery = (TemplateQuery) tqs.values().toArray()[0];
        doTemplateComparison(templateQuery, resource);
    }

    private void doTemplateComparison(TemplateQuery templateQuery, String resource) {
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
        doTemplateComparison(xml, "in-template");
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
        doTemplateComparison(xml, "not-in-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintMultiValue
     * ConstraintOp.ONE_OF
     */
    public void testTemplateQueryCodeGenerationWithConstraintOneOfValues() {
        String xml =
            "<template name=\"TEMP_NAME\"><query model=\"testmodel\" view=\"Employee.name\">" +
            "<constraint path=\"Employee.name\" editable=\"true\" op=\"ONE OF\">" +
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
            "<constraint path=\"Employee.name\" editable=\"true\" op=\"NONE OF\">" +
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
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        String code = pq.addConstraint(Constraints.inIds("Employee", Arrays.asList(1, 2, 3, 4, 5)));

        TemplateQuery tq = new TemplateQuery("TEMP_NAME", "TEMP_TITLE", "TEMP_DESC", pq);
        tq.setEditable(tq.getConstraintForCode(code), true);

        doTemplateComparison(tq, "inids-query");

    }

    /**
     * This method tests when a template query has one constraint - PathConstraintIds
     * ConstraintOp.NOT_IN
     *
     * Can not be tested
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotInIds() {
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        String code = pq.addConstraint(Constraints.notInIds("Employee", Arrays.asList(1, 2, 3, 4, 5)));

        TemplateQuery tq = new TemplateQuery("TEMP_NAME", "TEMP_TITLE", "TEMP_DESC", pq);
        tq.setEditable(tq.getConstraintForCode(code), true);

        doTemplateComparison(tq, "inids-query");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintSubclass
     *
     */
    public void testTemplateQueryCodeGenerationWithSubClassConstraint() {
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        pq.addConstraint(Constraints.type("Employee.department.manager", "CEO"));

        TemplateQuery tq = new TemplateQuery("TEMP_NAME", "TEMP_TITLE", "TEMP_DESC", pq);

        doTemplateComparison(tq, "no-editable-constraints");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintLoop
     * ConstraintOp.EQUALS
     *
     */
    public void testTemplateQueryCodeGenerationWithConstraintEqualToLoop() {
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        String code = pq.addConstraint(Constraints.equalToLoop("Employee.department.manager", "Employee.department.company.CEO"));

        TemplateQuery tq = new TemplateQuery("TEMP_NAME", "TEMP_TITLE", "TEMP_DESC", pq);
        tq.setEditable(tq.getConstraintForCode(code), true);

        doTemplateComparison(tq, "loop-template");
    }

    /**
     * This method tests when a template query has one constraint - PathConstraintLoop
     * ConstraintOp.NOT_EQUALS
     *
     * Uneditable
     */
    public void testTemplateQueryCodeGenerationWithConstraintNotEqualToLoop() {
        Model testmodel = Model.getInstanceByName("testmodel");
        PathQuery pq = new PathQuery(testmodel);
        pq.addViews("Employee.name", "Employee.age");
        String code = pq.addConstraint(Constraints.notEqualToLoop("Employee.department.manager", "Employee.department.company.CEO"));

        TemplateQuery tq = new TemplateQuery("TEMP_NAME", "TEMP_TITLE", "TEMP_DESC", pq);
        tq.setEditable(tq.getConstraintForCode(code), true);

        doTemplateComparison(tq, "loop-template");
    }

    // Other private methods can be added...
}
