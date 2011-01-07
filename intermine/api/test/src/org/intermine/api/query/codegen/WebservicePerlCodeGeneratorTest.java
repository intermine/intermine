package org.intermine.api.query.codegen;

/*
 * Copyright (C) 2002-2011 modMine_Test-2.M
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.Map;

import org.intermine.api.template.TemplateQuery;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

import junit.framework.TestCase;

/**
 * Tests for the WebservicePerlCodeGenerator class.
 *
 * @author Fengyuan Hu
 */
public class WebservicePerlCodeGeneratorTest extends TestCase {

    private String INDENT = WebservicePerlCodeGenerator.INDENT;
    private String ENDL = WebservicePerlCodeGenerator.ENDL;

    private String INVALID_QUERY = WebservicePerlCodeGenerator.INVALID_QUERY;
    private String NULL_QUERY = WebservicePerlCodeGenerator.NULL_QUERY;
    private String TEMPLATE_BAG_CONSTRAINT = WebservicePerlCodeGenerator.TEMPLATE_BAG_CONSTRAINT;
    private String PATH_BAG_CONSTRAINT = WebservicePerlCodeGenerator.PATH_BAG_CONSTRAINT;
    private String LOOP_CONSTRAINT = WebservicePerlCodeGenerator.LOOP_CONSTRAINT;

    private String serviceRootURL = "http://newt.flymine.org:8080/modminepreview";
    private String projectTitle = "modMine_Test-2.M";
    private String perlWSModuleVer = "0.9412";

    private WebservicePerlCodeGenerator cg;

