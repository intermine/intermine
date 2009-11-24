package org.intermine.api.template;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.api.query.MainHelper;
import org.intermine.api.xml.TemplateQueryBinding;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;

public class TemplatePrecomputeHelperTest extends TestCase {

    private Map templates;

    public void setUp() throws Exception {
        super.setUp();
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new InputStreamReader(TemplatePrecomputeHelperTest.class.getClassLoader().getResourceAsStream("default-template-queries.xml"));
        templates = binding.unmarshal(reader, new HashMap(), PathQuery.USERPROFILE_VERSION);
    }
    
    public void testPrecomputeQuery() throws Exception {
        Iterator i = templates.keySet().iterator();
        TemplateQuery t = (TemplateQuery) templates.get("employeeByName");
        String expIql =
            "SELECT DISTINCT a1_, a1_.name AS a2_, a1_.age AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_ ORDER BY a1_.name, a1_.age";
        String queryXml = "<query name=\"\" model=\"testmodel\" view=\"Employee Employee.name Employee.age\"><node path=\"Employee\" type=\"Employee\"></node></query>";
        Map pathToQueryNode = new HashMap();
        PathQuery pathQuery = PathQueryBinding.unmarshal(new StringReader(queryXml), PathQuery.USERPROFILE_VERSION).values().iterator().next();
        MainHelper.makeQuery(pathQuery, new HashMap(), pathToQueryNode, null, null, false);
        List indexes = new ArrayList();
        String precomputeQuery = TemplatePrecomputeHelper.getPrecomputeQuery(t, indexes).toString();
        assertEquals(expIql, precomputeQuery);
        //assertTrue(indexes.size() == 2);
        System.out.println("pathToQueryNode: " + pathToQueryNode);
        List expIndexes = Arrays.asList(new Object[] {pathToQueryNode.get("Employee"), pathToQueryNode.get("Employee.name"), pathToQueryNode.get("Employee.age")});
        assertEquals(expIndexes.toString(), indexes.toString());
    }
    
    public void testGetPrecomputeQuery() throws Exception {
        TemplateQuery t = (TemplateQuery) templates.get("employeesFromCompanyAndDepartment");
        assertEquals("SELECT DISTINCT a1_, a3_, a2_, a3_.name AS a4_, a2_.name AS a5_, a1_.name AS a6_, a1_.age AS a7_ FROM org.intermine.model.testmodel.Employee AS a1_, org.intermine.model.testmodel.Department AS a2_, org.intermine.model.testmodel.Company AS a3_ WHERE (a1_.department CONTAINS a2_ AND a2_.company CONTAINS a3_) ORDER BY a1_.name, a1_.age, a3_.name, a2_.name", TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList()).toString());
    }
    
    public void testBugWhereTrue() throws Exception {
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new StringReader("<template name=\"flibble\" title=\"flobble\" longDescription=\"wurble\" comment=\"wibble\" >"
                + "<query name=\"flibble\" model=\"testmodel\" view=\"Employee.name\" constraintLogic=\"A and B and C and D\">"
                + "<node path=\"Employee\" type=\"Employee\"></node>"
                + "<node path=\"Employee.age\" type=\"Integer\">"
                + "    <constraint op=\"!=\" value=\"10\" description=\"a\" identifier=\"\" code=\"A\"></constraint>"
                + "    <constraint op=\"!=\" value=\"20\" description=\"b\" identifier=\"\" code=\"B\" editable=\"true\"></constraint>"
                + "    <constraint op=\"!=\" value=\"30\" description=\"c\" identifier=\"\" code=\"C\"></constraint>"
                + "    <constraint op=\"!=\" value=\"40\" description=\"d\" identifier=\"\" code=\"D\" editable=\"true\"></constraint>"
                + "</node></query></template>");
        TemplateQuery t =
            (TemplateQuery) binding.unmarshal(reader, new HashMap(), PathQuery.USERPROFILE_VERSION).values().iterator().next();
        TemplateQuery tc = t.cloneWithoutEditableConstraints();
        System.out.println(t.getConstraintLogic() + " -> " + tc.getConstraintLogic());
        assertEquals("SELECT DISTINCT a1_, a1_.age AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Employee AS a1_ WHERE (a1_.age != 10 AND a1_.age != 30) ORDER BY a1_.name, a1_.age", TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList()).toString());
    }

    public void testGetPrecomputeLookup() throws Exception {
        TemplateQueryBinding binding = new TemplateQueryBinding();
        Reader reader = new StringReader("<template name=\"ManagerLookup\" title=\"Search for Managers\" longDescription=\"Use a LOOKUP constraint to search for Managers.\" comment=\"\">\n" +
        "  <query name=\"ManagerLookup\" model=\"testmodel\" view=\"Manager.name Manager.title\">\n" +
        "    <node path=\"Manager\" type=\"Manager\">\n" +
        "      <constraint op=\"LOOKUP\" value=\"Mr.\" description=\"\" identifier=\"\" editable=\"true\" code=\"A\">\n" +
        "      </constraint>\n" +
        "    </node>\n" +
        "  </query>\n" +
        "</template>");
        List indexes = new ArrayList();
        TemplateQuery t =
            (TemplateQuery) binding.unmarshal(reader, new HashMap(), PathQuery.USERPROFILE_VERSION).values().iterator().next();
        Query precomputeQuery = TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList());
        assertEquals("SELECT DISTINCT a1_, a1_.name AS a2_, a1_.title AS a3_ FROM org.intermine.model.testmodel.Manager AS a1_ ORDER BY a1_.name, a1_.title",
                     precomputeQuery.toString());
    }

    public void testGetPrecomputeQuery2() throws Exception {
        TemplateQuery t = (TemplateQuery) templates.get("InnerInsideOuter");
        assertEquals("SELECT DISTINCT a1_, a1_.departments(SELECT default, a1_, a1_.name FROM org.intermine.model.testmodel.Manager AS a1_ WHERE default.manager CONTAINS a1_) AS a2_, a1_.name AS a3_ FROM org.intermine.model.testmodel.Company AS a1_ ORDER BY a1_.name", TemplatePrecomputeHelper.getPrecomputeQuery(t, new ArrayList()).toString());
    }
}
