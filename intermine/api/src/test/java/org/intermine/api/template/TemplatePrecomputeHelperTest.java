package org.intermine.api.template;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.query.MainHelper;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.template.TemplateQuery;
import org.intermine.template.xml.TemplateQueryBinding;

public class TemplatePrecomputeHelperTest extends TestCase {

    private Map<String, TemplateQuery> templates;

    public void setUp() throws Exception {
        super.setUp();
        Reader reader = new InputStreamReader(TemplatePrecomputeHelperTest.class.getClassLoader().getResourceAsStream("default-template-queries.xml"));
        templates = TemplateQueryBinding.unmarshalTemplates(reader, PathQuery.USERPROFILE_VERSION);
    }

    public void testPrecomputeQuery() throws Exception {
        TemplateQuery t = (TemplateQuery) templates.get("employeeByName");
        String expIql =
            "SELECT DISTINCT a1_, a1_.name AS a2_, a1_.age AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name, a1_.age";
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.age\"></query>";
        Map<String, QuerySelectable> pathToQueryNode = new HashMap<String, QuerySelectable>();
        PathQuery pathQuery = PathQueryBinding.unmarshalPathQuery(new StringReader(queryXml), PathQuery.USERPROFILE_VERSION);
        MainHelper.makeQuery(pathQuery, new HashMap<String, InterMineBag>(), pathToQueryNode, null, null);
        List<?> indexes = new ArrayList<Object>();
        String precomputeQuery = TemplatePrecomputeHelper.getPrecomputeQuery(t, indexes).toString();
        assertEquals(expIql, precomputeQuery);
        //assertTrue(indexes.size() == 2);
        System.out.println("pathToQueryNode: " + pathToQueryNode);
        List<Object> expIndexes = Arrays.asList(new Object[] {pathToQueryNode.get("Employee"), pathToQueryNode.get("Employee.name"), pathToQueryNode.get("Employee.age")});
        assertEquals(expIndexes.toString(), indexes.toString());
    }

    public void testGetPrecomputeQuery() throws Exception {
        TemplateQuery t = (TemplateQuery) templates.get("employeesFromCompanyAndDepartment");
        assertEquals("SELECT DISTINCT a1_, a3_, a2_, a3_.name AS a4_, a2_.name AS a5_, a1_.name AS a6_, a1_.age AS a7_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_) ORDER BY a1_.name, a1_.age, a3_.name, a2_.name", TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList<Object>()).toString());
    }

    public void testBugWhereTrue() throws Exception {
        Reader reader = new StringReader("<template name=\"flibble\" title=\"flobble\" comment=\"wibble\" >"
                + "<query name=\"flibble\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"A and B and C and D\">"
                + "<constraint path=\"Employee.age\" op=\"!=\" value=\"10\" code=\"A\" />"
                + "<constraint path=\"Employee.age\" op=\"!=\" value=\"20\" code=\"B\" editable=\"true\" />"
                + "<constraint path=\"Employee.age\" op=\"!=\" value=\"30\" code=\"C\" />"
                + "<constraint path=\"Employee.age\" op=\"!=\" value=\"40\" code=\"D\" editable=\"true\" />"
                + "</query></template>");
        TemplateQuery t =
            (TemplateQuery) TemplateQueryBinding.unmarshalTemplates(reader, PathQuery.USERPROFILE_VERSION).values().iterator().next();
        TemplateQuery tc = t.cloneWithoutEditableConstraints();
        System.out.println(t.getConstraintLogic() + " -> " + tc.getConstraintLogic());
        System.out.println(TemplateQueryBinding.marshal(t, 2));
        String expected = "<template name=\"flibble\" title=\"flobble\" comment=\"wibble\">"
            + "<query name=\"flibble\" model=\"testmodel\" view=\"Employee.name\" longDescription=\"\" constraintLogic=\"A and B and C and D\">"
            + "<constraint path=\"Employee.age\" code=\"C\" editable=\"false\" op=\"!=\" value=\"30\"/>"
            + "<constraint path=\"Employee.age\" code=\"A\" editable=\"false\" op=\"!=\" value=\"10\"/>"
            + "<constraint path=\"Employee.age\" code=\"B\" editable=\"true\" op=\"!=\" value=\"20\"/>"
            + "<constraint path=\"Employee.age\" code=\"D\" editable=\"true\" op=\"!=\" value=\"40\"/>"
            + "</query></template>";
        System.out.println(expected);
        assertEquals(expected.trim(), TemplateQueryBinding.marshal(t, 2).trim()); // Ignore whitespace issues
        Query precomputeQuery = TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList<Object>());
        assertEquals(precomputeQuery.toString(), "SELECT DISTINCT a1_, a1_.age AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age != 10 AND a1_.age != 30) ORDER BY a1_.name, a1_.age", precomputeQuery.toString());
    }

    public void testGetPrecomputeLookup() throws Exception {
        Reader reader = new StringReader("<template name=\"ManagerLookup\" title=\"Search for Managers\" longDescription=\"Use a LOOKUP constraint to search for Managers.\" comment=\"\">\n" +
        "  <query name=\"ManagerLookup\" model=\"testmodel\" view=\"Manager.name Manager.title\">\n" +
        "    <constraint path=\"Manager\" editable=\"true\" code=\"A\" op=\"LOOKUP\" value=\"Mr.\"/>\n" +
        "  </query>\n" +
        "</template>");
        TemplateQuery t =
            (TemplateQuery) TemplateQueryBinding.unmarshalTemplates(reader, PathQuery.USERPROFILE_VERSION).values().iterator().next();
        Query precomputeQuery = TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList<Object>());
        String expected = "SELECT DISTINCT a1_, a1_.id AS a2_, a1_.name AS a3_, a1_.title AS a4_ FROM org.intermine.model.testmodel.Manager AS a1_ ORDER BY a1_.name, a1_.title, a1_.id";
        assertEquals(expected, precomputeQuery.toString());
    }

    public void testGetPrecomputeQuery2() throws Exception {
        TemplateQuery t = (TemplateQuery) templates.get("InnerInsideOuter");
        assertEquals("SELECT DISTINCT a1_, a1_.departments(SELECT default, a1_, a1_.name FROM org.intermine.model.testmodel.Manager AS a1_ WHERE default.manager CONTAINS a1_) AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name", TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList<Object>()).toString());
    }
}