    public WebservicePerlCodeGeneratorTest(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     * (Called before every test case method.)
     */
    public void setUp() {
        cg = new WebservicePerlCodeGenerator();
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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# Join status" + ENDL +
        "$query->add_join(" + ENDL +
        INDENT + "path => 'Gene.organism'," + ENDL +
        INDENT + "style => 'OUTER'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.secondaryIdentifier'," + ENDL +
        INDENT + "op    => '='," + ENDL +
        INDENT + "value => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.secondaryIdentifier'," + ENDL +
        INDENT + "op    => '!='," + ENDL +
        INDENT + "value => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.organism.commonName'," + ENDL +
        INDENT + "op    => 'LIKE'," + ENDL +
        INDENT + "value => 'D.*'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.organism.commonName'," + ENDL +
        INDENT + "op    => 'NOT LIKE'," + ENDL +
        INDENT + "value => 'D.*'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.length'," + ENDL +
        INDENT + "op    => '>'," + ENDL +
        INDENT + "value => '1024'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.length'," + ENDL +
        INDENT + "op    => '>='," + ENDL +
        INDENT + "value => '1024'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.length'," + ENDL +
        INDENT + "op    => '<'," + ENDL +
        INDENT + "value => '1024'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.length'," + ENDL +
        INDENT + "op    => '<='," + ENDL +
        INDENT + "value => '1024'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene'," + ENDL +
        INDENT + "op    => 'LOOKUP'," + ENDL +
        INDENT + "value => 'zen'," + ENDL +
        INDENT + "extra_value => 'C. elegans'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = PATH_BAG_CONSTRAINT;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = PATH_BAG_CONSTRAINT;

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
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.organism.commonName\" op=\"ONE OF\"><value>fruit fly</value><value>honey bee</value></constraint>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.organism.commonName'," + ENDL +
        INDENT + "op    => 'ONE OF'," + ENDL +
        INDENT + "value => [" + ENDL +
        INDENT + INDENT + "'fruit fly'," + ENDL +
        INDENT + INDENT + "'honey bee'," + ENDL +
        INDENT + "]," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.organism.commonName'," + ENDL +
        INDENT + "op    => 'NONE OF'," + ENDL +
        INDENT + "value => [" + ENDL +
        INDENT + INDENT + "'fruit fly'," + ENDL +
        INDENT + INDENT + "'honey bee'," + ENDL +
        INDENT + "]," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.primaryIdentifier'," + ENDL +
        INDENT + "op    => 'IS NOT NULL'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.primaryIdentifier'," + ENDL +
        INDENT + "op    => 'IS NULL'," + ENDL +
        ");" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
     */
    public void testPathQueryCodeGenerationWithConstraintEqualToLoop() {
        String queryXml = "<query name=\"\" model=\"genomic\" view=\"Gene.primaryIdentifier " +
        "Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName\" " +
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.proteins.genes\" op=\"=\" loopPath=\"InterMineObject\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = LOOP_CONSTRAINT;

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
        "sortOrder=\"Gene.primaryIdentifier asc\"><constraint path=\"Gene.proteins.genes\" op=\"!=\" loopPath=\"InterMineObject\"/>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = LOOP_CONSTRAINT;

        assertEquals(expected, cg.generate(wsCodeGenInfo));
    }

    /**
    * This method tests when a path query has two or more constraints.
    *
    * Test PathQuery:
    * <query name="" model="genomic" view="Gene.primaryIdentifier Gene.secondaryIdentifier Gene.symbol Gene.name Gene.organism.shortName" sortOrder="Gene.primaryIdentifier asc" constraintLogic="(A or B) and C">
    *   <constraint path="Gene.organism.shortName" code="A" op="=" value="D. melanogaster"/>
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
        "<constraint path=\"Gene.organism.shortName\" code=\"A\" op=\"=\" value=\"D. melanogaster\"/>" +
        "<constraint path=\"Gene\" code=\"B\" op=\"LOOKUP\" value=\"zen\" extraValue=\"\"/>" +
        "<constraint path=\"Gene.organism.commonName\" code=\"C\" op=\"ONE OF\"><value>fruit fly</value><value>honey bee</value></constraint>" +
        "</query>";
        // Parse xml to PathQuery - PathQueryBinding
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(
                new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);

        WebserviceCodeGenInfo wsCodeGenInfo =
            new WebserviceCodeGenInfo(pathQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M query" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# query description - no description" + ENDL +
        "my $query = Webservice::InterMine->new_query;" + ENDL + ENDL +
        "# The view specifies the output columns" + ENDL +
        "$query->add_view(qw/" + ENDL +
        INDENT + "Gene.primaryIdentifier" + ENDL +
        INDENT + "Gene.secondaryIdentifier" + ENDL +
        INDENT + "Gene.symbol" + ENDL +
        INDENT + "Gene.name" + ENDL +
        INDENT + "Gene.organism.shortName" + ENDL +
        "/);" + ENDL + ENDL +
        "# Sort by" + ENDL +
        "$query->set_sort_order('Gene.primaryIdentifier' => 'ASC');" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.organism.shortName'," + ENDL +
        INDENT + "op    => '='," + ENDL +
        INDENT + "value => 'D. melanogaster'," + ENDL +
        INDENT + "code => 'A'," + ENDL +
        ");" + ENDL + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene'," + ENDL +
        INDENT + "op    => 'LOOKUP'," + ENDL +
        INDENT + "value => 'zen'," + ENDL +
        INDENT + "extra_value => ''," + ENDL +
        INDENT + "code => 'B'," + ENDL +
        ");" + ENDL + ENDL +
        "$query->add_constraint(" + ENDL +
        INDENT + "path  => 'Gene.organism.commonName'," + ENDL +
        INDENT + "op    => 'ONE OF'," + ENDL +
        INDENT + "value => [" + ENDL +
        INDENT + INDENT + "'fruit fly'," + ENDL +
        INDENT + INDENT + "'honey bee'," + ENDL +
        INDENT + "]," + ENDL +
        INDENT + "code => 'C'," + ENDL +
        ");" + ENDL + ENDL +
        "# Constraint Logic" + ENDL +
        "$query->logic('(A or B) and C');" + ENDL + ENDL +
        "print $query->results(as => 'string').\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - im_available_organisms" + ENDL +
        "# template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('im_available_organisms')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => '='," + ENDL +
        INDENT + "valueA => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - im_available_organisms" + ENDL +
        "# template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('im_available_organisms')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => '!='," + ENDL +
        INDENT + "valueA => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - im_available_organisms" + ENDL +
        "# template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('im_available_organisms')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => '<'," + ENDL +
        INDENT + "valueA => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - im_available_organisms" + ENDL +
        "# template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('im_available_organisms')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => '<='," + ENDL +
        INDENT + "valueA => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - im_available_organisms" + ENDL +
        "# template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('im_available_organisms')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => '>'," + ENDL +
        INDENT + "valueA => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - im_available_organisms" + ENDL +
        "# template description - For all genes, list the taxonIds available.  Used by webservice to construct links to other intermines." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('im_available_organisms')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => '>='," + ENDL +
        INDENT + "valueA => 'zen'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - Organism_Gene" + ENDL +
        "# template description - Show all the genes for a particular organism." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('Organism_Gene')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.organism.name    Show all the genes from organism:" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'LIKE'," + ENDL +
        INDENT + "valueA => 'Drosophila melanogas*'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - Organism_Gene" + ENDL +
        "# template description - Show all the genes for a particular organism." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('Organism_Gene')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.organism.name    Show all the genes from organism:" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'NOT LIKE'," + ENDL +
        INDENT + "valueA => 'Drosophila melanogas*'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - Clone_gene" + ENDL +
        "# template description - For a cDNA clone or list of clones give the corresponding gene identifiers." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('Clone_gene')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    CDNAClone    Show corresponding genes for clone(s):" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'LOOKUP'," + ENDL +
        INDENT + "valueA => 'LD14383'," + ENDL +
        INDENT + "extra_valueA => 'H. sapiens'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - Organism_Gene" + ENDL +
        "# template description - Show all the genes for a particular organism." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('Organism_Gene')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.organism.name    Show all the genes from organism:" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'ONE OF'," + ENDL +
        INDENT + "valueA => [" + ENDL +
        INDENT + INDENT + "'Caenorhabditis elegans'," + ENDL +
        INDENT + INDENT + "'Drosophila melanogaster'," + ENDL +
        INDENT + "]," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - Organism_Gene" + ENDL +
        "# template description - Show all the genes for a particular organism." + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('Organism_Gene')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.organism.name    Show all the genes from organism:" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'NONE OF'," + ENDL +
        INDENT + "valueA => [" + ENDL +
        INDENT + INDENT + "'Caenorhabditis elegans'," + ENDL +
        INDENT + INDENT + "'Drosophila melanogaster'," + ENDL +
        INDENT + "]," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - AAANotNull" + ENDL +
        "# template description - no description" + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('AAANotNull')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'IS NOT NULL'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - AAANotNull" + ENDL +
        "# template description - no description" + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('AAANotNull')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# A    Gene.primaryIdentifier    no constraint description" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opA    => 'IS NULL'," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
            new WebserviceCodeGenInfo(templateQuery, serviceRootURL, projectTitle, perlWSModuleVer);

        String expected = "use Webservice::InterMine 0.9412 'http://newt.flymine.org:8080/modminepreview/service';" + ENDL + ENDL +
        "# This is an automatically generated script to run the modMine_Test-2.M template" + ENDL +
        "# You should install the Webservice::InterMine modules to run this example, e.g. sudo cpan Webservice::InterMine" + ENDL + ENDL +
        "# template name - Gene_OrthologueOrganism_new" + ENDL +
        "# template description - For a particular gene, show predicted orthologues in one particular organism.  " + ENDL + ENDL +
        "my $template = Webservice::InterMine->template('Gene_OrthologueOrganism_new')" + ENDL +
        INDENT + "or die 'Could not find template';" + ENDL + ENDL +
        "# You can edit the constraint values below" + ENDL +
        "# D    Gene    no constraint description" + ENDL +
        "# A    Gene.homologues.homologue.organism.name    In organism:" + ENDL + ENDL +
        "my $results = $template->results_with(" + ENDL +
        INDENT + "as     => 'string'," + ENDL +
        INDENT + "opD    => 'LOOKUP'," + ENDL +
        INDENT + "valueD => 'lin-28'," + ENDL +
        INDENT + "extra_valueD => 'D. melanogaster'," + ENDL +
        INDENT + "opA    => 'ONE OF'," + ENDL +
        INDENT + "valueA => [" + ENDL +
        INDENT + INDENT + "'Homo sapiens'," + ENDL +
        INDENT + INDENT + "'Mus musculus'," + ENDL +
        INDENT + "]," + ENDL +
        ");" + ENDL + ENDL +
        "print $results.\"\\n\";" + ENDL;

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
